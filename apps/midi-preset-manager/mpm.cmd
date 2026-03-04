@rem ---------------------------------------------------------------------------------------------------
@rem This script is designed to be run anywhere, it will look for the jar in the script folder
@rem Typically you run this script in the folder of your current project, it will generate a config.yml
@rem ---------------------------------------------------------------------------------------------------
set JAVA_HOME=C:\Java\graalvm-jdk-24.0.1+9.1
set DEBUG=-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:10092
%JAVA_HOME%\bin\java %DEBUG% -jar mpm-win.jar %*