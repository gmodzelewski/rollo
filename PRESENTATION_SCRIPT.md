# Argo Rollouts Presentation Script

**Complete presenter's guide for DeploymentConfig → Argo Rollouts migration**

**Total Duration:** ~70 minutes (4 demos + slides + Q&A)

**Key Message:** OpenShift recommends migrating from DeploymentConfigs to Deployments. We recommend going one step further: skip basic Deployments and use Argo Rollouts instead. Rollouts manage ReplicaSets just like Deployments do, but with Blue-Green, Canary, and progressive delivery built-in.

**What's Included:**
- ✅ 4 complete live demos (rolling, blue-green, canary, image updater)
- ✅ Honest discussion about trade-offs (especially ImageChange triggers)
- ✅ Step-by-step commands for each demo
- ✅ Installation and next steps
- ✅ All slides and talking points

---

## Legend

- 🎯 **Goal/Message** - Key takeaway for this section
- 📊 **Slide** - Content to show on slides
- 💬 **Say** - What to say (verbatim talking points)
- 💻 **Terminal** - Commands to execute (with pane labels)
- 👁️ **Watch** - What to observe in terminal output
- 🌐 **Browser** - Browser actions

---

## Pre-Presentation Setup

### Terminal Layout

Set up **2 terminal panes** (use tmux or iTerm2 split):

```
┌─────────────────────────────┬─────────────────────────────┐
│  PANE A: Watch              │  PANE B: Actions            │
│  (rollout status --watch)   │  (commands)                 │
└─────────────────────────────┴─────────────────────────────┘
```

**Optional:** Add 3rd pane for `watch oc get pods`

### Browser Tabs

For Demo 2, open these URLs in separate tabs:
- Active Route: `https://rollo-demo-2-active-rollo-demo.apps.YOUR-CLUSTER.com`
- Preview Route: `https://rollo-demo-2-preview-rollo-demo.apps.YOUR-CLUSTER.com`

Get URLs:
```bash
oc get routes -n rollo-demo
```

### Pre-Flight Checklist

- [ ] Cluster has Argo Rollouts controller installed
- [ ] `oc argo rollouts version` works
- [ ] All three demos deployed and healthy:
  ```bash
  oc argo rollouts list rollouts -n rollo-demo
  ```
- [ ] Images accessible: `quay.io/modzelewski/rollo:v1` and `:v2`
- [ ] Demo 1 on v1 (will update to v2 during demo)
- [ ] Demo 2 and 3 on v1
- [ ] Browser tabs open to Demo 2 Routes
- [ ] Slides ready

---

# PRESENTATION START

## Section 1: Opening (1 min)

### 📊 Slide: Title

```
From DeploymentConfigs to Progressive Delivery
with Argo Rollouts

Your Name | Date
```

### 💬 Say:

> "Today we're talking about migrating from DeploymentConfigs. OpenShift recommends moving to standard Kubernetes Deployments—but we're going one step further. We'll use Argo Rollouts, which gives you everything Deployments do, plus Blue-Green, Canary, and progressive delivery. Three quick demos, then installation."

---

## Section 2: Context (3 min)

### 📊 Slide: Why Move from DeploymentConfigs?

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

### 💬 Say:

> "DeploymentConfigs are deprecated. OpenShift says: migrate to Deployments. We say: skip plain Deployments and go straight to Argo Rollouts. Why? Because Rollouts are built on the same foundation—they manage ReplicaSets just like Deployments—but they give you advanced strategies that Deployments don't have. You're not losing anything; you're gaining control."

### 📊 Slide: Under the Hood

```
DeploymentConfig → ReplicaSet → Pods
Deployment       → ReplicaSet → Pods
Rollout         → ReplicaSet → Pods ← Same mechanism!

The difference: Rollout has smarter rollout strategies
```

### 💬 Say:

> "Here's the key: all three manage ReplicaSets. DeploymentConfigs, Deployments, and Rollouts all work the same way under the hood. Rollouts just give you more control over *how* the rollout happens—Blue-Green, Canary, manual promotion. Same reliability, more options."

---

## Section 3: Agenda (1 min)

### 📊 Slide: What We'll Cover

```
1. Demo 1: First Rollout – "It's just a Deployment with a strategy"
2. Demo 2: Blue-Green – Preview before production
3. Demo 3: Canary with steps – Progressive rollout
4. Demo 4: Auto-Deploy with Image Updater – GitOps automation
5. Installation & Next Steps (at the end)
```

### 💬 Say:

> "Four demos today. The first three show the core deployment strategies—rolling update, Blue-Green, and Canary. The fourth demo addresses a question everyone asks: what about ImageChange triggers? We'll show you the GitOps replacement with Image Updater. Installation comes at the end when you're ready for it."

---

# DEMO 1: First Rollout (8 min)

🎯 **Goal:** Show that a Rollout is a drop-in replacement for a Deployment. Same pod template, different resource kind + strategy.

## Part 1: Show Before/After (1 min)

### 📊 Slide: Before → After

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

### 💬 Say:

> "Here's the key insight: the pod template is identical. We're just changing the resource type and adding a strategy section. Let's see it live."

## Part 2: Show Running Rollout (2 min)

### 💻 Terminal - Pane A:

```bash
oc argo rollouts get rollout rollo-demo-1 -n rollo-demo --watch
```

### 👁️ Watch:

- Status: Healthy
- Image: `quay.io/modzelewski/rollo:v1`
- ReplicaSet: 1 (current revision)
- Pods: 3/3 ready

### 💬 Say:

> "This is our first Rollout. Already running, healthy. It's using image v1. The strategy is 'canary with no steps'—which behaves like a rolling update."

**Optional:** Briefly show `demo1/rollout.yaml` in editor

## Part 3: Trigger New Version (4 min)

### 💻 Terminal - Pane B:

```bash
oc argo rollouts set image rollo-demo-1 rollo=quay.io/modzelewski/rollo:v2 -n rollo-demo
```

### 💬 Say:

> "We push a new image tag—v2. In production, your GitOps pipeline would update the Rollout spec in Git. Here we're doing it directly. Watch the rollout happen."

### 👁️ Watch (Pane A):

- New ReplicaSet created
- Pods rolling out (1 by 1, respecting maxSurge/maxUnavailable)
- Old ReplicaSet scaling down
- Status: Progressing → Healthy
- All pods on v2

### 💬 Say (while watching):

> "New ReplicaSet created. Pods are replaced one by one—just like a Deployment rolling update. No manual steps needed."

### 💬 Say (when complete):

> "Done. One line changed—the image tag. Controller did the rest. Same outcome as DeploymentConfig ImageChange, but driven by Git."

## Part 4: Takeaway (30 sec)

### 💬 Say:

> "That's it. Same pod template, different resource kind. New image → controller rolls out. Migration is mostly a YAML change; no application code changes needed."

---

# TRANSITION: ImageChange Triggers (3 min)

### 📊 Slide: What About ImageChange Triggers?

```
DeploymentConfig                   Rollouts + GitOps
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Mechanism:                         Mechanism:
  ImageChange trigger watches        GitOps updates manifest in Git
  registry → updates DC spec         (pipeline, Image Updater, or manual)
  → auto-deploys                     → Argo CD syncs → Rollout deploys

State:                             State:
  Ephemeral (trigger fires)          Durable (Git commit)

Rollback:                          Rollback:
  oc rollout undo                    Revert Git commit

Audit:                             Audit:
  Event logs                         Git history

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Outcome: Same auto-deploy capability, GitOps-native
```

### 💬 Say:

> "One of the most loved DC features is ImageChange triggers—'new image pushed, auto-deploy.' Rollouts don't have triggers built-in because they follow the GitOps pattern. The idea is: desired state lives in Git. When a new image is ready, something updates the manifest in Git—could be your CI/CD pipeline, could be Argo CD Image Updater which is available in OpenShift GitOps, or even manual edits for simple workflows. Once Git is updated, Argo CD syncs and the Rollout deploys. The outcome is the same auto-deploy, but now everything is auditable in Git. We'll talk more about automating this at the end."

---

# DEMO 2: Blue-Green (8 min)

🎯 **Goal:** Show controlled cutover with preview-then-promote. Replaces "two DCs + manual route switch" pattern.

## Part 1: Introduce Blue-Green (2 min)

### 📊 Slide: Blue-Green Pattern

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

### 💬 Say:

> "Demo 2: Blue-Green. Some of you may have built this pattern manually with two DeploymentConfigs and switching Routes. Rollouts makes it first-class."

### 💻 Terminal - Pane A:

```bash
oc argo rollouts get rollout rollo-demo-2 -n rollo-demo --watch
```

### 👁️ Watch:

- Status: Healthy
- Services: rollo-demo-2-active, rollo-demo-2-preview
- Pods: 3/3 on v1

### 💬 Say:

> "Two services defined: active for production, preview for testing. Currently both point to v1. Auto-promotion is disabled—we control when to switch."

**Optional:** Briefly show `demo2/rollout.yaml` (highlight activeService, previewService, autoPromotionEnabled: false)

## Part 2: Deploy New Version (5 min)

### 🌐 Browser:

Show both Route tabs—both currently showing v1 (blue UI)

### 💻 Terminal - Pane B:

```bash
oc argo rollouts set image rollo-demo-2 rollo=quay.io/modzelewski/rollo:v2 -n rollo-demo
```

### 💬 Say:

> "Deploying v2. Watch the status—it'll pause."

### 👁️ Watch (Pane A):

- New ReplicaSet created for v2
- New pods spin up
- Status: **Paused** (BlueGreenPause)
- Preview revision: v2
- Active revision: still v1

### 🌐 Browser:

- Refresh **Preview Route** tab → Shows v2 (yellow UI)
- Check **Active Route** tab → Still shows v1 (blue UI)

### 💬 Say:

> "Preview service now serves v2—yellow UI. Active service still on v1—blue. Production traffic is unaffected. We can test v2 on preview. When happy, we promote."

### 💻 Terminal - Pane B (after ~30 sec):

```bash
oc argo rollouts promote rollo-demo-2 -n rollo-demo
```

### 💬 Say:

> "Promoting now. Watch the active service switch."

### 👁️ Watch (Pane A):

- Rollout progressing
- Active service switches to v2 ReplicaSet
- Status: Healthy
- Old v1 ReplicaSet scales down

### 🌐 Browser:

- Refresh **Active Route** tab → Now shows v2 (yellow UI)

### 💬 Say:

> "Active service now serves v2. Production cutover complete. One Rollout, two services, no manual route switching."

## Part 3: Takeaway (30 sec)

### 💬 Say:

> "Blue-Green with Rollouts: preview-then-promote, built-in. Replaces the pattern of managing two separate deployments. Safe: production stays stable until you explicitly promote."

---

# DEMO 3: Canary with Steps - Basic + Service Mesh (12 min)

🎯 **Goal:** Show progressive rollout (20%→40%→60%→80%→100%) in two flavors:
1. Basic replica-based (simple, no dependencies)
2. Service Mesh (exact traffic %, production-ready)

**Flow:** First rollout v1→v2 basic, then upgrade to Service Mesh, trigger second rollout to show VirtualService automation.

## Part 1: Introduce (1 min)

### 📊 Slide: Demo 3 Overview

```
Progressive Canary Rollout:
- 20% → manual pause (check metrics, decide)
- 40% → auto-pause 10s
- 60% → auto-pause 10s
- 80% → auto-pause 10s
- 100% → complete

Two Approaches:
1. Basic (replica-based) - ~20% traffic via pod ratios
2. Production (Service Mesh) - exactly 20% traffic via Istio

Control blast radius at every step.
More than DeploymentConfig's maxSurge/maxUnavailable ever offered.
```

### 💬 Say:

> "Demo 3: Progressive canary. We'll show two approaches. First, basic canary using replica ratios—simple, no dependencies. Then we'll upgrade to Service Mesh and show exact traffic control via Istio VirtualService automation. Same rollout strategy, different traffic mechanisms."

## Part 2: Basic Rollout v1→v2 (3 min)

### 💻 Terminal - Pane A:

```bash
oc argo rollouts get rollout rollo-demo-3 -n rollo-demo --watch
```

### 👁️ Watch:

- Status: Healthy
- Replicas: 5
- Pods: 5/5 on v1

### 💬 Say:

> "Starting with basic replica-based canary. Five replicas for clear percentage splits. Watch as we trigger the rollout."

### 💻 Terminal - Pane B:

```bash
# Trigger rollout to v2
oc argo rollouts set image rollo-demo-3 rollo=quay.io/modzelewski/rollo:v2 -n rollo-demo
```

### 👁️ Watch (Pane A):

- New ReplicaSet created
- Status: **Paused** at step 1/8
- SetWeight: 20
- Canary pods: 1, Stable pods: 4

### 💬 Say:

> "Paused at 20%. One canary pod, four stable. Traffic is approximately 20% to canary based on pod ratio."

### 💻 Terminal - Pane B (after showing pause):

```bash
oc argo rollouts promote rollo-demo-3 -n rollo-demo
```

### 👁️ Watch (Pane A) - Auto-progression:

- 40% (2 canary, 3 stable) → 10s pause
- 60% (3 canary, 2 stable) → 10s pause
- 80% (4 canary, 1 stable) → 10s pause
- 100% (5 canary, 0 stable)
- Status: Healthy

### 💬 Say:

> "Auto-advancing through the steps. Watch the replica counts change. In about 30 seconds, we're at v2. Simple, works without any infrastructure."

## Part 3: Explain Replica-Based (1 min)

### 📊 Slide: How Replica-Based Works

```
Why two services? Rollout API requires both.
Controller sets selectors so they're disjoint:
  stableService  → stable ReplicaSet only (4 pods)
  canaryService  → canary ReplicaSet only (1 pod)

To get ~20% traffic to canary: Route must split.
  One Route, two backends: 80% stable, 20% canary (alternateBackends).
  Round-robin within each backend → ~20% to canary.

(One service + all pods + round-robin would also give ~20%, but
 Rollouts doesn't support that—it always partitions by ReplicaSet.)

Limitations:
  ❌ Route weights are static (80/20); for 40%→60%→80% use Istio
  ❌ Not exact (round-robin within each backend)
```

### 💬 Say:

> "You might think: one service with all five pods, round-robin—every fifth request to canary. But Argo Rollouts requires two services and assigns each to one ReplicaSet: stable service to the four stable pods, canary service to the one canary pod. So we need the Route to split—this demo uses one Route with 80% to stable, 20% to canary. That gives you actual ~20% traffic to the canary. For step-wise 40%, 60%, 80% you need Service Mesh, which updates the weights automatically."

## Part 4: Upgrade to Service Mesh (3 min)

### 📊 Slide: Upgrade to Service Mesh 3

```
Minimal Setup (3 commands):
1. Label namespace for ambient mode
2. Deploy waypoint proxy (L7 routing)
3. Update rollout with Istio integration

Result: Exact traffic percentages via VirtualService
```

### 💬 Say:

> "Let's upgrade to Service Mesh for exact traffic. Three quick steps—watch how simple this is."

### 💻 Terminal - Pane B:

```bash
# Step 1: Enable ambient mode
oc label namespace rollo-demo \
  istio.io/dataplane-mode=ambient \
  istio-discovery=enabled \
  istio.io/use-waypoint=rollo-demo-waypoint \
  --overwrite

# Step 2: Deploy waypoint (L7 proxy for VirtualService routing)
oc apply -f demo3/waypoint.yaml

# Verify waypoint
oc get gateway rollo-demo-waypoint -n rollo-demo
```

### 👁️ Watch:

```
NAME                  CLASS            ADDRESS          PROGRAMMED
rollo-demo-waypoint   istio-waypoint   172.30.x.x       True
```

### 💻 Terminal - Pane B (continue):

```bash
# Step 3: Deploy VirtualService
oc apply -f demo3/virtualservice.yaml
```

### 💬 Say:

> "Done. Waypoint is running, VirtualService is deployed. Now let's upgrade the Rollout to use Istio and watch the VirtualService automation in action."

## Part 5: Service Mesh Rollout - Watch VirtualService Automation (3 min)

### Setup 3 Terminal Panes:

```
┌──────────────────┬──────────────────┐
│  Pane A: Rollout │  Pane B: Command │
├──────────────────┴──────────────────┤
│  Pane C: VirtualService Weights     │
│  (THE MAGIC - watch automation)     │
└─────────────────────────────────────┘
```

### 💻 Terminal - Pane C (NEW):

```bash
# Watch VirtualService weights change automatically
watch -n 2 "oc get virtualservice rollo-demo-3-vsvc -n rollo-demo \
  -o jsonpath='Stable: {.spec.http[0].route[0].weight}%  Canary: {.spec.http[0].route[1].weight}%{\"\\n\"}'"
```

### 👁️ Watch (Pane C - Initial state):

```
Stable: 100%  Canary: 0%
```

### 💬 Say:

> "Pane C shows VirtualService weights. Currently 100% stable, 0% canary. Watch this pane—Argo Rollouts will update these automatically."

### 💻 Terminal - Pane B:

```bash
# Upgrade Rollout to use Istio traffic routing (image stays v1)
oc apply -f demo3/rollout-istio.yaml

# Trigger rollout to v2
oc argo rollouts set image rollo-demo-3 rollo=quay.io/modzelewski/rollo:v2 -n rollo-demo
```

### 👁️ Watch (Pane C) - **THE MAGIC:**

```
Stable: 100%  Canary: 0%    (initial)
↓
Stable: 80%   Canary: 20%   ← CHANGED! Argo Rollouts updated VirtualService!
```

### 👁️ Watch (Pane A):

- Status: Paused at step 1/8
- SetWeight: 20
- **ActualWeight: 20** ← VirtualService updated!

### 💬 Say:

> "Look at Pane C! Weights changed to 80/20. We didn't touch the VirtualService—Argo Rollouts did it automatically. The rollout says setWeight 20%, and Argo Rollouts updated the VirtualService to route exactly 20% of traffic to canary. This is the key difference: exact 20%, not approximate."

### 💻 Terminal - Pane B:

```bash
oc argo rollouts promote rollo-demo-3 -n rollo-demo
```

### 👁️ Watch (Pane C) - Progressive changes:

```
Stable: 60%   Canary: 40%   ← Auto-updated to 40%!
(wait 10s)
Stable: 40%   Canary: 60%   ← Auto-updated to 60%!
(wait 10s)
Stable: 20%   Canary: 80%   ← Auto-updated to 80%!
(wait 10s)
Stable: 100%  Canary: 0%    ← Canary promoted to stable
```

### 💬 Say:

> "Watch Pane C—VirtualService weights updating automatically at each step. 60/40, then 40/60, then 20/80. Argo Rollouts is manipulating Istio resources to achieve exact traffic percentages. You define the strategy in the Rollout, Argo Rollouts makes it happen."

## Part 6: Takeaway (1 min)

### 📊 Slide: Demo 3 Key Points

```
Two Approaches, Same Rollout Strategy:

Replica-Based (Basic):
  ✓ Simple, no dependencies
  ✓ Route 80/20 → ~20% to canary (approximate)
  ✓ Good for learning, non-critical apps
  ❌ Route weights static (use Istio for 40%→60%→80%)

Service Mesh (Production):
  ✓ Exactly 20% traffic
  ✓ Argo Rollouts auto-manages VirtualService
  ✓ Two services used by VirtualService routing
  ✓ Production-ready for critical apps
  ⚠️ Requires Service Mesh infrastructure

Both extend far beyond DeploymentConfig's rolling strategy.
```

### 💬 Say:

> "Two flavors, one strategy. Basic replica-based is simple—great for learning and non-critical apps. Service Mesh gives you exact traffic control—Argo Rollouts automatically manipulates VirtualService weights at each step. You saw it live: 20%, 40%, 60%, 80%, all exact percentages. This is progressive delivery for production. And both approaches give you way more control than DeploymentConfig's basic rolling updates ever could."

---

# DEMO 4: Auto-Deploy with Image Updater (10 min)

🎯 **Goal:** Show how to replace DeploymentConfig ImageChange triggers with Argo CD Image Updater. Demonstrate the complete flow: push image → Git commit → Argo CD sync → Rollout deploys.

**Why This Demo Matters:** ImageChange triggers are one of the most loved features of DeploymentConfig. This demo shows you can get the same automation with GitOps benefits—and it's included in OpenShift GitOps.

## Part 1: The Challenge (1 min)

### 📊 Slide: DeploymentConfig ImageChange - Simple but Limited

```
DeploymentConfig + ImageStream (the old way):
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
triggers:
  - type: ImageChange
    imageChangeParams:
      automatic: true
      from:
        kind: ImageStreamTag
        name: myapp:latest

✅ Simple: Built-in, zero extra config
✅ Automatic: New image → instant deploy
❌ No audit trail: Ephemeral trigger
❌ No review: Deploys to prod immediately
❌ OpenShift-specific: Not portable
```

### 💬 Say:

> "DeploymentConfig ImageChange triggers were simple. Zero setup, automatic deploys. But they had downsides: no audit trail of when or why an image changed, no approval gate before production, and OpenShift-specific. Let's see how Image Updater gives us the same automation with GitOps benefits."

## Part 2: Enable Image Updater (2 min)

### 📊 Slide: Step 1 - Enable in OpenShift GitOps

```yaml
# Edit the ArgoCD custom resource
apiVersion: argoproj.io/v1beta1
kind: ArgoCD
metadata:
  name: argocd
  namespace: openshift-gitops
spec:
  imageUpdater:
    enabled: true    # ← Add this line
```

### 💻 Terminal - Pane B:

```bash
# Edit ArgoCD CR
oc edit argocd argocd -n openshift-gitops

# Add spec.imageUpdater.enabled: true
# Save and exit

# Wait for Image Updater pod to start
oc get pods -n openshift-gitops | grep image-updater
```

### 👁️ Watch:

- Pod `argocd-image-updater-xxxxx` appears and goes Running

### 💬 Say:

> "First step: enable Image Updater in the OpenShift GitOps operator. We edit the ArgoCD custom resource and add 'imageUpdater: enabled: true'. The operator deploys the Image Updater pod. That's it for cluster-side setup."

## Part 3: Configure Application (3 min)

### 📊 Slide: Step 2 - Configure Argo CD Application

```yaml
apiVersion: argoproj.io/v1alpha1
kind: Application
metadata:
  name: rollo-demo-4
  namespace: openshift-gitops
  annotations:
    # Tell Image Updater which image to watch
    argocd-image-updater.argoproj.io/image-list: rollo=quay.io/modzelewski/rollo

    # Update strategy: latest tag, semver, digest, etc.
    argocd-image-updater.argoproj.io/rollo.update-strategy: latest

    # Write back to Git (creates commits)
    argocd-image-updater.argoproj.io/write-back-method: git
    argocd-image-updater.argoproj.io/git-branch: main
spec:
  project: default
  source:
    repoURL: https://github.com/gmodzelewski/rollo
    path: demo4
    targetRevision: main
  destination:
    server: https://kubernetes.default.svc
    namespace: rollo-demo
  syncPolicy:
    automated:
      prune: true
      selfHeal: true
```

### 💻 Terminal - Pane B:

```bash
# Apply the Application (already prepared in demo4/application.yaml)
oc apply -f demo4/application.yaml

# Check Application status
oc get application rollo-demo-4 -n openshift-gitops
```

### 💬 Say:

> "Second step: configure the Argo CD Application. The key is the annotations. We tell Image Updater: watch quay.io/modzelewski/rollo, use the 'latest' tag strategy, and write changes back to Git. The Application points to our Git repo where the Rollout manifest lives."

### 📊 Slide: Step 3 - Git Credentials (Important!)

```
Image Updater needs to PUSH commits to your Git repo.

Options:
1. SSH key (recommended)
2. Git token (HTTPS)
3. Use Argo CD repo credentials (if already configured)

For this demo: We've already configured Git credentials
in the argocd-image-updater-config ConfigMap.

Production: Use a deploy key with write access to your repo.
```

### 💬 Say:

> "Important: Image Updater needs to push commits to Git. You need to give it credentials—either an SSH key or a Git token. For this demo, we've pre-configured this. In production, use a deploy key with write access to your repository."

## Part 4: Watch the Magic Happen (4 min)

### 💻 Terminal - Pane A:

```bash
# Watch Argo CD Application
oc get application rollo-demo-4 -n openshift-gitops -w
```

### 💻 Terminal - Pane B:

```bash
# Watch Rollout status
oc argo rollouts get rollout rollo-demo-4 -n rollo-demo --watch
```

### 📊 Slide: The Flow

```
What happens when a new image is pushed:

1. Developer pushes quay.io/modzelewski/rollo:v3 to registry
2. Image Updater polls registry (every 2 minutes by default)
3. Image Updater detects new tag: v3
4. Image Updater:
   - Clones your Git repo
   - Updates demo4/rollout.yaml: image: rollo:v2 → rollo:v3
   - Commits: "build: automatic update of rollo to v3"
   - Pushes to Git
5. Argo CD detects Git commit
6. Argo CD syncs the Application
7. Rollout controller deploys v3

All automatic. All auditable in Git.
```

### 💬 Say:

> "Here's the flow. We've pushed a new image—rollo:v3—to Quay. Image Updater polls the registry every two minutes. When it sees v3, it clones our Git repo, updates the Rollout manifest, creates a commit, and pushes. Now watch—Argo CD will detect the Git change and sync."

### 👁️ Watch (Pane A - Application):

- Application shows `OutOfSync` briefly
- Then shows `Syncing`
- Then shows `Synced`

### 👁️ Watch (Pane B - Rollout):

- New ReplicaSet created for v3
- Rollout progresses (following whatever strategy is configured)
- Status: Healthy

### 💬 Say (when Git commit happens):

> "There it is. Image Updater just committed to Git. Let's look at the commit."

### 💻 Terminal - Pane B (optional):

```bash
# Show recent Git commits (if you have the repo cloned locally)
cd /path/to/your/repo
git log --oneline -5

# Or show via GitHub/GitLab web UI
```

### 👁️ Watch (Git):

```
abc1234 build: automatic update of rollo
def5678 Add demo4 manifests
...
```

### 💬 Say:

> "Look at that commit. 'build: automatic update of rollo.' It shows the old tag, the new tag, when it happened. If something goes wrong, we can revert this commit. That's your audit trail. That's what you didn't have with DeploymentConfig triggers."

## Part 5: Takeaway (1 min)

### 📊 Slide: Comparison

```
DeploymentConfig ImageChange    vs    Argo CD Image Updater
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
✅ Built-in (zero setup)              ❌ Requires setup
✅ Automatic                          ✅ Automatic
❌ No audit trail                     ✅ Git commits (full audit)
❌ No approval gate                   ✅ Can add PR review
❌ Can't rollback easily              ✅ Git revert
❌ OpenShift-only                     ✅ Works anywhere (K8s)

Trade-off: More setup, more control
```

### 💬 Say:

> "Here's the honest comparison. DeploymentConfig was simpler to set up. Image Updater requires configuration, Git credentials, understanding GitOps. But you get something DC never had: every change is in Git. You can review it, approve it via pull request, revert it if needed. For production, most teams consider this a better pattern. It's not simpler, but it's more controlled."

---

# SUPPORTING CONTENT

## Section 4: Troubleshooting & Safety (2 min)

### 📊 Slide: What If Something Goes Wrong?

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

### 💬 Say:

> "What if the new version crashes? Rollouts pause automatically if pods aren't ready. You can abort, rollback to a previous revision, or check history. Safety is built-in."

### 📊 Slide: Safety Features

```
- Automatic pause if new pods fail readiness checks
- Manual promotion for Blue-Green and Canary (you control timing)
- Abort at any step → instant rollback to stable
- Revision history preserved (view and rollback to any version)
- Progressive rollout limits blast radius (canary steps)
- Integration with metrics (AnalysisTemplates) for automated checks
```

### 💬 Say:

> "Safety is built-in. Rollouts won't blindly deploy broken versions. You have multiple escape hatches—abort, undo, or revert via Git. For critical apps, you can add AnalysisTemplates to automatically check metrics and abort if thresholds are exceeded."

---

## Section 5: Recap & Migration (2 min)

### 📊 Slide: What We Saw

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

### 💬 Say:

> "Recap: We skipped plain Deployments and went straight to Rollouts. Why? Look at this table. Deployments give you basic rolling updates—nothing more. Rollouts give you that PLUS Blue-Green, Canary with steps, manual promotion, and GitOps-driven automation. You get everything a Deployment does, plus features it doesn't have. The migration isn't a compromise; it's an upgrade."

---

## Section 6: Migration Guide (3 min)

### 📊 Slide: How to Migrate Your Apps

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

### 💬 Say:

> "Practical migration: understand that we're skipping the intermediate step. Instead of DC → Deployment → Rollout, we go DC → Rollout directly. The pod template stays identical—only the resource kind and strategy change. Replace ImageChange triggers with GitOps. Always test on non-prod first."

---

## Section 7: Installation (5 min)

### 📊 Slide: Enabling Argo Rollouts on OpenShift

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

### 💻 Terminal - Pane B (optional):

```bash
# Show existing RolloutManager
oc get rolloutmanager -A

# Show CRD
oc get crd rollouts.argoproj.io

# Show controller pod
oc get pods -n openshift-gitops | grep rollout
```

### 💬 Say:

> "Installation is straightforward via OpenShift GitOps. One custom resource—RolloutManager—and the operator handles the rest. Controller deploys, CRDs are installed, you're ready to create Rollouts."

### 📊 Slide: CLI Installation

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

### 💬 Say:

> "For local development, install the oc plugin. It gives you the watch, promote, abort commands we used today."

---

## Section 8: FAQ (3 min)

### 📊 Slide: Common Questions

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

Q: We heavily use ImageChange triggers. Is the migration harder?
A: Honest answer: Yes, you'll need extra setup. DC ImageChange was simpler.

   Options:
   1. Argo CD Image Updater (GitOps, included in OpenShift GitOps)
      - Requires: Git credentials, Application annotations, GitOps understanding
      - Benefit: Audit trail in Git, reviewable commits, rollback via Git revert

   2. CI/CD pipeline updates manifests (Jenkins/Tekton)
      - Pipeline updates rollout.yaml after image build
      - Common pattern in enterprise environments

   3. Manual updates for simple/low-traffic apps
      - Developer edits Git when ready to deploy
      - Good for non-critical apps

   Trade-off: Less automatic, more controlled.
   Many production teams actually prefer this over "auto-deploy to prod."

Q: What's the migration effort?
A: Low for simple DCs (YAML conversion). Higher if complex triggers/hooks.

Q: Can we test on non-prod first?
A: Absolutely recommended. Start small.

Q: Do we need to change application code?
A: No. Only infrastructure (manifests).
```

### 💬 Say:

> "Most common question: why skip Deployments? Answer: because you don't lose anything. Rollouts are built on the same ReplicaSet mechanism. You get all of Deployment's capabilities plus Blue-Green and Canary. No code changes needed—this is purely infrastructure. Start with non-prod apps."
>
> "One question that comes up a lot: we rely on ImageChange triggers. DeploymentConfig made this simple—built-in, zero setup. I'll be honest: Rollouts require extra setup for this. You can use Image Updater, a CI/CD pipeline, or manual Git updates. It's more complex, but you gain something: everything in Git with an audit trail. Many production teams actually prefer this because auto-deploying to production with no review was risky. We showed Image Updater in action in Demo 4—same automation, better control."

---

## Section 9: Resources (1 min)

### 📊 Slide: Learn More

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

### 💬 Say:

> "All docs are linked here. Demo manifests are in the repo. Argo Rollouts has an active community—Slack and GitHub if you hit issues."

---

## Section 10: Next Steps & Advanced Topics (3 min)

🎯 **Goal:** Point to advanced features and next steps for teams adopting Rollouts.

### 📊 Slide: What We've Covered

```
✅ Demo 1: Rolling updates (same as DC, new resource kind)
✅ Demo 2: Blue-Green (preview-then-promote)
✅ Demo 3: Canary with steps (progressive delivery)
✅ Demo 4: Image Updater (GitOps automation for ImageChange)

You now know:
- How to migrate DC → Rollout
- Blue-Green and Canary strategies
- How to automate image updates with GitOps
```

### 💬 Say:

> "We've covered the complete migration story: basic rolling updates, advanced Blue-Green and Canary strategies, and GitOps automation with Image Updater. You've seen how to replace everything DeploymentConfigs did, plus new capabilities they didn't have."

### 📊 Slide: Advanced Features (For Later)

```
Traffic Management:
- Integrate with OpenShift Routes for exact traffic %
- Use Service Mesh (Istio) for fine-grained control
- See: argo-rollouts.readthedocs.io/features/traffic-management/

Analysis & Metrics:
- AnalysisTemplates for automated metric checks
- Query Prometheus during rollout
- Auto-abort if error rate exceeds threshold
- See: argo-rollouts.readthedocs.io/features/analysis/

Notifications:
- Slack/email alerts on rollout events
- Integration with Argo CD Notifications
- Custom webhooks for approval systems

Multi-cluster:
- ApplicationSets for deploying to multiple clusters
- Progressive rollout across environments
- Region-by-region deployment strategies
```

### 💬 Say:

> "There's more you can do once you master the basics. Traffic management lets you split traffic by exact percentages using Routes or Service Mesh. AnalysisTemplates let you automate rollout decisions based on metrics—like auto-aborting if error rates spike. And you can integrate with notification systems for Slack alerts or approval workflows. These are topics for your next steps after the basic migration."

### 📊 Slide: Your Migration Journey

```
Phase 1: Learn (1-2 weeks)
- Pick 1-2 simple non-prod apps
- Convert DC → Rollout
- Test rolling updates, Blue-Green, Canary
- Get comfortable with oc argo rollouts commands

Phase 2: Automate (2-4 weeks)
- Set up Image Updater or pipeline automation
- Configure Git credentials
- Test automated deployments
- Document your process

Phase 3: Scale (ongoing)
- Migrate more non-prod apps
- Move to staging, then production
- Add AnalysisTemplates for critical apps
- Establish team runbooks

Don't rush. DeploymentConfigs still work.
Migrate when you're ready, one app at a time.
```

### 💬 Say:

> "Here's a realistic migration journey. Start small with one or two non-production apps. Get comfortable with the commands and strategies. Then add automation with Image Updater or your CI/CD pipeline. Finally, scale to more apps and production environments. Don't rush—DeploymentConfigs still work. Migrate when you're ready, one application at a time."

---

## Section 11: Q&A (5-10 min)

### 📊 Slide: Questions?

### 💬 Say:

> "That's the end of the prepared content. Questions?"

---

# QUICK REFERENCE

## Terminal Commands Cheat Sheet

### Demo 1 Commands

```bash
# Pane A: Watch rollout
oc argo rollouts get rollout rollo-demo-1 -n rollo-demo --watch

# Pane B: Update image
oc argo rollouts set image rollo-demo-1 rollo=quay.io/modzelewski/rollo:v2 -n rollo-demo
```

### Demo 2 Commands

```bash
# Pane A: Watch rollout
oc argo rollouts get rollout rollo-demo-2 -n rollo-demo --watch

# Pane B: Update image
oc argo rollouts set image rollo-demo-2 rollo=quay.io/modzelewski/rollo:v2 -n rollo-demo

# Pane B: Promote (after testing preview)
oc argo rollouts promote rollo-demo-2 -n rollo-demo

# Optional: Abort (if something is wrong)
oc argo rollouts abort rollo-demo-2 -n rollo-demo
```

### Demo 3 Commands

```bash
# Pane A: Watch rollout
oc argo rollouts get rollout rollo-demo-3 -n rollo-demo --watch

# Pane B: Update image
oc argo rollouts set image rollo-demo-3 rollo=quay.io/modzelewski/rollo:v2 -n rollo-demo

# Pane B: Promote (after checking 20% canary)
oc argo rollouts promote rollo-demo-3 -n rollo-demo

# Optional: Abort (if something is wrong)
oc argo rollouts abort rollo-demo-3 -n rollo-demo
```

### Demo 4 Commands (Optional - Image Updater)

```bash
# Enable Image Updater in ArgoCD CR
oc edit argocd argocd -n openshift-gitops
# Add: spec.imageUpdater.enabled: true

# Check Image Updater pod
oc get pods -n openshift-gitops | grep image-updater

# Apply Argo CD Application
oc apply -f demo4/application.yaml

# Watch Application sync
oc get application rollo-demo-4 -n openshift-gitops -w

# Watch Rollout (separate terminal)
oc argo rollouts get rollout rollo-demo-4 -n rollo-demo --watch

# Check Image Updater logs (troubleshooting)
oc logs -n openshift-gitops deployment/argocd-image-updater -f

# View Git commits (if repo cloned locally)
cd /path/to/repo && git log --oneline -5
```

### General Commands

```bash
# List all rollouts
oc argo rollouts list rollouts -n rollo-demo

# Check rollout status
oc argo rollouts status <rollout-name> -n rollo-demo

# View revision history
oc argo rollouts history <rollout-name> -n rollo-demo

# Rollback to previous version
oc argo rollouts undo <rollout-name> -n rollo-demo

# Get Routes (for Demo 2 URLs)
oc get routes -n rollo-demo

# Watch pods (optional 3rd pane)
watch -n 1 'oc get pods -n rollo-demo -l app=rollo-demo-1'
```

---

## Timing Summary

### Complete Presentation (Recommended - All 4 Demos)

| Section | Duration | Total |
|---------|----------|-------|
| Opening & Context | 4 min | 4 min |
| Demo 1: Rolling Update | 8 min | 12 min |
| ImageChange Transition | 3 min | 15 min |
| Demo 2: Blue-Green | 8 min | 23 min |
| Demo 3: Canary with Steps | 8 min | 31 min |
| **Demo 4: Image Updater** | **10 min** | **41 min** |
| Troubleshooting | 2 min | 43 min |
| Recap & Migration | 5 min | 48 min |
| Installation | 5 min | 53 min |
| FAQ | 3 min | 56 min |
| Resources | 1 min | 57 min |
| Next Steps | 3 min | 60 min |
| **Total Core Content** | | **60 min** |
| Q&A | +10 min | **70 min** |

### Shorter Version (If Time Constrained)

If you only have 60 minutes total, you can:
- Skip Demo 4 (reduce by 10 min)
- Shorten FAQ section (reduce by 2 min)
- Shorten Next Steps (reduce by 2 min)
- Total: ~50 min + 10 min Q&A = 60 min

**Important:** Demo 4 is highly recommended because ImageChange triggers are a critical DC feature. Skipping it leaves a gap in the migration story.

---

## Emergency Backup

**If a demo fails:**
1. Switch to showing the relevant demo README (demo1/2/3/README.md)
2. Walk through the YAML files instead of live demo
3. Reference the commands that would have been run
4. Show expected output in the README

**If terminal is unresponsive:**
1. Have a backup terminal ready
2. Re-run the watch command in the new terminal
3. Continue from where you left off

**If cluster is down:**
1. Have screenshots of successful rollouts ready
2. Walk through the manifests and explain the flow
3. Show the YAML files directly

---

**END OF PRESENTATION SCRIPT**
