# Argo Rollouts FAQ: DeploymentConfig Migration

Frequently asked questions about migrating from DeploymentConfigs to Deployments with Argo Rollouts.

---

## General Migration Questions

### Q: Why skip Deployments and go straight to Rollouts?

**A:** **You don't lose anything by skipping Deployments.** Here's why:

**What Deployments do:**
- Manage ReplicaSets (create, scale, delete)
- Rolling updates via RollingUpdate strategy
- Rollback/undo to previous revisions
- Revision history tracking

**What Rollouts do:**
- ✅ **Everything above** (manage ReplicaSets the same way)
- ✅ **Plus:** Blue-Green deployments
- ✅ **Plus:** Canary with progressive steps and pauses
- ✅ **Plus:** Manual promotion and abort controls
- ✅ **Plus:** Analysis runs (automated metric checks)
- ✅ **Plus:** Traffic management integration

**Under the hood:**
- Deployment → ReplicaSet → Pods
- Rollout → ReplicaSet → Pods ← **Same mechanism**

**What you might lose:**
- **Simplicity:** Rollouts require the Argo Rollouts controller (small operational dependency)
- **Recreate strategy:** Deployments support "delete all old pods first, then create new" (rare use case)

**Bottom line:** Rollouts give you everything Deployments offer plus advanced progressive delivery. Why migrate twice (DC → Deployment → Rollout) when you can do it once (DC → Rollout)?

**Only use plain Deployments if:**
- You want zero dependencies (Deployments are built-in to Kubernetes)
- You specifically need the Recreate strategy
- You want the absolute simplest migration path with no additional features

---

### Q: Can we still use DeploymentConfigs?

**A:** Yes, DeploymentConfigs still work and are supported, but they are **deprecated** in favor of standard Kubernetes Deployments. Red Hat recommends migrating to Deployments, and we recommend going one step further to Argo Rollouts for:
- Standard Kubernetes resources (not OpenShift-specific)
- Better GitOps integration
- Advanced deployment strategies (Blue-Green, Canary)
- Active upstream CNCF community and feature development

DeploymentConfigs will continue to be supported, but new features and improvements are focused on the Deployment/Rollout path.

---

### Q: What's the migration effort?

**A:** It depends on your DeploymentConfig complexity:

| DC Type | Migration Effort | Notes |
|---------|------------------|-------|
| **Simple DC** (basic rolling update, no hooks) | **Low** | Mostly YAML conversion; pod template stays the same |
| **DC with ImageChange triggers** | **Low-Medium** | Add GitOps automation (Argo CD Image Updater) |
| **DC with lifecycle hooks** (pre/mid/post) | **Medium** | Convert hooks to Jobs or Workflows |
| **Complex DC** (multiple triggers, custom logic) | **Medium-High** | May need architecture review |

**Recommendation:** Start with a simple non-production app to learn the pattern, then scale to more complex applications.

---

### Q: Do we need to change application code?

**A:** **No.** This is purely an infrastructure/deployment change. Your application code, container images, and runtime behavior remain the same. Only the deployment manifests (YAML) change.

---

### Q: Can we test this on non-prod first?

**A:** **Absolutely, and highly recommended.** Best practice:
1. Pick a simple non-prod app
2. Convert DC → Rollout
3. Test deploy, rollback, promote/abort
4. Validate monitoring, logging, alerting still work
5. Document lessons learned
6. Apply to staging, then production apps

---

## ImageChange Triggers

### Q: What about ImageChange triggers? We love auto-deploy when a new image is pushed.

**A:** The **outcome is the same**, but the **mechanism changes** from imperative triggers to declarative GitOps:

**DeploymentConfig approach:**
- ImageChange trigger watches ImageStreamTag
- New image pushed → trigger fires → DC spec updated in-cluster → deploy

**Rollout + GitOps approach:**
- Argo CD Image Updater watches image registry (Quay, Docker Hub, etc.)
- New image pushed → Image Updater updates Rollout spec **in Git** → commits change
- Argo CD syncs Git → Rollout controller deploys

**Benefits of GitOps approach:**
- **Auditable:** Git commit shows what changed and when
- **Rollback-able:** Revert Git commit to rollback
- **Consistent:** Same mechanism for all changes (not just image updates)
- **Multi-cluster:** Git is source of truth for all clusters

**Tools:**
- [Argo CD Image Updater](https://argocd-image-updater.readthedocs.io/)
- Jenkins/Tekton pipelines can update manifests in Git
- Custom automation (watch registry → update Git)

---

### Q: Can we still use ImageStreams?

**A:** Yes, but they're OpenShift-specific. For portability, consider using standard image registries (Quay, Artifactory, etc.) with Argo CD Image Updater.

If you want to keep ImageStreams:
- Use OpenShift's built-in image trigger integration
- Or use a custom controller to watch ImageStreamTags and update Git

---

## Deployment Strategies

### Q: What about DC lifecycle hooks (pre, mid, post)?

**A:** Rollouts **do not have lifecycle hooks**. DeploymentConfig hooks were OpenShift-specific.

**Alternatives:**

| DC Hook | Rollout Alternative |
|---------|---------------------|
| **Pre-deployment** (e.g. DB migration) | Kubernetes **Job** (run before updating Rollout) or Argo Workflows |
| **Mid-deployment** (e.g. health check) | **Readiness probes** (already in pod spec) |
| **Post-deployment** (e.g. smoke test) | Kubernetes **Job** or Rollout **Analysis** (metrics-based validation) |

**Example:** For database migrations, create a Job that runs before updating the Rollout image. Manage Jobs via GitOps alongside Rollouts.

**Rollout Analysis:**
- Argo Rollouts has **AnalysisTemplates** for metrics-based checks during rollout
- Example: Query Prometheus during canary to check error rate; auto-abort if threshold exceeded
- More advanced than DC hooks; integrates with observability stack

---

### Q: Which strategy should we use: Rolling, Blue-Green, or Canary?

**A:** Depends on your requirements:

| Strategy | Use Case | Complexity | Manual Steps |
|----------|----------|------------|--------------|
| **Canary (no steps)** | Simple rolling update; same as DC rolling | Low | None |
| **Blue-Green** | Preview-then-promote; want to test new version before production cutover | Medium | Promote to switch |
| **Canary (with steps)** | Progressive rollout with pause points; fine control over blast radius | Medium-High | Promote at pauses |

**Recommendation:**
- **Start with Canary (no steps)** for apps that used DC rolling updates → easiest migration
- **Use Blue-Green** if you need preview environments or manual approval before production
- **Use Canary with steps** for critical apps where you want gradual rollout with metrics checks

---

### Q: How does Canary traffic splitting work?

**A:** This repo uses two modes, in that order:

**1. Replica-based ([demo 3](demo3/)):**
- The controller rewrites `stable` / `canary` Service selectors and replica counts
- 5 replicas, 20% weight → 1 canary pod, 4 stable pods
- Each OpenShift Route stays 100% to one Service; Route weights do not follow the steps
- **Not** exact HTTP percentage on a single URL

**2. HTTPRoute weights ([demo 4](demo4/)):**
- Same canary steps; the Argo Rollouts Gateway API plugin updates HTTPRoute backend weights
- Service Mesh 3 ambient waypoint enforces the split (exact 20/40/60/80)
- A Route to the app Service skips the waypoint. A Route to the **Istio Gateway Service** is only how a browser reaches the proxy when the cluster has no LoadBalancer

Upstream Argo also has an OpenShift Route weight plugin. These demos do not use it for exact percentages.

See: https://argo-rollouts.readthedocs.io/en/stable/features/traffic-management/

---

## Integration and Compatibility

### Q: Does this work with our existing OpenShift Routes?

**A:** **Yes**, for rolling update, Blue-Green, and replica-based canary ([demo 1](demo1/)–[demo 3](demo3/)):
- **Rolling:** one Route to the Service
- **Blue-Green:** two Routes (active + preview); the Rollout switches which pods sit behind each Service
- **Canary (demo 3):** two Routes, each 100% to one Service; selectors change, Route weights do not

Exact HTTP percentages are [demo 4](demo4/): Gateway API HTTPRoute weights at the waypoint, not Route-to-app weights.

See: https://argo-rollouts.readthedocs.io/en/stable/features/traffic-management/

---

### Q: Can we use Rollouts with OpenShift Pipelines (Tekton)?

**A:** **Yes.** Common pattern:
1. Tekton pipeline builds image → pushes to registry
2. Pipeline updates Rollout manifest in Git (change image tag)
3. Argo CD syncs Git → Rollout deploys new version
4. (Optional) Pipeline calls `oc argo rollouts promote` after tests pass

Rollouts fit naturally into GitOps + CI/CD workflows.

---

### Q: What about monitoring and observability?

**A:** No change needed. Rollouts manage ReplicaSets and Pods (same as Deployments), so:
- **Logs:** Same (`oc logs`, Elasticsearch/Splunk, etc.)
- **Metrics:** Same (Prometheus scrapes pod metrics)
- **Tracing:** Same (Jaeger/Zipkin if you use it)
- **APM:** Same (Dynatrace, New Relic, etc.)

**Additional observability:**
- Argo Rollouts has a **built-in UI** (can be enabled in Argo CD)
- `oc argo rollouts get rollout <name> --watch` for CLI status
- Rollout events and status in `oc describe rollout <name>`

---

### Q: Can we integrate Rollouts with our approval process (e.g. JIRA, ServiceNow)?

**A:** **Yes.** Blue-Green and Canary have manual pause points where you can:
- Wait for human approval (via JIRA ticket, Slack notification, etc.)
- Run automated checks (tests, metrics queries)
- Call `oc argo rollouts promote` when approved (can be triggered by webhook, pipeline, etc.)

**Common pattern:**
1. Rollout reaches manual pause
2. Automation creates JIRA ticket or Slack message
3. Approver reviews, clicks "Approve"
4. Webhook/API calls `oc argo rollouts promote`
5. Rollout continues

---

## Installation and Operations

### Q: How do we enable Argo Rollouts on OpenShift?

**A:** Via **OpenShift GitOps Operator** (recommended):

1. Install OpenShift GitOps Operator (if not already installed)
2. Create a **RolloutManager** custom resource:
   ```yaml
   apiVersion: argoproj.io/v1alpha1
   kind: RolloutManager
   metadata:
     name: rollout-manager
     namespace: openshift-gitops
   ```
3. Operator installs Rollout controller, CRDs, and (optionally) UI

**Alternative:** Manual install via `oc apply` (upstream Argo Rollouts), but operator-managed is preferred on OpenShift.

See: [Red Hat OpenShift GitOps: Argo Rollouts](https://docs.redhat.com/en/documentation/red_hat_openshift_gitops/1.19/html/argo_rollouts)

---

### Q: Do we need the oc plugin?

**A:** **Recommended for local development and troubleshooting**, not required for automation.

**What it gives you:**
- `oc argo rollouts get rollout <name> --watch` (live status)
- `oc argo rollouts promote <name>` (manual promotion)
- `oc argo rollouts abort <name>` (abort rollout)
- `oc argo rollouts set image <name> ...` (update image)
- `oc argo rollouts undo <name>` (rollback)

**Install:**
- Mac: `brew install argoproj/tap/kubectl-argo-rollouts`
- Linux: Download from https://github.com/argoproj/argo-rollouts/releases/latest

**For automation (GitOps):** You don't need the plugin; Argo CD and the Rollout controller handle everything.

---

### Q: What's the resource overhead of Argo Rollouts?

**A:** Minimal. The Rollout controller is a lightweight component:
- **Memory:** ~100-200 MB (typical cluster)
- **CPU:** Low (watches Rollout resources, creates/manages ReplicaSets)
- **Pods:** 1-2 replicas of the controller (depends on HA setup)

No additional overhead per Rollout—controller manages all Rollouts in the cluster (or namespace, if scoped).

---

### Q: Can we run Rollouts in specific namespaces only?

**A:** Yes. RolloutManager can be:
- **Cluster-scoped** (watches all namespaces)
- **Namespace-scoped** (watches specific namespaces)

Configure via RolloutManager spec. See OpenShift GitOps docs for details.

---

## Rollback and Safety

### Q: How do we rollback if a deployment fails?

**A:** Multiple options:

1. **Automatic rollback (during rollout):**
   - If new ReplicaSet isn't ready (pods crash, fail readiness), rollout pauses automatically
   - Use `oc argo rollouts abort <name>` to stop and revert to stable

2. **Manual rollback (after rollout):**
   - `oc argo rollouts undo <name>` → rollback to previous revision
   - Or update Rollout manifest back to old image (via Git) → Argo CD syncs

3. **Git revert (GitOps):**
   - Revert the Git commit that changed the Rollout
   - Argo CD syncs → Rollout redeploys old version

**View revision history:**
```bash
oc argo rollouts history <name> -n <namespace>
```

---

### Q: What happens if the Rollout controller crashes?

**A:** Rollouts are declarative; state is stored in the Rollout resource (etcd). If the controller crashes:
- Existing pods keep running (no disruption)
- Controller restarts → resumes managing Rollouts from current state
- In-progress rollouts may pause until controller is back

**High availability:** Run multiple controller replicas for production.

---

## Migration Planning

### Q: Should we migrate all apps at once?

**A:** **No.** Gradual migration is best practice:

**Phase 1: Learn (1-2 weeks)**
- Pick 1-2 simple non-prod apps
- Convert DC → Rollout
- Test deploy, rollback, promote
- Document process and lessons learned

**Phase 2: Expand (1-2 months)**
- Migrate more non-prod apps
- Try different strategies (Blue-Green, Canary)
- Establish team expertise and runbooks

**Phase 3: Production (ongoing)**
- Start with low-risk production apps
- Gradually move critical apps
- Monitor, adjust, iterate

**Don't rush.** DeploymentConfigs still work; no need to migrate everything immediately.

---

### Q: What about apps with complex DC configurations?

**A:** Review case-by-case:
- **Simple rolling update:** Easy migration to Rollout
- **ImageChange triggers:** Replace with GitOps automation
- **Lifecycle hooks:** Convert to Jobs or Workflows
- **Custom triggers (Generic, GitHub):** Replace with GitOps webhooks or pipelines
- **Complex strategies (Custom, Recreate):** May need architecture discussion

For very complex DCs, consider:
1. Simplifying the deployment (do you still need all that complexity?)
2. Using Argo Workflows for complex multi-step deployments
3. Keeping the DC for now and migrating later

---

### Q: Where can we get help?

**A:** Resources:
- **Red Hat Support:** If you have an OpenShift subscription
- **Red Hat Docs:** https://docs.redhat.com/en/documentation/red_hat_openshift_gitops/1.19/html/argo_rollouts
- **Argo Rollouts Docs:** https://argo-rollouts.readthedocs.io/
- **CNCF Argo Slack:** #argo-rollouts channel (https://argoproj.github.io/community/join-slack/)
- **GitHub Issues:** https://github.com/argoproj/argo-rollouts/issues

---

## Advanced Topics

### Q: Can we use Rollouts with multi-cluster deployments?

**A:** Yes. Common pattern with Argo CD:
- Rollout manifests in Git
- Argo CD ApplicationSet for multi-cluster deployment
- Each cluster has Rollout controller
- Same Rollout spec deployed to multiple clusters

Rollouts work per-cluster; GitOps orchestrates across clusters.

---

### Q: Can we do A/B testing or feature flags with Rollouts?

**A:** **Rollouts handle deployment**, not feature flags. For A/B testing:
- Use Canary to route % of traffic to new version
- Combine with feature flag service (LaunchDarkly, Unleash, etc.) for feature-level control

Rollouts provide the infrastructure; feature flags provide the application-level control.

---

### Q: What about serverless or FaaS (Knative, OpenShift Serverless)?

**A:** Argo Rollouts is for **Deployment-based workloads** (long-running pods). For serverless:
- Knative has its own progressive delivery (traffic splitting, revisions)
- Rollouts don't apply to Knative Services

Use Rollouts for traditional microservices; use Knative's features for serverless.

---

## Summary

**Key Takeaways:**
- Migration is mostly YAML conversion; no code changes
- Start small (non-prod apps first)
- GitOps replaces ImageChange triggers with better auditability
- Blue-Green and Canary provide more control than DC strategies
- Rollouts integrate with existing OpenShift Routes, Ingress, and observability
- Gradual migration is recommended (no rush)

**Next Steps:**
1. Read the conversion steps in [README.md](README.md)
2. Run [demo 1](demo1/)–[demo 4](demo4/) in order
3. Pick a simple non-prod app to try
4. Test on dev/staging before production

**Questions?** Check the resources above or ask your platform team.
