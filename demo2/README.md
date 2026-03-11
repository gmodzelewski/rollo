# Demo 2: Blue-Green with manual promotion

**Goal:** Preview new version on preview service; promote to switch production traffic.

## Apply order

```bash
kubectl apply -f namespace.yaml
kubectl apply -f services.yaml
kubectl apply -f rollout.yaml
```

## Watch

```bash
kubectl argo rollouts get rollout rollo-demo-2 -n rollo-demo --watch
```

## Deploy new version

```bash
kubectl argo rollouts set image rollo-demo-2 rollo=quay.io/modzelewski/rollo:v2 -n rollo-demo
```

- **Active** service keeps serving v1; **preview** service serves v2.
- When satisfied, promote:

```bash
kubectl argo rollouts promote rollo-demo-2 -n rollo-demo
```

## Abort (optional)

```bash
kubectl argo rollouts abort rollo-demo-2 -n rollo-demo
```
