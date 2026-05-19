# Local Docker Deployment

This path is for a full local container-based run on one machine.

## Files

- `local-docker.env.example`
- `local-docker.secrets.env.example`
- `local-docker.sh`
- `local-docker.ps1`
- `local-docker-cleanup.sh`

## First-time setup

```bash
cp deploy/local/local-docker.env.example deploy/local/local-docker.env
cp deploy/local/local-docker.secrets.env.example deploy/local/local-docker.secrets.env
```

Update only the secrets file if you want your own local passwords.

## Run locally

### Linux / macOS / WSL / Git Bash

```bash
chmod +x deploy/local/local-docker.sh
./deploy/local/local-docker.sh
```

### PowerShell

```powershell
./deploy/local/local-docker.ps1
```

## Local URLs

- Frontend: `http://localhost:8080/`
- Frontend health: `http://localhost:8080/healthz`
- Backend health: `http://localhost:8080/api/actuator/health`

## Cleanup

```bash
./deploy/local/local-docker-cleanup.sh
```
