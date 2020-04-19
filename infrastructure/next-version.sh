#!/bin/bash
latestVersion=$(git tag -l | sed 's/^v//g' | sort -nr | head -n1)
nextVersion=${latestVersion:-0}
let "nextVersion+=1"
echo "v$nextVersion"