#!/usr/bin/env bash
# Saves the contents of ci-out/ to the branch ci/<name> so results can be read with plain git.
set -u
name="$1"
[ -d ci-out ] || mkdir ci-out
cd ci-out
git init -q -b results
git add -A
git -c user.name="hh-ci" -c user.email="hh-ci@users.noreply.github.com" commit -qm "${name} results for ${GITHUB_SHA}" || exit 0
for i in 1 2 3; do
  git push -q -f "https://x-access-token:${GH_TOKEN}@github.com/${GITHUB_REPOSITORY}.git" "HEAD:refs/heads/ci/${name}" && exit 0
  sleep 5
done
echo "could not publish results"
