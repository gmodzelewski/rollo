# Demo 4: Auto-Deploy with Argo CD Image Updater

**Goal:** Replace DeploymentConfig ImageChange triggers with Argo CD Image Updater. Show the complete flow: push image → Git commit → Argo CD sync → Rollout deploys.

**Key Message:** Image Updater provides the same "auto-deploy on new image" behavior as DC ImageChange, but with GitOps benefits: audit trail in Git, reviewable commits, and rollback via git revert.

---

## What This Demo Shows

**The Complete Auto-Deploy Flow:**

```
1. Developer pushes new image (rollo:v3) to Quay.io
2. Image Updater polls registry (every 2 min)
3. Image Updater detects new tag
4. Image Updater:
   - Clones your Git repo
   - Updates rollout.yaml: image: rollo:v2 → rollo:v3
   - Creates commit: "build: automatic update of rollo to v3"
   - Pushes to Git
5. Argo CD detects Git commit
6. Argo CD syncs the Application
7. Rollout controller deploys v3 following the strategy
```

**Result:** Fully automated, GitOps-native, with full audit trail.

---

## Prerequisites

Before running this demo, you need:

1. **OpenShift GitOps Operator** installed
2. **Argo Rollouts** enabled (RolloutManager)
3. **Git repository** with write access
4. **Git credentials** configured for Image Updater
5. **Argo CD** already managing applications

---

## Part 1: Enable Image Updater in OpenShift GitOps

### Step 1: Edit ArgoCD Custom Resource

```bash
oc edit argocd openshift-gitops -n openshift-gitops
```

Add this to the `spec` section:

```yaml
spec:
  imageUpdater:
    enabled: true
```

Save and exit.

### Step 2: Verify Image Updater Pod

```bash
oc get pods -n openshift-gitops | grep image-updater
```

Expected output:
```
argocd-image-updater-xxxxx-yyyyy   1/1     Running   0          30s
```

### Step 3: Check Image Updater Logs

```bash
oc logs -n openshift-gitops deployment/openshift-gitops-argocd-image-updater -f
```

You should see:
```
time="..." level=info msg="Starting image update cycle, considering 0 annotated application(s)"
```

---

## Part 2: Configure Git Credentials

Image Updater needs **write access** to your Git repository to commit changes.

### Option A: SSH Key (Recommended)

1. Generate an SSH key:
```bash
ssh-keygen -t ed25519 -C "openshift-gitops-argocd-image-updater" -f ./image-updater-key
```

2. Add the public key (`image-updater-key.pub`) to your Git provider:
   - **GitHub:** Settings → Deploy keys → Add (with write access)
   - **GitLab:** Settings → Repository → Deploy keys → Add (with write access)

3. Create Secret in OpenShift:
```bash
oc create secret generic git-creds \
  --from-file=sshPrivateKey=./image-updater-key \
  -n openshift-gitops
```

4. Configure Image Updater to use the secret:
```bash
oc edit configmap argocd-image-updater-config -n openshift-gitops
```

Add:
```yaml
data:
  git.user: git
  git.email: noreply@example.com
  git.commit-message-template: |
    build: automatic update of {{ .AppName }}

    updates image {{ .Image }} tag '{{ .OldTag }}' to '{{ .NewTag }}'
```

### Option B: Personal Access Token (HTTPS)

1. Create a token in your Git provider with `repo` scope

2. Create Secret:
```bash
oc create secret generic git-creds \
  --from-literal=username=YOUR-USERNAME \
  --from-literal=password=YOUR-TOKEN \
  -n openshift-gitops
```

3. Update ArgoCD Application (see below) to reference the secret

---

## Part 3: Prepare Your Git Repository

Your Git repo needs to have the Rollout manifest that Image Updater will update.

### Directory Structure

```
your-repo/
├── demo4/
│   ├── rollout.yaml       # Image Updater will update the image here
│   ├── service.yaml
│   └── route.yaml
```

### Initial rollout.yaml

```yaml
apiVersion: argoproj.io/v1alpha1
kind: Rollout
metadata:
  name: rollo-demo-4
  namespace: rollo-demo
spec:
  replicas: 3
  revisionHistoryLimit: 5
  selector:
    matchLabels:
      app: rollo-demo-4
  template:
    metadata:
      labels:
        app: rollo-demo-4
    spec:
      containers:
        - name: rollo
          image: quay.io/modzelewski/rollo:v1  # ← Image Updater will update this
          imagePullPolicy: Always
          ports:
            - name: http
              containerPort: 8080
              protocol: TCP
          resources:
            requests:
              memory: 32Mi
              cpu: 5m
  strategy:
    canary:
      steps: []  # Simple rolling update
```

Commit and push this to your Git repo.

---

## Part 4: Create Argo CD Application with Image Updater Annotations

Create `application.yaml`:

```yaml
apiVersion: argoproj.io/v1alpha1
kind: Application
metadata:
  name: rollo-demo-4
  namespace: openshift-gitops
  annotations:
    # Which image to watch
    argocd-image-updater.argoproj.io/image-list: rollo=quay.io/modzelewski/rollo

    # Update strategy: latest, semver, digest, etc.
    argocd-image-updater.argoproj.io/rollo.update-strategy: latest

    # Write back to Git (creates commits)
    argocd-image-updater.argoproj.io/write-back-method: git
    argocd-image-updater.argoproj.io/git-branch: main

    # Optional: Specify Git credentials secret
    # argocd-image-updater.argoproj.io/write-back-target: "git:secret:openshift-gitops/git-creds"
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
    syncOptions:
      - CreateNamespace=true
```

### Apply the Application

```bash
oc apply -f application.yaml
```

---

## Part 5: Watch the Magic Happen

### Terminal Setup (2 panes)

**Pane A: Watch Argo CD Application**
```bash
oc get application rollo-demo-4 -n openshift-gitops -w
```

**Pane B: Watch Rollout**
```bash
oc argo rollouts get rollout rollo-demo-4 -n rollo-demo --watch
```

### Trigger the Flow

**Option 1: Push a new image to your registry**
```bash
# If you control the image registry
docker tag quay.io/modzelewski/rollo:v2 quay.io/modzelewski/rollo:v3
docker push quay.io/modzelewski/rollo:v3
```

**Option 2: Wait for the official image** (if using the demo images)

The official `quay.io/modzelewski/rollo` repository has tags `:v1`, `:v2`, etc.

### What You'll See

1. **Image Updater logs** (check with `oc logs`):
   ```
   level=info msg="Checking image rollo=quay.io/modzelewski/rollo"
   level=info msg="Setting new image to quay.io/modzelewski/rollo:v3"
   level=info msg="Committing 1 parameter update(s) for application rollo-demo-4"
   ```

2. **Git commit** (check your Git repo):
   ```
   commit abc1234567890
   Author: argocd-image-updater <noreply@example.com>
   Date:   Thu Mar 12 10:15:00 2026

   build: automatic update of rollo-demo-4

   updates image quay.io/modzelewski/rollo tag 'v2' to 'v3'
   ```

3. **Argo CD Application** (Pane A):
   ```
   NAME           SYNC STATUS   HEALTH STATUS
   rollo-demo-4   OutOfSync     Healthy
   rollo-demo-4   Syncing       Healthy
   rollo-demo-4   Synced        Progressing
   rollo-demo-4   Synced        Healthy
   ```

4. **Rollout** (Pane B):
   - New ReplicaSet created for v3
   - Pods rolling out
   - Status: Healthy

---

## Troubleshooting

### Image Updater Not Detecting New Images

**Check polling interval:**
```bash
oc logs -n openshift-gitops deployment/argocd-image-updater | grep "Starting image update cycle"
```

Default is 2 minutes. To change:
```bash
oc edit configmap argocd-image-updater-config -n openshift-gitops
```

Add:
```yaml
data:
  interval: "1m"  # Check every 1 minute
```

### Image Updater Can't Push to Git

**Check credentials:**
```bash
oc logs -n openshift-gitops deployment/argocd-image-updater | grep -i "error\|permission"
```

Common issues:
- SSH key not added to Git provider
- Token doesn't have write permissions
- Wrong repository URL (SSH vs HTTPS)

**Verify secret exists:**
```bash
oc get secret git-creds -n openshift-gitops
```

### Application Not Syncing After Git Commit

**Check Argo CD Application:**
```bash
oc describe application rollo-demo-4 -n openshift-gitops
```

Make sure `syncPolicy.automated` is enabled.

**Force sync:**
```bash
argocd app sync rollo-demo-4
```

---

## Update Strategies

Image Updater supports different update strategies:

### 1. Latest Tag (Default)

```yaml
argocd-image-updater.argoproj.io/rollo.update-strategy: latest
```

Always updates to the most recent tag.

### 2. Semantic Versioning

```yaml
argocd-image-updater.argoproj.io/rollo.update-strategy: semver
argocd-image-updater.argoproj.io/rollo.allow-tags: "regexp:^v[0-9]+\\.[0-9]+\\.[0-9]+$"
```

Only updates to valid semver tags (e.g., v1.2.3).

### 3. Digest (SHA)

```yaml
argocd-image-updater.argoproj.io/rollo.update-strategy: digest
```

Updates to the latest digest for the current tag.

### 4. Name-based

```yaml
argocd-image-updater.argoproj.io/rollo.update-strategy: name
argocd-image-updater.argoproj.io/rollo.allow-tags: "regexp:^prod-.*"
```

Only tags matching the regex (e.g., prod-v1, prod-v2).

See: https://argocd-image-updater.readthedocs.io/en/stable/basics/update-strategies/

---

## Comparison: DeploymentConfig vs Image Updater

| Aspect | DeploymentConfig ImageChange | Argo CD Image Updater |
|--------|------------------------------|------------------------|
| **Setup** | ✅ Built-in, zero config | ❌ Requires: Git credentials, annotations, ArgoCD |
| **Automatic** | ✅ Yes | ✅ Yes |
| **Audit Trail** | ❌ No (ephemeral trigger) | ✅ Yes (Git commits) |
| **Approval Gate** | ❌ No | ✅ Can add PR review before merge |
| **Rollback** | ⚠️ `oc rollout undo` | ✅ Git revert (clear history) |
| **Portable** | ❌ OpenShift only | ✅ Works on any Kubernetes |
| **Control** | ❌ All or nothing | ✅ Fine-grained (update strategies, regex) |

**Trade-off:** More setup, more control, better audit trail.

---

## Key Takeaways

1. **Same outcome, different mechanism:** Auto-deploy on new image, but via GitOps
2. **Audit trail:** Every image update is a Git commit you can review and revert
3. **Approval gates:** Can require PR review before Image Updater's commits are merged
4. **More complex:** Requires Git credentials, ArgoCD Application annotations, understanding GitOps
5. **Better for production:** Most teams prefer auditable, reviewable changes over automatic triggers

---

## Next Steps

- Configure multiple images in one Application
- Use semver strategy for production apps
- Add AnalysisTemplates to validate new images before promoting
- Integrate with PR workflows (Image Updater → PR → review → merge → deploy)
- Explore Argo CD Notifications for alerting on image updates

---

## Resources

- [Argo CD Image Updater Docs](https://argocd-image-updater.readthedocs.io/)
- [OpenShift GitOps - Image Updater](https://docs.redhat.com/en/documentation/red_hat_openshift_gitops/1.19/html/argo_rollouts)
- [Update Strategies](https://argocd-image-updater.readthedocs.io/en/stable/basics/update-strategies/)
- [Git Write-Back Methods](https://argocd-image-updater.readthedocs.io/en/stable/basics/update-methods/)
