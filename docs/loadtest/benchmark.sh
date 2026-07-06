#!/bin/bash

# ================================================================
# 即刻App热点榜单引擎 - 性能压测脚本
# 工具：wrk (Windows可用wrk2或Apache Bench替代)
# ================================================================

BASE_URL="http://localhost:8080"

echo "=========================================="
echo "即刻App热点榜单引擎 - 性能压测"
echo "=========================================="
echo ""

# 检查是否安装了curl
if ! command -v curl &> /dev/null; then
    echo "错误：未找到curl命令"
    exit 1
fi

# ================================================================
# 测试1：全站热榜查询性能
# ================================================================
echo "【测试1】全站热榜查询性能"
echo "并发数: 100, 请求数: 1000"
echo "URL: $BASE_URL/api/ranking/global"
echo ""

# 使用循环模拟并发请求
SUCCESS_COUNT=0
FAIL_COUNT=0
TOTAL_TIME=0

for i in $(seq 1 100); do
    START_TIME=$(date +%s%N)
    HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/api/ranking/global")
    END_TIME=$(date +%s%N)

    DURATION=$(( (END_TIME - START_TIME) / 1000000 ))
    TOTAL_TIME=$((TOTAL_TIME + DURATION))

    if [ "$HTTP_CODE" = "200" ]; then
        SUCCESS_COUNT=$((SUCCESS_COUNT + 1))
    else
        FAIL_COUNT=$((FAIL_COUNT + 1))
    fi
done

echo "完成！成功: $SUCCESS_COUNT, 失败: $FAIL_COUNT"
echo "平均响应时间: $((TOTAL_TIME / 100)) ms"
echo ""

# ================================================================
# 测试2：圈子热榜查询性能
# ================================================================
echo "【测试2】圈子热榜查询性能"
echo "并发数: 100, 请求数: 1000"
echo "URL: $BASE_URL/api/ranking/circle/1"
echo ""

SUCCESS_COUNT=0
FAIL_COUNT=0
TOTAL_TIME=0

for i in $(seq 1 100); do
    START_TIME=$(date +%s%N)
    HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/api/ranking/circle/1")
    END_TIME=$(date +%s%N)

    DURATION=$(( (END_TIME - START_TIME) / 1000000 ))
    TOTAL_TIME=$((TOTAL_TIME + DURATION))

    if [ "$HTTP_CODE" = "200" ]; then
        SUCCESS_COUNT=$((SUCCESS_COUNT + 1))
    else
        FAIL_COUNT=$((FAIL_COUNT + 1))
    fi
done

echo "完成！成功: $SUCCESS_COUNT, 失败: $FAIL_COUNT"
echo "平均响应时间: $((TOTAL_TIME / 100)) ms"
echo ""

# ================================================================
# 测试3：互动事件写入性能
# ================================================================
echo "【测试3】互动事件写入性能"
echo "并发数: 100, 请求数: 1000"
echo "URL: $BASE_URL/api/interaction"
echo ""

SUCCESS_COUNT=0
FAIL_COUNT=0
TOTAL_TIME=0

for i in $(seq 1 100); do
    USER_ID=$((9000 + i))
    START_TIME=$(date +%s%N)
    HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" \
        -X POST "$BASE_URL/api/interaction" \
        -H "Content-Type: application/json" \
        -d "{\"topicId\":1,\"userId\":$USER_ID,\"interactionType\":1,\"deviceFingerprint\":\"bench_$i\",\"ipAddress\":\"10.0.0.$i\"}")
    END_TIME=$(date +%s%N)

    DURATION=$(( (END_TIME - START_TIME) / 1000000 ))
    TOTAL_TIME=$((TOTAL_TIME + DURATION))

    if [ "$HTTP_CODE" = "200" ]; then
        SUCCESS_COUNT=$((SUCCESS_COUNT + 1))
    else
        FAIL_COUNT=$((FAIL_COUNT + 1))
    fi
done

echo "完成！成功: $SUCCESS_COUNT, 失败: $FAIL_COUNT"
echo "平均响应时间: $((TOTAL_TIME / 100)) ms"
echo ""

echo "=========================================="
echo "压测完成！"
echo "=========================================="
