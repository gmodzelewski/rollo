# Demo 1: First Rollout (rolling-update style)

**Goal:** Rollout as drop-in for Deployment; show what happens when a new image version is deployed.

## Apply order

```bash
kubectl apply -f namespace.yaml
kubectl apply -f service.yaml
kubectl apply -f rollout.yaml
```

## Show rollout

```bash
kubectl argo rollouts get rollout rollo-demo-1 -n rollo-demo --watch
```

## Trigger new version (v2)

```bash
kubectl argo rollouts set image rollo-demo-1 rollo=quay.io/modzelewski/rollo:v2 -n rollo-demo
```

Or edit `rollout.yaml` (image → `quay.io/modzelewski/rollo:v2`) and `kubectl apply -f rollout.yaml`.

## Images

- `quay.io/modzelewski/rollo:v1` – first version (blue-style UI)
- `quay.io/modzelewski/rollo:v2` – second version (yellow-style UI)
