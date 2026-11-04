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
git merge --no-ff "$ref"
git submodule update --init --recursive
