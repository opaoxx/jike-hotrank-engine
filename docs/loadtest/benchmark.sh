#!/usr/bin/env bash

set -uo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
TOKEN="${TOKEN:-perf_test_token}"
REQUESTS="${REQUESTS:-100}"
RESULT_DIR="${RESULT_DIR:-docs/loadtest/results}"
TIMESTAMP="$(date +%Y%m%d-%H%M%S)"
RESULT_FILE="$RESULT_DIR/benchmark-$TIMESTAMP.csv"

mkdir -p "$RESULT_DIR"

if ! command -v curl >/dev/null 2>&1; then
  echo "curl is required."
  exit 1
fi

echo "Jike HotRank Engine benchmark"
echo "BASE_URL=$BASE_URL"
echo "REQUESTS=$REQUESTS"
echo "RESULT_FILE=$RESULT_FILE"
echo

echo "scenario,method,path,http_code,time_ms" > "$RESULT_FILE"

run_get() {
  local scenario="$1"
  local path="$2"
  local output
  output="$(curl -s -o /dev/null -w "%{http_code} %{time_total}" "$BASE_URL$path")"
  local code="${output%% *}"
  local seconds="${output##* }"
  local millis
  millis="$(awk "BEGIN { printf \"%.2f\", $seconds * 1000 }")"
  echo "$scenario,GET,$path,$code,$millis" >> "$RESULT_FILE"
}

run_post_interaction() {
  local scenario="interaction_write"
  local index="$1"
  local user_id=$((900000 + index))
  local body
  body="{\"topicId\":1,\"userId\":$user_id,\"interactionType\":1,\"deviceFingerprint\":\"bench_$index\",\"ipAddress\":\"10.0.0.$((index % 255))\"}"
  local output
  output="$(curl -s -o /dev/null -w "%{http_code} %{time_total}" \
    -X POST "$BASE_URL/api/interaction" \
    -H "Content-Type: application/json" \
    -d "$body")"
  local code="${output%% *}"
  local seconds="${output##* }"
  local millis
  millis="$(awk "BEGIN { printf \"%.2f\", $seconds * 1000 }")"
  echo "$scenario,POST,/api/interaction,$code,$millis" >> "$RESULT_FILE"
}

for i in $(seq 1 "$REQUESTS"); do
  bucket=$((i % 10))
  if [ "$bucket" -lt 4 ]; then
    run_get "global_ranking" "/api/ranking/global?limit=50"
  elif [ "$bucket" -lt 6 ]; then
    run_get "circle_ranking" "/api/ranking/circle/1?limit=20"
  elif [ "$bucket" -lt 9 ]; then
    run_post_interaction "$i"
  else
    run_get "anti_spam_report" "/api/anti-spam/report"
  fi
done

echo
echo "Summary"
awk -F, '
NR > 1 {
  count[$1]++
  total[$1]+=$5
  if ($4 >= 200 && $4 < 300) success[$1]++
}
END {
  printf "%-22s %-10s %-10s %-12s\n", "scenario", "requests", "success", "avg_ms"
  for (scenario in count) {
    printf "%-22s %-10d %-10d %-12.2f\n", scenario, count[scenario], success[scenario], total[scenario] / count[scenario]
  }
}
' "$RESULT_FILE"

echo
echo "Built-in load test endpoint:"
echo "curl -X POST \"$BASE_URL/api/perf/load-test?qps=20&duration=5&token=$TOKEN\""
