@echo off
setlocal EnableDelayedExpansion

set "SCRIPT_DIR=%~dp0"
pushd "%SCRIPT_DIR%.." >nul

call :printBanner

where mvn >nul 2>nul
if errorlevel 1 (
    echo [ERROR] Maven not found in PATH.
    echo Install Maven 3.6+ and ensure "mvn" is available.
    goto :eof
)

set "RUN_ARGS="
set "MAVEN_ARGS="
set "EXCLUDE_TAGS="
set "REPORT_ONLY=false"

:parseArgs
if "%~1"=="" goto :postParse
if /I "%1"=="--quick" (
    if defined EXCLUDE_TAGS (
        set "EXCLUDE_TAGS=%EXCLUDE_TAGS%,networking,rendering"
    ) else (
        set "EXCLUDE_TAGS=networking,rendering"
    )
    shift
    goto :parseArgs
)
if /I "%1"=="--report-only" (
    set "REPORT_ONLY=true"
    shift
    goto :parseArgs
)
set "MAVEN_ARGS=!MAVEN_ARGS! %~1"
shift
goto :parseArgs

:postParse
if /I "%REPORT_ONLY%"=="true" (
    call :openLatestReport
    goto :eof
)

if defined EXCLUDE_TAGS (
    set "MAVEN_ARGS=%MAVEN_ARGS% -Djunit.jupiter.tags.exclude=%EXCLUDE_TAGS%"
)

set "MAVEN_ARGS=%MAVEN_ARGS%"
echo Running: mvn -B clean test %MAVEN_ARGS%
call mvn -B clean test %MAVEN_ARGS%
set "EXIT_CODE=%ERRORLEVEL%"

if "%EXIT_CODE%"=="0" (
    echo.
    echo [SUCCESS] Test suite completed successfully.
    call :printReportSummary
) else (
    echo.
    echo [FAILURE] Test suite reported failures. See reports below.
    call :printReportSummary
)

popd >nul
exit /b %EXIT_CODE%

:printBanner
echo ==============================================
echo      PoorCraft Automated Test Suite Runner
echo ==============================================
exit /b 0

:printReportSummary
set "REPORT_DIR=target\test-reports"
if not exist "%REPORT_DIR%" (
    echo Reports directory not found: %REPORT_DIR%
    exit /b 0
)
for /f "delims=" %%F in ('dir /b /a:-d /o:-d "%REPORT_DIR%\test-report-*.html" 2^>nul') do (
    echo Latest HTML report: %REPORT_DIR%\%%F
    echo Latest Markdown report: %REPORT_DIR%\%%~nF.md
    exit /b 0
)
echo No HTML reports found in %REPORT_DIR%
exit /b 0

:openLatestReport
set "REPORT_DIR=target\test-reports"
if not exist "%REPORT_DIR%" (
    echo No reports found. Run tests first.
    exit /b 1
)
set "LATEST="
for /f "delims=" %%F in ('dir /b /a:-d /o:-d "%REPORT_DIR%\test-report-*.html" 2^>nul') do (
    set "LATEST=%%F"
    goto :openFound
)
echo No HTML reports available.
exit /b 1

:openFound
if not defined LATEST (
    echo No HTML reports available.
    exit /b 1
)
set "HTML_PATH=%REPORT_DIR%\%LATEST%"
echo Opening %HTML_PATH%
start "" "%HTML_PATH%"
exit /b 0
