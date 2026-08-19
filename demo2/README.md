# Demo 2: Blue-Green with manual promotion

**Goal:** Deploy the new version to a preview environment; test it; then promote to production.

**Key Message:** Blue-Green replaces the manual pattern of "two DeploymentConfigs + switch route" with a first-class Rollout strategy.

**Namespace:** `argo-rollouts-demo-2`

---

## Namespace

If you skipped `./0_bootstrap.sh`, apply namespaces once: `oc apply -f ../demo0-prep/namespace.yaml`.

---

## What is Blue-Green?

**Two services:**
- **Active Service** → production traffic (current stable version)
- **Preview Service** → new version only (for testing before it goes live)

**Process:**
1. Deploy new version → preview gets v2, active stays on v1
2. Test on preview
3. Promote → active switches to v2
4. (Optional) Abort → active stays on v1

**Replaces:** managing two DeploymentConfigs and manually switching Routes.

---

## Apply

From `demo2/`:

```bash
oc apply -f .
```

Watch status:

```bash
oc argo rollouts get rollout rollo-demo-2 -n argo-rollouts-demo-2 --watch
```

**What you'll see:**
- Rollout: Healthy
- Services: `rollo-demo-2-active` (production), `rollo-demo-2-preview` (testing)
- Pods: 3/3 on v1

---

## Access via Routes

```bash
oc get routes -n argo-rollouts-demo-2
```

**Example:**
```
NAME                     HOST/PORT
rollo-demo-2-active      rollo-demo-2-active-argo-rollouts-demo-2.apps...
rollo-demo-2-preview     rollo-demo-2-preview-argo-rollouts-demo-2.apps...
```

Open both URLs in the browser:
- **Active (production):** `https://rollo-demo-2-active-argo-rollouts-demo-2.apps.your-cluster.com` → v1 (blue UI)
- **Preview (testing):** `https://rollo-demo-2-preview-argo-rollouts-demo-2.apps.your-cluster.com` → v1 (blue UI) initially

Keep both tabs open side-by-side.

---

## Trigger v2

Live demo (in-cluster image change):

```bash
oc argo rollouts set image rollo-demo-2 rollo=quay.io/modzelewski/rollo:v2 -n argo-rollouts-demo-2
```

In production, GitOps does the same thing: change the image in Git (or `oc apply -f rollout.yaml`) and the controller rolls out.

**What happens:**
1. New ReplicaSet created for v2
2. New pods spin up
3. **Preview service** points at v2
4. **Active service** stays on v1
5. Status: **Paused** (`BlueGreenPause`)

**Watch:**
- Active revision: still v1
- Preview revision: v2

**Browser:**
- Refresh preview Route → v2 (yellow UI)
- Active Route → still v1 (blue UI)

Production traffic is unaffected.

---

## Promote to production

```bash
oc argo rollouts promote rollo-demo-2 -n argo-rollouts-demo-2
```

**What happens:**
1. Active service switches to v2
2. Old v1 ReplicaSet scales down after `scaleDownDelaySeconds` (30s)
3. Status: Healthy

**Browser:** refresh the active Route → v2 (yellow UI).

---

## Abort / rollback (optional)

If preview looks wrong and you have **not** promoted:

```bash
oc argo rollouts abort rollo-demo-2 -n argo-rollouts-demo-2
```

Active stays on v1; the v2 ReplicaSet scales down.

If you already promoted:

```bash
oc argo rollouts undo rollo-demo-2 -n argo-rollouts-demo-2
```

Or in GitOps: revert the Git commit that changed the image.

---

## Key configuration

```yaml
strategy:
  blueGreen:
    activeService: rollo-demo-2-active
    previewService: rollo-demo-2-preview
    autoPromotionEnabled: false
    scaleDownDelaySeconds: 30
```

`autoPromotionEnabled: false` means you control when production switches.

---

## Key Takeaways

1. **Two services** (active + preview) give you a built-in testing environment
2. **Manual promotion** controls when production traffic switches
3. **Replaces** "two DCs + manual route switching"
4. **Safe:** production stays on stable until you promote

---

## Next Steps

- Try **[Demo 3](../demo3/)** for Canary with steps (controller changes Service selectors)
- See **[FAQ.md](../FAQ.md)** for common questions about Blue-Green
