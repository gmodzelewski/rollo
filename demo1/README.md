# Demo 1: First Rollout (rolling-update style)

**Goal:** Show that a Rollout is a drop-in replacement for a Deployment (or DeploymentConfig). Same pod template, different resource kind + strategy.

**Key Message:** Migration from DC to Rollout is mostly a YAML change; no application code changes needed.

---

## Visual Comparison: Before → After

### Before (DeploymentConfig)
```yaml
apiVersion: apps.openshift.io/v1
kind: DeploymentConfig
metadata:
  name: my-app
spec:
  template:
    spec:
      containers:
        - name: my-app
          image: quay.io/myorg/my-app:v1
          ports: [...]
  triggers:
    - type: ImageChange      # ← Auto-deploy when image changes
  strategy:
    type: Rolling
    rollingParams:
      maxSurge: 25%
      maxUnavailable: 25%
```

### After (Rollout)
```yaml
apiVersion: argoproj.io/v1alpha1
kind: Rollout
metadata:
  name: my-app
spec:
  template:
    spec:
      containers:
        - name: my-app
          image: quay.io/myorg/my-app:v1  # ← Same pod template!
          ports: [...]
  # No triggers (replaced by GitOps)
  strategy:
    canary:                  # ← New control mechanism
      steps: []              # Empty = rolling-update behavior
      maxSurge: 25%
      maxUnavailable: 25%
```

**What changed:**
- `kind: DeploymentConfig` → `kind: Rollout`
- `triggers:` section removed (replaced by GitOps updating the manifest)
- `strategy:` changed to canary with empty steps (same rolling-update behavior)
- Pod template (`spec.template`) is **identical**

---

## Apply Demo

### 1. Create namespace and resources
```bash
oc apply -f namespace.yaml
oc apply -f service.yaml
oc apply -f rollout.yaml
oc apply -f route.yaml  # OpenShift Route for external access
```

### 2. Watch rollout status
```bash
oc argo rollouts get rollout rollo-demo-1 -n rollo-demo --watch
```

**What you'll see:**
- Rollout: Healthy
- ReplicaSet: 1 (current revision)
- Pods: 3/3 ready

---

## Trigger New Version (v2)

### Option 1: Using oc argo rollouts (recommended for demo)
```bash
oc argo rollouts set image rollo-demo-1 rollo=quay.io/modzelewski/rollo:v2 -n rollo-demo
```

### Option 2: Edit manifest and apply (GitOps way)
```bash
# Edit rollout.yaml: change image to quay.io/modzelewski/rollo:v2
oc apply -f rollout.yaml
```

**What happens:**
1. New ReplicaSet created for v2
2. Pods roll out one by one (respecting maxSurge/maxUnavailable)
3. Old ReplicaSet scaled down
4. No manual promotion needed (canary with empty steps = automatic)

**Watch the rollout in the terminal** (Pane A)—you'll see:
- New ReplicaSet appears
- Pods transition from v1 to v2
- Rollout completes: Healthy, all replicas on v2

---

## Access the Application

Get the Route URL:
```bash
oc get route rollo-demo-1 -n rollo-demo
```

Open the URL in your browser to see the application UI.

---

## Rollback to v1 (if needed)

```bash
oc argo rollouts undo rollo-demo-1 -n rollo-demo
```

Or in GitOps: revert the Git commit that changed the image.

---

## Images

- `quay.io/modzelewski/rollo:v1` – first version (blue-style UI)
- `quay.io/modzelewski/rollo:v2` – second version (yellow-style UI)

---

## Key Takeaways

1. **Same pod template** as Deployment/DeploymentConfig—no app changes
2. **New resource kind** (Rollout) with configurable strategy
3. **"New image" flow:**
   - DeploymentConfig: ImageChange trigger → auto-deploys
   - Rollout: GitOps updates spec → controller rolls out (same outcome, better audit trail)
4. **Rolling update** is just "canary with no steps"—simple migration path

---

## Next Steps

- Try **Demo 2** for Blue-Green (preview-then-promote)
- Try **Demo 3** for Canary with steps (progressive rollout)
- See [../conversion-example.md](../conversion-example.md) for more DC → Rollout examples
