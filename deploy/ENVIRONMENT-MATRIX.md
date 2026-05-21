# OG Fashions Environment Matrix

This document defines the expected behavior of each environment level.

## 1. Environment levels

| Level | Purpose | Access model | Deployment style | Approval |
| --- | --- | --- | --- | --- |
| `local` | developer machine | `localhost` | local run or local Docker | no |
| `l1` | shared dev validation | IP or LB address | Docker or K8s | no |
| `l2` | QA or SIT | IP or LB address | Docker or K8s | no |
| `l3` | UAT | IP or LB address | Docker or K8s | no |
| `l6` | pre-production or staging | IP or LB address | Docker or K8s | no |
| `lp` | production | domain | Docker or K8s | yes |

## 2. Recommended access examples

| Level | Example URL |
| --- | --- |
| `local` | `http://localhost:8081` |
| `l1` | `http://<l1-ip>` |
| `l2` | `http://<l2-ip>` |
| `l3` | `http://<l3-lb-address>` |
| `l6` | `http://<l6-lb-address>` |
| `lp` | `https://ogstyles.store` |

## 3. Recommended promotion order

1. `local`
2. `l1`
3. `l2`
4. `l3`
5. `l6`
6. `lp`

## 4. Release rule

The same immutable image tag should move across all levels.

Example:

1. `defect-102-44` built in Jenkins
2. deploy `defect-102-44` to `l1`
3. deploy `defect-102-44` to `l2`
4. deploy `defect-102-44` to `l3`
5. deploy `defect-102-44` to `l6`
6. deploy `defect-102-44` to `lp`

## 5. Common health checks for every level

1. `/`
2. `/healthz`
3. `/api/actuator/health`
4. `/api/actuator/prometheus`
