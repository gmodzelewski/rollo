install argo rollouts for oc tool
- `brew install argoproj/tap/kubectl-argo-rollouts`

test 
- `kubectl argo rollouts version`


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



Sources:
- https://argo-rollouts.readthedocs.io/en/stable/installation/
- https://github.com/SMACAcademy/Mastering-Argo-Rollouts-Progressive-Delivery-in-Kubernetes
- https://docs.redhat.com/en/documentation/red_hat_openshift_gitops/1.19/html/argo_rollouts
