#!/bin/bash
gh workflow list
gh run list
REPO_INFO=$(gh repo view --json name,owner)
REPO_FULL_NAME="$(echo $REPO_INFO | jq '.owner.login' -r)/$(echo $REPO_INFO | jq '.name' -r)"
# remove all deployments
gh api "repos/$REPO_FULL_NAME/deployments" -q '.[].id' --paginate --slurp | xargs -t -I{} gh api --silent -X DELETE "repos/$REPO_FULL_NAME/deployments/{}"
# remove all runs
gh run list --json databaseId -q '.[].databaseId' --limit 10000 | xargs -t -I{} gh api --silent -X DELETE "repos/$REPO_FULL_NAME/actions/runs/{}"
