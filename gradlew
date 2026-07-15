#!/bin/sh
# Gradle start-up script (minimal)
DIR="$(cd "$(dirname "$0")" && pwd)"
exec java -jar "$DIR/gradle/wrapper/gradle-wrapper.jar" "$@"
