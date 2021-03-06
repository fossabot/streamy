#!/usr/bin/env bash

# Load common
source "${PWD}/scripts/common.sh"

# Extract current version
current_version=$(grep -oP 'version := "(.*)"' "${BASE_PROJECT}/build.sbt" | cut -f2 -d'"')

# Ask for new version
echo "Current version: ${current_version}"
read -p 'New version: ' new_version
grep -R "version = \"${current_version}\"" "${BASE_PROJECT}" | cut -f1 -d':' | xargs -i sed -i "s/version = \"${current_version}\"/version = \"${new_version}\"/g" {}
sed -i "s/version := \"${current_version}\"/version := \"${new_version}\"/g" "${BASE_PROJECT}/build.sbt"