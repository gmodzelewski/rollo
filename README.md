# rollo – Argo Rollouts demos (DeploymentConfig migration)

Demo app and manifests for **Argo Rollouts** on OpenShift with OpenShift GitOps.

**Key Message:** OpenShift recommends migrating from DeploymentConfigs to Deployments. We recommend going one step further: **skip plain Deployments and use Argo Rollouts**. Rollouts manage ReplicaSets the same way Deployments do, plus Blue-Green, Canary, and progressive delivery.

**Images:** `quay.io/modzelewski/rollo:v1`, `quay.io/modzelewski/rollo:v2` (from [argoproj/rollouts-demo](https://hub.docker.com/r/argoproj/rollouts-demo)).

---

## Quick Start

1. **Initial state:** `./0_bootstrap.sh` (namespaces + demo1–4 YAML at v1, including demo4 ambient labels)
2. Run the demos in order (each folder README is the live `set image` / promote path):
   - [demo1/](demo1/) rolling-update style
   - [demo2/](demo2/) Blue-Green (preview then promote)
   - [demo3/](demo3/) Canary steps (Service selectors)
   - [demo4/](demo4/) Canary with Service Mesh 3 (HTTPRoute weights)
3. **Tear down:** `./0_destroy.sh` (deletes the four demo namespaces)
4. [FAQ.md](FAQ.md) for common questions

Each demo runs in its own namespace (`argo-rollouts-demo-1` … `argo-rollouts-demo-4`). Commands use `oc`. Live demos use `oc argo rollouts set image`; in production the same image change lives in Git.

---

## Demos

| Demo | Folder | What it proves | Strategy | Objects |
|------|--------|----------------|----------|---------|
| 1 | [demo1/](demo1/) | Rollout is a drop-in rolling update | Canary (empty steps) | Rollout, Service, Route |
| 2 | [demo2/](demo2/) | Preview then promote | Blue-Green | Rollout, Services (2), Routes (2) |
| 3 | [demo3/](demo3/) | Controller changes Service selectors; Route weights do not follow steps | Canary (with steps) | Rollout, Services (2), Routes (2) |
| 4 | [demo4/](demo4/) | Exact 20→40→60→80 via HTTPRoute (waypoint); N-S is Gateway API | Canary + Istio Gateway (ambient) | Rollout, Services (2), waypoint, Istio Gateway, HTTPRoutes, Route to Istio Gateway Service |

---

## Prerequisites

- OpenShift 4.x
- OpenShift GitOps operator
- Argo Rollouts via a **RolloutManager** in OpenShift GitOps
- `oc` with the Argo Rollouts plugin
- **Demo 4 only:** OpenShift Service Mesh 3 (Istio in `istio-system`); Red Hat Connectivity Link if present is the **policy** layer (Kuadrant), not the ingress proxy. Apply [demo0-prep/gatewayapi-plugin.yaml](demo0-prep/gatewayapi-plugin.yaml) once and add **only** `argoproj-labs/gatewayAPI` to the existing RolloutManager (the operator rejects listing the built-in OpenShift plugin in `spec.plugins`).

### RolloutManager

Name on the cluster varies (`argo-rollout` on some installs). Find yours with `oc get rolloutmanager -A`. Example:

```yaml
apiVersion: argoproj.io/v1alpha1
kind: RolloutManager
metadata:
  name: rollout-manager
  namespace: openshift-gitops
```

### Rollouts UI in Argo CD (optional)

Patch the Argo CD CR (`spec.server.enableRolloutsUI: true`). The server pod restarts. The UI needs an Application that includes Rollout resources.

See [Red Hat OpenShift GitOps: Argo Rollouts](https://docs.redhat.com/en/documentation/red_hat_openshift_gitops/1.19/html/argo_rollouts).

### CLI plugin

```bash
# Mac
brew install argoproj/tap/kubectl-argo-rollouts

# Linux
curl -LO https://github.com/argoproj/argo-rollouts/releases/latest/download/kubectl-argo-rollouts-linux-amd64
chmod +x kubectl-argo-rollouts-linux-amd64
sudo mv kubectl-argo-rollouts-linux-amd64 /usr/local/bin/kubectl-argo-rollouts

oc argo rollouts version
```

---

## DeploymentConfig to Rollout

**Why skip Deployments?** Rollouts do what Deployments do (ReplicaSets, rolling updates, rollback) plus Blue-Green, stepped canary, analysis, and traffic management. Details: [FAQ.md](FAQ.md).

**Conversion (no application code changes):**

1. Copy `spec.template` (pod template stays identical)
2. Change `kind:` from `DeploymentConfig` to `Rollout`
3. Remove `triggers:` (replace with GitOps)
4. Add `strategy:` (`canary` or `blueGreen`)
5. Services: 1 for rolling (demo1), 2 for blue-green (demo2) and canary (demo3/4)
6. Test on a non-prod app first

Official DC vs Deployment: [OpenShift docs](https://docs.openshift.com/container-platform/latest/applications/deployments/what-deployments-are.html).

---

## Resources

- [Red Hat OpenShift GitOps – Argo Rollouts](https://docs.redhat.com/en/documentation/red_hat_openshift_gitops/1.19/html/argo_rollouts)
- [Argo Rollouts docs](https://argo-rollouts.readthedocs.io/en/stable/)
- [CNCF Argo Slack #argo-rollouts](https://argoproj.github.io/community/join-slack/)
- [argoproj/argo-rollouts](https://github.com/argoproj/argo-rollouts)
