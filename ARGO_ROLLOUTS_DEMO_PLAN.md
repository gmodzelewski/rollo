# Argo Rollouts Demo Plan: DeploymentConfig Migration

Recommendations for demos to support a presentation on **migration from DeploymentConfigs** on OpenShift, using **Argo Rollouts** (enabled via OpenShift GitOps).

**Key Message:** OpenShift recommends migrating from DeploymentConfigs to standard Kubernetes Deployments. This demo plan goes one step further: **skip plain Deployments and use Argo Rollouts instead**. Rollouts manage ReplicaSets the same way Deployments do, but with Blue-Green, Canary, and progressive delivery built-in.

**References:**
- [Red Hat OpenShift GitOps – Argo Rollouts (1.19)](https://docs.redhat.com/en/documentation/red_hat_openshift_gitops/1.19/html/argo_rollouts)
- [Argo Rollouts – Progressive Delivery](https://argo-rollouts.readthedocs.io/)

---

## 1. Best approach: multiple focused demos

Use **3–4 short demos** rather than one long one:

| Approach | Pros | Cons |
|----------|------|------|
| **Single long demo** | One narrative | Hard to re-run, risky if one step fails, too much for one slide |
| **Multiple focused demos** | Repeatable, clear learning per demo, easy to skip or reorder | Need a bit of prep (scripts/manifests) |

**Recommendation:** 3–4 demos that build on each other. Each demo is 5–10 minutes and answers one question.

---

## 2. What the demos should cover

**Order:** Start with the migration story (first rollout) so you get to the point immediately. Do installation (RolloutManager) at the **end** of the presentation—when concentration is lower and installation is not the main topic yet.

---

### Demo 1: First Rollout – "Deployment with a different kind"

**Goal:** Show that a Rollout is a drop-in replacement for a Deployment (same pod template, same `Service` idea), and what happens when a new image version is pushed.

**Demo app:** Use **`quay.io/modzelewski/rollo`**. You can base the app on the [official Argo Rollouts demo application](https://github.com/argoproj/argo-rollouts) so the UI clearly shows which version is serving (e.g. version string or color per tag).

**Cover:**
- Start from a **Deployment** (or a DeploymentConfig) and its **Service**.
- Replace the Deployment with a **Rollout** that has the same `spec.template` and `selector`.
- Use a **canary strategy with no steps** (or a single `setWeight: 100`) so behavior is "rolling update–like."
- **First deployment:** Deploy with one image tag (e.g. `quay.io/modzelewski/rollo:v1`).
- **New version:** Push a second image to the same repo with a different tag (e.g. `quay.io/modzelewski/rollo:v2`). Update the Rollout to use the new tag (via manifest change or `oc argo rollouts set image ...`) and show the rollout progressing.

**Takeaway:** Migration from Deployment/DC to Rollout is mostly a resource kind + strategy change; no app code change. "New image pushed" → update Rollout spec → controller rolls out the new version (similar outcome to DC's ImageChange trigger, but driven by GitOps or pipeline updating the Rollout).

---

### Demo 2: Blue-Green with manual promotion

**Goal:** Show controlled cutover and "preview before production."

**Cover:**
- **Rollout** with `strategy.blueGreen`:
  - `activeService`: production traffic.
  - `previewService`: new version only (for testing).
  - `autoPromotionEnabled: false` so promotion is manual.
- Deploy new version → traffic stays on active (old); new version only on preview.
- Manually **promote** (`oc argo rollouts promote <name>`) and show active switching to new version.
- Optionally show **abort** if something is wrong.

**Artifacts:** Rollout manifest + two Services (active + preview). Optional OpenShift Route pointing at the active Service.

**How this aligns to DC → Deployment migration:** See [§ 2.1](#21-how-blue-green-and-canary-relate-to-the-dc--deployment-migration) below.

---

### Demo 3: Canary with steps (and optional traffic)

**Goal:** Show progressive traffic shift and pause points.

**Cover:**
- **Rollout** with `strategy.canary` and **steps**, e.g.:
  - `setWeight: 20` → `pause: {}` (manual) → `setWeight: 40` → `pause: {duration: 10s}` → … → 100%.
- Trigger update; show rollout pausing at each step.
- Show **promote** to move past a manual pause and **abort** to roll back.
- If time: mention that **fine-grained traffic %** (e.g. true 10% canary) needs an ingress or service mesh (e.g. OpenShift Route or Service Mesh integration per [Red Hat docs](https://docs.redhat.com/en/documentation/red_hat_openshift_gitops/1.19/html/argo_rollouts)).

**Takeaway:** Canary gives control over speed and blast radius; without a traffic provider, weight is approximated by replica ratio.

**How this aligns to DC → Deployment migration:** See [§ 2.1](#21-how-blue-green-and-canary-relate-to-the-dc--deployment-migration) below.

---

#### 2.1 How Blue-Green and Canary relate to the DC migration

**What DeploymentConfigs gave you:** Rolling updates (maxSurge, maxUnavailable), optional ImageChange trigger (deploy when new image is pushed), lifecycle hooks (pre/mid/post), and a single "version" rolling out over time. DC did **not** provide built-in blue-green (two stacks, switch traffic) or percentage-based canary with pause points.

**What the migration requirement is:** Move from DeploymentConfig to standard Kubernetes **Deployment**. But we recommend **skipping Deployments** and going straight to Argo Rollouts. Why? Both Deployments and Rollouts manage ReplicaSets the same way—Rollouts just give you more control over the rollout strategy.

**Under the hood:**
- DeploymentConfig → ReplicaSet → Pods
- Deployment → ReplicaSet → Pods
- **Rollout → ReplicaSet → Pods** ← Same mechanism

| Need | DeploymentConfig | Deployment | Argo Rollouts (Rollout) |
|------|------------------|------------|-------------------------|
| Rolling update when image/spec changes | ✅ Rolling strategy | ✅ RollingUpdate | ✅ Canary with no steps (same idea) |
| "Deploy when new image is pushed" | ✅ ImageChange trigger | ❌ No built-in | ✅ GitOps + Image Updater (same outcome, better audit) |
| "Test new version before production traffic" | ❌ Manual (2 DCs + switch Route) | ❌ Manual | ✅ **Blue-Green** (active + preview, promote when ready) |
| "Roll out slowly with pause points / percentage steps" | ❌ Only maxSurge/maxUnavailable | ❌ Only maxSurge/maxUnavailable | ✅ **Canary with steps** (setWeight + pause) |
| Manual promotion/abort during rollout | ❌ No | ❌ No | ✅ Yes (promote/abort commands) |

**Summary:** Rollouts give you everything Deployments do (ReplicaSet management, rolling updates, rollback) **plus** Blue-Green and Canary strategies. They **replace** what DC did and **extend** beyond what plain Deployments offer. You're not losing capabilities by skipping Deployments—you're gaining them.

---

### Optional Demo 4: GitOps-driven rollout

**Goal:** Tie Rollouts into your existing GitOps flow.

**Cover:**
- Put Rollout (and Services) in **Git** (e.g. same repo as `wind-turbine-app` or a dedicated demo repo).
- Argo CD / ApplicationSet syncs the app; changing the image (or Rollout spec) in Git triggers the rollout.
- Show sync in Argo CD UI and rollout status with `oc argo rollouts get rollout <name> --watch`.

**Takeaway:** Rollouts fit the GitOps model: desired state in Git, operator/controller applies it.

---

### Demo last: Enable Argo Rollouts in OpenShift GitOps (installation at the end)

**Goal:** Show how to enable Rollouts when the audience is ready for it; installation is not the main topic during the migration story.

**When:** End of the presentation (when concentration is lower and "how do I install it?" is a natural closing topic).

**Cover:**
- Create a **RolloutManager** CR (cluster-scoped default), or mention namespace-scoped + feature flag if relevant.
- Verify: controller running, Rollout CRD present, optional Argo Rollouts UI in Argo CD.

**Red Hat angle:** This is the "recommended" path on OpenShift (operator-managed, no raw install.yaml).

**Example RolloutManager (minimal):**
```yaml
apiVersion: argoproj.io/v1alpha1
kind: RolloutManager
metadata:
  name: rollout-manager
  namespace: openshift-gitops   # or dedicated namespace
```

**Commands to show:**
- `oc get rolloutmanager`
- `oc get pods -n openshift-gitops | grep rollout`
- `oc get crd rollouts.argoproj.io`

---

## 3. Suggested order and time

| # | Demo | Duration | When |
|---|------|----------|------|
| 1 | First Rollout (Deployment → Rollout, two image tags) | ~10 min | **Start** — get to the point |
| 2 | Blue-Green + promote/abort | ~10 min | Core migration story |
| 3 | Canary with steps | ~10 min | Core migration story |
| 4 (optional) | GitOps-driven rollout | ~5 min | If time |
| **Last** | Enable Argo Rollouts (RolloutManager) | ~5 min | **End** — installation when it fits |

Prerequisite: Argo Rollouts controller is already installed (e.g. from a previous session or pre-installed cluster) for demos 1–4; the last demo shows how to enable it.

---

## 4. Practical tips

- **oc plugin:** Install [Argo Rollouts oc plugin](https://argo-rollouts.readthedocs.io/en/stable/installation/#oc-plugin-installation) so you can use `oc argo rollouts get rollout … --watch`, `promote`, `abort`, `set image`.
- **Demo app:** Use **`quay.io/modzelewski/rollo`** with two (or more) version tags (e.g. `v1`, `v2`). Base the app on the [official Argo Rollouts demo](https://github.com/argoproj/argo-rollouts) so the UI shows which version is serving. Push both tags to the repo before the demo so "new image pushed" is just updating the Rollout to the next tag.
- **Scripts:** Consider a small script or doc per demo (e.g. `apply rollout + service`, `set image` to next tag, `promote`) so you can re-run reliably during the presentation.
- **Slides:** One slide per demo with: goal, what you show, one key takeaway. For Blue-Green and Canary, include the migration alignment (replaces DC patterns / extends beyond DC).

---

## 5. Mapping to your narrative

- **"Why move off DeploymentConfigs?"** → OpenShift says: move to Deployments. We say: skip to Rollouts for more control and GitOps alignment.
- **"Why skip Deployments?"** → Rollouts manage ReplicaSets the same way Deployments do, but with Blue-Green, Canary, and progressive delivery. You don't lose anything; you gain features.
- **"Is migration hard?"** → Demo 1 (same pod template, new kind + strategy; two image tags show "new version" flow).
- **"What do we gain?"** → Demo 2 (Blue-Green) and Demo 3 (Canary) with [§ 2.1](#21-how-blue-green-and-canary-relate-to-the-dc-migration): Blue-Green replaces "two DCs + switch route"; Canary extends beyond what DC or plain Deployment offer.
- **"How does it fit our GitOps?"** → Optional Demo 4 (Rollout in Git, Argo CD sync).
- **"How do we get Rollouts on OpenShift?"** → Demo last (RolloutManager at the end of the presentation).

If you want, next step can be concrete manifests (Rollout + Services) for demos 1–3 in this repo.
