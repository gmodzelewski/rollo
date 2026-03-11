# rollo – Argo Rollouts demos (DC → Deployment migration)

Demo app and manifests for **Argo Rollouts** on OpenShift (migration from DeploymentConfigs to Deployments).

**Images:** `quay.io/modzelewski/rollo:v1`, `quay.io/modzelewski/rollo:v2` (from [argoproj/rollouts-demo](https://hub.docker.com/r/argoproj/rollouts-demo)).

**Plan:** See [ARGO_ROLLOUTS_DEMO_PLAN.md](ARGO_ROLLOUTS_DEMO_PLAN.md).

## Demos

| Demo | Folder | Description |
|------|--------|-------------|
| 1 | [demo1/](demo1/) | First Rollout – rolling-update style; deploy v1, then set image to v2 |
| 2 | [demo2/](demo2/) | Blue-Green – active + preview services, manual promote |
| 3 | [demo3/](demo3/) | Canary with steps – 20% → pause → 40% → … → 100% |

Each `demoN/` has a README with apply order and commands. Use one demo per namespace or apply in separate namespaces if you run multiple.

## Prerequisites

- Argo Rollouts controller (e.g. RolloutManager in OpenShift GitOps)
- `kubectl` with Argo Rollouts plugin

Install CLI (Mac):

- `brew install argoproj/tap/kubectl-argo-rollouts`
- `kubectl argo rollouts version`


Install in OpenShift GitOps:
```sh
apiVersion: argoproj.io/v1beta1
kind: ArgoCD
metadata:
  name: argocd
spec:
  server:
    enableRolloutsUI: true
```
--> restarts argocd server deployment pod
--> To access the Argo Rollouts UI in the Argo CD Web UI, configure a sample application that includes the Argo Rollouts resources.^1(https://docs.redhat.com/en/documentation/red_hat_openshift_gitops/1.19/html/argo_rollouts/using-argo-rollouts-for-progressive-deployment-delivery#gitops-installing-argo-rollouts-cli-on-mac-os_using-argo-rollouts-for-progressive-deployment-delivery)



Sources:
- https://argo-rollouts.readthedocs.io/en/stable/installation/
- https://github.com/SMACAcademy/Mastering-Argo-Rollouts-Progressive-Delivery-in-Kubernetes
- https://docs.redhat.com/en/documentation/red_hat_openshift_gitops/1.19/html/argo_rollouts
