#!/usr/bin/env bash
set -euo pipefail

# Merge latest main into current branch
# Usage: ./scripts/merge-main.sh

CURRENT_BRANCH=$(git branch --show-current)

if [[ "$CURRENT_BRANCH" == "main" ]]; then
  echo "❌ Already on main branch"
  exit 1
fi

echo "📥 Fetching origin/main..."
git fetch origin main

echo "🔀 Merging origin/main into $CURRENT_BRANCH..."
git merge origin/main -m "Merge main into $CURRENT_BRANCH"

echo "✅ Merged main into $CURRENT_BRANCH"
