@REM ----------------------------------------------------------------------------
@REM Maven Wrapper for Windows
@REM 用法：mvnw.cmd spring-boot:run
@REM ----------------------------------------------------------------------------
@setlocal

@if "%DEBUG%"=="" @echo off

set "CLASSWORLDS_CONF=%~dp0.mvn\wrapper\maven-wrapper.properties"
set "MVNW_REPOURL=https://repo.maven.apache.org/maven2"
set "MVNW_USERNAME="
set "MVNW_PASSWORD="

@REM Find project base directory
set "MAVEN_BASEDIR=%~dp0"
cd "%MAVEN_BASEDIR%"

set "WRAPPER_JAR=%~dp0.mvn\wrapper\maven-wrapper.jar"

@REM Download maven-wrapper.jar if not present
if not exist "%WRAPPER_JAR%" (
    echo Downloading Maven Wrapper...
    powershell -Command "(New-Object Net.WebClient).DownloadFile('%MVNW_REPOURL%/org/apache/maven/wrapper/maven-wrapper/3.3.2/maven-wrapper-3.3.2.jar', '%WRAPPER_JAR%')"
    if errorlevel 1 (
        echo ERROR: Failed to download maven-wrapper.jar
        exit /b 1
    )
)

@REM Find Java
set "JAVA_EXE=java.exe"
if defined JAVA_HOME (
    set "JAVA_EXE=%JAVA_HOME%\bin\java.exe"
)
if not defined JAVA_HOME (
    for %%i in (java.exe) do set "JAVA_EXE=%%~$PATH:i"
)

@REM Set Maven options (Java 24 needs --enable-native-access)
set "MAVEN_OPTS=-Xmx512m -Dmaven.multiModuleProjectDirectory=%MAVEN_BASEDIR% --enable-native-access=ALL-UNNAMED"

@REM Run Maven
"%JAVA_EXE%" %MAVEN_OPTS% -classpath "%WRAPPER_JAR%" org.apache.maven.wrapper.MavenWrapperMain %*

@endlocal
