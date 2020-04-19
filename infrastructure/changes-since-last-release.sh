#/bin/bash
latestVersion=$(git tag -l | sed 's/^v//g' | sort -nr | head -n1)
git log v${latestVersion}..HEAD --pretty='format:%s'
