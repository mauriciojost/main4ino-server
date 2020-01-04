#!/usr/bin/env bash

source settings.conf

set +x 

function session() {
  curl $CURL_OPTS -s -u $USER_PASSWORD -X POST "$SERVER_ADDRESS/api/v1/session"
}


AUTHENTICATED_CURL_CMD="curl $CURL_OPTS --header session:`session`"

echo $AUTHENTICATED_CURL_CMD

function main4ino_post_description() {
  local device="$1"
  local description="$2"
  $AUTHENTICATED_CURL_CMD -X POST "$SERVER_ADDRESS/api/v1/devices/$device/descriptions" -d "$description"
}

function main4ino_post_report() {
  local device="$1"
  local actor="$2"
  local body="$3"

  $AUTHENTICATED_CURL_CMD -X POST $SERVER_ADDRESS/api/v1/devices/$device/reports/actors/$actor -d "$body"
}

function main4ino_post_target() {
  local device="$1"
  local actor="$2"
  local body="$3"

  $AUTHENTICATED_CURL_CMD -X POST $SERVER_ADDRESS/api/v1/devices/$device/targets/actors/$actor -d "$body"
}


function main4ino_get_logs() {
  local device="$1"
  local ignore="${2:-0}"
  local length="${3:-2000}"
  $AUTHENTICATED_CURL_CMD -X GET "$SERVER_ADDRESS/api/v1/devices/$device/logs?ignore=$ignore&length=$length"
}


function main4ino_get_last_report() {
  local device="$1"
  $AUTHENTICATED_CURL_CMD -X GET $SERVER_ADDRESS/api/v1/devices/$device/reports/last
}

function main4ino_get_last_target() {
  local device="$1"
  $AUTHENTICATED_CURL_CMD -X GET $SERVER_ADDRESS/api/v1/devices/$device/targets/last
}

function main4ino_get_summary_report() {
  local device="$1"
  $AUTHENTICATED_CURL_CMD -X GET $SERVER_ADDRESS/api/v1/devices/$device/reports/summary
}

function main4ino_get_summary_target() {
  local device="$1"
  $AUTHENTICATED_CURL_CMD -X GET $SERVER_ADDRESS/api/v1/devices/$device/targets/summary
}

function main4ino_get_last_report_actor() {
  local device="$1"
  local actor="$2"
  $AUTHENTICATED_CURL_CMD -X GET $SERVER_ADDRESS/api/v1/devices/$device/reports/actors/$actor/last
}

function main4ino_get_last_target_actor() {
  local device="$1"
  local actor="$2"
  $AUTHENTICATED_CURL_CMD -X GET $SERVER_ADDRESS/api/v1/devices/$device/target/actors/$actor/last
}

function main4ino_get_firmwares() {
  local project="$1"
  local platform="$2"
  $AUTHENTICATED_CURL_CMD -X GET $SERVER_ADDRESS/firmwares/$project/$platform
}


