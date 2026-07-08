# Docker Deployment

This project can run as a Docker Compose stack with three services:

- `app`: Spring Boot application on port `8080`
- `mysql`: MySQL 8.0 with persistent data
- `redis`: Redis 7 with append-only persistence

The container profile uses service names instead of `localhost`:

- MySQL host: `mysql`
- Redis host: `redis`

## Prerequisites

Docker Engine and the Compose plugin must be available in WSL:

```bash
docker --version
docker compose version
```

## Start

From the repository root:

```bash
cp .env.example .env
docker compose up -d --build
```

Open:

```text
http://localhost:8080
```

## Logs

```bash
docker compose logs -f app
docker compose logs -f mysql
docker compose logs -f redis
```

## Stop

```bash
docker compose down
```

Keep database and Redis volumes:

```bash
docker compose down
```

Remove all persisted Compose data:

```bash
docker compose down -v
```

## Database Initialization

On first startup, the MySQL container runs:

- `src/main/resources/sql/schema.sql`
- `src/main/resources/sql/data.sql`

These scripts only run when the `mysql-data` volume is first created. To re-run them from a clean database, remove the Compose volumes:

```bash
docker compose down -v
docker compose up -d --build
```

## Redis Ranking Sync

After the app starts, initialize Redis ranking data when needed:

```bash
curl -X POST "http://localhost:8080/api/redis-ranking/sync?token=ops_demo_token"
```

## Notes For WSL

The Compose stack runs its own Redis container and does not publish Redis port `6379` to the Windows host. This avoids conflicts with the Redis service already installed in WSL.

If Docker Hub is reachable through a Windows proxy, configure the Docker daemon inside WSL. Replace the IP and port with your WSL gateway and local proxy port:

```bash
sudo mkdir -p /etc/systemd/system/docker.service.d
sudo tee /etc/systemd/system/docker.service.d/http-proxy.conf >/dev/null <<'EOF'
[Service]
Environment="HTTP_PROXY=http://172.18.192.1:7897"
Environment="HTTPS_PROXY=http://172.18.192.1:7897"
Environment="NO_PROXY=localhost,127.0.0.1,::1"
EOF

sudo systemctl daemon-reload
sudo systemctl restart docker
```

If Docker Hub times out in WSL, set a Docker Hub mirror prefix in `.env`:

```env
DOCKERHUB_PREFIX=docker.1ms.run/library/
```

Then build again:

```bash
docker compose up -d --build
```
