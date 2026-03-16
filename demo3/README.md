# Demo 3: Canary with Steps - Progressive Delivery

**Goal:** Progressive canary rollout with pause points (20% → 40% → 60% → 80% → 100%).

**Key Message:** This demo shows two approaches:

1. **Basic (replica-based)** - Simple, works without extra infrastructure
2. **Production (Service Mesh)** - Exact traffic percentages, production-ready

---

## Quick Start - Basic Canary (Replica-Based)

### What You'll See

Progressive rollout controlled by replica ratios:

- 20% weight = 1 canary pod, 4 stable pods (≈20% traffic)
- 40% weight = 2 canary pods, 3 stable pods (≈40% traffic)
- Manual pause at 20%, auto-advance with timed pauses

**No external dependencies. No Service Mesh. Just Argo Rollouts.**

### Deploy

```bash
cd demo3

# 1. Create services
oc apply -f services.yaml

# 2. Deploy Rollout (basic canary, no traffic routing)
oc apply -f rollout.yaml

# 3. Watch rollout status
oc argo rollouts get rollout rollo-demo-3 -n rollo-demo --watch
```

### Trigger Rollout

Open two terminal panes:

**Terminal 1: Watch status**

```bash
oc argo rollouts get rollout rollo-demo-3 -n rollo-demo --watch
```

**Terminal 2: Trigger rollout**

```bash
# Update to v2
oc argo rollouts set image rollo-demo-3 rollo=quay.io/modzelewski/rollo:v2 -n rollo-demo

# Wait for rollout to pause at 20%...
# You'll see: Status: Paused, Step: 1/8, SetWeight: 20

# Check pod distribution
oc get pods -n rollo-demo -l app=rollo-demo-3

# Expected: 1 canary pod (v2), 4 stable pods (v1)

# Promote to continue
oc argo rollouts promote rollo-demo-3 -n rollo-demo

# Watch it auto-advance through 40%, 60%, 80% to 100%
```

### How It Works (Replica-Based)

**Do we really need two services?** In theory, one service selecting all 5 pods (1 canary + 4 stable) with round-robin would send ~20% of requests to the canary pod—every fifth request. But Argo Rollouts does not support that:

- The canary strategy **requires** both `canaryService` and `stableService` in the spec (API requirement).
- The Rollout controller **updates** each service’s selector so they point at different ReplicaSets:
  - `stableService` → only stable ReplicaSet pods (e.g. 4 pods)
  - `canaryService` → only canary ReplicaSet pods (e.g. 1 pod)

So you get two disjoint pod sets, not “one service with all pods.” To get actual ~20% traffic to the canary, the Route must split traffic between the two services. This demo uses a single Route with two backends: 80% to stable, 20% to canary (`alternateBackends`).

**Traffic distribution at 20% weight:**

- Total replicas: 5 (4 stable, 1 canary)
- Route: 80% → stableService (4 pods), 20% → canaryService (1 pod)
- **Result: ~20% of requests hit the canary** (round-robin within each backend)

**Replica progression:**


| Step  | Weight | Canary Pods | Stable Pods | Route split | Traffic to canary |
| ----- | ------ | ----------- | ----------- | ----------- | ----------------- |
| 1     | 20%    | 1           | 4           | 80/20       | ~20%              |
| 2     | 40%    | 2           | 3           | 80/20       | ~20% (fixed)*     |
| 3     | 60%    | 3           | 2           | 80/20       | ~20% (fixed)*     |
| 4     | 80%    | 4           | 1           | 80/20       | ~20% (fixed)*     |
| Final | 100%   | 5           | 0           | —           | 100%              |


 With a static 80/20 Route, traffic to canary stays ~20% until promotion. For step-wise 40%→60%→80% you need Service Mesh (Istio), which updates weights automatically.

**When to use:**

- ✅ Learning and demos
- ✅ Non-critical applications
- ✅ Environments without Service Mesh
- ✅ Teams that want simple progressive delivery

**Limitations:**

- ❌ Route weights are static (e.g. 80/20); only Istio gives step-wise 40%→60%→80%
- ❌ Not exact percentages (round-robin within each backend)
- ❌ Two services are required by the Rollout API (controller assigns each to one ReplicaSet)

---

## Production Upgrade - Service Mesh 3 (Exact Traffic)

**Why upgrade?** Get **exact traffic percentages** (20% = exactly 20 out of 100 requests).

### Prerequisites

- OpenShift Service Mesh 3 operator installed
- Istio control plane deployed in `istio-system` namespace

### Upgrade Steps

#### 1. Enable Ambient Mode on Namespace

```bash
# Add Service Mesh labels to namespace
oc label namespace rollo-demo \
  istio.io/dataplane-mode=ambient \
  istio-discovery=enabled \
  --overwrite
```

#### 2. Deploy Waypoint Proxy (L7 Routing)

```bash
# Waypoint handles VirtualService routing in ambient mode
oc apply -f waypoint.yaml

# Wait for waypoint to be ready
oc get gateway rollo-demo-waypoint -n rollo-demo
# Expected: PROGRAMMED = True

# Verify waypoint pod is running
oc get pods -n rollo-demo -l gateway.networking.k8s.io/gateway-name=rollo-demo-waypoint
```

#### 3. Add Waypoint Label to Namespace

```bash
oc label namespace rollo-demo \
  istio.io/use-waypoint=rollo-demo-waypoint \
  --overwrite
```

#### 4. Deploy VirtualService

```bash
# VirtualService will be MANAGED by Argo Rollouts
oc apply -f virtualservice.yaml

# Check initial weights (100% stable, 0% canary)
oc get virtualservice rollo-demo-3-vsvc -n rollo-demo \
  -o jsonpath='{range .spec.http[0].route[*]}{.destination.host}{" = "}{.weight}{"%\n"}{end}'
```

#### 5. Update Rollout with Istio Traffic Routing

```bash
# Replace basic rollout with Istio-enabled version
oc apply -f rollout-istio.yaml

# Verify trafficRouting is configured
oc get rollout rollo-demo-3 -n rollo-demo -o yaml | grep -A5 trafficRouting
```

### Test with Service Mesh

Open **three terminal panes** for the best experience:

**Terminal 1: Watch Rollout**

```bash
oc argo rollouts get rollout rollo-demo-3 -n rollo-demo --watch
```

**Terminal 2: Watch VirtualService Weights (THE MAGIC!)**

```bash
watch -n 2 "oc get virtualservice rollo-demo-3-vsvc -n rollo-demo \
  -o jsonpath='Stable: {.spec.http[0].route[0].weight}%  Canary: {.spec.http[0].route[1].weight}%{\"\\n\"}'"
```

**Terminal 3: Trigger Rollout**

```bash
# Trigger rollout
oc argo rollouts set image rollo-demo-3 rollo=quay.io/modzelewski/rollo:v2 -n rollo-demo

# Watch Terminal 2 - VirtualService weights will change automatically!
# Initial:  Stable: 100%  Canary: 0%
# Step 1:   Stable: 80%   Canary: 20%   ← Argo Rollouts updated this!

# Promote
oc argo rollouts promote rollo-demo-3 -n rollo-demo

# Watch Terminal 2 as weights change automatically:
# Step 2:   Stable: 60%   Canary: 40%   ← Auto-updated!
# Step 3:   Stable: 40%   Canary: 60%   ← Auto-updated!
# Step 4:   Stable: 20%   Canary: 80%   ← Auto-updated!
# Final:    Stable: 100%  Canary: 0%    ← Canary promoted to stable
```

### How It Works (Service Mesh)

**Argo Rollouts automatically manipulates VirtualService:**

```
Rollout defines:        VirtualService weights:
  setWeight: 20    →    stable: 80%, canary: 20%  ← Argo Rollouts updates this!
  setWeight: 40    →    stable: 60%, canary: 40%  ← Automatic!
  setWeight: 60    →    stable: 40%, canary: 60%  ← Automatic!
  setWeight: 80    →    stable: 20%, canary: 80%  ← Automatic!
```

**Traffic flow:**

```
Request
  ↓
Waypoint Proxy (L7 routing via VirtualService)
  ├─ 80% → rollo-demo-3-stable service
  │         ↓
  │    Kubernetes load-balances to pods
  │
  └─ 20% → rollo-demo-3-canary service
            ↓
       Kubernetes load-balances to pods
```

**Key insight:** Istio's VirtualService routing happens at **Layer 7 BEFORE** Kubernetes Service load-balancing. This allows exact traffic control even though both services select all pods.

### Verified Results (2026-03-13)


| Step    | Rollout State   | VirtualService Weights   | Result             |
| ------- | --------------- | ------------------------ | ------------------ |
| Initial | Healthy (v1)    | Stable: 100%, Canary: 0% | ✅                  |
| Step 1  | Paused at 20%   | Stable: 80%, Canary: 20% | ✅ **Auto-updated** |
| Step 2  | Progressing 40% | Stable: 60%, Canary: 40% | ✅ **Auto-updated** |
| Step 3  | Progressing 60% | Stable: 40%, Canary: 60% | ✅ **Auto-updated** |
| Step 4  | Progressing 80% | Stable: 20%, Canary: 80% | ✅ **Auto-updated** |
| Final   | Healthy (v2)    | Stable: 100%, Canary: 0% | ✅ Promoted         |


**Exact traffic verified:** At 20% weight, exactly 20 out of 100 requests went to canary.

---

## Comparison: Replica-Based vs Service Mesh


| Aspect                    | Replica-Based               | Service Mesh (Istio)            |
| ------------------------- | --------------------------- | ------------------------------- |
| **Traffic accuracy**      | ~20% (approximate)          | Exactly 20%                     |
| **How weights work**      | Controls replica count      | Controls VirtualService routing |
| **Replica dependency**    | 5 replicas needed for 20%   | Any replica count works         |
| **Minimum increment**     | ~20% (1/5 pods)             | 1% (or less)                    |
| **Setup complexity**      | Simple                      | Requires Service Mesh           |
| **Two services**          | Redundant (both select all) | Used by VirtualService routing  |
| **Production use**        | Learning/non-critical       | Critical applications           |
| **External dependencies** | None                        | Service Mesh 3 + waypoint       |


---

## Key Findings

### Why Two Services?

**Short answer:** The Rollout API requires both, and the controller assigns each to one ReplicaSet.

**Without traffic routing (replica-based):**

- The controller sets **stableService** selector → stable ReplicaSet only (e.g. 4 pods).
- The controller sets **canaryService** selector → canary ReplicaSet only (e.g. 1 pod).
- So the two services target **disjoint** pod sets, not “all pods.”
- To get ~20% traffic to canary, the Route must split: this demo uses one Route with 80% to stable, 20% to canary (`alternateBackends`). One service with all pods + round-robin would also give ~20%, but Rollouts does not support that—it always uses two services and partitions by ReplicaSet.

**With traffic routing (Istio):**

- VirtualService routes X% to `stable` and Y% to `canary`; Argo Rollouts updates X/Y at each step.
- **Two services are essential** so Istio has two distinct backends to weight.
- Istio controls traffic at L7; the controller still assigns each service to the correct ReplicaSet.

### What "Weight" Does

**Without traffic routing:**

- `setWeight: 20` → Scales canary to 1 pod (20% of 5 replicas)
- Traffic is approximated by pod count

**With traffic routing:**

- `setWeight: 20` → Argo Rollouts updates VirtualService to route 20% to canary
- Pods still scale (1 canary, 4 stable) but traffic is controlled by VirtualService

### Who Manages VirtualService?

**Answer: Argo Rollouts controller manages it automatically.**

- You define weights in Rollout: `setWeight: 20`
- Argo Rollouts updates VirtualService: `canary: 20, stable: 80`
- You never touch VirtualService manually during rollout
- VirtualService is a **managed resource** (like ReplicaSet is managed by Deployment)

**Analogy:**

- Deployment manages ReplicaSets (you don't edit ReplicaSets)
- Rollout manages VirtualService weights (you don't edit weights)

---

## GitOps with Argo CD

**⚠️ Important:** If using Argo CD to manage manifests, you MUST configure `ignoreDifferences` to avoid conflicts.

**The Problem:**

- Git has VirtualService with weights: `stable=100, canary=0`
- Argo Rollouts changes to: `stable=80, canary=20`
- Argo CD detects drift and reverts back
- **Infinite loop!**

**The Solution:**

Add to your Argo CD Application:

```yaml
apiVersion: argoproj.io/v1alpha1
kind: Application
metadata:
  name: rollo-demo-3
  namespace: openshift-gitops
spec:
  # Tell Argo CD to ignore VirtualService weight fields
  ignoreDifferences:
    - group: networking.istio.io
      kind: VirtualService
      name: rollo-demo-3-vsvc
      jsonPointers:
        - /spec/http/0/route/0/weight
        - /spec/http/0/route/1/weight

  syncPolicy:
    automated:
      prune: true
      selfHeal: true  # Safe with ignoreDifferences
```

**See:** [GITOPS_CONFLICT.md](GITOPS_CONFLICT.md) for detailed explanation and alternatives.

---

## Rollout Configuration

### Basic Rollout (rollout.yaml)

```yaml
strategy:
  canary:
    canaryService: rollo-demo-3-canary
    stableService: rollo-demo-3-stable
    # No trafficRouting = replica-based
    steps:
      - setWeight: 20   # 1 canary pod, 4 stable pods
      - pause: {}       # Manual pause
      - setWeight: 40   # 2 canary pods, 3 stable pods
      - pause: { duration: 10 }
      - setWeight: 60
      - pause: { duration: 10 }
      - setWeight: 80
      - pause: { duration: 10 }
```

### Istio Rollout (rollout-istio.yaml)

```yaml
strategy:
  canary:
    canaryService: rollo-demo-3-canary
    stableService: rollo-demo-3-stable
    trafficRouting:
      istio:
        virtualService:
          name: rollo-demo-3-vsvc
          routes:
            - primary
    steps:
      - setWeight: 20   # VirtualService: stable=80, canary=20
      - pause: {}
      - setWeight: 40   # VirtualService: stable=60, canary=40
      - pause: { duration: 10 }
      # ... etc
```

---

## Troubleshooting

### Rollout paused but VirtualService weights not changing

**Check:**

```bash
# Verify trafficRouting is configured
oc get rollout rollo-demo-3 -n rollo-demo -o yaml | grep -A5 trafficRouting

# Check VirtualService exists and name matches
oc get virtualservice -n rollo-demo

# Check for errors in Rollout
oc describe rollout rollo-demo-3 -n rollo-demo
```

**Common issues:**

- VirtualService name mismatch
- Route name mismatch (must match `http[].name` in VirtualService)
- Waypoint not programmed

### Waypoint not ready

```bash
# Check waypoint status
oc get gateway rollo-demo-waypoint -n rollo-demo

# Check namespace labels
oc get namespace rollo-demo --show-labels

# Required labels:
# - istio.io/dataplane-mode=ambient
# - istio-discovery=enabled
# - istio.io/use-waypoint=rollo-demo-waypoint
```

### Pods stuck on Init:0/2

This happens when pods try to use sidecar mode instead of ambient mode.

**Solution:** Ensure waypoint is deployed and namespace has correct labels (see upgrade steps above).

---

## Cleanup

```bash
# Delete Rollout (scales down all pods)
oc delete rollout rollo-demo-3 -n rollo-demo

# Delete Istio resources (if Service Mesh was added)
oc delete virtualservice rollo-demo-3-vsvc -n rollo-demo
oc delete gateway rollo-demo-waypoint -n rollo-demo

# Delete services
oc delete -f services.yaml

# Delete namespace
oc delete namespace rollo-demo

# ALTERNATIVE: Just remove labels
oc label namespace rollo-demo \
  istio.io/dataplane-mode- \
  istio-discovery- \
  istio.io/use-waypoint-
```

---

## Files in This Demo

### Basic Canary (Replica-Based)

- `services.yaml` - Stable and canary services
- `rollout.yaml` - Rollout without traffic routing
- `routes.yaml` - OpenShift Route (optional, for external access)

### Service Mesh Upgrade

- `waypoint.yaml` - Waypoint proxy for ambient mode
- `virtualservice.yaml` - VirtualService (managed by Argo Rollouts)
- `rollout-istio.yaml` - Rollout with Istio traffic routing

### Documentation

- `README.md` - This file (consolidated guide)
- `GITOPS_CONFLICT.md` - Argo CD integration guide

---

## Summary

**Demo 3 shows progressive canary delivery in two flavors:**

1. **Basic (Replica-Based)**
  - Simple setup, no dependencies
  - Approximate traffic percentages
  - Good for learning and non-critical apps
2. **Production (Service Mesh)**
  - Exact traffic percentages
  - Argo Rollouts automatically manages VirtualService
  - Production-ready pattern for critical applications

**Key Takeaway:** The Rollout API requires two services; the controller assigns each to one ReplicaSet. Without traffic routing, the Route must split (e.g. 80/20) to send actual traffic to the canary. With Istio, the VirtualService weights are updated automatically at each step for exact control.

**This extends far beyond DeploymentConfig's rolling strategy** - you get pause points, exact traffic control, and the ability to abort at any step.