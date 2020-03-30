#!/usr/bin/env bash -e

source credentials.conf
source settings.conf

set +x 

# for highlighting
declare -A fg_color_map
fg_color_map[black]=30
fg_color_map[red]=31
fg_color_map[green]=32
fg_color_map[yellow]=33
fg_color_map[blue]=34
fg_color_map[magenta]=35
fg_color_map[cyan]=36
fg_color_map[gray]=90

function highlight() {
  if [ "$COLORED_LOGS_ENABLED" == "true" ]
  then
    fg_c=$(echo -e "\e[1;${fg_color_map[$1]}m")
    c_rs=$'\e[0m'
    sed -u s"/$2/$fg_c\0$c_rs/g"
  else
    cat -
  fi
}

function info() {
  echo "$1" | highlight green ".*"
}

function warn() {
  echo "$1" | highlight yellow ".*"
}

function error() {
  echo "$1" | highlight red ".*"
  exit 1
}

function session() {
  curl $CURL_OPTS -s -u "$USER_PASSWORD" -X POST "$SERVER_ADDRESS/api/v1/session"
}

info "Trying to log in..."
authenticated_curl_cmd="curl $CURL_OPTS --header session:`session`"
info "Logged in successfully."

function main4ino_post_description() {
  local device="$1"
  local description="$2"
  $authenticated_curl_cmd -X POST "$SERVER_ADDRESS/api/v1/devices/$device/descriptions" -d "$description"
}

function main4ino_post_report() {
  local device="$1"
  local body="$2"

  $authenticated_curl_cmd -X POST $SERVER_ADDRESS/api/v1/devices/$device/reports -d "$body"
}

function main4ino_post_report_actor() {
  local device="$1"
  local actor="$2"
  local body="$3"

  $authenticated_curl_cmd -X POST $SERVER_ADDRESS/api/v1/devices/$device/reports/actors/$actor -d "$body"
}

function main4ino_post_target() {
  local device="$1"
  local body="$2"

  $authenticated_curl_cmd -X POST $SERVER_ADDRESS/api/v1/devices/$device/targets -d "$body"
}

function main4ino_post_target_actor() {
  local device="$1"
  local actor="$2"
  local body="$3"

  $authenticated_curl_cmd -X POST $SERVER_ADDRESS/api/v1/devices/$device/targets/actors/$actor -d "$body"
}

function main4ino_get_help() {
  $authenticated_curl_cmd -X GET "$SERVER_ADDRESS/api/v1/help"
}

function main4ino_get_version() {
  $authenticated_curl_cmd -X GET "$SERVER_ADDRESS/api/v1/version"
}

function main4ino_get_logs() {
  main4ino_get_logs_raw $@ | jq -r .[].content
}

function main4ino_get_logs_raw() {
  local device="$1"
  local length="${2:-2000}"
  local now=`date +'%s'`
  let from=$now-$length
  $authenticated_curl_cmd -X GET "$SERVER_ADDRESS/api/v1/devices/$device/logs?from=$from&to=$now"
}


function main4ino_get_last_report() {
  local device="$1"
  $authenticated_curl_cmd -X GET $SERVER_ADDRESS/api/v1/devices/$device/reports/last
}

function main4ino_get_reports() {
  local device="$1"
  local status="$2"
  $authenticated_curl_cmd -X GET "$SERVER_ADDRESS/api/v1/devices/$device/reports?status=$status"
}

function main4ino_get_targets() {
  local device="$1"
  local status="$2"
  $authenticated_curl_cmd -X GET "$SERVER_ADDRESS/api/v1/devices/$device/targets?status=$status"
}



function main4ino_get_last_target() {
  local device="$1"
  $authenticated_curl_cmd -X GET $SERVER_ADDRESS/api/v1/devices/$device/targets/last
}

function main4ino_get_summary_report() {
  local device="$1"
  $authenticated_curl_cmd -X GET $SERVER_ADDRESS/api/v1/devices/$device/reports/summary
}

function main4ino_get_summary_target() {
  local device="$1"
  $authenticated_curl_cmd -X GET $SERVER_ADDRESS/api/v1/devices/$device/targets/summary
}

function main4ino_get_last_report_actor() {
  local device="$1"
  local actor="$2"
  $authenticated_curl_cmd -X GET $SERVER_ADDRESS/api/v1/devices/$device/reports/actors/$actor/last
}

function main4ino_get_last_target_actor() {
  local device="$1"
  local actor="$2"
  $authenticated_curl_cmd -X GET $SERVER_ADDRESS/api/v1/devices/$device/target/actors/$actor/last
}

function main4ino_get_firmwares() {
  local project="$1"
  local platform="$2"
  $authenticated_curl_cmd -X GET $SERVER_ADDRESS/firmwares/$project/$platform
}

info "Functions loaded."

