#!/bin/bash
# Run from the classifier_api/ directory.
cd "$(dirname "$0")"
uvicorn classifier_api:app --host 0.0.0.0 --port 8002 --reload
