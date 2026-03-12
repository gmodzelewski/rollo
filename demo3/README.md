# Demo 3: Canary with steps

**Goal:** Progressive rollout with pause points—roll out in stages (20% → 40% → 60% → 80% → 100%) with manual or timed pauses.

**Key Message:** Canary with steps gives you fine control over blast radius and rollout speed. This extends beyond what DeploymentConfig's rolling strategy offered.

---

## What is Canary with Steps?

**Progressive rollout:**
- Start with small percentage of traffic to new version (canary)
- Pause to check metrics, errors, performance
- Gradually increase percentage
- If something's wrong, abort and rollback

**Steps in this demo:**
1. **20% → manual pause** (you decide when to continue)
2. **40% → 10s pause** (auto-advance after 10 seconds)
3. **60% → 10s pause**
4. **80% → 10s pause**
5. **100%** (all traffic on new version)

**Use case:** Critical apps where you want to minimize blast radius and have pause points to validate metrics.

---

## Apply Demo

### 1. Create namespace, services, and rollout
```bash
oc apply -f namespace.yaml
oc apply -f services.yaml  # Creates stable and canary services
oc apply -f rollout.yaml
oc apply -f routes.yaml    # Creates Routes for stable and canary services
```

### 2. Watch rollout status
```bash
oc argo rollouts get rollout rollo-demo-3 -n rollo-demo --watch
```

**What you'll see:**
- Rollout: Healthy
- Services: rollo-demo-3-stable (production), rollo-demo-3-canary (new version)
- Pods: 5/5 on current version (v1)
- Replicas: 5 (for clearer percentage splits)

**Access the application:**
```bash
oc get routes -n rollo-demo
```
You can open the stable and canary Routes in your browser to see which version is served.

---

## How Traffic Distribution Works (Important!)

### Without Traffic Provider (this demo)

**Mechanism:** Traffic weight is approximated by **replica ratio**.

**Example at 20% weight:**
- Total replicas: 5
- Canary pods: 1 (20% of 5)
- Stable pods: 4 (80% of 5)
- Traffic is load-balanced across all 5 pods
- **Result: ~20% to canary on average** (not exact)

**Percentage to replica mapping:**
| Step | Weight | Canary Pods | Stable Pods | Approximate Traffic |
|------|--------|-------------|-------------|---------------------|
| 1    | 20%    | 1           | 4           | ~20% to canary      |
| 2    | 40%    | 2           | 3           | ~40% to canary      |
| 3    | 60%    | 3           | 2           | ~60% to canary      |
| 4    | 80%    | 4           | 1           | ~80% to canary      |
| 5    | 100%   | 5           | 0           | 100% to canary      |

**Note:** This is replica-based, not exact percentage control. Good for learning and non-critical apps.

---

### With Traffic Provider (Production Recommendation)

**Mechanism:** Integration with ingress controller or service mesh for **exact traffic splitting**.

**Supported providers:**
- **OpenShift Routes** (via Argo Rollouts plugin)
- **Istio** (VirtualService manipulation)
- **NGINX Ingress**
- **AWS ALB**
- **SMI (Service Mesh Interface)**

**Example at 20% weight with Istio:**
- VirtualService routes 20% of requests → canary service
- VirtualService routes 80% of requests → stable service
- **Result: Exactly 20% traffic** regardless of replica count

**Configuration (example for Istio):**
```yaml
strategy:
  canary:
    canaryService: rollo-demo-3-canary
    stableService: rollo-demo-3-stable
    trafficRouting:
      istio:
        virtualService:
          name: rollo-demo-3-vsvc
    steps: [...]
```

**See:** https://argo-rollouts.readthedocs.io/en/stable/features/traffic-management/

---

## Trigger Update

```bash
oc argo rollouts set image rollo-demo-3 rollo=quay.io/modzelewski/rollo:v2 -n rollo-demo
```

**What happens:**
1. Rollout creates new ReplicaSet with v2
2. Scales canary to 1 pod (20% of 5 replicas)
3. **Pauses at step 1** (manual pause)

**Watch in terminal** (Pane A):
- Status: `Paused` at step 1/8
- Message: "CanaryPauseStep"
- Canary weight: 20
- Stable pods: 4, Canary pods: 1

**Check pods:**
```bash
oc get pods -n rollo-demo -l app=rollo-demo-3
```
You'll see 4 pods on old ReplicaSet, 1 pod on new ReplicaSet.

---

## Promote (Continue Rollout)

When you're satisfied with the 20% canary (check metrics, logs, errors):

```bash
oc argo rollouts promote rollo-demo-3 -n rollo-demo
```

**What happens:**
1. Rollout advances to step 2: 40% weight → 2 canary pods, 3 stable pods
2. Pauses for 10 seconds (automatic)
3. Advances to step 3: 60% weight → 3 canary pods, 2 stable pods
4. Pauses for 10 seconds
5. Advances to step 4: 80% weight → 4 canary pods, 1 stable pod
6. Pauses for 10 seconds
7. Final: 100% → all 5 pods on v2, old ReplicaSet scaled to 0

**Watch in terminal:**
- You'll see steps advancing automatically with short pauses
- Replica counts change at each step
- Finally: Rollout: Healthy, all pods on v2

**Total time (after promote):** ~30 seconds (3 steps × 10s pauses)

---

## Abort (if something is wrong)

At any step, if you see high error rates, crashes, or issues:

```bash
oc argo rollouts abort rollo-demo-3 -n rollo-demo
```

**What happens:**
- Rollout aborts immediately
- Canary ReplicaSet scaled to 0
- Stable ReplicaSet scaled back to 5
- All traffic back to stable version (v1)

**Safe rollback!**

---

## Monitoring During Rollout (Recommended)

While the rollout is progressing, you should monitor:
- **Error rates** (from logs or APM)
- **Latency** (P95, P99 from metrics)
- **Crash rate** (pod restarts)
- **Business metrics** (transactions, revenue, etc.)

**Advanced:** Use Argo Rollouts **AnalysisTemplates** to automate this:
- Query Prometheus during rollout
- Auto-abort if error rate exceeds threshold
- Example: "Abort if error rate > 5% for 1 minute"

See: https://argo-rollouts.readthedocs.io/en/stable/features/analysis/

---

## Key Configuration

From `rollout.yaml`:
```yaml
strategy:
  canary:
    canaryService: rollo-demo-3-canary  # New version traffic
    stableService: rollo-demo-3-stable  # Stable version traffic
    steps:
      - setWeight: 20
      - pause: {}                       # Manual pause (wait for promote)
      - setWeight: 40
      - pause:
          duration: 10                  # Auto-advance after 10 seconds
      - setWeight: 60
      - pause:
          duration: 10
      - setWeight: 80
      - pause:
          duration: 10
      # Final step: 100% (implicit)
```

**pause: {}** = manual pause (requires `oc argo rollouts promote`)
**pause: {duration: N}** = automatic pause for N seconds

---

## Key Takeaways

1. **Progressive rollout** with pause points minimizes blast radius
2. **Manual pause at 20%** gives you control; auto-pauses at later steps
3. **Replica-based traffic** (this demo) is simple; **traffic provider** (Istio/Routes) gives exact percentages
4. **Abort at any step** for safe rollback
5. **Extends beyond DC** rolling strategy—DeploymentConfigs only had maxSurge/maxUnavailable, not percentage steps

---

## Next Steps

- For exact traffic splitting, integrate with OpenShift Routes or Istio (see [traffic management docs](https://argo-rollouts.readthedocs.io/en/stable/features/traffic-management/))
- Try AnalysisTemplates for automated metrics-based validation
- See [../conversion-example.md](../conversion-example.md) for Canary YAML examples
- See [../FAQ.md](../FAQ.md) for common questions about Canary strategy
