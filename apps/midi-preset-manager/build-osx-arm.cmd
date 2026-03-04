@echo on

set JAVA_HOME=C:\Java\graalvm-jdk-21.0.7+8.1

..\mvnw -Posx-arm -Djavafx.platform=mac-aarch64 -DskipTests -B clean package




