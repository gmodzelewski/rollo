# Presentation plan: DC → Deployment migration with Argo Rollouts

**Pre-deployed:** All three demos are already applied in namespace `rollo-demo`. Use this plan for **when to show what**, **when to run commands**, and **which watch commands to keep visible**.

---

## Terminal layout (recommended)

Use **two terminals** (or split panes):

| Pane | Purpose | Keep running |
|------|--------|--------------|
| **A – Watch** | Live rollout status | `oc argo rollouts get rollout <name> -n rollo-demo --watch` (change `<name>` per demo) |
| **B – Actions** | Apply / set image / promote / abort | Commands from the plan below |

Switch the watch in pane A to the relevant rollout when you change demo (rollo-demo-1 → rollo-demo-2 → rollo-demo-3).

**Optional third pane:** `watch -n 1 'oc get pods -n rollo-demo -l app=rollo-demo-1'` (or demo-2 / demo-3) to show pod names and status.

### Visual Terminal Layout

```
┌─────────────────────────────────────────┬─────────────────────────────────────────┐
│                                         │                                         │
│  PANE A: Watch (Rollout Status)        │  PANE B: Actions (Commands)            │
│  ────────────────────────────────       │  ───────────────────────────            │
│                                         │                                         │
│  $ oc argo rollouts get rollout   │  $ oc argo rollouts set image ...  │
│    rollo-demo-1 -n rollo-demo --watch  │                                         │
│                                         │  $ oc argo rollouts promote ...    │
│  Name:            rollo-demo-1          │                                         │
│  Namespace:       rollo-demo            │  $ oc argo rollouts abort ...      │
│  Status:          ✔ Healthy             │                                         │
│  Strategy:        Canary                │                                         │
│  Images:          quay.io/.../rollo:v2  │                                         │
│  Replicas:                              │                                         │
│    Desired:       3                     │                                         │
│    Current:       3                     │                                         │
│    Updated:       3                     │                                         │
│    Ready:         3                     │                                         │
│    Available:     3                     │                                         │
│                                         │                                         │
│  NAME              KIND        STATUS   │                                         │
│  ⟳ rollo-demo-1   Rollout     ✔ Healthy│                                         │
│  └──# revision:2                        │                                         │
│     └──⧉ rollo... ReplicaSet  ✔ Healthy│                                         │
│        ├──□ rollo... Pod       ✔ Running│                                         │
│        ├──□ rollo... Pod       ✔ Running│                                         │
│        └──□ rollo... Pod       ✔ Running│                                         │
│                                         │                                         │
│  [Live updating with --watch]           │  [Ready for commands]                   │
│                                         │                                         │
└─────────────────────────────────────────┴─────────────────────────────────────────┘
│                     OPTIONAL PANE C: Pod Watch                                    │
│  ──────────────────────────────────────────────────────────────────────────       │
│  $ watch -n 1 'oc get pods -n rollo-demo -l app=rollo-demo-1'               │
│                                                                                   │
│  NAME                                    READY   STATUS    RESTARTS   AGE        │
│  rollo-demo-1-5d4c8f9b7-abcde           1/1     Running   0          2m         │
│  rollo-demo-1-5d4c8f9b7-fghij           1/1     Running   0          2m         │
│  rollo-demo-1-5d4c8f9b7-klmno           1/1     Running   0          2m         │
│                                                                                   │
└───────────────────────────────────────────────────────────────────────────────────┘
```

**Tips:**
- Use `tmux` or `iTerm2` split panes for easy management
- Keep Pane A visible throughout the presentation (status updates live)
- Use Pane B for executing commands as you narrate
- Pane C is optional but helps show pod-level changes during rollout

---

## 1. Intro and context (≈2 min)

| When | What to say / show | Do / show |
|------|--------------------|-----------|
| Start | Migration from DeploymentConfig to Deployment; OpenShift recommends Argo Rollouts via GitOps. Today: three short demos, installation at the end. | Slide: title + agenda (Demo 1 → 2 → 3 → Installation). |

**Terminal:** Nothing yet, or leave pane A on a generic `oc get rollouts -n rollo-demo` if you want.

---

## 2. Demo 1: First Rollout – “Deployment with a different kind” (≈8 min)

**Message:** Rollout = same idea as Deployment (pod template + Service). New image tag → update spec → controller rolls out. Same outcome as DC ImageChange, different mechanism.

### 2.1 Show the rollout and app (≈2 min)

| When | What to say / show | Do / show |
|------|--------------------|-----------|
| 0:00 | “First demo: a single Rollout, rolling-update style. Already running.” | **Pane A:** Start watch for Demo 1: `oc argo rollouts get rollout rollo-demo-1 -n rollo-demo --watch` |
| 0:30 | Show manifest: Rollout + one Service, image `quay.io/modzelewski/rollo:v1`, canary strategy with no steps. | Open `demo1/rollout.yaml` and `demo1/service.yaml` (or slide with snippet). |
| 1:00 | “CLI shows status: Healthy, one ReplicaSet, all pods ready.” | Point to pane A. Optionally: `oc get pods -n rollo-demo -l app=rollo-demo-1` once. |

**Watch command (Pane A):**
```bash
oc argo rollouts get rollout rollo-demo-1 -n rollo-demo --watch
```

### 2.2 Trigger new version (≈3 min)

| When | What to say / show | Do / show |
|------|--------------------|-----------|
| 2:00 | “We push a new image tag (v2). In real life: GitOps or pipeline updates the Rollout spec. Here we do it with the CLI.” | **Pane B:** Run: `oc argo rollouts set image rollo-demo-1 rollo=quay.io/modzelewski/rollo:v2 -n rollo-demo` |
| 2:10 | “Watch: new ReplicaSet created, pods roll out, no manual steps—like a rolling update.” | Keep pane A visible; let rollout complete. |
| 4:00 | “Done. One line changed (image tag); controller did the rest. Same idea as DC ImageChange, but driven by desired state in Git or pipeline.” | Optional: show app in browser (if you exposed it) to show v2. |

**Action command (Pane B):**
```bash
oc argo rollouts set image rollo-demo-1 rollo=quay.io/modzelewski/rollo:v2 -n rollo-demo
```

**Takeaway:** Migration = same template, new resource kind + strategy. “New image” = update Rollout spec → controller rolls out.

---

## 3. Demo 2: Blue-Green – preview then promote (≈8 min)

**Message:** Active service = production. Preview service = new version only. Promote when ready. Replaces “two DCs + switch route” with one Rollout.

### 3.1 Introduce Blue-Green (≈2 min)

| When | What to say / show | Do / show |
|------|--------------------|-----------|
| 0:00 | “Second demo: Blue-Green. Two services—active and preview. New version goes to preview first; production stays on old until we promote.” | **Pane A:** Switch watch to Demo 2: `oc argo rollouts get rollout rollo-demo-2 -n rollo-demo --watch` |
| 0:30 | Show manifest: `activeService`, `previewService`, `autoPromotionEnabled: false`. | Open `demo2/rollout.yaml` and `demo2/services.yaml` (or slide). |

**Watch command (Pane A):**
```bash
oc argo rollouts get rollout rollo-demo-2 -n rollo-demo --watch
```

### 3.2 Deploy new version and promote (≈4 min)

| When | What to say / show | Do / show |
|------|--------------------|-----------|
| 2:00 | “We deploy the new image. Traffic stays on active (v1); preview gets v2.” | **Pane B:** `oc argo rollouts set image rollo-demo-2 rollo=quay.io/modzelewski/rollo:v2 -n rollo-demo` |
| 2:30 | “Status shows: Paused, waiting for promotion. Preview has new version; active still on old.” | Point to pane A. **Optional:** Show browser tabs (see below). |
| 4:00 | “When we’re happy, we promote. Active switches to the new ReplicaSet.” | **Pane B:** `oc argo rollouts promote rollo-demo-2 -n rollo-demo` |
| 5:00 | “Rollout completes. Production is now on v2. No second DeploymentConfig, no manual route switch—first-class in the Rollout.” | Let watch show completion. **Optional:** Refresh browser tabs to show v2. |

**Optional: Visual Demo with Browser**
If you set up port-forwards before the presentation (see setup section above), you can show:
- Browser Tab 1 (http://localhost:8080 - active): Shows v1/blue before promote, v2/yellow after promote
- Browser Tab 2 (http://localhost:8081 - preview): Shows v2/yellow immediately after deploy

This provides visual confirmation of the Blue-Green pattern.

**Action commands (Pane B):**
```bash
# Deploy new version (stays on preview until promote)
oc argo rollouts set image rollo-demo-2 rollo=quay.io/modzelewski/rollo:v2 -n rollo-demo

# When ready, switch production to new version
oc argo rollouts promote rollo-demo-2 -n rollo-demo
```

### 3.3 Optional: abort (≈1 min)

| When | What to say / show | Do / show |
|------|--------------------|-----------|
| 6:00 | “If something’s wrong, we can abort; traffic stays on stable.” | **Pane B:** `oc argo rollouts abort rollo-demo-2 -n rollo-demo` (only if you want to show abort; otherwise skip). |

**Takeaway:** Blue-Green = “test on preview, then promote.” Replaces the pattern of two DCs + manual route switch.

---

## 4. Demo 3: Canary with steps (≈8 min)

**Message:** Roll out in steps (e.g. 20% → pause → 40% → …). Manual or timed pauses. More control than DC’s rolling strategy.

### 4.1 Introduce Canary (≈2 min)

| When | What to say / show | Do / show |
|------|--------------------|-----------|
| 0:00 | “Third demo: Canary with steps. We move traffic in stages—20%, 40%, 60%, 80%—with pauses. First pause is manual; rest are timed.” | **Pane A:** Switch watch: `oc argo rollouts get rollout rollo-demo-3 -n rollo-demo --watch` |
| 0:30 | Show manifest: `canaryService`, `stableService`, steps with `setWeight` and `pause`. | Open `demo3/rollout.yaml` (or slide). |

**Watch command (Pane A):**
```bash
oc argo rollouts get rollout rollo-demo-3 -n rollo-demo --watch
```

### 4.2 Trigger update and promote (≈4 min)

| When | What to say / show | Do / show |
|------|--------------------|-----------|
| 2:00 | “We trigger an update. Rollout will pause at 20%.” | **Pane B:** `oc argo rollouts set image rollo-demo-3 rollo=quay.io/modzelewski/rollo:v2 -n rollo-demo` |
| 2:30 | “Status: Paused at step 1 (20%). We decide when to continue.” | Point to pane A. |
| 4:00 | “We promote. Remaining steps run (40% → 60% → 80% → 100%) with short pauses.” | **Pane B:** `oc argo rollouts promote rollo-demo-3 -n rollo-demo` |
| 6:00 | “Rollout completes. Without a traffic provider, weight is approximated by replica count; with Route or Service Mesh you get exact percentages.” | Let watch show completion. |

**Action commands (Pane B):**
```bash
# Start canary (pauses at 20%)
oc argo rollouts set image rollo-demo-3 rollo=quay.io/modzelewski/rollo:v2 -n rollo-demo

# After checking, continue through steps
oc argo rollouts promote rollo-demo-3 -n rollo-demo
```

### 4.3 Optional: abort (≈1 min)

| When | What to say / show | Do / show |
|------|--------------------|-----------|
| 7:00 | “We can abort at any step; rollout reverts to stable.” | **Pane B:** `oc argo rollouts abort rollo-demo-3 -n rollo-demo` (optional). |

**Takeaway:** Canary with steps = controlled blast radius and pause points. Extends beyond what DC’s rolling strategy offered.

---

## 5. Troubleshooting and Safety (≈3 min)

**Message:** What happens if something goes wrong? Rollouts have built-in safety and rollback capabilities.

### 5.1 Automatic pausing on failures (≈1 min)

| When | What to say / show | Do / show |
|------|--------------------|-----------|
| 0:00 | "What if the new version crashes? Rollout automatically pauses if pods aren’t ready." | Slide or verbal explanation. |
| 0:30 | "Check rollout health with status command. Shows if rollout is progressing, paused, or degraded." | **Pane B (optional demo):** `oc argo rollouts status rollo-demo-3 -n rollo-demo` |

**Commands to show (optional):**
```bash
# Check detailed status (progressing/degraded/healthy)
oc argo rollouts status rollo-demo-3 -n rollo-demo

# Describe for detailed events
oc describe rollout rollo-demo-3 -n rollo-demo
```

### 5.2 Rollback options (≈1 min)

| When | What to say / show | Do / show |
|------|--------------------|-----------|
| 1:00 | "Multiple ways to rollback: abort (during rollout), undo (after rollout), or Git revert (GitOps)." | Slide with commands or verbal. |

**Rollback commands:**
```bash
# During rollout: abort and revert to stable
oc argo rollouts abort <rollout-name> -n rollo-demo

# After rollout: undo to previous revision
oc argo rollouts undo <rollout-name> -n rollo-demo

# View revision history
oc argo rollouts history <rollout-name> -n rollo-demo

# GitOps way: revert Git commit → Argo CD syncs → rollback
git revert <commit-hash>
```

### 5.3 Key safety features (≈1 min)

| When | What to say / show | Do / show |
|------|--------------------|-----------|
| 2:00 | "Built-in safety: automatic pause on failures, manual promotion controls, revision history, abort at any step." | Slide with bullet points. |

**Slide: Safety Features**
```
- Automatic pause if new pods fail readiness checks
- Manual promotion for Blue-Green and Canary (you control timing)
- Abort at any step → instant rollback to stable
- Revision history preserved (view and rollback to any version)
- Progressive rollout limits blast radius (canary steps)
- Integration with metrics (AnalysisTemplates) for automated checks
```

**Say:**
> "Safety is built-in. Rollouts won’t blindly deploy broken versions. You have multiple escape hatches—abort, undo, or revert via Git. For critical apps, you can add AnalysisTemplates to automatically check metrics and abort if thresholds are exceeded."

---

## 6. Recap and migration alignment (≈2 min)

| When | What to say / show | Do / show |
|------|--------------------|-----------|
| 0:00 | “Recap: Rollout = Deployment with a strategy. Same template, new kind. New image = update spec.” | Slide: table from ARGO_ROLLOUTS_DEMO_PLAN.md § 2.1 (DC vs Rollouts). |
| 1:00 | “Blue-Green replaces ‘two DCs + switch route.’ Canary with steps gives you what DC didn’t: percentage steps and pauses.” | No terminal. |

**Terminal:** Can leave pane A on any rollout or close.

---

## 7. Installation at the end (≈5 min)

**Message:** Rollouts are enabled via OpenShift GitOps (RolloutManager). Not the focus of the migration story; show when people ask “how do I get it?”

| When | What to say / show | Do / show |
|------|--------------------|-----------|
| 0:00 | “How do you get this on OpenShift? Via OpenShift GitOps: one custom resource.” | Show RolloutManager YAML (from ARGO_ROLLOUTS_DEMO_PLAN.md). |
| 1:00 | “Apply it; the operator installs the controller and CRDs.” | **Pane B:** `oc get rolloutmanager -A` and/or `oc get crd rollouts.argoproj.io` (if already installed). |
| 2:00 | “Optional: enable Rollouts UI in Argo CD server spec.” | Show `enableRolloutsUI: true` snippet. |
| 3:00 | “Docs and CLI: Red Hat OpenShift GitOps, Argo Rollouts docs, oc argo rollouts.” | Slide: links + `brew install argoproj/tap/oc-argo-rollouts`. |

**Terminal:** No watch needed; show one-off commands.

---

## Quick reference: watch commands (copy-paste)

**Demo 1**
```bash
oc argo rollouts get rollout rollo-demo-1 -n rollo-demo --watch
```

**Demo 2**
```bash
oc argo rollouts get rollout rollo-demo-2 -n rollo-demo --watch
```

**Demo 3**
```bash
oc argo rollouts get rollout rollo-demo-3 -n rollo-demo --watch
```

**All rollouts (overview)**
```bash
oc argo rollouts list rollouts -n rollo-demo
```

**Pods per demo (optional second watch)**
```bash
# Demo 1
watch -n 1 'oc get pods -n rollo-demo -l app=rollo-demo-1'

# Demo 2
watch -n 1 'oc get pods -n rollo-demo -l app=rollo-demo-2'

# Demo 3
watch -n 1 'oc get pods -n rollo-demo -l app=rollo-demo-3'
```

---

## Pre-presentation checklist

**Cluster and tools:**
- [ ] Cluster has Argo Rollouts controller (RolloutManager or existing install).
- [ ] `oc` and `oc argo rollouts` work; context points to the right cluster.
- [ ] All three demos applied: `oc get rollouts -n rollo-demo` shows rollo-demo-1, rollo-demo-2, rollo-demo-3.
- [ ] Verify all Rollouts are Healthy: `oc argo rollouts list rollouts -n rollo-demo`
- [ ] Demo 1 is on v2 (you may have set image earlier); Demo 2 and 3 can be v1 or v2—you’ll set image during the demo.

**Images:**
- [ ] Both image tags exist and are accessible:
  - `quay.io/modzelewski/rollo:v1`
  - `quay.io/modzelewski/rollo:v2`
- [ ] Test pull: `oc run test --image=quay.io/modzelewski/rollo:v1 --rm -it -- /bin/sh` (then exit)

**Terminal and workspace:**
- [ ] Two terminals (or split panes) ready: one for watch, one for action commands.
- [ ] Optional third pane for pod watching (if you want to show pods scaling).
- [ ] Manifests or slides open: `demo1/rollout.yaml`, `demo2/rollout.yaml`, `demo3/rollout.yaml` (or slides with key snippets).

**Visual demo (Demo 2 - OpenShift Routes):**
- [ ] Routes created for Demo 2:
  ```bash
  oc get routes -n rollo-demo
  ```
- [ ] Browser tabs open with Route URLs and showing v1:
  - rollo-demo-2-active (production)
  - rollo-demo-2-preview (testing)
- [ ] Note both Route URLs for easy access during presentation

**Presentation materials:**
- [ ] Slides ready (title, context, before/after comparison, DC vs Rollouts table, FAQ, etc.)
- [ ] Links ready: Red Hat docs, Argo Rollouts docs, demo repo URL
- [ ] Backup plan if live demo fails (screenshots or recorded video)
