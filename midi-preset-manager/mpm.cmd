@rem ---------------------------------------------------------------------------------------------------
@rem This script is designed to be run anywhere, it will look for the jar in the script folder
@rem Typically you run this script in the folder of your current project, it will generate a config.yml
@rem ---------------------------------------------------------------------------------------------------
set JAVA_HOME=C:\Java\graalvm-jdk-24.0.1+9.1
set DEBUG=-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:10092
set "JAR_FILE="
@rem Search for the first JAR file in the script's directory.
for %%f in ("%~dp0*.jar") do (
    set "JAR_FILE=%%f"
    goto :found_jar
)
echo No JAR found in directory "%~dp0"
goto :eof
:found_jar
@rem Execute the Java command, passing all command-line arguments to it.
%JAVA_HOME%\bin\java %DEBUG% -jar "%JAR_FILE%" %*