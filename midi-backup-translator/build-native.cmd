@echo on

set JAVA_HOME=C:\Java\graalvm-jdk-21.0.2+13.1
call "C:\Program Files\Microsoft Visual Studio\2022\Community\VC\Auxiliary\Build\vcvars64.bat"

..\mvnw -Pnative clean install native:compile -DskipTests


