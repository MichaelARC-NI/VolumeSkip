#!/bin/sh
GRADLE=/usr/bin/gradle
if [ -f "$GRADLE" ]; then
    exec "$GRADLE" "$@"
else
    echo "Gradle not found. Please install Gradle."
    exit 1
fi
