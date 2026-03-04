@echo on

set JAVA_HOME=C:\Java\graalvm-jdk-21.0.7+8.1

@rem https://stackoverflow.com/questions/75006480/javafx-maven-platform-specific-build-mac-aarm64-qualifier
..\mvnw -Posx-intel -Djavafx.platform=mac -DskipTests -B clean package




