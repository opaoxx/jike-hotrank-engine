@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

:: ================================================================
:: 即刻App热点榜单引擎 - Windows 性能压测脚本
:: ================================================================

set BASE_URL=http://localhost:8080

echo ==========================================
echo 即刻App热点榜单引擎 - 性能压测
echo ==========================================
echo.

:: ================================================================
:: 测试1：全站热榜查询性能
:: ================================================================
echo 【测试1】全站热榜查询性能
echo 并发数: 50, 请求数: 500
echo URL: %BASE_URL%/api/ranking/global
echo.

set SUCCESS=0
set FAIL=0
set TOTAL_TIME=0

for /L %%i in (1,1,50) do (
    for /f "tokens=*" %%a in ('curl -s -o nul -w "%%{http_code} %%{time_total}" "%BASE_URL%/api/ranking/global"') do (
        set RESULT=%%a
        set HTTP_CODE=!RESULT:~0,3!
        if "!HTTP_CODE!"=="200" (
            set /a SUCCESS+=1
        ) else (
            set /a FAIL+=1
        )
    )
)

echo 完成！成功: %SUCCESS%, 失败: %FAIL%
echo.

:: ================================================================
:: 测试2：圈子热榜查询性能
:: ================================================================
echo 【测试2】圈子热榜查询性能
echo URL: %BASE_URL%/api/ranking/circle/1
echo.

set SUCCESS=0
set FAIL=0

for /L %%i in (1,1,50) do (
    for /f "tokens=*" %%a in ('curl -s -o nul -w "%%{http_code}" "%BASE_URL%/api/ranking/circle/1"') do (
        if "%%a"=="200" (
            set /a SUCCESS+=1
        ) else (
            set /a FAIL+=1
        )
    )
)

echo 完成！成功: %SUCCESS%, 失败: %FAIL%
echo.

:: ================================================================
:: 测试3：互动事件写入性能
:: ================================================================
echo 【测试3】互动事件写入性能
echo URL: %BASE_URL%/api/interaction
echo.

set SUCCESS=0
set FAIL=0

for /L %%i in (1,1,50) do (
    set /a USER_ID=8000 + %%i
    for /f "tokens=*" %%a in ('curl -s -o nul -w "%%{http_code}" -X POST "%BASE_URL%/api/interaction" -H "Content-Type: application/json" -d "{\"topicId\":1,\"userId\":!USER_ID!,\"interactionType\":1,\"deviceFingerprint\":\"bench_%%i\",\"ipAddress\":\"10.0.0.%%i\"}"') do (
        if "%%a"=="200" (
            set /a SUCCESS+=1
        ) else (
            set /a FAIL+=1
        )
    )
)

echo 完成！成功: %SUCCESS%, 失败: %FAIL%
echo.

echo ==========================================
echo 压测完成！
echo ==========================================

pause
