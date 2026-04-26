#!/bin/bash
# Run from the ranker_api/ directory.
cd "$(dirname "$0")"
uvicorn ranker_api:app --host 0.0.0.0 --port 8010 --reload
