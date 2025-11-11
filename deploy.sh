#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")" && pwd)"
COMPOSE_FILE="${ROOT}/docker-compose.production.yml"   # update if your file has different name

echo "ROOT = $ROOT"
cd "$ROOT"

# 1) Build React admin with correct PUBLIC_URL so assets reference /admin/
if [ -d "shaadisharthi-react" ]; then
  echo "Building React admin..."
  cd shaadisharthi-react
  # install dependencies (use npm ci for CI reproducibility)
  npm ci
  PUBLIC_URL=/admin npm run build
  # copy build output into frontend folder used by nginx
  mkdir -p "$ROOT/shaadisharthi-frontend/admin"
  rm -rf "$ROOT/shaadisharthi-frontend/admin/*" || true
  cp -r build/* "$ROOT/shaadisharthi-frontend/admin/"
  cd "$ROOT"
fi

# 2) Build Angular provider with correct base-href so assets reference /provider/
if [ -d "shaadisharthi-angular" ]; then
  echo "Building Angular provider..."
  cd shaadisharthi-angular
  npm ci
  # Update output path to top-level frontend folder
  ng build --configuration production --base-href /provider/ --output-path "$ROOT/shaadisharthi-frontend/provider" || {
    # fallback if CLI not present in PATH
    echo "Angular build failed. Ensure @angular/cli is installed."
    exit 1
  }
  cd "$ROOT"
fi

# 3) Build Next (customer) image
if [ -d "shaadisharthi-next" ]; then
  echo "Building Next.js (docker image)..."
  cd shaadisharthi-next
  # If you want to build a production docker image for next, keep Dockerfile build steps
  docker build -t shaadisharthi-next:latest .
  cd "$ROOT"
fi

# 4) Build backend image
if [ -d "shaadisharthi-backend/docker" ]; then
  echo "Building backend docker image..."
  cd shaadisharthi-backend/docker
  docker build -t shaadisharthi-backend:latest .
  cd "$ROOT"
fi

# 5) Build frontend nginx image (serves admin/provider & proxies)
echo "Building nginx frontend image..."
docker build -t shaadisharthi-frontend-nginx:latest -f nginx/Dockerfile .

# 6) Start the stack
echo "Bringing up docker-compose stack using $COMPOSE_FILE..."
docker compose -f "$COMPOSE_FILE" up -d --remove-orphans --build

echo "Done. Use: docker compose -f $COMPOSE_FILE ps  and docker compose -f $COMPOSE_FILE logs -f <service>"

