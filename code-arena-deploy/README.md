# CodeArena — Kubernetes Deployment Guide

End-to-end recipe for deploying CodeArena (Angular 18 frontend + Spring Boot 3.3.5 backend + MySQL 8.0) to a 2-master / 3-worker Kubernetes 1.32.13 cluster on OpenStack with no StorageClass.

## Cluster facts assumed by this guide

| Role     | Hostname | Floating IP    | Private IP   |
|----------|----------|----------------|--------------|
| master   | master1  | 192.168.0.209  | 10.0.1.112   |
| master   | master2  | 192.168.0.211  | 10.0.1.228   |
| worker   | worker1  | 192.168.0.250  | 10.0.1.123   |
| worker   | worker2  | 192.168.0.205  | 10.0.1.124   |
| worker   | worker3  | 192.168.0.233  | 10.0.1.220   |

Notes:
- **MySQL is pinned to `worker2`** via `nodeSelector` (data lives on `/data/mysql-codearena` hostPath).
- **`worker3` is excluded** from frontend/backend scheduling — it's been flaky.
- **Browser entry point**: `worker1` floating IP `192.168.0.250` (Ingress NodePort).
- **nip.io host**: `codearena.10.0.1.123.nip.io` (worker1's *private* IP — used only for Host: matching, not for browser DNS).

The deterministic public URL is:

```
https://codearena.10.0.1.123.nip.io:32090
```

`32090` is the Ingress controller's HTTPS NodePort — we pin it explicitly below.

---

## What this repo contains

```
code-arena-frontend/Dockerfile         # 2-stage: node:20-alpine -> nginx:1.27-alpine-slim
code-arena-frontend/nginx.conf         # SPA + gzip + 1y asset cache + /healthz
code-arena-frontend/.dockerignore
code-arena-frontend/src/environments/environment.prod.ts   # prod replacement (apiBaseUrl etc.)
code-arena-frontend/angular.json       # fileReplacements wired for production

code-arena-backend/Dockerfile          # 2-stage: temurin-17-jdk -> temurin-17-jre, non-root
code-arena-backend/.dockerignore

code-arena-deploy/k8s/00-namespace.yaml
code-arena-deploy/k8s/01-secrets-configmaps.yaml
code-arena-deploy/k8s/02-mysql.yaml
code-arena-deploy/k8s/03-backend.yaml
code-arena-deploy/k8s/04-frontend.yaml
code-arena-deploy/k8s/05-ingress.yaml
```

---

## 0. One-time prep on the cluster

### 0.1 — Make sure /data/mysql-codearena exists on worker2

```bash
ssh ubuntu@192.168.0.205 'sudo mkdir -p /data/mysql-codearena && sudo chmod 777 /data/mysql-codearena'
```

### 0.2 — Confirm node hostnames match the `nodeSelector`

```bash
kubectl get nodes -o wide
```

If your worker2 hostname differs from literal `worker2`, update the `nodeSelector` in [k8s/02-mysql.yaml](k8s/02-mysql.yaml).

---

## 1. Deploy NGINX Ingress Controller (and pin its HTTPS NodePort)

```bash
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/controller-v1.10.1/deploy/static/provider/baremetal/deploy.yaml

# Wait for controller pod to be Running
kubectl -n ingress-nginx wait --for=condition=Available deploy/ingress-nginx-controller --timeout=300s

# Pin HTTPS NodePort to 32090 so the URL is stable
kubectl -n ingress-nginx patch svc ingress-nginx-controller --type='json' -p='[
  {"op":"replace","path":"/spec/ports/1/nodePort","value":32090}
]'

# Confirm
kubectl -n ingress-nginx get svc ingress-nginx-controller
```

**If the controller pod is stuck in `ImagePullBackOff`** — usually a transient TLS timeout to registry.k8s.io. Just delete the pod and let it retry:

```bash
kubectl -n ingress-nginx delete pod -l app.kubernetes.io/component=controller
```

---

## 2. Generate self-signed TLS cert (Auth0 requires HTTPS)

```bash
mkdir -p tls && cd tls

openssl req -x509 -nodes -days 825 \
  -newkey rsa:2048 \
  -keyout codearena.key \
  -out codearena.crt \
  -subj "/CN=codearena.10.0.1.123.nip.io/O=codearena" \
  -addext "subjectAltName=DNS:codearena.10.0.1.123.nip.io"

kubectl -n codearena create namespace codearena --dry-run=client -o yaml | kubectl apply -f -

kubectl -n codearena create secret tls codearena-tls \
  --cert=codearena.crt --key=codearena.key \
  --dry-run=client -o yaml | kubectl apply -f -

cd ..
```

(The namespace itself is also created by `00-namespace.yaml` — harmless to create twice.)

---

## 3. Build & push Docker images

Login as `adamzahi`:

```bash
sudo docker login -u adamzahi
```

### 3.1 — Frontend

```bash
cd code-arena-frontend
sudo DOCKER_BUILDKIT=1 docker build -t adamzahi/codearena-frontend:2.0 .
sudo docker push adamzahi/codearena-frontend:2.0
cd ..
```

> The legacy Docker builder fails with `esbuild ETXTBSY` on the Angular build step. **BuildKit is required.**

### 3.2 — Backend

```bash
cd code-arena-backend
sudo DOCKER_BUILDKIT=1 docker build --network=host -t adamzahi/codearena-backend:2.0 .
sudo docker push adamzahi/codearena-backend:2.0
cd ..
```

> `--network=host` lets Maven reach Maven Central directly through the host's NAT, which is more reliable on slow OpenStack uplinks. The Dockerfile uses retry-friendly Maven flags and skips `dependency:go-offline` (which doubles downloads).

---

## 4. Wire the real secrets

Edit [k8s/01-secrets-configmaps.yaml](k8s/01-secrets-configmaps.yaml) and replace these placeholders:

- `mysql-secret.MYSQL_ROOT_PASSWORD`
- `mysql-secret.MYSQL_PASSWORD` (must equal `backend-secret.DB_PASSWORD`)
- `backend-secret.DB_PASSWORD`
- `backend-secret.AUTH0_CLIENT_SECRET`
- `backend-secret.GEMINI_API_KEY`

`APP_CORS_ALLOWED_ORIGINS` and `CODEARENA_BASE_URL` already match the deterministic URL `https://codearena.10.0.1.123.nip.io:32090`. If you ever change the NodePort or host, update both, plus `apiBaseUrl` in [code-arena-frontend/src/environments/environment.prod.ts](../code-arena-frontend/src/environments/environment.prod.ts), then rebuild and push the frontend image.

---

## 5. Apply manifests in order

```bash
kubectl apply -f k8s/00-namespace.yaml
kubectl apply -f k8s/01-secrets-configmaps.yaml

kubectl apply -f k8s/02-mysql.yaml
kubectl -n codearena rollout status deploy/mysql --timeout=300s

kubectl apply -f k8s/03-backend.yaml
kubectl apply -f k8s/04-frontend.yaml
kubectl -n codearena rollout status deploy/backend  --timeout=300s
kubectl -n codearena rollout status deploy/frontend --timeout=180s

kubectl apply -f k8s/05-ingress.yaml
```

---

## 6. Initialize MySQL schema

JPA's `ddl-auto: update` will create the bulk of the tables on first boot, but the repo also includes a phpMyAdmin dump (`codearena.sql`) with seed data. Load it after the DB is up:

```bash
MYSQL_POD=$(kubectl -n codearena get pod -l app=mysql -o jsonpath='{.items[0].metadata.name}')

# Copy the dump into the pod
kubectl -n codearena cp ../codearena.sql "${MYSQL_POD}:/tmp/codearena.sql"

# Replay it as root
kubectl -n codearena exec -i "${MYSQL_POD}" -- \
  sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" codearena < /tmp/codearena.sql'
```

If you'd rather avoid restoring schema collisions on a hot DB, do this **before** rolling out the backend (i.e. between step 5's `mysql` rollout and the `kubectl apply` for backend).

---

## 7. Verify

```bash
kubectl -n codearena get pods -o wide
kubectl -n codearena get svc
kubectl -n codearena get ingress
kubectl -n ingress-nginx get svc ingress-nginx-controller
```

Expect: 1 mysql pod on worker2, 2 backend pods Running, 2 frontend pods Running, 1 ingress, NodePort 32090 mapped to 443.

Smoke test from inside the cluster:

```bash
kubectl -n codearena run curl --rm -it --image=curlimages/curl:8.10.1 --restart=Never -- \
  curl -sk https://codearena.10.0.1.123.nip.io/api/actuator/health || true
# (Actuator is not enabled — expect a 404, which still proves Ingress + backend are routing.)
```

From the cluster network, reach the public URL through worker1:

```bash
curl -sk -o /dev/null -w "%{http_code}\n" https://192.168.0.250:32090/ -H "Host: codearena.10.0.1.123.nip.io"
# expect: 200
```

---

## 8. Windows access setup

On the Windows machine, edit `C:\Windows\System32\drivers\etc\hosts` (as Administrator) and add:

```
192.168.0.250  codearena.10.0.1.123.nip.io
```

Then open in your browser:

```
https://codearena.10.0.1.123.nip.io:32090
```

The browser will warn about the self-signed cert — **Advanced → Proceed**.

---

## 9. Auth0 configuration

In the Auth0 dashboard (https://manage.auth0.com → Applications → your SPA → Settings), append `https://codearena.10.0.1.123.nip.io:32090` to:

- **Allowed Callback URLs**
- **Allowed Logout URLs**
- **Allowed Web Origins**

If you also test from `http://localhost:4200`, keep that entry in addition. Save changes.

---

## 10. Troubleshooting cheatsheet

| Symptom | Likely cause | Fix |
|---|---|---|
| Backend pods CrashLoopBackOff with HTTP probe failures | `/actuator/health` probe in use | Manifests already use `tcpSocket` — check you applied [03-backend.yaml](k8s/03-backend.yaml) as-is |
| Backend boots but errors on DB | `application.yml` default datasource is hitting wrong host | Make sure `SPRING_PROFILES_ACTIVE=prod` is set (it is, in `backend-config`) |
| Frontend builds fine but black screen, console says "auth0-spa-js must run on a secure origin" | Hitting HTTP not HTTPS | Use the HTTPS URL with NodePort `32090`, accept the self-signed cert |
| Angular build fails with TS2339 referencing `groqApiKey` | `environment.prod.ts` missing the field | Already fixed — file includes `groqApiKey: ''` |
| Ingress webhook rejects `configuration-snippet` annotation | Cluster admin disabled snippets | Manifests use only `proxy-*`, `ssl-redirect`, and `server-snippet` (allowed in this controller's defaults). If `server-snippet` is also blocked, drop it and accept that WebSocket upgrade headers come from `proxy-http-version: "1.1"` alone. |
| Maven build times out during `docker build` | Slow uplink + parallel downloads | Already mitigated: BuildKit + retry flags + `--network=host`. Re-run; layers cache. |
| Worker3 NodePort returns TLS handshake timeout | Worker3 is unreliable | Use worker1 (`192.168.0.250`) only. Manifests already exclude worker3 from scheduling. |
| Pods on worker2 won't start, mount errors | `/data/mysql-codearena` missing | `ssh worker2 sudo mkdir -p /data/mysql-codearena && sudo chmod 777 /data/mysql-codearena` |

---

## 11. Tear down

```bash
kubectl delete -f k8s/05-ingress.yaml --ignore-not-found
kubectl delete -f k8s/04-frontend.yaml --ignore-not-found
kubectl delete -f k8s/03-backend.yaml --ignore-not-found
kubectl delete -f k8s/02-mysql.yaml --ignore-not-found
kubectl delete -f k8s/01-secrets-configmaps.yaml --ignore-not-found
kubectl delete -f k8s/00-namespace.yaml --ignore-not-found
# DB data on worker2:/data/mysql-codearena is NOT deleted automatically.
```
