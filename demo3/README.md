# Demo 3: Canary with steps

**Goal:** Progressive traffic steps with manual and timed pauses.

## Apply order

```bash
kubectl apply -f namespace.yaml
kubectl apply -f services.yaml
kubectl apply -f rollout.yaml
```

## Watch

```bash
kubectl argo rollouts get rollout rollo-demo-3 -n rollo-demo --watch
```

## Trigger update

```bash
kubectl argo rollouts set image rollo-demo-3 rollo=quay.io/modzelewski/rollo:v2 -n rollo-demo
```

Rollout will pause at 20% (manual step). Then:

```bash
kubectl argo rollouts promote rollo-demo-3 -n rollo-demo
```

Further steps (40%, 60%, 80%) have duration pauses and will complete automatically.

## Abort (optional)

```bash
kubectl argo rollouts abort rollo-demo-3 -n rollo-demo
```
