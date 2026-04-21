@echo off
REM Run from the ranker_api/ directory on Windows.
cd /d "%~dp0"
uvicorn ranker_api:app --host 0.0.0.0 --port 8001 --reload
