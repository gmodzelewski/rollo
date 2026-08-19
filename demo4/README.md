# Demo 4: Canary with Service Mesh 3 (Gateway API HTTPRoute weights)

**Goal:** Same canary steps as demo 3 (20% → 40% → 60% → 80% → 100%), but Argo Rollouts **updates HTTPRoute backend weights** so HTTP traffic matches each step. North-south is Gateway API on the Istio Gateway. A Route to the app Service skips the waypoint; a Route to `rollo-demo-4-gateway-istio` is only how a browser reaches that proxy when the cluster has no LoadBalancer.

**Key Message:** Demo 3 changes Service selectors. Route-to-app weights cannot follow those steps. Service Mesh 3 ambient plus a Gateway API HTTPRoute is how you get exact 20/40/60/80. The browser URL is the Istio Gateway (via ClusterIP, or a Route to the Gateway Service).

**Namespace:** `argo-rollouts-demo-4`

---

## Three layers (do not mix them up)

| Layer | What it is | In this demo |
|-------|------------|--------------|
| **Connectivity Link (Kuadrant)** | Policy/control plane (`AuthPolicy`, `DNSPolicy`, …) | If present on the cluster. The Istio Gateway is labeled `kuadrant.io/gateway: "true"` so policies *can* attach later. **No AuthPolicy here** (would block an unauthenticated browser). Connectivity Link is **not** the ingress proxy. |
| **OSSM 3 Istio Gateway** | Data plane that terminates north-south | `gatewayClassName: istio` (`rollo-demo-4-gateway`). Not `openshift-default` — that Ingress Operator class would skip the waypoint the same way a Route-to-app does. |
| **Waypoint** | Ambient L7 | Enforces the **split** HTTPRoute (parentRef = stable Service). |

An OpenShift Route pointing at `rollo-demo-4-stable` never hits this path: the router uses pod IPs, so L7 weights are invisible. That Route is deleted on purpose.

If the platform already has a shared Connectivity Link Gateway, point [httproute-ingress.yaml](httproute-ingress.yaml) `parentRefs` at it and omit [ingress-gateway.yaml](ingress-gateway.yaml).

---

## Prerequisites

- OpenShift Service Mesh 3 and Istio in `istio-system`
- Red Hat Connectivity Link if present (policy layer; not required to create objects in this folder)
- Gateway API plugin + HTTPRoute RBAC (once per cluster):

```bash
oc apply -f ../demo0-prep/gatewayapi-plugin.yaml
# Then add only argoproj-labs/gatewayAPI to the existing RolloutManager
# (see that file). Do not list the built-in OpenShift plugin in spec.plugins.
# Restart deploy/argo-rollouts in openshift-gitops after the ConfigMap updates.
```

- Namespaces:

```bash
oc apply -f ../demo0-prep/namespace.yaml
```

---

## Enable ambient mode

Label **this** namespace only:

```bash
oc label namespace argo-rollouts-demo-4 \
  istio.io/dataplane-mode=ambient \
  istio-discovery=enabled \
  istio.io/use-waypoint=rollo-demo-4-waypoint \
  --overwrite
```

---

## Apply

From `demo4/` (or `./0_bootstrap.sh` from the repo root):

```bash
oc apply -f .
```

Wait until both Gateways are programmed:

```bash
oc get gateway -n argo-rollouts-demo-4
```

Expected: `rollo-demo-4-waypoint` and `rollo-demo-4-gateway` show `PROGRAMMED=True`.

---

## Watch three panes

**Pane A — rollout:**

```bash
oc argo rollouts get rollout rollo-demo-4 -n argo-rollouts-demo-4 --watch
```

**Pane B — split HTTPRoute weights (the point of this demo):**

```bash
watch -n 2 "oc get httproute rollo-demo-4-split -n argo-rollouts-demo-4 \
  -o jsonpath='Stable: {.spec.rules[0].backendRefs[0].weight}  Canary: {.spec.rules[0].backendRefs[1].weight}{\"\\n\"}'"
```

Initial: `Stable: 100  Canary: 0`.

**Pane C — trigger:**

```bash
oc argo rollouts set image rollo-demo-4 rollo=quay.io/modzelewski/rollo:v2 -n argo-rollouts-demo-4
```

In production, GitOps does the same thing: change the image in Git (or `oc apply -f rollout.yaml`) and the controller rolls out.

At the 20% pause, pane B should show `Stable: 80  Canary: 20` without you editing the HTTPRoute.

```bash
oc argo rollouts promote rollo-demo-4 -n argo-rollouts-demo-4
```

Pane B then auto-updates: 60/40 → 40/60 → 20/80 → 100/0 when the canary is promoted to stable.

---

## Access

The Istio Gateway is ClusterIP (`networking.istio.io/service-type`). `status.addresses` is the in-cluster Service hostname, not a browser URL:

```bash
oc get gateway rollo-demo-4-gateway -n argo-rollouts-demo-4 -o jsonpath='{.status.addresses}{"\n"}'
```

From outside the cluster, use the Route that targets the **Istio Gateway Service** (`rollo-demo-4-gateway-istio`), not the app:

```bash
oc get route rollo-demo-4-gw -n argo-rollouts-demo-4
curl -sS "https://$(oc get route rollo-demo-4-gw -n argo-rollouts-demo-4 -o jsonpath='{.spec.host}')/color"
```

At 20% you should see a mix of `"blue"` (v1) and `"yellow"` (v2). Do not Route to `rollo-demo-4-stable` — that skips the waypoint.

---

## Argo CD note

If an Argo CD Application manages these manifests, ignore HTTPRoute backend weights so sync does not fight the Rollout controller:

```yaml
ignoreDifferences:
  - group: gateway.networking.k8s.io
    kind: HTTPRoute
    name: rollo-demo-4-split
    jqPathExpressions:
      - .spec.rules[].backendRefs[].weight
```

---

## Abort (optional)

```bash
oc argo rollouts abort rollo-demo-4 -n argo-rollouts-demo-4
```

---

## Key Takeaways

1. **Same canary steps** as demo 3; the traffic object is an HTTPRoute, not a VirtualService
2. **Connectivity Link** attaches policy; **OSSM 3** is the ingress proxy; the **waypoint** applies the split
3. **Argo Rollouts** writes HTTPRoute backend weights at each `setWeight` (Gateway API plugin)
4. **No Route to the app** — that path never sees L7. The Route in this folder targets the Istio Gateway Service only.

---

## Next Steps

- See **[FAQ.md](../FAQ.md)** for traffic providers, ImageChange triggers, and other migration questions
