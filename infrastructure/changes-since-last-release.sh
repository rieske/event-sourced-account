#/bin/bash
latestVersion=$(git tag -l | sed 's/^v//g' | sort -nr | head -n1)
git log ${latestVersion}..HEAD