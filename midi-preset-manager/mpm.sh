#!/bin/bash

export JAVA_HOME=/Library/Java/JavaVirtualMachines/graalvm-jdk-24.0.1+9.1/Contents/Home
DEBUG="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:10092"

SCRIPT_DIR=$(dirname "$(realpath "$0")")
JAR_PATH="${SCRIPT_DIR}/mpm-osx.jar"

"$JAVA_HOME/bin/java" $DEBUG -jar "$JAR_PATH" "$@"