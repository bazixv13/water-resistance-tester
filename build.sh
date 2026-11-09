#!/usr/bin/env bash
# Build helper: keeps the JDK, Android SDK and Gradle caches inside this folder.
set -euo pipefail

cd "$(dirname "$0")"

export JAVA_HOME="$PWD/.toolchain/jdk-21.0.13+9"
export ANDROID_HOME="$PWD/.toolchain/android-sdk"
export GRADLE_USER_HOME="$PWD/.gradle-home"
export PATH="$JAVA_HOME/bin:$PATH"

task="${1:-:app:assembleDebug}"
shift || true

exec "$PWD/.toolchain/gradle-9.8.2/bin/gradle" "$task" "$@"
