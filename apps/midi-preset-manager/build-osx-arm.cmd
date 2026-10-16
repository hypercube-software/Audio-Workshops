@echo on

set JAVA_HOME=C:\Java\graalvm-jdk-21.0.7+8.1

..\mvnw -Posx-arm -DskipTests -B clean package




