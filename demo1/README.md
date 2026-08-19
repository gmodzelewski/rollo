# Demo 1: First Rollout (rolling-update style)

**Goal:** Show that a Rollout is a drop-in replacement for a Deployment (or DeploymentConfig). Same pod template, different resource kind + strategy.

**Key Message:** Migration from DC to Rollout is mostly a YAML change; no application code changes needed.

**Namespace:** `argo-rollouts-demo-1`

---

## Namespace

If you skipped `./0_bootstrap.sh`, apply namespaces once: `oc apply -f ../demo0-prep/namespace.yaml`.

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
      maxSurge: "25%"
      maxUnavailable: "25%"
      steps: []              # Empty = rolling-update behavior
```

**What changed:**
- `kind: DeploymentConfig` → `kind: Rollout`
- `triggers:` section removed (replaced by GitOps updating the manifest)
- `strategy:` changed to canary with empty steps (same rolling-update behavior)
- Pod template (`spec.template`) is **identical**

---

## Apply

From `demo1/`:

```bash
oc apply -f .
```

Watch status:

```bash
oc argo rollouts get rollout rollo-demo-1 -n argo-rollouts-demo-1 --watch
```

**What you'll see:**
- Rollout: Healthy
- ReplicaSet: 1 (current revision)
- Pods: 3/3 ready
- Image: `quay.io/modzelewski/rollo:v1` (blue-style UI)

---

## Trigger v2

Live demo (in-cluster image change):

```bash
oc argo rollouts set image rollo-demo-1 rollo=quay.io/modzelewski/rollo:v2 -n argo-rollouts-demo-1
```

In production, GitOps does the same thing: change the image in Git (or `oc apply -f rollout.yaml`) and the controller rolls out.

**What happens:**
1. New ReplicaSet created for v2
2. Pods roll (3 replicas, `maxSurge`/`maxUnavailable` 25% → at most 1 extra pod / 1 unavailable)
3. Old ReplicaSet scaled down
4. No pause, no promote — empty canary steps finish automatically

**Watch the rollout** (same `--watch` command)—you'll see:
- New ReplicaSet appears
- Pods transition from v1 to v2
- Rollout completes: Healthy, all replicas on v2

---

## Access the Application

```bash
oc get route rollo-demo-1 -n argo-rollouts-demo-1
```

Open the URL: v1 is blue-style UI, v2 is yellow-style UI.

---

## Rollback (optional)

If you need to go back to v1:

```bash
oc argo rollouts undo rollo-demo-1 -n argo-rollouts-demo-1
```

Or in GitOps: revert the Git commit that changed the image.

---

## Key Takeaways

1. **Same pod template** as Deployment/DeploymentConfig—no app changes
2. **New resource kind** (Rollout) with configurable strategy
3. **One Service, one Route** — no preview, no promote, no Service Mesh
4. **"New image" flow:**
   - DeploymentConfig: ImageChange trigger → auto-deploys
   - Rollout: GitOps updates spec → controller rolls out (same outcome, better audit trail)
   - Live demo uses `set image` so you can watch ReplicaSets move
5. **Rolling update** is just "canary with no steps"—simple migration path

---

## Next Steps

- Try **[Demo 2](../demo2/)** for Blue-Green (preview-then-promote)
- See **[FAQ.md](../FAQ.md)** for ImageChange triggers, hooks, and other migration questions
