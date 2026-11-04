#!/usr/bin/env bash
# Merge a release tag (or branch) from osfans/trime into the current branch.
# usage: script/sync-upstream.sh v3.3.14
set -euo pipefail

ref="${1:?usage: $0 <upstream tag or branch, e.g. v3.3.14>}"

git remote get-url upstream >/dev/null 2>&1 || git remote add upstream https://github.com/osfans/trime.git

# merge=ours in .gitattributes only works once this driver exists in the local clone.
git config merge.ours.driver true
# Remember conflict resolutions so repeated syncs don't ask twice.
git config rerere.enabled true

git fetch upstream --tags
git merge --no-ff "$ref" || true

# merge=ours does not cover files we deleted and upstream changed; keep them deleted.
for f in nightly-ci release-ci pull-request-ci check-ci; do
  p=".github/workflows/$f.yml"
  if git ls-files -u -- "$p" | grep -q .; then git rm -q "$p"; fi
done

if git ls-files -u | grep -q .; then
  echo "Unresolved conflicts remain. Fix them, then run: git commit --no-edit" >&2
  exit 1
fi
if git rev-parse -q --verify MERGE_HEAD >/dev/null; then git commit --no-edit; fi
git submodule update --init --recursive
