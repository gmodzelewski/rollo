# Demo 2: Blue-Green with manual promotion

**Goal:** Deploy new version to preview environment; test it; then promote to production.

**Key Message:** Blue-Green replaces the manual pattern of "two DeploymentConfigs + switch route" with a first-class Rollout strategy.

---

## What is Blue-Green?

**Two services:**
- **Active Service** → Production traffic (current stable version)
- **Preview Service** → New version only (for testing before it goes live)

**Process:**
1. Deploy new version → preview service gets v2, active stays on v1
2. Test new version on preview (smoke tests, manual QA, etc.)
3. Promote → active service switches to v2
4. (Optional) Abort if something is wrong → active stays on v1

**Replaces:** The pattern of managing two separate DeploymentConfigs and manually switching Routes.

---

## Apply Demo

### 1. Create services and rollout
```bash
oc apply -f services.yaml
oc apply -f rollout.yaml
oc apply -f routes.yaml
```

### 2. Watch rollout status
```bash
oc argo rollouts get rollout rollo-demo-2 -n rollo-demo --watch
```

**What you'll see:**
- Rollout: Healthy
- Services: rollo-demo-2-active (production), rollo-demo-2-preview (testing)
- Pods: 3/3 on current version (v1)

---

## Visual Demo: Access via OpenShift Routes

The Routes are already created via `routes.yaml`. Get the URLs:

```bash
oc get routes -n rollo-demo
```

**Output:**
```
NAME                     HOST/PORT
rollo-demo-2-active      rollo-demo-2-active-rollo-demo.apps...
rollo-demo-2-preview     rollo-demo-2-preview-rollo-demo.apps...
```

**Open both Routes in your browser:**
- **Active (production):** `https://rollo-demo-2-active-rollo-demo.apps.your-cluster.com` → shows v1 (blue UI)
- **Preview (testing):** `https://rollo-demo-2-preview-rollo-demo.apps.your-cluster.com` → shows v1 (blue UI) initially

**Tip:** Keep both browser tabs open side-by-side during the demo to show the difference.

---

## Deploy New Version

```bash
oc argo rollouts set image rollo-demo-2 rollo=quay.io/modzelewski/rollo:v2 -n rollo-demo
```

**What happens:**
1. Rollout creates new ReplicaSet with v2 image
2. New pods spin up
3. **Preview service** automatically points to v2 pods
4. **Active service** stays pointing to v1 pods
5. Rollout status: **Paused** (waiting for manual promotion)

**Watch in terminal** (Pane A):
- Status: `Paused`, message: "BlueGreenPause"
- Active revision: still v1
- Preview revision: v2

**Check in browser:**
- Refresh http://localhost:8081 (preview) → **now shows v2 (yellow UI)**
- Check http://localhost:8080 (active) → **still shows v1 (blue UI)**

**Production traffic is unaffected.** You can now test v2 on preview.

---

## Promote to Production

When you're satisfied with the new version:

```bash
oc argo rollouts promote rollo-demo-2 -n rollo-demo
```

**What happens:**
1. Active service switches to point at v2 pods
2. Old v1 ReplicaSet scales down (after scaleDownDelaySeconds)
3. Rollout status: Healthy

**Check in browser:**
- Refresh http://localhost:8080 (active) → **now shows v2 (yellow UI)**
- Production traffic is now on v2

**Rollout complete!**

---

## Abort (if something is wrong)

If you see issues on preview and don't want to promote:

```bash
oc argo rollouts abort rollo-demo-2 -n rollo-demo
```

**What happens:**
- Rollout aborts
- Active service stays on v1
- New v2 ReplicaSet is scaled down
- You can fix the issue and deploy again

---

## Rollback After Promotion

If you already promoted but need to rollback:

```bash
oc argo rollouts undo rollo-demo-2 -n rollo-demo
```

Or revert the Git commit (GitOps way).

---

## Key Configuration

From `rollout.yaml`:
```yaml
strategy:
  blueGreen:
    activeService: rollo-demo-2-active    # Production traffic
    previewService: rollo-demo-2-preview  # Testing traffic
    autoPromotionEnabled: false           # Manual promotion required
    scaleDownDelaySeconds: 30             # Keep old version for 30s after promotion
```

**autoPromotionEnabled: false** means you control when production switches. Set to `true` for automatic promotion after a delay (less common).

---

## Key Takeaways

1. **Two services** (active + preview) give you a built-in testing environment
2. **Manual promotion** ensures you control when production traffic switches
3. **Replaces** the pattern of "two DCs + manual route switching"
4. **Visual confirmation** (browser) is powerful for demos and validation
5. **Safe**: Production stays on stable until you explicitly promote

---

## Next Steps

- Try **Demo 3** for Canary with progressive rollout steps
- See [../conversion-example.md](../conversion-example.md) for Blue-Green YAML examples
- See [../FAQ.md](../FAQ.md) for common questions about Blue-Green strategy
