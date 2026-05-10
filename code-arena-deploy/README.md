# CodeArena — Kubernetes Deployment Guide

End-to-end recipe for deploying CodeArena (Angular 18 frontend + Spring Boot 3.3.5 backend + MySQL 8.0) to a 2-master / 3-worker **local Kubernetes 1.32.13** cluster (containerd 2.2.1, Ubuntu 24.04) with no `StorageClass`.

## Cluster facts assumed by this guide

| Role          | Hostname              | Internal IP   |
|---------------|-----------------------|---------------|
| control-plane | k8s-masters-master1   | 10.0.1.112    |
| control-plane | k8s-masters-master2   | 10.0.1.228    |
| worker        | k8s-5vm-worker1       | 10.0.1.123    |
| worker        | k8s-5vm-worker2       | 10.0.1.124    |
| worker        | k8s-5vm-worker3       | 10.0.1.220    |

Verify with:

```bash
kubectl get nodes -o wide
```

If your hostnames or IPs differ, update [k8s/02-mysql.yaml](k8s/02-mysql.yaml) (`nodeSelector`), [k8s/03-backend.yaml](k8s/03-backend.yaml) and [k8s/04-frontend.yaml](k8s/04-frontend.yaml) (`nodeAffinity`), and the nip.io host in [k8s/05-ingress.yaml](k8s/05-ingress.yaml) and [k8s/01-secrets-configmaps.yaml](k8s/01-secrets-configmaps.yaml).

### Scheduling rules baked into the manifests

- **MySQL is pinned to `k8s-5vm-worker2`** via `nodeSelector` — data lives on a `hostPath` at `/data/mysql-codearena`. Moving the pod off this node loses access to the data.
- **`k8s-5vm-worker3` is excluded** from frontend/backend scheduling via `nodeAffinity` (historically flaky). Drop the `NotIn` rule if your worker3 is healthy.
- **Browser entry point**: any worker's IP — example uses `k8s-5vm-worker1` at `10.0.1.123`. The Ingress NodePort (`32090`) is reachable on **every** node, so any worker IP works.

The deterministic public URL is:

```
https://codearena.10.0.1.123.nip.io:32090
```

`32090` is the Ingress controller's HTTPS NodePort — we pin it explicitly in step 1. The `nip.io` hostname resolves `<anything>.10.0.1.123.nip.io` → `10.0.1.123` via public DNS, so no /etc/hosts entry is needed *if* your browser machine can reach `10.0.1.123` directly.

> **Note on monitoring**: the cluster already runs the `kube-prometheus-stack` (Prometheus + Grafana + Alertmanager + node-exporter + kube-state-metrics) in the `monitoring` namespace. CodeArena deploys into its own `codearena` namespace and does not interfere. See [§ 12](#12-monitoring-integration-optional) for optional ServiceMonitor wiring.

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

All `kubectl` commands below are intended to run from a master node (e.g. `master@k8s-masters-master1`) or any host with a kubeconfig that points at this cluster.

### 0.1 — Create the data directory on `k8s-5vm-worker2`

The MySQL pod mounts `/data/mysql-codearena` from the worker's filesystem. Create it on **worker2** before you apply manifests:

```bash
# From worker2 directly:
sudo mkdir -p /data/mysql-codearena && sudo chmod 777 /data/mysql-codearena

# Or, if you can SSH to worker2 from a master:
ssh master@10.0.1.124 'sudo mkdir -p /data/mysql-codearena && sudo chmod 777 /data/mysql-codearena'
```

> The `hostPath` volume uses `type: DirectoryOrCreate`, but pre-creating with `chmod 777` avoids first-boot permission issues with the mysql container's UID.

### 0.2 — Confirm node hostnames match the manifests

```bash
kubectl get nodes -o wide
```

You should see `k8s-5vm-worker1/2/3` and `k8s-masters-master1/2`. If your worker2 hostname differs, edit `nodeSelector` in [k8s/02-mysql.yaml](k8s/02-mysql.yaml) and the `NotIn` lists in [k8s/03-backend.yaml](k8s/03-backend.yaml) / [k8s/04-frontend.yaml](k8s/04-frontend.yaml).

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

# Confirm — you want to see "443:32090/TCP" in the PORT(S) column
kubectl -n ingress-nginx get svc ingress-nginx-controller
```

**If the controller pod is stuck in `ImagePullBackOff`** — usually a transient TLS timeout to `registry.k8s.io`. Just delete the pod and let it retry:

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

kubectl create namespace codearena --dry-run=client -o yaml | kubectl apply -f -

kubectl -n codearena create secret tls codearena-tls \
  --cert=codearena.crt --key=codearena.key \
  --dry-run=client -o yaml | kubectl apply -f -

cd ..
```

(The namespace is also created by `00-namespace.yaml` — harmless to apply twice.)

---

## 3. Build & push Docker images

Login as `adamzahi` (or your own Docker Hub account — remember to update the `image:` field in `03-backend.yaml` / `04-frontend.yaml` if you change the prefix):

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

> `--network=host` lets Maven reach Maven Central directly through the host's NAT. The Dockerfile uses retry-friendly Maven flags and skips `dependency:go-offline` (which doubles downloads).

---

## 4. Wire the real secrets

Edit [k8s/01-secrets-configmaps.yaml](k8s/01-secrets-configmaps.yaml) and replace these placeholders:

- `mysql-secret.MYSQL_ROOT_PASSWORD`
- `mysql-secret.MYSQL_PASSWORD` (must equal `backend-secret.DB_PASSWORD`)
- `backend-secret.DB_PASSWORD`
- `backend-secret.AUTH0_CLIENT_SECRET`
- `backend-secret.GEMINI_API_KEY`

`APP_CORS_ALLOWED_ORIGINS` and `CODEARENA_BASE_URL` already match the deterministic URL `https://codearena.10.0.1.123.nip.io:32090`. If you change the NodePort or host, update:

1. Both env vars in [k8s/01-secrets-configmaps.yaml](k8s/01-secrets-configmaps.yaml).
2. The `host:` and `tls.hosts` in [k8s/05-ingress.yaml](k8s/05-ingress.yaml).
3. `apiBaseUrl` in [code-arena-frontend/src/environments/environment.prod.ts](../code-arena-frontend/src/environments/environment.prod.ts), then rebuild and push the frontend image.
4. The OpenSSL `CN=` and `subjectAltName=DNS:` from step 2.

---

## 5. Apply manifests in order

```bash
cd code-arena-deploy

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

If you'd rather avoid restoring schema collisions on a hot DB, do this **before** rolling out the backend (i.e. between the `mysql` rollout and `kubectl apply -f k8s/03-backend.yaml`).

---

## 7. Verify

```bash
kubectl -n codearena get pods -o wide
kubectl -n codearena get svc
kubectl -n codearena get ingress
kubectl -n ingress-nginx get svc ingress-nginx-controller
```

Expect: 1 mysql pod on `k8s-5vm-worker2`, 2 backend pods Running, 2 frontend pods Running, 1 ingress, NodePort `32090` mapped to `443`.

Smoke test from inside the cluster:

```bash
kubectl -n codearena run curl --rm -it --image=curlimages/curl:8.10.1 --restart=Never -- \
  curl -sk https://codearena.10.0.1.123.nip.io/api/actuator/health || true
# (Actuator is not enabled — expect a 404, which still proves Ingress + backend are routing.)
```

From any node, reach the public URL through worker1's NodePort:

```bash
curl -sk -o /dev/null -w "%{http_code}\n" https://10.0.1.123:32090/ -H "Host: codearena.10.0.1.123.nip.io"
# expect: 200
```

---

## 8. Browser access

You have three realistic options depending on how your dev machine reaches the cluster:

### 8a. Direct (your machine routes to `10.0.1.0/24`)

If your laptop can ping `10.0.1.123` — for instance you're on the same VPN/subnet, or running this cluster in VMs with a host-only network — open:

```
https://codearena.10.0.1.123.nip.io:32090
```

`nip.io` resolves the host to `10.0.1.123` via public DNS; no hosts file edit needed. Accept the self-signed cert (**Advanced → Proceed**).

### 8b. SSH tunnel from a Windows/macOS/Linux laptop

If you can SSH to a master but cannot route to the worker network directly, tunnel `32090` through:

```bash
# Run on your laptop. -L binds 32090 locally and forwards to worker1:32090 via the master.
ssh -L 32090:10.0.1.123:32090 master@<master1-reachable-ip>
```

Then add a single line to your hosts file (`C:\Windows\System32\drivers\etc\hosts` on Windows, `/etc/hosts` on macOS/Linux), as Administrator/root:

```
127.0.0.1  codearena.10.0.1.123.nip.io
```

Open: `https://codearena.10.0.1.123.nip.io:32090` — the SSH tunnel forwards `127.0.0.1:32090` to the cluster.

### 8c. `kubectl port-forward` (quickest, only for local testing)

```bash
kubectl -n ingress-nginx port-forward svc/ingress-nginx-controller 32090:443
```

Then add `127.0.0.1 codearena.10.0.1.123.nip.io` to your hosts file as in 8b and browse to `https://codearena.10.0.1.123.nip.io:32090`. Note: port-forward bypasses NodePort entirely; useful only when 32090 isn't reachable on the workers.

---

## 9. Auth0 configuration

In the Auth0 dashboard (https://manage.auth0.com → Applications → your SPA → Settings), append `https://codearena.10.0.1.123.nip.io:32090` to:

- **Allowed Callback URLs**
- **Allowed Logout URLs**
- **Allowed Web Origins**

If you also test from `http://localhost:4200`, keep that entry too. Save changes.

---

## 10. Troubleshooting cheatsheet

| Symptom | Likely cause | Fix |
|---|---|---|
| Backend pods CrashLoopBackOff with HTTP probe failures | `/actuator/health` probe in use | Manifests already use `tcpSocket` — re-apply [03-backend.yaml](k8s/03-backend.yaml) as-is |
| Backend boots but errors on DB | `application.yml` default datasource is hitting wrong host | Make sure `SPRING_PROFILES_ACTIVE=prod` is set (it is, in `backend-config`) |
| Frontend builds fine but blank screen, console says "auth0-spa-js must run on a secure origin" | Hitting HTTP not HTTPS | Use the HTTPS URL with NodePort `32090`, accept the self-signed cert |
| Angular build fails with TS2339 referencing `groqApiKey` | `environment.prod.ts` missing the field | Already fixed — file includes `groqApiKey: ''` |
| Ingress webhook rejects `configuration-snippet` annotation | Cluster admin disabled snippets | Manifests use only `proxy-*`, `ssl-redirect`, and `server-snippet` (allowed in this controller's defaults). If `server-snippet` is also blocked, drop it and rely on `proxy-http-version: "1.1"` for WebSocket support. |
| Maven build times out during `docker build` | Slow uplink + parallel downloads | Already mitigated: BuildKit + retry flags + `--network=host`. Re-run; layers cache. |
| `k8s-5vm-worker3` NodePort returns TLS handshake timeout | Worker3 unreliable | Use `k8s-5vm-worker1` (`10.0.1.123`) instead. Manifests already exclude worker3 from app scheduling. |
| MySQL pod stuck `Pending` with `0/5 nodes available: node(s) didn't match Pod's node affinity` | `nodeSelector` hostname mismatch | Run `kubectl get nodes` and update `nodeSelector` in [02-mysql.yaml](k8s/02-mysql.yaml) |
| MySQL pod runs but data doesn't persist across restarts | hostPath dir missing on worker2 | `ssh worker2 sudo mkdir -p /data/mysql-codearena && sudo chmod 777 /data/mysql-codearena` |
| Browser can't reach `10.0.1.123:32090` | Laptop has no route to the cluster's internal subnet | Use the SSH tunnel (§ 8b) or `kubectl port-forward` (§ 8c) |

---

## 11. Tear down

```bash
kubectl delete -f k8s/05-ingress.yaml --ignore-not-found
kubectl delete -f k8s/04-frontend.yaml --ignore-not-found
kubectl delete -f k8s/03-backend.yaml --ignore-not-found
kubectl delete -f k8s/02-mysql.yaml --ignore-not-found
kubectl delete -f k8s/01-secrets-configmaps.yaml --ignore-not-found
kubectl delete -f k8s/00-namespace.yaml --ignore-not-found
# DB data on k8s-5vm-worker2:/data/mysql-codearena is NOT deleted automatically.
# To wipe it: ssh into worker2 and `sudo rm -rf /data/mysql-codearena`.
```

---

## 12. Monitoring integration (optional)

The cluster already has `kube-prometheus-stack` running in the `monitoring` namespace (verify with `kubectl -n monitoring get pods`). CodeArena's backend doesn't expose Actuator metrics by default, so there's nothing for Prometheus to scrape out of the box. If you later enable `spring-boot-starter-actuator` + `micrometer-registry-prometheus`, add a `ServiceMonitor` selector matching `app: backend` in the `codearena` namespace — but that's a code change in the backend, not a deployment concern.

To browse the existing Grafana from your laptop:

```bash
kubectl -n monitoring port-forward svc/kube-prometheus-stack-grafana 3000:80
# Default creds: admin / prom-operator (unless changed at install time)
```
