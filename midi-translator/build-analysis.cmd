@echo on

set JAVA_HOME=C:\Java\graalvm-jdk-21.0.2+13.1

@rem get the complete jar filename
FOR %%A in (target\midi-translator*.jar) do (set "JAR_PATH=%%A")

@rem We run the application and instruct graalvm to analyze on the fly and update the configuration
set GRAAL_ANALYSIS_FOLDER=./src/main/resources/META-INF/native-image/
%JAVA_HOME%\bin\java -agentlib:native-image-agent=config-merge-dir=%GRAAL_ANALYSIS_FOLDER% -jar %JAR_PATH% list
@rem %JAVA_HOME%\bin\java -agentlib:native-image-agent=config-merge-dir=%GRAAL_ANALYSIS_FOLDER% -jar %JAR_PATH% restore
