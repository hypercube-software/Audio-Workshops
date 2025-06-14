set JAVA=C:\Java\graalvm-jdk-21.0.7+8.1\bin\java
set DEBUG=-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:10092
set "JAR_FILE="
for %%f in ("%~dp0*.jar") do (
    set "JAR_FILE=%%f"
    goto :found_jar
)
echo No JAR found in directory "%~dp0"
goto :eof
:found_jar
%JAVA% %DEBUG% -jar "%JAR_FILE%"