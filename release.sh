#!/bin/sh
# One-command release prep.
#
#   ./release.sh <version>          prep only, then push yourself:  git push origin main <tag>
#   ./release.sh <version> --push   prep AND push (skips the manual last-look before going public)
#
# This bumps the version, builds the reproducible fat jar, pins the Homebrew formula to that jar's
# checksum, commits, and tags. The push then triggers .github/workflows/release.yml, which rebuilds
# the (byte-identical) jar, publishes it + the Maven repo, and verifies the formula checksum matches.
#
# The push is the irreversible, public step — the default stops just before it so you can inspect
# the commit/diff first. Pass --push only when you're confident.
set -eu

VERSION=""
PUSH=0
for arg in "$@"; do
    case "$arg" in
        --push) PUSH=1 ;;
        [0-9]*.[0-9]*.[0-9]*) VERSION="$arg" ;;
        *) echo "usage: ./release.sh <version> [--push]   (e.g. ./release.sh 4.0.1 --push)" >&2; exit 1 ;;
    esac
done
[ -n "$VERSION" ] || { echo "usage: ./release.sh <version> [--push]   (e.g. ./release.sh 4.0.1)" >&2; exit 1; }
TAG="v$VERSION"
ASSET="buildchecks-${VERSION}-all.jar"

# --- Guards: refuse to run unless the tree is a clean, releasable state ---
[ -n "$(git status --porcelain)" ] && { echo "error: working tree not clean — commit or stash first" >&2; exit 1; }
[ "$(git rev-parse --abbrev-ref HEAD)" = "main" ] || { echo "error: not on main" >&2; exit 1; }
git rev-parse -q --verify "refs/tags/$TAG" >/dev/null 2>&1 && { echo "error: tag $TAG already exists" >&2; exit 1; }

sha256() { if command -v sha256sum >/dev/null 2>&1; then sha256sum "$1" | awk '{print $1}'; else shasum -a 256 "$1" | awk '{print $1}'; fi; }

# 1. Version in the build. (`-i.bak` + rm is portable across BSD/macOS and GNU sed.)
sed -i.bak -E "s/^version = \".*\"/version = \"$VERSION\"/" build.gradle.kts && rm -f build.gradle.kts.bak

# 2. Build the reproducible fat jar.
./gradlew --quiet assemble
JAR="build/libs/$ASSET"
[ -f "$JAR" ] || { echo "error: expected $JAR was not produced" >&2; exit 1; }

# 3. Pin the Homebrew formula to this version + its checksum.
SHA="$(sha256 "$JAR")"
sed -i.bak -E \
    -e "s#releases/download/v[^/]*/buildchecks-[^\"]*-all\.jar#releases/download/$TAG/$ASSET#" \
    -e "s#buildchecks-[0-9][^\"]*-all\.jar#$ASSET#g" \
    -e "s#sha256 \"[0-9a-f]*\"#sha256 \"$SHA\"#" \
    Formula/buildchecks.rb && rm -f Formula/buildchecks.rb.bak

# 4. Commit + tag.
git add build.gradle.kts Formula/buildchecks.rb
git commit -q -m "Release $TAG"
git tag "$TAG"

echo "Prepared $TAG  (jar sha256 $SHA)"
if [ "$PUSH" -eq 1 ]; then
    echo "Pushing main + $TAG to origin ..."
    git push origin main "$TAG"
else
    echo "Release it with:  git push origin main $TAG"
fi
