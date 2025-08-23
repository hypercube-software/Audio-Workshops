#!/bin/bash

# ---------------------------------------------------------------------------------------------------
# This script is designed to be run anywhere, it will look for the jar in the script folder
# Typically you run this script in the folder of your current project, it will generate a config.yml
# ---------------------------------------------------------------------------------------------------

# Set the JAVA_HOME variable. Adjust this path to your GraalVM installation on macOS.
export JAVA_HOME="/somewhere/graalvm-jdk-24.0.1+9.1"

DEBUG="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:10092"
JAR_FILE=""

# Search for the first JAR file in the script's directory.
JAR_FILE=$(find "$(dirname "$0")" -maxdepth 1 -name "*.jar" | head -n 1)

if [ -z "$JAR_FILE" ]; then
    echo "No JAR found in directory $(dirname "$0")"
    exit 1
fi

# Execute the Java command, passing all command-line arguments to it.
# The "$@" variable correctly handles arguments with spaces.
"$JAVA_HOME/bin/java" $DEBUG -jar "$JAR_FILE" "$@"