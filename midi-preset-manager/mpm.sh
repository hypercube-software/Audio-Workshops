#!/bin/bash

# --- Configuration ---
# Set the path to the Java installation (e.g., GraalVM, OpenJDK)
export JAVA_HOME=/Library/Java/JavaVirtualMachines/graalvm-jdk-24.0.1+9.1/Contents/Home

# Debugging options (uncomment the line below to enable remote debugging)
#DEBUG="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:10092"
# --- End Configuration ---

# Get the script's directory for relative path calculation
# This approach handles symbolic links correctly.
SOURCE="${BASH_SOURCE[0]}"
while [ -h "$SOURCE" ]; do # resolve $SOURCE until the file is no longer a symlink
  SCRIPT_DIR="$( cd -P "$( dirname "$SOURCE" )" && pwd )"
  SOURCE="$(readlink "$SOURCE")"
  [[ $SOURCE != /* ]] && SOURCE="$SCRIPT_DIR/$SOURCE" # if $SOURCE was a relative symlink, we need to resolve it relative to the path where the symlink file was located
done
SCRIPT_DIR="$( cd -P "$( dirname "$SOURCE" )" && pwd )"

## 1. DETERMINE SYSTEM ARCHITECTURE
# 'uname -m' returns 'arm64' for Apple Silicon (ARM) and 'x86_64' for Intel.
ARCH=$(uname -m)

if [ "$ARCH" == "arm64" ]; then
    # ARM Architecture (Apple Silicon)
    ARCH_SUFFIX="arm"
elif [ "$ARCH" == "x86_64" ]; then
    # Intel Architecture
    ARCH_SUFFIX="intel"
else
    # Unknown architecture, default to Intel as a fallback
    echo "Unrecognized architecture ($ARCH). Attempting to launch with the Intel JAR."
    ARCH_SUFFIX="intel"
fi

## 2. CONSTRUCT THE JAR FILE NAME
# The file name will be e.g., 'mpm-osx-arm.jar' or 'mpm-osx-intel.jar'
JAR_NAME="mpm-osx-${ARCH_SUFFIX}.jar"
JAR_PATH="${SCRIPT_DIR}/${JAR_NAME}"

## 3. CHECK EXISTENCE AND LAUNCH
if [ -f "$JAR_PATH" ]; then
    echo "Launching application $JAR_NAME on $ARCH architecture..."
    # Execute the JAR using the determined JAVA_HOME and pass all script arguments ($@)
    "$JAVA_HOME/bin/java" $DEBUG -jar "$JAR_PATH" "$@"
else
    echo "ERROR: The JAR file for the $ARCH architecture was not found."
    echo "Searched path: $JAR_PATH"
    exit 1
fi