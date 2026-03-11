# Presentation plan: DC → Deployment migration with Argo Rollouts

**Pre-deployed:** All three demos are already applied in namespace `rollo-demo`. Use this plan for **when to show what**, **when to run commands**, and **which watch commands to keep visible**.

---

## Terminal layout (recommended)

Use **two terminals** (or split panes):

| Pane | Purpose | Keep running |
|------|--------|--------------|
| **A – Watch** | Live rollout status | `kubectl argo rollouts get rollout <name> -n rollo-demo --watch` (change `<name>` per demo) |
| **B – Actions** | Apply / set image / promote / abort | Commands from the plan below |

Switch the watch in pane A to the relevant rollout when you change demo (rollo-demo-1 → rollo-demo-2 → rollo-demo-3).

**Optional third pane:** `watch -n 1 'kubectl get pods -n rollo-demo -l app=rollo-demo-1'` (or demo-2 / demo-3) to show pod names and status.

---

## 1. Intro and context (≈2 min)

| When | What to say / show | Do / show |
|------|--------------------|-----------|
| Start | Migration from DeploymentConfig to Deployment; OpenShift recommends Argo Rollouts via GitOps. Today: three short demos, installation at the end. | Slide: title + agenda (Demo 1 → 2 → 3 → Installation). |

**Terminal:** Nothing yet, or leave pane A on a generic `kubectl get rollouts -n rollo-demo` if you want.

---

## 2. Demo 1: First Rollout – “Deployment with a different kind” (≈8 min)

**Message:** Rollout = same idea as Deployment (pod template + Service). New image tag → update spec → controller rolls out. Same outcome as DC ImageChange, different mechanism.

### 2.1 Show the rollout and app (≈2 min)

| When | What to say / show | Do / show |
|------|--------------------|-----------|
| 0:00 | “First demo: a single Rollout, rolling-update style. Already running.” | **Pane A:** Start watch for Demo 1: `kubectl argo rollouts get rollout rollo-demo-1 -n rollo-demo --watch` |
| 0:30 | Show manifest: Rollout + one Service, image `quay.io/modzelewski/rollo:v1`, canary strategy with no steps. | Open `demo1/rollout.yaml` and `demo1/service.yaml` (or slide with snippet). |
| 1:00 | “CLI shows status: Healthy, one ReplicaSet, all pods ready.” | Point to pane A. Optionally: `kubectl get pods -n rollo-demo -l app=rollo-demo-1` once. |

**Watch command (Pane A):**
```bash
kubectl argo rollouts get rollout rollo-demo-1 -n rollo-demo --watch
```

### 2.2 Trigger new version (≈3 min)

| When | What to say / show | Do / show |
|------|--------------------|-----------|
| 2:00 | “We push a new image tag (v2). In real life: GitOps or pipeline updates the Rollout spec. Here we do it with the CLI.” | **Pane B:** Run: `kubectl argo rollouts set image rollo-demo-1 rollo=quay.io/modzelewski/rollo:v2 -n rollo-demo` |
| 2:10 | “Watch: new ReplicaSet created, pods roll out, no manual steps—like a rolling update.” | Keep pane A visible; let rollout complete. |
| 4:00 | “Done. One line changed (image tag); controller did the rest. Same idea as DC ImageChange, but driven by desired state in Git or pipeline.” | Optional: show app in browser (if you exposed it) to show v2. |

**Action command (Pane B):**
```bash
kubectl argo rollouts set image rollo-demo-1 rollo=quay.io/modzelewski/rollo:v2 -n rollo-demo
```

**Takeaway:** Migration = same template, new resource kind + strategy. “New image” = update Rollout spec → controller rolls out.

---

## 3. Demo 2: Blue-Green – preview then promote (≈8 min)

**Message:** Active service = production. Preview service = new version only. Promote when ready. Replaces “two DCs + switch route” with one Rollout.

### 3.1 Introduce Blue-Green (≈2 min)

| When | What to say / show | Do / show |
|------|--------------------|-----------|
| 0:00 | “Second demo: Blue-Green. Two services—active and preview. New version goes to preview first; production stays on old until we promote.” | **Pane A:** Switch watch to Demo 2: `kubectl argo rollouts get rollout rollo-demo-2 -n rollo-demo --watch` |
| 0:30 | Show manifest: `activeService`, `previewService`, `autoPromotionEnabled: false`. | Open `demo2/rollout.yaml` and `demo2/services.yaml` (or slide). |

**Watch command (Pane A):**
```bash
kubectl argo rollouts get rollout rollo-demo-2 -n rollo-demo --watch
```

### 3.2 Deploy new version and promote (≈4 min)

| When | What to say / show | Do / show |
|------|--------------------|-----------|
| 2:00 | “We deploy the new image. Traffic stays on active (v1); preview gets v2.” | **Pane B:** `kubectl argo rollouts set image rollo-demo-2 rollo=quay.io/modzelewski/rollo:v2 -n rollo-demo` |
| 2:30 | “Status shows: Paused, waiting for promotion. Preview has new version; active still on old.” | Point to pane A. |
| 4:00 | “When we’re happy, we promote. Active switches to the new ReplicaSet.” | **Pane B:** `kubectl argo rollouts promote rollo-demo-2 -n rollo-demo` |
| 5:00 | “Rollout completes. Production is now on v2. No second DeploymentConfig, no manual route switch—first-class in the Rollout.” | Let watch show completion. |

**Action commands (Pane B):**
```bash
# Deploy new version (stays on preview until promote)
kubectl argo rollouts set image rollo-demo-2 rollo=quay.io/modzelewski/rollo:v2 -n rollo-demo

# When ready, switch production to new version
kubectl argo rollouts promote rollo-demo-2 -n rollo-demo
```

### 3.3 Optional: abort (≈1 min)

| When | What to say / show | Do / show |
|------|--------------------|-----------|
| 6:00 | “If something’s wrong, we can abort; traffic stays on stable.” | **Pane B:** `kubectl argo rollouts abort rollo-demo-2 -n rollo-demo` (only if you want to show abort; otherwise skip). |

**Takeaway:** Blue-Green = “test on preview, then promote.” Replaces the pattern of two DCs + manual route switch.

---

## 4. Demo 3: Canary with steps (≈8 min)

**Message:** Roll out in steps (e.g. 20% → pause → 40% → …). Manual or timed pauses. More control than DC’s rolling strategy.

### 4.1 Introduce Canary (≈2 min)

| When | What to say / show | Do / show |
|------|--------------------|-----------|
| 0:00 | “Third demo: Canary with steps. We move traffic in stages—20%, 40%, 60%, 80%—with pauses. First pause is manual; rest are timed.” | **Pane A:** Switch watch: `kubectl argo rollouts get rollout rollo-demo-3 -n rollo-demo --watch` |
| 0:30 | Show manifest: `canaryService`, `stableService`, steps with `setWeight` and `pause`. | Open `demo3/rollout.yaml` (or slide). |

**Watch command (Pane A):**
```bash
kubectl argo rollouts get rollout rollo-demo-3 -n rollo-demo --watch
```

### 4.2 Trigger update and promote (≈4 min)

| When | What to say / show | Do / show |
|------|--------------------|-----------|
| 2:00 | “We trigger an update. Rollout will pause at 20%.” | **Pane B:** `kubectl argo rollouts set image rollo-demo-3 rollo=quay.io/modzelewski/rollo:v2 -n rollo-demo` |
| 2:30 | “Status: Paused at step 1 (20%). We decide when to continue.” | Point to pane A. |
| 4:00 | “We promote. Remaining steps run (40% → 60% → 80% → 100%) with short pauses.” | **Pane B:** `kubectl argo rollouts promote rollo-demo-3 -n rollo-demo` |
| 6:00 | “Rollout completes. Without a traffic provider, weight is approximated by replica count; with Route or Service Mesh you get exact percentages.” | Let watch show completion. |

**Action commands (Pane B):**
```bash
# Start canary (pauses at 20%)
kubectl argo rollouts set image rollo-demo-3 rollo=quay.io/modzelewski/rollo:v2 -n rollo-demo

# After checking, continue through steps
kubectl argo rollouts promote rollo-demo-3 -n rollo-demo
```

### 4.3 Optional: abort (≈1 min)

| When | What to say / show | Do / show |
|------|--------------------|-----------|
| 7:00 | “We can abort at any step; rollout reverts to stable.” | **Pane B:** `kubectl argo rollouts abort rollo-demo-3 -n rollo-demo` (optional). |

**Takeaway:** Canary with steps = controlled blast radius and pause points. Extends beyond what DC’s rolling strategy offered.

---

## 5. Recap and migration alignment (≈2 min)

| When | What to say / show | Do / show |
|------|--------------------|-----------|
| 0:00 | “Recap: Rollout = Deployment with a strategy. Same template, new kind. New image = update spec.” | Slide: table from ARGO_ROLLOUTS_DEMO_PLAN.md § 2.1 (DC vs Rollouts). |
| 1:00 | “Blue-Green replaces ‘two DCs + switch route.’ Canary with steps gives you what DC didn’t: percentage steps and pauses.” | No terminal. |

**Terminal:** Can leave pane A on any rollout or close.

---

## 6. Installation at the end (≈5 min)

**Message:** Rollouts are enabled via OpenShift GitOps (RolloutManager). Not the focus of the migration story; show when people ask “how do I get it?”

| When | What to say / show | Do / show |
|------|--------------------|-----------|
| 0:00 | “How do you get this on OpenShift? Via OpenShift GitOps: one custom resource.” | Show RolloutManager YAML (from ARGO_ROLLOUTS_DEMO_PLAN.md). |
| 1:00 | “Apply it; the operator installs the controller and CRDs.” | **Pane B:** `kubectl get rolloutmanager -A` and/or `kubectl get crd rollouts.argoproj.io` (if already installed). |
| 2:00 | “Optional: enable Rollouts UI in Argo CD server spec.” | Show `enableRolloutsUI: true` snippet. |
| 3:00 | “Docs and CLI: Red Hat OpenShift GitOps, Argo Rollouts docs, kubectl argo rollouts.” | Slide: links + `brew install argoproj/tap/kubectl-argo-rollouts`. |

**Terminal:** No watch needed; show one-off commands.

---

## Quick reference: watch commands (copy-paste)

**Demo 1**
```bash
kubectl argo rollouts get rollout rollo-demo-1 -n rollo-demo --watch
```

**Demo 2**
```bash
kubectl argo rollouts get rollout rollo-demo-2 -n rollo-demo --watch
```

**Demo 3**
```bash
kubectl argo rollouts get rollout rollo-demo-3 -n rollo-demo --watch
```

**All rollouts (overview)**
```bash
kubectl argo rollouts list rollouts -n rollo-demo
```

**Pods per demo (optional second watch)**
```bash
# Demo 1
watch -n 1 'kubectl get pods -n rollo-demo -l app=rollo-demo-1'

# Demo 2
watch -n 1 'kubectl get pods -n rollo-demo -l app=rollo-demo-2'

# Demo 3
watch -n 1 'kubectl get pods -n rollo-demo -l app=rollo-demo-3'
```

---

## Pre-presentation checklist

- [ ] Cluster has Argo Rollouts controller (RolloutManager or existing install).
- [ ] `kubectl` and `kubectl argo rollouts` work; context points to the right cluster.
- [ ] All three demos applied: `kubectl get rollouts -n rollo-demo` shows rollo-demo-1, rollo-demo-2, rollo-demo-3.
- [ ] Demo 1 is on v2 (you may have set image earlier); Demo 2 and 3 can be v1 or v2—you’ll set image during the demo.
- [ ] Two terminals (or split panes) ready: one for watch, one for action commands.
- [ ] Manifests or slides open: `demo1/rollout.yaml`, `demo2/rollout.yaml`, `demo3/rollout.yaml` (or slides with key snippets).
