#!/usr/bin/env sh
set -eu

GRADLE_VERSION="8.11.1"
ROOT_DIR="$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"
GRADLE_CACHE="${GRADLE_USER_HOME:-$HOME/.gradle}/manual-distributions"
DIST_DIR="$GRADLE_CACHE/gradle-$GRADLE_VERSION"
GRADLE_BIN="$DIST_DIR/bin/gradle"
ZIP_FILE="$GRADLE_CACHE/gradle-$GRADLE_VERSION-bin.zip"

if [ ! -x "$GRADLE_BIN" ]; then
  mkdir -p "$GRADLE_CACHE"
  if ! command -v curl >/dev/null 2>&1; then
    echo "curl is required to download Gradle $GRADLE_VERSION." >&2
    exit 1
  fi
  echo "Downloading Gradle $GRADLE_VERSION..."
  curl --fail --location --retry 3 \
    "https://services.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip" \
    --output "$ZIP_FILE"
  if command -v unzip >/dev/null 2>&1; then
    unzip -q -o "$ZIP_FILE" -d "$GRADLE_CACHE"
  else
    python3 - "$ZIP_FILE" "$GRADLE_CACHE" <<'PY'
import sys, zipfile
with zipfile.ZipFile(sys.argv[1]) as archive:
    archive.extractall(sys.argv[2])
PY
  fi
fi

cd "$ROOT_DIR"
"$GRADLE_BIN" wrapper --gradle-version "$GRADLE_VERSION" --distribution-type bin
printf '\nGradle Wrapper generated. Use ./gradlew bundleRelease to build the Play bundle.\n'
