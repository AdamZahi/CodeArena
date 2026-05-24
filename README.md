# Code Arena

Code Arena is a competitive programming platform that combines a web-based problem solving experience with AI-assisted ranking, hinting, and difficulty classification services. This repository is a monorepo that contains the backend, frontend, and multiple AI services that support training and inference workflows.

## Projects
- `code-arena-backend` (Spring Boot 3 / Java 17)
- `code-arena-frontend` (Angular standalone)
- `code-arena-ai-service` (Python API for AI features)
- `code-arena-ai` (training and data pipelines)
- `ai-service` (additional AI utilities and models)
- `terminal-quest-ai-model` (experiments and datasets)
- Auth0 integration (see AUTH0_SETUP.md)

## Key Capabilities
- Competitive programming challenges and submissions flow
- AI-powered ranking, hints, and difficulty classification
- Dataset generation and model training utilities
- Docker-based local development scaffolding

## Repository Layout
- `code-arena-backend/` backend API and business logic
- `code-arena-frontend/` web client
- `code-arena-ai-service/` AI inference service
- `code-arena-ai/` training pipelines, datasets, and model assets
- `code-arena-deploy/` Kubernetes manifests and deployment assets

## Run (scaffold stage)
- Use `docker-compose up --build` after filling environment values.

## Environment Setup
- Copy `.env.example` to `.env` in each service that requires environment variables
- Configure Auth0 settings as described in AUTH0_SETUP.md

## Local Development Tips
- Keep services in separate terminals for clearer logs
- Start the backend before the frontend to avoid API connection errors

## Run (scaffold stage)
- Use `docker-compose up --build` after filling environment values.
