#!/bin/sh
# Gradle wrapper startup script. Licensed under the Apache License, Version 2.0.
APP_HOME=$(cd -P "${0%/*}" >/dev/null 2>&1 && pwd) || exit
if [ -n "$JAVA_HOME" ]; then JAVACMD="$JAVA_HOME/bin/java"; else JAVACMD=java; fi
if ! command -v "$JAVACMD" >/dev/null 2>&1; then
  echo "ERROR: Java 17 or newer is required. Set JAVA_HOME." >&2
  exit 1
fi
exec "$JAVACMD" -Xmx64m -Xms64m -Dorg.gradle.appname=gradlew -jar "$APP_HOME/gradle/wrapper/gradle-wrapper.jar" "$@"
