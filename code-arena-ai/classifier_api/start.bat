@echo off
REM Run from the classifier_api/ directory on Windows.
cd /d "%~dp0"
uvicorn classifier_api:app --host 0.0.0.0 --port 8011 --reload
