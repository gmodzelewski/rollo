# rollo – Argo Rollouts demos (DeploymentConfig migration)

Demo app and manifests for **Argo Rollouts** on OpenShift.

**Key Message:** OpenShift recommends migrating from DeploymentConfigs to Deployments. We recommend going one step further: **skip plain Deployments and use Argo Rollouts** instead. Rollouts manage ReplicaSets the same way Deployments do, but with Blue-Green, Canary, and progressive delivery built-in.

**Images:** `quay.io/modzelewski/rollo:v1`, `quay.io/modzelewski/rollo:v2` (from [argoproj/rollouts-demo](https://hub.docker.com/r/argoproj/rollouts-demo)).

---

## Quick Start

1. **Complete presentation script:** [PRESENTATION_SCRIPT.md](PRESENTATION_SCRIPT.md) ⭐ **NEW** - One file with everything: slides, demos, talking points, commands
2. **Read the plan:** [ARGO_ROLLOUTS_DEMO_PLAN.md](ARGO_ROLLOUTS_DEMO_PLAN.md) (original demo strategy)
3. **Run the demos:** See table below
4. **Learn migration:** [conversion-example.md](conversion-example.md) (DC → Rollout examples)
5. **Get answers:** [FAQ.md](FAQ.md) (common questions)

---

## Demos

| Demo | Folder | Description | Strategy | Includes | Duration |
|------|--------|-------------|----------|----------|----------|
| 1 | [demo1/](demo1/) | First Rollout – rolling-update style | Canary (no steps) | Rollout, Service, Route | 8 min |
| 2 | [demo2/](demo2/) | Blue-Green – preview then promote | Blue-Green | Rollout, Services (2), Routes (2) | 8 min |
| 3 | [demo3/](demo3/) | Canary with steps – progressive rollout | Canary (with steps) | Rollout, Services (2), Routes (2) | 8 min |
| 4* | [demo4/](demo4/) | Auto-deploy with Image Updater | Canary (no steps) | Rollout, Service, Route, Application | 10 min |

**Demo 4 is optional** - use it if:
- Your audience heavily relies on DeploymentConfig ImageChange triggers
- You want to show the complete GitOps automation story
- Time permits (adds 10 minutes to presentation)

Each `demoN/` has a detailed README with:
- Apply order and commands (using `oc` CLI)
- OpenShift Route setup for external access
- Visual examples and browser demo instructions
- Key takeaways and next steps

**Namespace:** All demos use `rollo-demo` namespace. You can run all simultaneously (different Rollout names).

**OpenShift-specific:** All demos include OpenShift Routes for external access. Commands use `oc` CLI.

## Prerequisites

- **OpenShift cluster** (4.x or later)
- **OpenShift GitOps** operator installed
- **Argo Rollouts** controller (via RolloutManager in OpenShift GitOps)
- **`oc` CLI** with Argo Rollouts plugin

Install Argo Rollouts plugin (Mac/Linux):

```bash
# Mac
brew install argoproj/tap/kubectl-argo-rollouts

# Linux
curl -LO https://github.com/argoproj/argo-rollouts/releases/latest/download/kubectl-argo-rollouts-linux-amd64
chmod +x kubectl-argo-rollouts-linux-amd64
sudo mv kubectl-argo-rollouts-linux-amd64 /usr/local/bin/kubectl-argo-rollouts

# Verify
oc argo rollouts version
```


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



---

## DeploymentConfig to Rollout Migration

### Why migrate?

- **DeploymentConfigs are deprecated** – OpenShift recommends Kubernetes Deployments
- **Why skip to Rollouts instead?** – Rollouts do everything Deployments do, plus:
  - Blue-Green deployments with preview/active services
  - Canary with progressive steps and manual promotion
  - Analysis runs for automated metric-based decisions
  - Traffic management integration (Routes, Service Mesh)
- **Same foundation:** Both Deployments and Rollouts manage ReplicaSets the same way
- **Better GitOps integration:** Desired state in Git, not ephemeral triggers
- **Active community:** CNCF project with ongoing feature development

**You don't lose anything by skipping Deployments.** See [FAQ.md](FAQ.md) for details.

### Migration guides

| Document | Description |
|----------|-------------|
| [conversion-example.md](conversion-example.md) | Practical examples: DC → Rollout (rolling, blue-green, canary) |
| [FAQ.md](FAQ.md) | Common questions: ImageChange triggers, hooks, traffic splitting, etc. |
| [OpenShift Docs](https://docs.openshift.com/container-platform/latest/applications/deployments/what-deployments-are.html) | Official DC vs Deployment comparison |

### Quick conversion steps

**Migration path:** DC → Rollout (skip Deployment)

1. **Copy `spec.template`** from DeploymentConfig (pod template is identical)
2. **Change `kind:`** from `DeploymentConfig` to `Rollout`
3. **Remove `triggers:`** section (replace with GitOps automation)
4. **Add `strategy:`** section (canary/blueGreen)
5. **Create Services** as needed (1 for rolling, 2 for blue-green/canary)
6. **Test** on non-prod app first

**No application code changes needed.** Only infrastructure manifests change.

**Why not DC → Deployment → Rollout?** Because Rollouts give you everything Deployments do plus advanced strategies. Why migrate twice when you can do it once?

---

## Resources and Documentation

Sources:
- https://argo-rollouts.readthedocs.io/en/stable/installation/
- https://github.com/SMACAcademy/Mastering-Argo-Rollouts-Progressive-Delivery-in-Kubernetes
- https://docs.redhat.com/en/documentation/red_hat_openshift_gitops/1.19/html/argo_rollouts
- https://docs.openshift.com/container-platform/latest/applications/deployments/what-deployments-are.html

Community:
- CNCF Argo Slack: #argo-rollouts (https://argoproj.github.io/community/join-slack/)
- GitHub: https://github.com/argoproj/argo-rollouts
