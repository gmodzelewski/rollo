# Presentation Flow: DC → Argo Rollouts Migration

**Duration:** ~40 minutes (3 demos + slides + Q&A)

This file contains the complete presentation flow including slides, demos, and talking points.

**Key Message:** OpenShift recommends migrating from DeploymentConfigs to Deployments. We recommend going one step further: skip basic Deployments and use **Argo Rollouts** instead. Rollouts manage ReplicaSets just like Deployments do, but with advanced progressive delivery strategies.

---

## Setup (Before Presentation)

**Terminals:**
- **Pane A (Watch):** For `oc argo rollouts get rollout ... --watch`
- **Pane B (Actions):** For commands (set image, promote, abort)
- **Optional Pane C:** For `watch oc get pods`

**Browser:**
- Tab 1: Demo 2 - Active Route (production)
- Tab 2: Demo 2 - Preview Route (testing)

**Pre-flight:**
```bash
# Get Route URLs for Demo 2
oc get routes -n rollo-demo

# Open these URLs in browser tabs:
# - rollo-demo-2-active (production traffic)
# - rollo-demo-2-preview (testing traffic)
```

---

## 1. Opening Slide (1 min)

**Slide: Title**
```
From DeploymentConfigs to Progressive Delivery
with Argo Rollouts

Your Name | Date
```

**Say:**
> "Today we're talking about migrating from DeploymentConfigs. OpenShift recommends moving to standard Kubernetes Deployments—but we're going one step further. We'll use Argo Rollouts, which gives you everything Deployments do, plus Blue-Green, Canary, and progressive delivery. Three quick demos, then installation."

---

## 2. Context Slide (3 min)

**Slide: Why Move from DeploymentConfigs?**
```
DeploymentConfigs (deprecated):
- OpenShift-specific (not standard Kubernetes)
- Limited to rolling updates
- Imperative triggers (ImageChange, ConfigChange)

Deployments (recommended by OpenShift):
- Standard Kubernetes resource
- Rolling updates via ReplicaSet management
- Declarative, but basic strategies only

Argo Rollouts (our recommendation):
✓ Everything Deployments do (manages ReplicaSets the same way)
✓ PLUS: Blue-Green, Canary with steps, progressive delivery
✓ GitOps-native, active CNCF community
```

**Say:**
> "DeploymentConfigs are deprecated. OpenShift says: migrate to Deployments. We say: skip plain Deployments and go straight to Argo Rollouts. Why? Because Rollouts are built on the same foundation—they manage ReplicaSets just like Deployments—but they give you advanced strategies that Deployments don't have. You're not losing anything; you're gaining control."

**Slide: Under the Hood**
```
DeploymentConfig → ReplicaSet → Pods
Deployment       → ReplicaSet → Pods
Rollout         → ReplicaSet → Pods ← Same mechanism!

The difference: Rollout has smarter rollout strategies
```

**Say:**
> "Here's the key: all three manage ReplicaSets. DeploymentConfigs, Deployments, and Rollouts all work the same way under the hood. Rollouts just give you more control over *how* the rollout happens—Blue-Green, Canary, manual promotion. Same reliability, more options."

---

## 3. What We'll Cover (1 min)

**Slide: Agenda**
```
1. Demo 1: First Rollout – "It's just a Deployment with a strategy"
2. Demo 2: Blue-Green – Preview before production
3. Demo 3: Canary with steps – Progressive rollout
4. Installation (at the end)
```

**Say:**
> "Three demos, each about 8 minutes. We start with the migration story immediately—installation comes at the end when you're ready for it."

---

## 4. DEMO 1: First Rollout (8 min)

**Slide: Before → After**
```
Before (DeploymentConfig)          After (Rollout)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
kind: DeploymentConfig             kind: Rollout
spec:                              spec:
  template:                          template:
    spec:                              spec:
      containers: [...]                  containers: [...]  ← Same!
  triggers:                          strategy:
    - type: ImageChange                canary: {}          ← New control
```

**Say:**
> "Here's the key insight: the pod template is identical. We're just changing the resource type and adding a strategy section. Let's see it live."

### 4.1 Show the running rollout (2 min)

**Terminal Pane A:**
```bash
oc argo rollouts get rollout rollo-demo-1 -n rollo-demo --watch
```

**Say:**
> "This is our first Rollout. Already running, healthy. It's using image v1. The strategy is 'canary with no steps'—which behaves like a rolling update."

**Show:** Open `demo1/rollout.yaml` in editor (briefly)

### 4.2 Trigger new version (4 min)

**Terminal Pane B:**
```bash
oc argo rollouts set image rollo-demo-1 rollo=quay.io/modzelewski/rollo:v2 -n rollo-demo
```

**Say:**
> "We push a new image tag—v2. In production, your GitOps pipeline would update the Rollout spec in Git. Here we're doing it directly. Watch the rollout happen."

**Watch in Pane A:** New ReplicaSet created, pods rolling out

**Say (while watching):**
> "New ReplicaSet created. Pods are replaced one by one—just like a Deployment rolling update. No manual steps needed."

### 4.3 Takeaway (30 sec)

**Say:**
> "That's it. Same pod template, different resource kind. New image → controller rolls out. Same outcome as DeploymentConfig ImageChange, but driven by Git."

---

## 5. Transition Slide: ImageChange Triggers (3 min)

**Slide: What About ImageChange Triggers?**
```
DeploymentConfig                   Rollouts + GitOps
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Mechanism:                         Mechanism:
  ImageChange trigger watches        Argo CD Image Updater watches
  registry → updates DC spec         registry → updates Git →
  → auto-deploys                     Argo CD syncs → Rollout deploys

State:                             State:
  Ephemeral (trigger fires)          Durable (Git commit)

Rollback:                          Rollback:
  oc rollout undo                    Revert Git commit

Audit:                             Audit:
  Event logs                         Git history

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Outcome: Same auto-deploy behavior, better auditability
```

**Say:**
> "One of the most loved DC features is ImageChange triggers—'new image pushed, auto-deploy.' Rollouts don't have triggers built-in, but the GitOps pattern replaces it. Argo CD Image Updater watches your registry, updates Git when a new image appears, and Argo CD syncs it. The outcome is the same, but now your desired state is in Git—auditable, rollback-able, no ephemeral trigger state."

---

## 6. DEMO 2: Blue-Green (8 min)

**Slide: Blue-Green Pattern**
```
Two Services:
- Active Service  → Production traffic (stable version)
- Preview Service → New version only (for testing)

Process:
1. Deploy new version → Preview gets v2, Active stays v1
2. Test on Preview
3. Promote → Active switches to v2

Replaces: "Two DCs + manual route switch"
```

**Say:**
> "Demo 2: Blue-Green. Some of you may have built this pattern manually with two DeploymentConfigs and switching Routes. Rollouts makes it first-class."

### 6.1 Introduce services (2 min)

**Terminal Pane A:**
```bash
oc argo rollouts get rollout rollo-demo-2 -n rollo-demo --watch
```

**Say:**
> "Two services defined: active for production, preview for testing. Currently both point to v1. Auto-promotion is disabled—we control when to switch."

**Show:** Open `demo2/rollout.yaml` briefly (show activeService, previewService, autoPromotionEnabled: false)

### 6.2 Deploy and promote (5 min)

**Browser:** Show both Route tabs (both showing v1/blue)

**Terminal Pane B:**
```bash
oc argo rollouts set image rollo-demo-2 rollo=quay.io/modzelewski/rollo:v2 -n rollo-demo
```

**Say:**
> "Deploying v2. Watch the status—it'll pause."

**Watch in Pane A:** Status shows "Paused" waiting for promotion

**Browser:**
- Refresh Preview Route tab: Shows v2/yellow
- Active Route tab: Still shows v1/blue

**Say:**
> "Preview service now serves v2—yellow UI. Active service still on v1—blue. Production traffic is unaffected. We can test v2 on preview. When happy, we promote."

**Terminal Pane B:**
```bash
oc argo rollouts promote rollo-demo-2 -n rollo-demo
```

**Watch in Pane A:** Rollout completes, active switches to new ReplicaSet

**Browser:**
- Refresh Active Route tab: Now shows v2/yellow

**Say:**
> "Active service now serves v2. Production cutover complete. One Rollout, two services, no manual route switching."

### 6.3 Takeaway (30 sec)

**Say:**
> "Blue-Green with Rollouts: preview-then-promote, built-in. Replaces the pattern of managing two separate deployments."

---

## 7. DEMO 3: Canary with Steps (8 min)

**Slide: Canary Pattern**
```
Progressive rollout with pause points:
- 20% → manual pause
- 40% → timed pause (10s)
- 60% → timed pause (10s)
- 80% → timed pause (10s)
- 100%

Control blast radius. More than DC's maxSurge/maxUnavailable.
```

**Say:**
> "Demo 3: Canary with steps. We roll out in stages—20%, 40%, and so on—with manual or timed pauses. This gives you fine control that DeploymentConfigs' rolling strategy didn't offer."

### 7.1 Show strategy (2 min)

**Terminal Pane A:**
```bash
oc argo rollouts get rollout rollo-demo-3 -n rollo-demo --watch
```

**Say:**
> "Five replicas. Strategy has steps: setWeight 20%, then a manual pause, then more steps with timed pauses."

**Show:** Open `demo3/rollout.yaml` briefly (show steps section)

### 7.2 Trigger and promote (5 min)

**Terminal Pane B:**
```bash
oc argo rollouts set image rollo-demo-3 rollo=quay.io/modzelewski/rollo:v2 -n rollo-demo
```

**Say:**
> "Triggering update. It'll pause at 20%."

**Watch in Pane A:** Status shows step 1/8, paused

**Say:**
> "Step 1: 20% weight. That's 1 canary pod, 4 stable pods. We're paused—controller waits for our decision."

**Terminal Pane B:**
```bash
oc argo rollouts promote rollo-demo-3 -n rollo-demo
```

**Say:**
> "Promoting. Now it'll proceed through the remaining steps—40%, 60%, 80%—with 10-second pauses between."

**Watch in Pane A:** Steps progress automatically with duration pauses

**Say (while watching):**
> "Each step advances automatically after 10 seconds. This is where you'd monitor metrics, check error rates, etc. If something's wrong, you can abort and rollback."

### 7.3 Traffic note (1 min)

**Slide: Traffic Distribution**
```
Without Traffic Provider (this demo):
- 20% weight = 1 canary pod (20% of 5 replicas), 4 stable pods
- Traffic load-balanced across all 5 pods
- ~20% to canary on average (not exact)

With Traffic Provider (OpenShift Routes):
- Argo Rollouts integrates with Routes to split traffic exactly
- Routes configured to send 20% → canary service, 80% → stable service
- True percentage control regardless of replica count
- Recommended for production canary deployments

Integration: Configure Rollout with trafficRouting.openshift
See: https://argo-rollouts.readthedocs.io/en/stable/features/traffic-management/openshift/
```

**Say:**
> "Important note: without a traffic provider, the percentage is approximated by replica count. For exact traffic splitting in production, configure Argo Rollouts to integrate with OpenShift Routes. This allows Routes to send exactly 20% of requests to the canary service, regardless of pod count."

### 7.4 Takeaway (30 sec)

**Say:**
> "Canary with steps: controlled rollout, pause at any point, abort if needed. This extends beyond what DeploymentConfig's rolling strategy could do."

---

## 8. Troubleshooting Slide (2 min)

**Slide: What If Something Goes Wrong?**
```
Scenario: New version crashes

Rollout behavior:
- Automatically pauses if new ReplicaSet isn't ready
- You can abort and rollback
- Revision history is preserved

Commands:
  oc argo rollouts status <name>    # Check health
  oc argo rollouts abort <name>     # Stop rollout
  oc argo rollouts undo <name>      # Rollback
  oc argo rollouts history <name>   # View revisions
```

**Say:**
> "What if the new version crashes? Rollouts pause automatically if pods aren't ready. You can abort, rollback to a previous revision, or check history. Safety is built-in."

---

## 9. Recap Slide (2 min)

**Slide: What We Saw**
```
Migration Summary:

Need                          DeploymentConfig    Deployment         Argo Rollouts
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Rolling update                ✓ Rolling           ✓ RollingUpdate    ✓ Canary (no steps)
Auto-deploy on new image      ✓ ImageChange       ✗ None built-in    ✓ GitOps + Image Updater
Test before production        ✗ Manual (2 DCs)    ✗ Manual           ✓ Blue-Green (first-class)
Progressive with pauses       ✗ Only maxSurge     ✗ Only maxSurge    ✓ Canary with steps
Manual promotion/abort        ✗ No                ✗ No               ✓ Yes

Result: Rollouts = Deployment capabilities + progressive delivery
```

**Say:**
> "Recap: We skipped plain Deployments and went straight to Rollouts. Why? Look at this table. Deployments give you basic rolling updates—nothing more. Rollouts give you that PLUS Blue-Green, Canary with steps, manual promotion, and GitOps-driven automation. You get everything a Deployment does, plus features it doesn't have. The migration isn't a compromise; it's an upgrade."

---

## 10. Migration Guide Slide (3 min)

**Slide: How to Migrate Your Apps**
```
Basic Steps:

1. Understand the context:
   - OpenShift recommends: DC → Deployment
   - We recommend: DC → Rollout (skip Deployment)
   - Why? Rollouts = Deployment + advanced strategies

   Read: https://docs.openshift.com/container-platform/latest/
         applications/deployments/what-deployments-are.html

2. Convert DC to Rollout:
   - Copy spec.template (pod template) → identical
   - Change kind: DeploymentConfig → kind: Rollout
   - Remove triggers: section
   - Add strategy: (canary/blueGreen)
   - Create Services (1 for rolling, 2 for blue-green/canary)

3. Replace ImageChange triggers with GitOps:
   - Use Argo CD + Image Updater
   - Or CI/CD pipeline updates Rollout spec in Git

4. Test on non-prod app first

See: conversion-example.md in demo repo
```

**Say:**
> "Practical migration: understand that we're skipping the intermediate step. Instead of DC → Deployment → Rollout, we go DC → Rollout directly. The pod template stays identical—only the resource kind and strategy change. Replace ImageChange triggers with GitOps. Always test on non-prod first."

---

## 11. Installation (Last) (5 min)

**Slide: Enabling Argo Rollouts on OpenShift**
```
Via OpenShift GitOps (Recommended):

1. Install OpenShift GitOps Operator (if not already)
2. Create RolloutManager custom resource:

apiVersion: argoproj.io/v1alpha1
kind: RolloutManager
metadata:
  name: rollout-manager
  namespace: openshift-gitops

3. Verify:
   oc get rolloutmanager
   oc get crd rollouts.argoproj.io
   oc get pods -n openshift-gitops | grep rollout

4. (Optional) Enable Rollouts UI in Argo CD server:
   spec:
     server:
       enableRolloutsUI: true
```

**Terminal:**
```bash
# Show existing RolloutManager
oc get rolloutmanager -A

# Show CRD
oc get crd rollouts.argoproj.io

# Show controller pod
oc get pods -n openshift-gitops | grep rollout
```

**Say:**
> "Installation is straightforward via OpenShift GitOps. One custom resource—RolloutManager—and the operator handles the rest. Controller deploys, CRDs are installed, you're ready to create Rollouts."

**Slide: CLI Installation**
```
Install Argo Rollouts plugin (for local use):

Mac:
  brew install argoproj/tap/kubectl-argo-rollouts

Linux:
  curl -LO https://github.com/argoproj/argo-rollouts/releases/latest/download/kubectl-argo-rollouts-linux-amd64
  chmod +x kubectl-argo-rollouts-linux-amd64
  sudo mv kubectl-argo-rollouts-linux-amd64 /usr/local/bin/kubectl-argo-rollouts

Verify: oc argo rollouts version

Usage:
  oc argo rollouts get rollout <name> --watch
  oc argo rollouts promote <name>
  oc argo rollouts abort <name>
  oc argo rollouts undo <name>
```

**Say:**
> "For local development, install the oc plugin. It gives you the watch, promote, abort commands we used today."

---

## 12. FAQ Slide (3 min)

**Slide: Common Questions**
```
Q: Why skip Deployments? Don't we lose anything?
A: No! Rollouts manage ReplicaSets the same way Deployments do.
   You get everything Deployments offer + advanced strategies.
   Only skip Rollouts if you want zero dependencies.

Q: Can we still use DeploymentConfigs?
A: Yes, but deprecated. Migration to Rollouts is recommended.

Q: What about DC lifecycle hooks (pre/mid/post)?
A: Rollouts don't have hooks. Use Kubernetes Jobs or Argo Workflows.

Q: Does this work with our Routes/Ingress?
A: Yes! Blue-Green and Canary integrate with OpenShift Routes and Service Mesh.

Q: What's the migration effort?
A: Low for simple DCs (YAML conversion). Higher if complex triggers/hooks.

Q: Can we test on non-prod first?
A: Absolutely recommended. Start small.

Q: Do we need to change application code?
A: No. Only infrastructure (manifests).
```

**Say:**
> "Most common question: why skip Deployments? Answer: because you don't lose anything. Rollouts are built on the same ReplicaSet mechanism. You get all of Deployment's capabilities plus Blue-Green and Canary. No code changes needed—this is purely infrastructure. Start with non-prod apps."

---

## 13. Resources Slide (1 min)

**Slide: Learn More**
```
Documentation:
- Red Hat OpenShift GitOps – Argo Rollouts
  https://docs.redhat.com/en/documentation/red_hat_openshift_gitops/1.19/html/argo_rollouts

- Argo Rollouts Official Docs
  https://argo-rollouts.readthedocs.io/

- Demo Repository
  [Your repo URL]

Community:
- CNCF Argo Slack: #argo-rollouts
- GitHub: argoproj/argo-rollouts
```

**Say:**
> "All docs are linked here. Demo manifests are in the repo. Argo Rollouts has an active community—Slack and GitHub if you hit issues."

---

## 14. Q&A (5-10 min)

**Slide: Questions?**

**Say:**
> "That's the end of the prepared content. Questions?"

---

## Quick Reference: Terminal Commands

**Demo 1:**
```bash
# Watch
oc argo rollouts get rollout rollo-demo-1 -n rollo-demo --watch

# Set image
oc argo rollouts set image rollo-demo-1 rollo=quay.io/modzelewski/rollo:v2 -n rollo-demo
```

**Demo 2:**
```bash
# Watch
oc argo rollouts get rollout rollo-demo-2 -n rollo-demo --watch

# Set image
oc argo rollouts set image rollo-demo-2 rollo=quay.io/modzelewski/rollo:v2 -n rollo-demo

# Promote
oc argo rollouts promote rollo-demo-2 -n rollo-demo
```

**Demo 3:**
```bash
# Watch
oc argo rollouts get rollout rollo-demo-3 -n rollo-demo --watch

# Set image
oc argo rollouts set image rollo-demo-3 rollo=quay.io/modzelewski/rollo:v2 -n rollo-demo

# Promote
oc argo rollouts promote rollo-demo-3 -n rollo-demo
```

---

## Timing Summary

| Section | Duration |
|---------|----------|
| Opening + Context | 4 min |
| Demo 1 | 8 min |
| ImageChange slide | 3 min |
| Demo 2 | 8 min |
| Demo 3 | 8 min |
| Troubleshooting slide | 2 min |
| Recap + Migration | 5 min |
| Installation | 5 min |
| FAQ | 3 min |
| Resources | 1 min |
| **Total** | **47 min** |
| Q&A | +10 min |
| **Grand Total** | **~60 min** |
