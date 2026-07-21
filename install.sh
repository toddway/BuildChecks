#!/bin/sh
# BuildChecks installer — downloads the self-contained jar and drops a `buildchecks` launcher on
# your PATH. The only runtime prerequisite is Java (a JRE); this script warns if it is missing.
#
#   curl -fsSL https://raw.githubusercontent.com/toddway/BuildChecks/main/install.sh | sh
#
# Overrides (environment variables):
#   BUILDCHECKS_VERSION      release to install, e.g. 4.0.0 (default: latest)
#   BUILDCHECKS_INSTALL_DIR  where the launcher + jar go (default: $HOME/.local/bin)
set -eu

REPO="toddway/BuildChecks"
VERSION="${BUILDCHECKS_VERSION:-latest}"
INSTALL_DIR="${BUILDCHECKS_INSTALL_DIR:-$HOME/.local/bin}"

command -v curl >/dev/null 2>&1 || { echo "error: curl is required" >&2; exit 1; }

# Resolve the tag. For "latest", follow the /releases/latest redirect (no API token, no jq).
if [ "$VERSION" = "latest" ]; then
    TAG=$(curl -fsSLI -o /dev/null -w '%{url_effective}' "https://github.com/$REPO/releases/latest" \
        | sed 's#.*/tag/##')
    [ -n "$TAG" ] || { echo "error: could not resolve the latest release" >&2; exit 1; }
else
    TAG="v${VERSION#v}"
fi
NUM="${TAG#v}"
ASSET="buildchecks-${NUM}-all.jar"
URL="https://github.com/$REPO/releases/download/$TAG/$ASSET"

echo "Installing BuildChecks $TAG to $INSTALL_DIR"
mkdir -p "$INSTALL_DIR"
INSTALL_DIR=$(cd "$INSTALL_DIR" && pwd)   # absolutize so the launcher works from anywhere

curl -fSL "$URL" -o "$INSTALL_DIR/buildchecks.jar" \
    || { echo "error: failed to download $URL" >&2; exit 1; }

cat > "$INSTALL_DIR/buildchecks" <<EOF
#!/bin/sh
exec java -jar "$INSTALL_DIR/buildchecks.jar" "\$@"
EOF
chmod +x "$INSTALL_DIR/buildchecks"

echo "Installed: $INSTALL_DIR/buildchecks"

# Post-install advisories (never fatal).
if ! command -v java >/dev/null 2>&1; then
    echo "note: 'java' was not found on your PATH. BuildChecks needs a JRE (17+)." >&2
    echo "      macOS: brew install openjdk   •   Debian/Ubuntu: apt install default-jre" >&2
fi
case ":$PATH:" in
    *":$INSTALL_DIR:"*) ;;
    *) echo "note: $INSTALL_DIR is not on your PATH — add it, e.g.:" >&2
       echo "      export PATH=\"$INSTALL_DIR:\$PATH\"" >&2 ;;
esac
