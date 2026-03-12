# OpenShift-Specific Updates

This document summarizes all OpenShift-specific changes made to the Argo Rollouts demo materials.

---

## Summary of Changes

All materials have been updated to be OpenShift-native:
- ✅ All `kubectl` commands replaced with `oc`
- ✅ OpenShift Routes created instead of Ingress
- ✅ Route manifests added to all demos
- ✅ Documentation updated with Route-specific instructions
- ✅ Browser demo instructions use Routes instead of port-forward
- ✅ Traffic management references updated to OpenShift Routes

---

## New Files Created

### Route Manifests

1. **demo1/route.yaml**
   - Single Route for the demo1 service
   - Edge TLS termination
   - Exposes the application externally

2. **demo2/routes.yaml**
   - Two Routes: `rollo-demo-2-active` (production) and `rollo-demo-2-preview` (testing)
   - Enables browser-based Blue-Green demonstration
   - Both use edge TLS termination

3. **demo3/routes.yaml**
   - Two Routes: `rollo-demo-3-stable` and `rollo-demo-3-canary`
   - Used for canary deployment demonstration
   - Includes note about traffic provider integration for exact percentages

---

## Files Updated

### Demo READMEs

**demo1/README.md**
- Apply steps include `oc apply -f route.yaml`
- Added "Access the Application" section with `oc get route`
- All commands use `oc` instead of `kubectl`

**demo2/README.md**
- Apply steps include `oc apply -f routes.yaml`
- Removed port-forward instructions
- Updated to use OpenShift Routes for browser demo
- Instructions reference Route URLs instead of localhost
- All commands use `oc`

**demo3/README.md**
- Apply steps include `oc apply -f routes.yaml`
- Added access section with `oc get routes`
- All commands use `oc`
- Traffic distribution section mentions OpenShift Route integration

### Presentation Materials

**PRESENTATION_FLOW.md**
- All `kubectl` → `oc`
- Setup section uses Routes instead of port-forward
- Demo 2 browser instructions reference Route tabs
- Traffic distribution slide updated with OpenShift Route integration
- CLI installation updated with correct plugin installation

**PRESENTATION_PLAN.md**
- All `kubectl` → `oc`
- Checklist updated to reference Routes instead of port-forwards
- Terminal layout examples use `oc`

**README.md**
- Prerequisites explicitly mention OpenShift cluster
- Demo table shows Route inclusion
- Added "OpenShift-specific" note
- CLI installation instructions updated
- All commands use `oc`

**conversion-example.md**
- All `kubectl` → `oc`
- Traffic management links point to OpenShift Route docs

**FAQ.md**
- All `kubectl` → `oc`
- "Routes and Ingress" question updated to focus on OpenShift Routes
- Traffic provider section emphasizes OpenShift Routes
- CLI installation updated

**ARGO_ROLLOUTS_DEMO_PLAN.md**
- All `kubectl` → `oc`

---

## OpenShift Route Configuration

All Routes use the following configuration:
```yaml
spec:
  tls:
    termination: edge
    insecureEdgeTerminationPolicy: Redirect
  wildcardPolicy: None
```

**Features:**
- **Edge TLS termination:** Route terminates TLS, backend communication is HTTP
- **HTTP to HTTPS redirect:** Insecure requests automatically redirected
- **Standard OpenShift pattern:** Compatible with all OpenShift clusters

---

## Accessing Applications

After applying the manifests, get Route URLs:

```bash
oc get routes -n rollo-demo
```

**Example output:**
```
NAME                     HOST/PORT
rollo-demo-1             rollo-demo-1-rollo-demo.apps.cluster.example.com
rollo-demo-2-active      rollo-demo-2-active-rollo-demo.apps.cluster.example.com
rollo-demo-2-preview     rollo-demo-2-preview-rollo-demo.apps.cluster.example.com
rollo-demo-3-stable      rollo-demo-3-stable-rollo-demo.apps.cluster.example.com
rollo-demo-3-canary      rollo-demo-3-canary-rollo-demo.apps.cluster.example.com
```

Open these URLs in your browser with `https://` prefix.

---

## Traffic Management with OpenShift Routes

For **production canary deployments** with exact percentage-based traffic splitting, configure Argo Rollouts to integrate with OpenShift Routes.

**Without integration (demo setup):**
- Traffic is load-balanced by replica count
- 20% weight = 1 canary pod, 4 stable pods
- Approximate percentage (~20% on average)

**With OpenShift Route integration:**
- Argo Rollouts manipulates Route weights
- Exact percentage control (20% of requests → canary service)
- Independent of replica count

**Configuration example:**
```yaml
strategy:
  canary:
    canaryService: my-app-canary
    stableService: my-app-stable
    trafficRouting:
      managedRoutes:
        - name: my-app-route
    steps:
      - setWeight: 20
      - pause: {}
```

**Documentation:**
https://argo-rollouts.readthedocs.io/en/stable/features/traffic-management/openshift/

---

## OpenShift GitOps Integration

All demos assume **OpenShift GitOps** (Argo CD + Argo Rollouts) is installed via the operator.

**Enable Argo Rollouts:**
```yaml
apiVersion: argoproj.io/v1beta1
kind: ArgoCD
metadata:
  name: openshift-gitops
  namespace: openshift-gitops
spec:
  server:
    route:
      enabled: true
    enableRolloutsUI: true  # Optional: Rollouts UI in Argo CD
```

**Install RolloutManager:**
```yaml
apiVersion: argoproj.io/v1alpha1
kind: RolloutManager
metadata:
  name: rollout-manager
  namespace: openshift-gitops
```

**Verify:**
```bash
oc get rolloutmanager -A
oc get crd rollouts.argoproj.io
oc get pods -n openshift-gitops | grep rollout
```

---

## CLI Usage on OpenShift

The Argo Rollouts plugin works seamlessly with `oc`:

```bash
# All these work with oc
oc argo rollouts get rollout <name> -n <namespace> --watch
oc argo rollouts promote <name> -n <namespace>
oc argo rollouts abort <name> -n <namespace>
oc argo rollouts undo <name> -n <namespace>
oc argo rollouts status <name> -n <namespace>
oc argo rollouts history <name> -n <namespace>
```

**Note:** The plugin is installed as `kubectl-argo-rollouts`, but works with `oc` as well.

---

## Demo Flow on OpenShift

### Demo 1: Rolling Update
1. Apply namespace, service, rollout, route
2. Access via `oc get route rollo-demo-1`
3. Open Route URL in browser → see v1
4. Update image to v2: `oc argo rollouts set image ...`
5. Watch rollout: `oc argo rollouts get rollout ... --watch`
6. Refresh browser → see v2

### Demo 2: Blue-Green
1. Apply namespace, services, rollout, routes
2. Get Route URLs: `oc get routes -n rollo-demo`
3. Open **both** Routes in browser tabs (active and preview)
4. Deploy v2: `oc argo rollouts set image ...`
5. Preview Route shows v2, Active Route still shows v1
6. Promote: `oc argo rollouts promote ...`
7. Active Route now shows v2

### Demo 3: Canary with Steps
1. Apply namespace, services, rollout, routes
2. Get Route URLs: `oc get routes -n rollo-demo`
3. Deploy v2: `oc argo rollouts set image ...`
4. Rollout pauses at 20% (1 canary pod, 4 stable pods)
5. Promote: `oc argo rollouts promote ...`
6. Rollout proceeds through 40%, 60%, 80%, 100% with timed pauses

---

## Differences from Generic Kubernetes

| Aspect | Kubernetes (original) | OpenShift (updated) |
|--------|----------------------|---------------------|
| CLI | `kubectl` | `oc` |
| Ingress | Ingress resources | OpenShift Routes |
| TLS | Ingress TLS config | Route edge termination |
| Access | Port-forward or LoadBalancer | Routes (built-in DNS) |
| GitOps | Argo CD standalone | OpenShift GitOps operator |
| Rollouts | Install via manifests | RolloutManager CR |

---

## Migration from Port-Forward to Routes

**Before (generic Kubernetes):**
```bash
kubectl port-forward svc/my-app 8080:80
# Access: http://localhost:8080
```

**After (OpenShift):**
```bash
oc get route my-app
# Access: https://my-app-namespace.apps.cluster.example.com
```

**Benefits:**
- No need for local port-forward processes
- Accessible from any machine (not just localhost)
- TLS included automatically
- Standard OpenShift pattern

---

## Testing the Demos

**Quick test of all demos:**
```bash
# Create namespace
oc new-project rollo-demo

# Demo 1
oc apply -f demo1/
oc get route rollo-demo-1
# Open Route URL

# Demo 2
oc apply -f demo2/
oc get routes | grep demo-2
# Open both Route URLs

# Demo 3
oc apply -f demo3/
oc get routes | grep demo-3
# Open both Route URLs

# Watch all rollouts
oc argo rollouts list rollouts -n rollo-demo
```

---

## Troubleshooting

**Routes not accessible:**
```bash
# Check route exists
oc get routes -n rollo-demo

# Check route details
oc describe route <route-name> -n rollo-demo

# Check if pods are ready
oc get pods -n rollo-demo
```

**Rollouts plugin not found:**
```bash
# Verify plugin installation
which kubectl-argo-rollouts

# Test with oc
oc argo rollouts version

# Reinstall if needed
brew reinstall argoproj/tap/kubectl-argo-rollouts
```

**Image pull errors:**
```bash
# Check if images are accessible
oc run test --image=quay.io/modzelewski/rollo:v1 --rm -it -- /bin/sh

# Check ImagePullPolicy
oc get rollout <name> -o yaml | grep imagePullPolicy
```

---

## Additional Resources

- [OpenShift GitOps Documentation](https://docs.redhat.com/en/documentation/red_hat_openshift_gitops/)
- [Argo Rollouts OpenShift Integration](https://argo-rollouts.readthedocs.io/en/stable/features/traffic-management/openshift/)
- [OpenShift Routes Documentation](https://docs.openshift.com/container-platform/latest/networking/routes/route-configuration.html)
- [Red Hat OpenShift GitOps - Argo Rollouts](https://docs.redhat.com/en/documentation/red_hat_openshift_gitops/1.19/html/argo_rollouts)

---

## Summary

All demo materials are now **OpenShift-native**:
- Commands use `oc` CLI
- External access via OpenShift Routes
- Route manifests included in all demos
- Documentation references OpenShift-specific features
- Compatible with OpenShift GitOps operator

The demos can be run on any OpenShift 4.x cluster with OpenShift GitOps installed.
