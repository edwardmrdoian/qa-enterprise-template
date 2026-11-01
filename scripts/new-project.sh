#!/usr/bin/env bash
# Turns this template into your own project: renames the Java package, groupId and artifactId.
# Usage: ./scripts/new-project.sh com.acme.qa acme-qa-tests
# On Windows run it from Git Bash or WSL.
set -euo pipefail

NEW_PKG="${1:?Usage: $0 <new.base.package> <artifactId>}"
ARTIFACT="${2:?Usage: $0 <new.base.package> <artifactId>}"
OLD_PKG="com.company.qa"
OLD_PATH="com/company/qa"
NEW_PATH="${NEW_PKG//.//}"

cd "$(dirname "$0")/.."

# 1) replace the package name inside all text files
grep -rIl --exclude-dir=.git --exclude-dir=target --exclude=new-project.sh "$OLD_PKG" . | while read -r file; do
  sed -i.bak "s/${OLD_PKG//./\\.}/${NEW_PKG}/g" "$file"
  rm -f "$file.bak"
done

# 2) set the artifactId
sed -i.bak "s#<artifactId>qa-enterprise-template</artifactId>#<artifactId>${ARTIFACT}</artifactId>#" pom.xml
rm -f pom.xml.bak

# 3) move the source folders to the new package path
for root in src/main/java src/test/java; do
  if [ -d "$root/$OLD_PATH" ]; then
    tmp="$(mktemp -d)"
    mv "$root/$OLD_PATH" "$tmp/pkg"
    rmdir -p --ignore-fail-on-non-empty "$root/$(dirname "$OLD_PATH")" 2>/dev/null || true
    mkdir -p "$root/$NEW_PATH"
    cp -r "$tmp/pkg/." "$root/$NEW_PATH/"
    rm -rf "$tmp"
  fi
done

echo "Done. Package: $NEW_PKG  ArtifactId: $ARTIFACT"
echo "Next: mvn clean test -Dgroups=smoke"
