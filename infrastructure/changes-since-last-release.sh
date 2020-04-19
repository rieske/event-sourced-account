#/bin/bash
latestVersion=$(git tag -l 'v*' | sed 's/^v//g' | sort -nr | head -n1)
if [ -z "$latestVersion" ]
then
  git log --pretty='format:%s'
else
  git log v${latestVersion}..HEAD --pretty='format:%s'
fi

