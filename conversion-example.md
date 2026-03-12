# DeploymentConfig to Rollout Conversion Example

This document shows a practical example of converting a DeploymentConfig to an Argo Rollout.

**Important:** We are **skipping Deployment objects** and going straight from DeploymentConfig to Rollout. Why? Because Rollouts manage ReplicaSets the same way Deployments do, but with Blue-Green, Canary, and progressive delivery built-in. You get everything a Deployment offers plus advanced strategies.

---

## Before: DeploymentConfig

```yaml
apiVersion: apps.openshift.io/v1
kind: DeploymentConfig
metadata:
  name: my-app
  namespace: my-namespace
spec:
  replicas: 3
  selector:
    app: my-app
    deploymentconfig: my-app
  template:
    metadata:
      labels:
        app: my-app
        deploymentconfig: my-app
    spec:
      containers:
        - name: my-app
          image: quay.io/myorg/my-app:v1.0.0
          ports:
            - containerPort: 8080
              protocol: TCP
          resources:
            limits:
              cpu: 500m
              memory: 512Mi
            requests:
              cpu: 100m
              memory: 128Mi
          livenessProbe:
            httpGet:
              path: /health
              port: 8080
            initialDelaySeconds: 30
          readinessProbe:
            httpGet:
              path: /ready
              port: 8080
            initialDelaySeconds: 10
  triggers:
    - type: ConfigChange
    - type: ImageChange
      imageChangeParams:
        automatic: true
        containerNames:
          - my-app
        from:
          kind: ImageStreamTag
          name: my-app:latest
  strategy:
    type: Rolling
    rollingParams:
      maxSurge: 25%
      maxUnavailable: 25%
      timeoutSeconds: 600
```

---

## After: Rollout (Rolling Update Style)

**Goal:** Same behavior as DC—rolling update when image changes.

```yaml
apiVersion: argoproj.io/v1alpha1
kind: Rollout
metadata:
  name: my-app
  namespace: my-namespace
spec:
  replicas: 3
  revisionHistoryLimit: 5
  selector:
    matchLabels:
      app: my-app
  template:
    metadata:
      labels:
        app: my-app
    spec:
      containers:
        - name: my-app
          image: quay.io/myorg/my-app:v1.0.0  # ← GitOps updates this
          ports:
            - containerPort: 8080
              protocol: TCP
          resources:
            limits:
              cpu: 500m
              memory: 512Mi
            requests:
              cpu: 100m
              memory: 128Mi
          livenessProbe:
            httpGet:
              path: /health
              port: 8080
            initialDelaySeconds: 30
          readinessProbe:
            httpGet:
              path: /ready
              port: 8080
            initialDelaySeconds: 10
  strategy:
    canary:
      # Empty steps = rollout to 100% like a rolling update
      steps: []
      # Optional: control surge/unavailable like DC rolling params
      maxSurge: "25%"
      maxUnavailable: "25%"
```

**Key changes:**
1. `kind: DeploymentConfig` → `kind: Rollout`
2. `spec.template` is **identical** (same pod definition)
3. Removed `triggers:` section
4. Added `strategy.canary` with empty steps (rolling-update behavior)
5. Simplified `selector` (Rollouts use `matchLabels`, not implicit label injection)

**What about ImageChange trigger?**
- Use **Argo CD Image Updater** to watch the image registry
- When a new image is pushed, Image Updater updates the Rollout manifest in Git
- Argo CD syncs the change → Rollout controller deploys the new version
- Same outcome, but desired state is now in Git (auditable, rollback-able)

---

## After: Rollout (Blue-Green)

**Goal:** Deploy new version to preview, manually promote to production.

**Service definitions** (required for Blue-Green):

```yaml
---
apiVersion: v1
kind: Service
metadata:
  name: my-app-active
  namespace: my-namespace
spec:
  ports:
    - port: 8080
      targetPort: 8080
      protocol: TCP
  selector:
    app: my-app  # Rollout controller manages the actual pod selector
---
apiVersion: v1
kind: Service
metadata:
  name: my-app-preview
  namespace: my-namespace
spec:
  ports:
    - port: 8080
      targetPort: 8080
      protocol: TCP
  selector:
    app: my-app  # Rollout controller manages the actual pod selector
```

**Rollout with Blue-Green strategy:**

```yaml
apiVersion: argoproj.io/v1alpha1
kind: Rollout
metadata:
  name: my-app
  namespace: my-namespace
spec:
  replicas: 3
  revisionHistoryLimit: 5
  selector:
    matchLabels:
      app: my-app
  template:
    metadata:
      labels:
        app: my-app
    spec:
      containers:
        - name: my-app
          image: quay.io/myorg/my-app:v1.0.0
          ports:
            - containerPort: 8080
              protocol: TCP
          resources:
            limits:
              cpu: 500m
              memory: 512Mi
            requests:
              cpu: 100m
              memory: 128Mi
          livenessProbe:
            httpGet:
              path: /health
              port: 8080
            initialDelaySeconds: 30
          readinessProbe:
            httpGet:
              path: /ready
              port: 8080
            initialDelaySeconds: 10
  strategy:
    blueGreen:
      activeService: my-app-active    # Production traffic
      previewService: my-app-preview  # Testing/preview traffic
      autoPromotionEnabled: false     # Manual promotion required
      scaleDownDelaySeconds: 30       # Keep old version for 30s after promotion
```

**Usage:**
1. Deploy new image → preview service gets v2, active stays on v1
2. Test new version via preview service
3. When ready: `oc argo rollouts promote my-app -n my-namespace`
4. Active service switches to v2

---

## After: Rollout (Canary with Steps)

**Goal:** Progressive rollout with pause points (20% → 40% → 60% → 80% → 100%).

**Service definitions** (required for Canary):

```yaml
---
apiVersion: v1
kind: Service
metadata:
  name: my-app-stable
  namespace: my-namespace
spec:
  ports:
    - port: 8080
      targetPort: 8080
      protocol: TCP
  selector:
    app: my-app  # Rollout controller manages the actual pod selector
---
apiVersion: v1
kind: Service
metadata:
  name: my-app-canary
  namespace: my-namespace
spec:
  ports:
    - port: 8080
      targetPort: 8080
      protocol: TCP
  selector:
    app: my-app  # Rollout controller manages the actual pod selector
```

**Rollout with Canary strategy:**

```yaml
apiVersion: argoproj.io/v1alpha1
kind: Rollout
metadata:
  name: my-app
  namespace: my-namespace
spec:
  replicas: 5  # 5 replicas for clearer percentage splits
  revisionHistoryLimit: 5
  selector:
    matchLabels:
      app: my-app
  template:
    metadata:
      labels:
        app: my-app
    spec:
      containers:
        - name: my-app
          image: quay.io/myorg/my-app:v1.0.0
          ports:
            - containerPort: 8080
              protocol: TCP
          resources:
            limits:
              cpu: 500m
              memory: 512Mi
            requests:
              cpu: 100m
              memory: 128Mi
          livenessProbe:
            httpGet:
              path: /health
              port: 8080
            initialDelaySeconds: 30
          readinessProbe:
            httpGet:
              path: /ready
              port: 8080
            initialDelaySeconds: 10
  strategy:
    canary:
      canaryService: my-app-canary  # Canary traffic goes here
      stableService: my-app-stable  # Stable traffic goes here
      steps:
        - setWeight: 20              # Step 1: 20% to canary (1 pod)
        - pause: {}                  # Manual pause (wait for promote)
        - setWeight: 40              # Step 2: 40% to canary (2 pods)
        - pause:
            duration: 30             # Auto-advance after 30 seconds
        - setWeight: 60              # Step 3: 60% to canary (3 pods)
        - pause:
            duration: 30
        - setWeight: 80              # Step 4: 80% to canary (4 pods)
        - pause:
            duration: 30
        # Final step: 100% (all 5 pods on new version)
```

**Usage:**
1. Deploy new image → rollout pauses at 20%
2. Check metrics, logs, errors
3. `oc argo rollouts promote my-app -n my-namespace` → proceeds through steps
4. Each step auto-advances after 30s
5. At any point: `oc argo rollouts abort my-app -n my-namespace` → rollback

---

## Comparison Table

| Aspect | DeploymentConfig | Deployment | Rollout (Rolling) | Rollout (Blue-Green) | Rollout (Canary) |
|--------|------------------|------------|-------------------|----------------------|------------------|
| **Resource kind** | DeploymentConfig | Deployment | Rollout | Rollout | Rollout |
| **Pod template** | spec.template | spec.template | spec.template (same) | spec.template (same) | spec.template (same) |
| **Manages** | ReplicaSet | ReplicaSet | ReplicaSet (same!) | ReplicaSet (same!) | ReplicaSet (same!) |
| **Update strategy** | Rolling | RollingUpdate | Canary (empty steps) | Blue-Green | Canary (with steps) |
| **Services** | 1 service | 1 service | 1 service | 2 (active + preview) | 2 (stable + canary) |
| **Auto-deploy on new image** | ImageChange trigger | None | GitOps + Image Updater | GitOps + Image Updater | GitOps + Image Updater |
| **Manual promotion** | No | No | No | Yes (promote command) | Yes (at pause steps) |
| **Rollback** | `oc rollout undo` | `kubectl rollout undo` | `oc argo rollouts undo` | `oc argo rollouts undo` or abort | `oc argo rollouts undo` or abort |
| **Use case** | Standard rolling | Standard rolling | Same as DC/Deployment rolling | Preview-then-promote | Progressive with control |

**Key insight:** Deployment and Rollout both manage ReplicaSets the same way. Rollout just gives you more control over the rollout strategy. By skipping Deployment, you're not losing capabilities—you're gaining Blue-Green and Canary options that Deployment doesn't have.

---

## Migration Checklist

- [ ] Understand the context:
  - OpenShift recommends: DC → Deployment
  - This guide: DC → Rollout (skip Deployment)
  - Why? Rollouts = Deployment capabilities + Blue-Green/Canary
- [ ] Read OpenShift DC → Deployment migration guide for background context
- [ ] Choose target strategy (rolling / blue-green / canary)
- [ ] Copy `spec.template` from DC to Rollout (should be identical)
- [ ] Remove `triggers:` section from DC
- [ ] Add `strategy:` section to Rollout (canary/blueGreen)
- [ ] Create required Services (1 for rolling, 2 for blue-green/canary)
- [ ] Update `selector` to use `matchLabels` (remove implicit DC labels)
- [ ] Set up GitOps automation (Argo CD + Image Updater) to replace ImageChange triggers
- [ ] Test on non-production app first
- [ ] Verify:
  - `oc argo rollouts get rollout <name> --watch` shows healthy status
  - Update image and confirm rollout behavior
  - Test promote/abort commands (for blue-green/canary)
- [ ] Document any DC-specific features you can't migrate (e.g. lifecycle hooks → convert to Jobs)
- [ ] Plan gradual migration (non-prod → staging → prod)

---

## What About Lifecycle Hooks?

**DeploymentConfig hooks** (pre, mid, post) are **not supported** in Rollouts.

**Alternatives:**
1. **Kubernetes Jobs**: Run Jobs before/after deployment
2. **Argo Workflows**: Complex multi-step workflows
3. **Rollout Analysis**: Use AnalysisTemplates to run checks during rollout (e.g. metrics queries, tests)
4. **Init containers**: For pre-start logic (same as DC)

**Example: Pre-deployment database migration (DC hook → Job)**

Before (DC):
```yaml
spec:
  strategy:
    rollingParams:
      pre:
        execNewPod:
          command: ["rake", "db:migrate"]
          containerName: my-app
```

After (Job):
```yaml
apiVersion: batch/v1
kind: Job
metadata:
  name: my-app-migration-v1-0-1
spec:
  template:
    spec:
      containers:
        - name: migration
          image: quay.io/myorg/my-app:v1.0.1
          command: ["rake", "db:migrate"]
      restartPolicy: Never
```

Run Job manually or via GitOps before updating Rollout image.

---

## Resources

- [OpenShift: DeploymentConfigs vs Deployments](https://docs.openshift.com/container-platform/latest/applications/deployments/what-deployments-are.html)
- [Argo Rollouts: Spec Reference](https://argo-rollouts.readthedocs.io/en/stable/features/specification/)
- [Argo CD Image Updater](https://argocd-image-updater.readthedocs.io/)
- [Red Hat OpenShift GitOps: Argo Rollouts](https://docs.redhat.com/en/documentation/red_hat_openshift_gitops/1.19/html/argo_rollouts)
