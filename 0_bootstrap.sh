#!/usr/bin/env bash
# Apply initial demo state (v1). Does not set image, promote, abort, or undo.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT"

apply_yaml_dir() {
  local dir="$1"
  local f
  shopt -s nullglob
  local files=("$dir"/*.yaml)
  shopt -u nullglob
  if ((${#files[@]} == 0)); then
    echo "error: no YAML in $dir" >&2
    return 1
  fi
  for f in "${files[@]}"; do
    oc apply -f "$f"
  done
}

wait_rollout() {
  local name="$1"
  local ns="$2"
  if oc argo rollouts status "$name" -n "$ns" --timeout 2m; then
    return 0
  fi
  echo "warning: $name in $ns is not healthy yet" >&2
}

echo "Applying namespaces"
oc apply -f "$ROOT/demo0-prep/namespace.yaml"

echo "Applying demo1 demo2 demo3"
apply_yaml_dir "$ROOT/demo1"
apply_yaml_dir "$ROOT/demo2"
apply_yaml_dir "$ROOT/demo3"

echo "Labeling argo-rollouts-demo-4 for Service Mesh 3 ambient"
oc label namespace argo-rollouts-demo-4 \
  istio.io/dataplane-mode=ambient \
  istio-discovery=enabled \
  istio.io/use-waypoint=rollo-demo-4-waypoint \
  --overwrite

echo "Applying demo4"
apply_yaml_dir "$ROOT/demo4"

if oc argo rollouts version >/dev/null 2>&1; then
  echo "Waiting for Rollouts (timeout 2m each)"
  wait_rollout rollo-demo-1 argo-rollouts-demo-1
  wait_rollout rollo-demo-2 argo-rollouts-demo-2
  wait_rollout rollo-demo-3 argo-rollouts-demo-3
  wait_rollout rollo-demo-4 argo-rollouts-demo-4
else
  echo "warning: oc argo rollouts plugin not found; skip rollout status wait" >&2
fi

echo "Waiting for demo4 Gateways"
if ! oc wait --for=condition=Programmed gateway/rollo-demo-4-waypoint \
  -n argo-rollouts-demo-4 --timeout=120s; then
  echo "warning: rollo-demo-4-waypoint is not Programmed yet" >&2
fi
if ! oc wait --for=condition=Programmed gateway/rollo-demo-4-gateway \
  -n argo-rollouts-demo-4 --timeout=120s; then
  echo "warning: rollo-demo-4-gateway is not Programmed yet" >&2
fi

echo "Initial demo state is applied (images still v1)."
