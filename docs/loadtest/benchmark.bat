@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

if "%BASE_URL%"=="" set BASE_URL=http://localhost:8080
if "%TOKEN%"=="" set TOKEN=perf_test_token
if "%REQUESTS%"=="" set REQUESTS=100
if "%RESULT_DIR%"=="" set RESULT_DIR=docs\loadtest\results

if not exist "%RESULT_DIR%" mkdir "%RESULT_DIR%"
set RESULT_FILE=%RESULT_DIR%\benchmark-%DATE:~0,4%%DATE:~5,2%%DATE:~8,2%-%TIME:~0,2%%TIME:~3,2%%TIME:~6,2%.csv
set RESULT_FILE=%RESULT_FILE: =0%

where curl >nul 2>nul
if errorlevel 1 (
    echo curl is required.
    exit /b 1
)

echo Jike HotRank Engine benchmark
echo BASE_URL=%BASE_URL%
echo REQUESTS=%REQUESTS%
echo RESULT_FILE=%RESULT_FILE%
echo.

echo scenario,method,path,http_code,time_ms>"%RESULT_FILE%"

for /L %%i in (1,1,%REQUESTS%) do (
    set /a BUCKET=%%i %% 10
    if !BUCKET! LSS 4 (
        call :GET global_ranking "/api/ranking/global?limit=50"
    ) else if !BUCKET! LSS 6 (
        call :GET circle_ranking "/api/ranking/circle/1?limit=20"
    ) else if !BUCKET! LSS 9 (
        call :POST_INTERACTION %%i
    ) else (
        call :GET anti_spam_report "/api/anti-spam/report"
    )
)

echo.
echo CSV result written to %RESULT_FILE%
echo Built-in load test endpoint:
echo curl -X POST "%BASE_URL%/api/perf/load-test?qps=20^&duration=5^&token=%TOKEN%"
exit /b 0

:GET
set SCENARIO=%~1
set PATH_VALUE=%~2
for /f "tokens=1,2" %%a in ('curl -s -o nul -w "%%{http_code} %%{time_total}" "%BASE_URL%%PATH_VALUE%"') do (
    set CODE=%%a
    set SECONDS=%%b
)
for /f %%m in ('powershell -NoProfile -Command "[math]::Round([double]'!SECONDS!' * 1000, 2)"') do set MILLIS=%%m
echo %SCENARIO%,GET,%PATH_VALUE%,!CODE!,!MILLIS!>>"%RESULT_FILE%"
exit /b 0

:POST_INTERACTION
set INDEX=%~1
set /a USER_ID=900000 + %INDEX%
set /a IP_LAST=%INDEX% %% 255
set BODY={\"topicId\":1,\"userId\":!USER_ID!,\"interactionType\":1,\"deviceFingerprint\":\"bench_%INDEX%\",\"ipAddress\":\"10.0.0.!IP_LAST!\"}
for /f "tokens=1,2" %%a in ('curl -s -o nul -w "%%{http_code} %%{time_total}" -X POST "%BASE_URL%/api/interaction" -H "Content-Type: application/json" -d "!BODY!"') do (
    set CODE=%%a
    set SECONDS=%%b
)
for /f %%m in ('powershell -NoProfile -Command "[math]::Round([double]'!SECONDS!' * 1000, 2)"') do set MILLIS=%%m
echo interaction_write,POST,/api/interaction,!CODE!,!MILLIS!>>"%RESULT_FILE%"
exit /b 0
