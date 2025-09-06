@echo on

set JAVA_HOME=C:\Java\graalvm-jdk-21.0.7+8.1

..\mvnw -Posx -Djavafx.platform=mac -Posx  -DskipTests -Djavafx.platform=mac -B package
@rem ..\mvnw -Posx -Djavafx.platform=mac dependency:tree



