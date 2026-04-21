from flask import Flask, request, jsonify
from flask_cors import CORS
import torch
import torch.nn as nn
from torch.nn import functional as F
import pickle
import os
from sklearn.metrics.pairwise import cosine_similarity

app = Flask(__name__)
CORS(app)

import pandas as pd

MODEL_PATH = 'models/nano_gpt.pth'
TOKENIZER_PATH = 'models/tokenizer.pkl'
RETRIEVER_PATH = 'models/retriever.pkl'
CSV_PATH = 'data/full_cleaned.csv'

model = None
tokenizer = None
vectorizer = None
tfidf_matrix = None
hints = None
descriptions = None
title_lookup = {}

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

import sys
sys.modules['__main__'].CharTokenizer = CharTokenizer

class Head(nn.Module):
    def __init__(self, n_embd, head_size, block_size, dropout):
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
    def __init__(self, n_embd, num_heads, head_size, block_size, dropout):
        super().__init__()
        self.heads = nn.ModuleList([Head(n_embd, head_size, block_size, dropout) for _ in range(num_heads)])
        self.proj = nn.Linear(n_embd, n_embd)
        self.dropout = nn.Dropout(dropout)

    def forward(self, x):
        out = torch.cat([h(x) for h in self.heads], dim=-1)
        return self.dropout(self.proj(out))

class FeedForward(nn.Module):
    def __init__(self, n_embd, dropout):
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
    def __init__(self, n_embd, n_head, block_size, dropout):
        super().__init__()
        head_size = n_embd // n_head
        self.sa = MultiHeadAttention(n_embd, n_head, head_size, block_size, dropout)
        self.ffwd = FeedForward(n_embd, dropout)
        self.ln1 = nn.LayerNorm(n_embd)
        self.ln2 = nn.LayerNorm(n_embd)

    def forward(self, x):
        x = x + self.sa(self.ln1(x))
        x = x + self.ffwd(self.ln2(x))
        return x

class NanoGPT(nn.Module):
    def __init__(self, vocab_size, block_size, n_embd, n_head, n_layer, dropout=0.2):
        super().__init__()
        self.block_size = block_size
        self.token_embedding = nn.Embedding(vocab_size, n_embd)
        self.position_embedding = nn.Embedding(block_size, n_embd)
        self.blocks = nn.Sequential(*[Block(n_embd, n_head, block_size, dropout) for _ in range(n_layer)])
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

def load_assets():
    global model, tokenizer, vectorizer, tfidf_matrix, hints, descriptions, title_lookup

    if not os.path.exists(MODEL_PATH) or \
       not os.path.exists(TOKENIZER_PATH) or \
       not os.path.exists(RETRIEVER_PATH):
        print("⚠️  Models not found.")
        return

    # Preload the entire database into memory for lightning-fast exact matches
    try:
        df = pd.read_csv(CSV_PATH)
        for _, row in df.dropna(subset=['title', 'hint']).iterrows():
            title_clean = str(row['title']).strip().lower()
            title_lookup[title_clean] = str(row['hint']).strip()
    except Exception as e:
        print(f"⚠️ Could not load CSV for exact matching: {e}")

    with open(TOKENIZER_PATH, 'rb') as f:
        tokenizer = pickle.load(f)

    with open(RETRIEVER_PATH, 'rb') as f:
        ret_data = pickle.load(f)
        vectorizer = ret_data['vectorizer']
        tfidf_matrix = ret_data['tfidf_matrix']
        hints = ret_data['hints']
        descriptions = ret_data['descriptions']

    checkpoint = torch.load(MODEL_PATH, map_location='cpu', weights_only=False)
    vocab_size = checkpoint['vocab_size']
    block_size = checkpoint.get('block_size', 128)
    n_embd = checkpoint.get('n_embd', 256)
    n_head = checkpoint.get('n_head', 8)
    n_layer = checkpoint.get('n_layer', 6)

    model = NanoGPT(vocab_size, block_size, n_embd, n_head, n_layer)
    model.load_state_dict(checkpoint['model_state'])
    model.eval()

    print(f"✅ Loaded {len(title_lookup)} exact titles, {len(hints)} retriever hints, vocab={tokenizer.vocab_size}")

load_assets()

# ── Fix 1: Lowered TF-IDF threshold from 0.80 → 0.65
# ── Fix 2: GPT seed now uses tags+difficulty context instead of unrelated hint prefix
@app.route('/predict-hint', methods=['POST'])
def predict_hint():
    data = request.json
    if not data:
        return jsonify({'error': 'Missing JSON body'}), 400

    title = data.get('title', '')
    title_clean = title.strip().lower()

    # ── Layer 1: Exact title match
    if title_clean in title_lookup:
        final_hint = title_lookup[title_clean]
        print(f"  📥 Title: {title} (Exact Match)")
    else:
        description = data.get('description', '')
        if not description:
            description = title

        query_vec = vectorizer.transform([description])
        sims = cosine_similarity(query_vec, tfidf_matrix).flatten()
        best_idx = sims.argmax()
        sim_score = sims[best_idx]
        retrieved_hint = hints[best_idx]

        # ── Layer 2: TF-IDF retrieval (threshold lowered to 0.65)
        if sim_score > 0.65:
            final_hint = retrieved_hint
            print(f"  📥 Title: {title} (TF-IDF sim={sim_score:.2f})")
        else:
            # ── Layer 3: NanoGPT generative fallback
            # Seed with the problem's own tags + difficulty for grounded generation
            tags = data.get('tags', '')
            difficulty = data.get('difficulty', '')
            seed = f"{tags} {difficulty}: Use "
            encoded = tokenizer.encode(seed)
            if len(encoded) > model.block_size - 80:
                encoded = encoded[-(model.block_size - 80):]

            idx_t = torch.tensor([encoded], dtype=torch.long)
            with torch.no_grad():
                out = model.generate(idx_t, max_new_tokens=80, temperature=0.4)

            generated = tokenizer.decode(out[0].tolist())
            # Take only the first sentence/line of generated text
            final_hint = generated.split('\n')[0]

            # Quality gate: if generation is too short, fall back to retriever
            if len(final_hint) < 10:
                final_hint = retrieved_hint

            print(f"  📥 Title: {title} (NanoGPT sim={sim_score:.2f})")

    print(f"  📤 Hint:  {final_hint[:80]}...")
    return jsonify({
        "hint": final_hint,
        "title": data.get('title', ''),
        "tags": data.get('tags', ''),
        "difficulty": data.get('difficulty', '')
    })

@app.route('/health', methods=['GET'])
def health():
    return jsonify({
        "status": "ok",
        "model": "NanoGPT + TF-IDF Retriever"
    })

# ── Fix 5: Model status endpoint for debugging without checking logs
@app.route('/retrain-status', methods=['GET'])
def retrain_status():
    csv_count = 0
    try:
        df = pd.read_csv(CSV_PATH)
        csv_count = len(df)
    except Exception:
        pass

    return jsonify({
        "csv_problems": csv_count,
        "exact_titles": len(title_lookup),
        "retriever_hints": len(hints) if hints else 0,
        "vocab_size": tokenizer.vocab_size if tokenizer else 0,
        "model_loaded": model is not None,
        "block_size": model.block_size if model else 0,
    })

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000)