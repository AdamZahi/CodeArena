import torch
import torch.nn as nn
from torch.nn import functional as F
from torch.utils.data import Dataset, DataLoader
import pandas as pd
import pickle
import math
import time
import random
from sklearn.feature_extraction.text import TfidfVectorizer

try:
    from google.colab import files
    IN_COLAB = True
except ImportError:
    IN_COLAB = False

if IN_COLAB:
    print("Upload your full_cleaned.csv...")
    uploaded = files.upload()
    csv_name = list(uploaded.keys())[0]
else:
    csv_name = 'data/full_cleaned.csv'

# ── Hyperparameters ───────────────────────────────────────────────────────────
block_size = 128
n_embd = 256
n_head = 8
n_layer = 6
dropout = 0.2
lr = 5e-4
epochs = 1000            # Fix 3: increased from 500 → 1000
batch_size = 128
temperature = 0.4
patience = 50            # Fix 3: early stopping patience

device = 'cuda' if torch.cuda.is_available() else 'cpu'

# ── Fix 4: Algorithmic keyword filter ─────────────────────────────────────────
ALGO_KEYWORDS = [
    "hash", "map", "pointer", "window", "stack", "queue", "binary", "sort",
    "tree", "graph", "dp", "dynamic", "greedy", "recursi", "dfs", "bfs",
    "heap", "trie", "prefix", "index", "array", "linked", "search",
    "traversal", "memoiz"
]

def hint_has_algo_keyword(hint_text):
    """Returns True if the hint contains at least one algorithmic keyword."""
    hint_lower = hint_text.lower()
    return any(kw in hint_lower for kw in ALGO_KEYWORDS)

# ── Tokenizer ─────────────────────────────────────────────────────────────────
class CharTokenizer:
    def __init__(self):
        self.char_to_idx = {}
        self.idx_to_char = {}
        self.vocab_size = 0

    def fit(self, text):
        chars = sorted(set(text))
        self.char_to_idx = {ch: i for i, ch in enumerate(chars)}
        self.idx_to_char = {i: ch for ch, i in self.char_to_idx.items()}
        self.vocab_size = len(chars)

    def encode(self, text):
        return [self.char_to_idx.get(ch, 0) for ch in text]

    def decode(self, indices):
        return ''.join(self.idx_to_char.get(i, '') for i in indices)

# ── Model Architecture ────────────────────────────────────────────────────────
class Head(nn.Module):
    def __init__(self, head_size):
        super().__init__()
        self.key = nn.Linear(n_embd, head_size, bias=False)
        self.query = nn.Linear(n_embd, head_size, bias=False)
        self.value = nn.Linear(n_embd, head_size, bias=False)
        self.register_buffer('tril', torch.tril(torch.ones(block_size, block_size)))
        self.dropout = nn.Dropout(dropout)

    def forward(self, x):
        B, T, C = x.shape
        k = self.key(x)
        q = self.query(x)
        wei = q @ k.transpose(-2, -1) * (C ** -0.5)
        wei = wei.masked_fill(self.tril[:T, :T] == 0, float('-inf'))
        wei = F.softmax(wei, dim=-1)
        wei = self.dropout(wei)
        v = self.value(x)
        return wei @ v

class MultiHeadAttention(nn.Module):
    def __init__(self, num_heads, head_size):
        super().__init__()
        self.heads = nn.ModuleList([Head(head_size) for _ in range(num_heads)])
        self.proj = nn.Linear(n_embd, n_embd)
        self.dropout = nn.Dropout(dropout)

    def forward(self, x):
        out = torch.cat([h(x) for h in self.heads], dim=-1)
        return self.dropout(self.proj(out))

class FeedForward(nn.Module):
    def __init__(self):
        super().__init__()
        self.net = nn.Sequential(
            nn.Linear(n_embd, 4 * n_embd),
            nn.GELU(),
            nn.Linear(4 * n_embd, n_embd),
            nn.Dropout(dropout),
        )

    def forward(self, x):
        return self.net(x)

class Block(nn.Module):
    def __init__(self):
        super().__init__()
        head_size = n_embd // n_head
        self.sa = MultiHeadAttention(n_head, head_size)
        self.ffwd = FeedForward()
        self.ln1 = nn.LayerNorm(n_embd)
        self.ln2 = nn.LayerNorm(n_embd)

    def forward(self, x):
        x = x + self.sa(self.ln1(x))
        x = x + self.ffwd(self.ln2(x))
        return x

class NanoGPT(nn.Module):
    def __init__(self, vocab_size):
        super().__init__()
        self.block_size = block_size
        self.token_embedding = nn.Embedding(vocab_size, n_embd)
        self.position_embedding = nn.Embedding(block_size, n_embd)
        self.blocks = nn.Sequential(*[Block() for _ in range(n_layer)])
        self.ln_f = nn.LayerNorm(n_embd)
        self.lm_head = nn.Linear(n_embd, vocab_size)

    def forward(self, idx, targets=None):
        B, T = idx.shape
        tok_emb = self.token_embedding(idx)
        pos_emb = self.position_embedding(torch.arange(T, device=idx.device))
        x = tok_emb + pos_emb
        x = self.blocks(x)
        x = self.ln_f(x)
        logits = self.lm_head(x)
        if targets is None:
            return logits, None
        B, T, C = logits.shape
        logits = logits.view(B * T, C)
        targets = targets.view(B * T)
        loss = F.cross_entropy(logits, targets)
        return logits, loss

    def generate(self, idx, max_new_tokens, temperature=0.4):
        for _ in range(max_new_tokens):
            idx_cond = idx[:, -self.block_size:]
            logits, _ = self(idx_cond)
            logits = logits[:, -1, :] / temperature
            probs = F.softmax(logits, dim=-1)
            idx_next = torch.multinomial(probs, num_samples=1)
            idx = torch.cat((idx, idx_next), dim=1)
        return idx

class HintDataset(Dataset):
    def __init__(self, hints, tokenizer):
        combined = '\n'.join(hints)
        encoded = tokenizer.encode(combined)
        self.samples = []
        stride = block_size // 4
        for i in range(0, len(encoded) - block_size, stride):
            self.samples.append(encoded[i:i + block_size + 1])

    def __len__(self):
        return len(self.samples)

    def __getitem__(self, idx):
        chunk = self.samples[idx]
        x = torch.tensor(chunk[:-1], dtype=torch.long)
        y = torch.tensor(chunk[1:], dtype=torch.long)
        return x, y

# ══════════════════════════════════════════════════════════════════════════════
# DATA LOADING & CLEANING
# ══════════════════════════════════════════════════════════════════════════════

df = pd.read_csv(csv_name)
df = df.dropna(subset=['description', 'hint'])
df['hint_len'] = df['hint'].astype(str).str.len()
df = df[(df['hint_len'] >= 10) & (df['hint_len'] <= 300)]

# ── Fix 4: Filter hints that lack algorithmic substance ───────────────────────
before_filter = len(df)
df = df[df['hint'].astype(str).apply(hint_has_algo_keyword)]
after_filter = len(df)
print(f"🧹 Hint quality filter: {before_filter} → {after_filter} hints "
      f"({before_filter - after_filter} vague hints removed)")

descriptions = df['description'].astype(str).tolist()
hints = df['hint'].astype(str).tolist()

# ── Build TF-IDF retriever ────────────────────────────────────────────────────
vectorizer = TfidfVectorizer(stop_words='english', max_features=5000)
tfidf_matrix = vectorizer.fit_transform(descriptions)

with open('retriever.pkl', 'wb') as f:
    pickle.dump({
        'vectorizer': vectorizer,
        'tfidf_matrix': tfidf_matrix,
        'hints': hints,
        'descriptions': descriptions
    }, f)

# ── Build character tokenizer (hint-only vocabulary) ──────────────────────────
tokenizer = CharTokenizer()
tokenizer.fit('\n'.join(hints))

with open('tokenizer.pkl', 'wb') as f:
    pickle.dump(tokenizer, f)

print(f"📊 Dataset: {len(hints)} hints | Vocab: {tokenizer.vocab_size} chars")

# ══════════════════════════════════════════════════════════════════════════════
# Fix 3: TRAIN/VALIDATION SPLIT (80/20)
# ══════════════════════════════════════════════════════════════════════════════

random.seed(42)
indices = list(range(len(hints)))
random.shuffle(indices)
split = int(0.8 * len(hints))

train_hints = [hints[i] for i in indices[:split]]
val_hints = [hints[i] for i in indices[split:]]

train_dataset = HintDataset(train_hints, tokenizer)
val_dataset = HintDataset(val_hints, tokenizer)

train_loader = DataLoader(train_dataset, batch_size=batch_size, shuffle=True, drop_last=True)
val_loader = DataLoader(val_dataset, batch_size=batch_size, shuffle=False, drop_last=True)

print(f"📂 Train: {len(train_dataset)} samples | Val: {len(val_dataset)} samples")

# ══════════════════════════════════════════════════════════════════════════════
# TRAINING LOOP WITH VALIDATION & EARLY STOPPING
# ══════════════════════════════════════════════════════════════════════════════

model = NanoGPT(tokenizer.vocab_size).to(device)
optimizer = torch.optim.AdamW(model.parameters(), lr=lr)
scheduler = torch.optim.lr_scheduler.CosineAnnealingLR(optimizer, T_max=epochs)

best_val_loss = float('inf')
best_state = None
epochs_without_improvement = 0

print(f"\n🚀 Starting training for up to {epochs} epochs on {device} "
      f"(early stopping patience={patience})...\n")

for epoch in range(epochs):
    t0 = time.time()

    # ── Training pass ─────────────────────────────────────────────────────────
    model.train()
    train_loss = 0
    train_batches = 0
    for batch_x, batch_y in train_loader:
        batch_x, batch_y = batch_x.to(device), batch_y.to(device)
        optimizer.zero_grad()
        _, loss = model(batch_x, batch_y)
        loss.backward()
        torch.nn.utils.clip_grad_norm_(model.parameters(), 1.0)
        optimizer.step()
        train_loss += loss.item()
        train_batches += 1
    scheduler.step()
    avg_train = train_loss / max(train_batches, 1)

    # ── Validation pass ───────────────────────────────────────────────────────
    model.eval()
    val_loss = 0
    val_batches = 0
    with torch.no_grad():
        for batch_x, batch_y in val_loader:
            batch_x, batch_y = batch_x.to(device), batch_y.to(device)
            _, loss = model(batch_x, batch_y)
            val_loss += loss.item()
            val_batches += 1
    avg_val = val_loss / max(val_batches, 1)

    # ── Logging ───────────────────────────────────────────────────────────────
    if (epoch + 1) % 10 == 0 or epoch == 0:
        elapsed = time.time() - t0
        print(f"  Epoch {epoch+1:4d}/{epochs} | Train: {avg_train:.4f} | "
              f"Val: {avg_val:.4f} | Time: {elapsed:.2f}s")

    # ── Best model checkpoint (based on VALIDATION loss) ──────────────────────
    if avg_val < best_val_loss:
        best_val_loss = avg_val
        epochs_without_improvement = 0
        model_cpu = model.to('cpu')
        best_state = model_cpu.state_dict()
        model.to(device)
    else:
        epochs_without_improvement += 1

    # ── Early stopping ────────────────────────────────────────────────────────
    if epochs_without_improvement >= patience:
        print(f"\n⏹️  Early stopping at epoch {epoch+1} "
              f"(no val improvement for {patience} epochs)")
        break

print(f"\n✅ Training complete! Best Val Loss: {best_val_loss:.4f}")

# ══════════════════════════════════════════════════════════════════════════════
# SAVE CHECKPOINT
# ══════════════════════════════════════════════════════════════════════════════

checkpoint = {
    'model_state': best_state,
    'vocab_size': tokenizer.vocab_size,
    'block_size': block_size,
    'n_embd': n_embd,
    'n_head': n_head,
    'n_layer': n_layer,
}
torch.save(checkpoint, 'nano_gpt.pth')

# ══════════════════════════════════════════════════════════════════════════════
# Fix 3: POST-TRAINING QUALITY INSPECTION — generate 5 sample hints
# ══════════════════════════════════════════════════════════════════════════════

model.load_state_dict(best_state)
model.to(device)
model.eval()

print("\n🔍 Sample generated hints (visual quality check):")
print("=" * 60)

sample_seeds = [
    "Use a hash map to ",
    "Sort the array and ",
    "Use two pointers from ",
    "Apply dynamic programming ",
    "Traverse the tree using ",
]

for i, seed in enumerate(sample_seeds, 1):
    encoded = tokenizer.encode(seed)
    idx_t = torch.tensor([encoded], dtype=torch.long).to(device)
    with torch.no_grad():
        out = model.generate(idx_t, max_new_tokens=80, temperature=temperature)
    generated = tokenizer.decode(out[0].tolist()).split('\n')[0]
    print(f"  [{i}] {generated}")

print("=" * 60)

# ══════════════════════════════════════════════════════════════════════════════
# RETRIEVER VALIDATION (same as before)
# ══════════════════════════════════════════════════════════════════════════════

from sklearn.metrics.pairwise import cosine_similarity
test_queries = [
    "Given an array of integers nums and an integer target, return indices of the two numbers such that they add up to target.",
    "Given a string containing just the characters '(', ')', '{', '}', '[' and ']', determine if the input string is valid.",
    "Reverse a singly linked list.",
    "Merge two sorted linked lists.",
    "Find the maximum subarray sum."
]

print("\n📡 Retriever validation:")
for query in test_queries:
    query_vec = vectorizer.transform([query])
    sims = cosine_similarity(query_vec, tfidf_matrix).flatten()
    best_idx = sims.argmax()
    sim_score = sims[best_idx]
    retrieved_hint = hints[best_idx]
    print(f"  sim={sim_score:.2f} | {retrieved_hint[:80]}...")

# ══════════════════════════════════════════════════════════════════════════════
# DOWNLOAD ARTIFACTS
# ══════════════════════════════════════════════════════════════════════════════

if IN_COLAB:
    files.download('nano_gpt.pth')
    files.download('tokenizer.pkl')
    files.download('retriever.pkl')
