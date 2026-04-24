# Complexity Classifier — Model Setup

This document explains how the Big-O complexity classifier model is distributed to any
machine that needs to run [classifier.py](classifier.py) or the FastAPI wrapper at
[classifier_api/classifier_api.py](../classifier_api/classifier_api.py).

## Why the model is not in git

The fine-tuned checkpoint is ~498 MB per copy (`best/` and `final/`). GitHub rejects
single files over 100 MB, and pushing 1 GB+ of binary weights on every retraining
cycle is slow and bandwidth-wasteful. The `code-arena-ai/classifier/model/` directory
is therefore listed in the repository `.gitignore`.

Instead, the trained weights live on the **Hugging Face Hub**, and
[classifier.py](classifier.py) transparently fetches them the first time the service
starts on a machine that does not already have a local copy.

## How the loader resolves the model

From [classifier.py](classifier.py):

```python
LOCAL_MODEL_PATH = ROOT / "model" / "best"
DEFAULT_HUB_MODEL_ID = "REPLACE_WITH_HF_USERNAME/codearena-complexity-classifier"
HUB_MODEL_ID = os.environ.get("COMPLEXITY_MODEL_ID", DEFAULT_HUB_MODEL_ID)

def _resolve_model_source() -> str:
    if LOCAL_MODEL_PATH.is_dir():
        return str(LOCAL_MODEL_PATH)
    return HUB_MODEL_ID
```

Resolution order:

1. If `code-arena-ai/classifier/model/best/` exists locally, load from it (offline,
   fast, good for development).
2. Otherwise, load from the Hugging Face Hub using the repo ID in `HUB_MODEL_ID`.
   Hugging Face's `from_pretrained()` downloads the weights on first call and caches
   them in `~/.cache/huggingface/` (or `%USERPROFILE%\.cache\huggingface\` on
   Windows). Subsequent boots are instant.

---

## Part 1 — Maintainer: upload the model to Hugging Face Hub

**You only do this once**, or again after retraining. This is a desktop task,
performed on the machine that has the trained `model/best/` directory on disk.

### 1.1 — Create a Hugging Face account

1. Go to https://huggingface.co/join and sign up (free).
2. Pick a username — you will use this as `<HF_USERNAME>` below.

### 1.2 — Create an access token

1. Go to https://huggingface.co/settings/tokens.
2. Click **New token**, give it a name like `codearena-upload`, select the
   **Write** role, and copy the token somewhere safe.

### 1.3 — Install the Hugging Face CLI

```bash
pip install -U "huggingface_hub[cli]"
```

### 1.4 — Log in

```bash
huggingface-cli login
```

Paste the write token when prompted. This writes the credential to
`~/.cache/huggingface/token`.

### 1.5 — Create the model repo

```bash
huggingface-cli repo create codearena-complexity-classifier --type model
```

Make it private if you prefer (`--private`). Private repos require that every
consumer also logs in with a read token — see Part 2.

The resulting repo ID is `<HF_USERNAME>/codearena-complexity-classifier`.

### 1.6 — Upload the trained checkpoint

From the project root:

```bash
huggingface-cli upload <HF_USERNAME>/codearena-complexity-classifier \
    code-arena-ai/classifier/model/best \
    . \
    --commit-message "Initial upload: Big-O complexity classifier (best checkpoint)"
```

This uploads the contents of `model/best/` (the `config.json`, `model.safetensors`,
`tokenizer.json`, `tokenizer_config.json`) to the root of the Hub repo. Resumable,
shows a progress bar, typically ~5 minutes on a reasonable home connection.

### 1.7 — Wire the repo ID into the code

Edit [classifier.py](classifier.py) and replace the placeholder:

```python
DEFAULT_HUB_MODEL_ID = "REPLACE_WITH_HF_USERNAME/codearena-complexity-classifier"
```

with your actual repo ID, e.g.:

```python
DEFAULT_HUB_MODEL_ID = "adamzahi/codearena-complexity-classifier"
```

Commit this change and push.

### 1.8 — Retraining workflow

Each time you retrain and want to roll out new weights:

```bash
huggingface-cli upload <HF_USERNAME>/codearena-complexity-classifier \
    code-arena-ai/classifier/model/best \
    . \
    --commit-message "Retrain $(date +%Y-%m-%d): <what changed>"
```

Remote machines pick up the new weights by clearing their HF cache (or by setting a
specific revision via the `revision=...` kwarg in `from_pretrained`).

---

## Part 2 — Remote machine: get the classifier running

These are the steps a collaborator follows on a fresh machine (Linux, macOS, or
Windows) to boot the classifier service against the branch.

### 2.1 — Prerequisites

- **Python 3.10** (the classifier was trained and exported with 3.10; 3.11+ also
  works, 3.9 does not).
- **git**.
- Internet access to `huggingface.co` (firewall whitelist if applicable).
- ~2 GB free disk space (model cache + Python deps).
- No GPU required — the service runs on CPU. If a CUDA GPU is present,
  [classifier.py](classifier.py) will auto-detect and use it.

### 2.2 — Clone the branch

```bash
git clone https://github.com/<ORG>/code-arena.git
cd code-arena
git checkout Battle_Room
```

### 2.3 — Create a virtual environment

```bash
cd code-arena-ai/classifier_api
python -m venv .venv
# Linux / macOS:
source .venv/bin/activate
# Windows (PowerShell):
.venv\Scripts\Activate.ps1
# Windows (cmd / bash on Windows):
.venv\Scripts\activate.bat
```

### 2.4 — Install Python dependencies

```bash
pip install -U pip
pip install -r requirements.txt
```

`requirements.txt` pulls in FastAPI + uvicorn. The classifier also needs `torch`,
`transformers`, and `huggingface_hub` — install these if they are not already
pinned in `requirements.txt`:

```bash
pip install torch transformers huggingface_hub
```

### 2.5 — (Only if the Hub repo is private) Authenticate

If the maintainer made the model repo private:

1. Ask the maintainer for a **read** token (or generate your own at
   https://huggingface.co/settings/tokens with the **Read** role if you have been
   added as a collaborator on the repo).
2. Run:
   ```bash
   huggingface-cli login
   ```
   and paste the read token. Alternatively, set the token as an environment
   variable (useful in CI / containers):
   ```bash
   # Linux / macOS:
   export HF_TOKEN=hf_xxxxxxxxxxxxxxxxxxxx
   # Windows PowerShell:
   $env:HF_TOKEN = "hf_xxxxxxxxxxxxxxxxxxxx"
   ```

If the repo is public, skip this section entirely.

### 2.6 — (Optional) Override the Hub repo ID

By default, [classifier.py](classifier.py) loads from the `DEFAULT_HUB_MODEL_ID`
baked into the source. If you are testing a staging or forked model, override it
without editing code:

```bash
# Linux / macOS:
export COMPLEXITY_MODEL_ID=someone-else/codearena-complexity-classifier-v2
# Windows PowerShell:
$env:COMPLEXITY_MODEL_ID = "someone-else/codearena-complexity-classifier-v2"
```

### 2.7 — Start the FastAPI wrapper

From `code-arena-ai/classifier_api/`:

```bash
# Linux / macOS:
./start.sh
# Windows:
start.bat
```

On **first boot**, `transformers` downloads ~500 MB of weights from the Hub and
caches them under `~/.cache/huggingface/hub/`. Subsequent boots load from the
cache and start in a couple of seconds.

Expected log line:

```
[...] INFO classifier_api: Complexity classifier loaded from .../classifier
```

If the load fails, the service still starts but `/health` reports
`model_ready: false` and `/classify` returns HTTP 503. Check the logs for the
underlying exception — the most common causes are missing `HF_TOKEN` for a
private repo, a wrong `COMPLEXITY_MODEL_ID`, or no network access to
`huggingface.co`.

### 2.8 — Smoke-test the service

```bash
curl http://localhost:8000/health
# → {"status":"ok","model_ready":true}

curl -X POST http://localhost:8000/classify \
     -H 'Content-Type: application/json' \
     -d '{"source_code":"for i in range(n):\n    for j in range(n):\n        print(i, j)"}'
# → {"label":"On2","display":"O(n²)","score":35.0,"confidence":0.9...,"all_scores":{...}}
```

### 2.9 — Point the Spring Boot backend at it

The backend reads the classifier URL from application config (see
`code-arena-backend/src/main/resources/application.yml` — the
`complexity-classifier.base-url` key). Default is `http://localhost:8000`. If the
classifier runs on another host, set that URL and restart the backend.

---

## Troubleshooting

| Symptom | Likely cause | Fix |
|---|---|---|
| `FileNotFoundError: Model not found` | Old version of `classifier.py` cached | Pull latest `Battle_Room` and delete `__pycache__/` |
| `401 Unauthorized` from Hugging Face | Private repo, no token | `huggingface-cli login` (read token) |
| `403 Forbidden` from Hugging Face | Token lacks access to repo | Ask maintainer to add you as a collaborator |
| Download hangs / times out | Proxy / firewall | Set `HF_ENDPOINT` or `HTTPS_PROXY` |
| OOM on small VMs | CPU inference peaks ~1.2 GB RAM | Use a VM with at least 2 GB RAM |
| Wrong predictions after retrain | HF cache stale | `rm -rf ~/.cache/huggingface/hub/models--<HF_USERNAME>--codearena-complexity-classifier` |

---

## Summary diagram

```
┌─────────────────────────────┐       push once per retrain       ┌────────────────────────────┐
│  Trainer machine            │ ────────────────────────────────▶ │  Hugging Face Hub          │
│  (has model/best/ on disk)  │     huggingface-cli upload ...    │  <HF_USERNAME>/codearena-…  │
└─────────────────────────────┘                                   └────────────────────────────┘
                                                                               │
                                                        first-run download     │
                                                            (cached locally)   ▼
                              ┌──────────────────────────────────────────────────┐
                              │  Remote machine running classifier_api (FastAPI) │
                              │  — from_pretrained() auto-fetches + caches       │
                              │  — Spring Boot backend calls /classify           │
                              └──────────────────────────────────────────────────┘
```
