# Demo 3: Canary with steps (replica counts and Service selectors)

**Goal:** Progressive canary with pause points (20% → 40% → 60% → 80% → 100%) by changing **which pods sit behind each Service**.

**Key Message:** The Rollout controller rewrites `stable` and `canary` Service selectors so each Service owns one ReplicaSet. Five replicas make the math obvious (20% = 1 canary pod). Route weights are **not** part of this demo.

**Namespace:** `argo-rollouts-demo-3`

---

## Namespace

If you skipped `./0_bootstrap.sh`, apply namespaces once: `oc apply -f ../demo0-prep/namespace.yaml`.

---

## What you are seeing

Two Services (`rollo-demo-3-stable`, `rollo-demo-3-canary`). At each `setWeight` step the controller:

- Scales ReplicaSets (20% weight → 1 canary + 4 stable)
- Sets the stable Service selector → stable ReplicaSet only
- Sets the canary Service selector → canary ReplicaSet only

Two Routes, each 100% to one Service. Those weights never change. Pod counts are not the same as 40/60/80% of HTTP traffic on a single URL. **[Demo 4](../demo4/) + Service Mesh 3** is how HTTPRoute weights move 20→40→60→80.

---

## Apply

From `demo3/`:

```bash
oc apply -f .
```

Watch status:

```bash
oc argo rollouts get rollout rollo-demo-3 -n argo-rollouts-demo-3 --watch
```

**What you'll see:**
- Rollout: Healthy
- Replicas: 5/5 on v1
- Image: `quay.io/modzelewski/rollo:v1` (blue-style UI)

---

## Trigger v2

Live demo (in-cluster image change):

```bash
oc argo rollouts set image rollo-demo-3 rollo=quay.io/modzelewski/rollo:v2 -n argo-rollouts-demo-3
```

In production, GitOps does the same thing: change the image in Git (or `oc apply -f rollout.yaml`) and the controller rolls out.

The rollout **pauses** at 20% (step 1/8, `SetWeight: 20`).

```bash
oc get pods -n argo-rollouts-demo-3 -l app=rollo-demo-3
oc get svc -n argo-rollouts-demo-3 -o wide
```

Expected at the pause: 1 canary pod (v2), 4 stable pods (v1). Each Service selector points at one ReplicaSet.

```bash
oc get routes -n argo-rollouts-demo-3
```

- Stable Route → v1 (blue UI)
- Canary Route → v2 (yellow UI)

Promote to continue; later steps auto-advance after 10s:

```bash
oc argo rollouts promote rollo-demo-3 -n argo-rollouts-demo-3
```

Watch replica counts: 40% (2/3) → 60% (3/2) → 80% (4/1) → 100% (5/0). Status: Healthy, all on v2.

---

## Abort / rollback (optional)

```bash
oc argo rollouts abort rollo-demo-3 -n argo-rollouts-demo-3
```

```bash
oc argo rollouts undo rollo-demo-3 -n argo-rollouts-demo-3
```

Or in GitOps: revert the Git commit that changed the image.

---

## Key Takeaways

1. **Canary with steps** gives pause points DeploymentConfig rolling never had
2. **Services change** (selectors + replica counts), not Route weights
3. **Five replicas** make 20% = 1 pod visible in `oc get pods`
4. **Exact HTTP percentages** need a traffic manager — that is [Demo 4](../demo4/)

---

## Next Steps

- Try **[Demo 4](../demo4/)** for Service Mesh 3 ambient (HTTPRoute weights, Gateway API north-south)
- See **[FAQ.md](../FAQ.md)** for traffic splitting and other migration questions
