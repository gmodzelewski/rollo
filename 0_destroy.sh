#!/usr/bin/env bash
# Remove demo namespaces and everything in them. Leaves GitOps and Service Mesh operators.
set -euo pipefail

oc delete namespace \
  argo-rollouts-demo-1 \
  argo-rollouts-demo-2 \
  argo-rollouts-demo-3 \
  argo-rollouts-demo-4 \
  --ignore-not-found \
  --wait

echo "Demo namespaces deleted."
