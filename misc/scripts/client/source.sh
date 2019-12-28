#!/usr/bin/env bash

source settings.conf

set +x 

function session() {
  curl $CURL_OPTS -s -u $USER_PASSWORD -X POST "$SERVER_ADDRESS/api/v1/session"
}


AUTHENTICATED_CURL_CMD="curl $CURL_OPTS --header session:`session`"

echo $AUTHENTICATED_CURL_CMD

function description() {
  local device="$1"
  local description="$2"
  $AUTHENTICATED_CURL_CMD -X POST "$SERVER_ADDRESS/api/v1/devices/$device/descriptions" -d "$description"
}

function insert() {
  local device="$1"
  local actor="$2"
  local body="$3"

  $AUTHENTICATED_CURL_CMD -X POST $SERVER_ADDRESS/api/v1/devices/$device/reports/actors/$actor -d "$body"
}

function getlast() {
  local device="$1"
  local actor="$2"
  $AUTHENTICATED_CURL_CMD -X GET $SERVER_ADDRESS/api/v1/devices/$device/reports/actors/$actor/last
}

function getlastall() {
  local device="$1"
  $AUTHENTICATED_CURL_CMD -X GET $SERVER_ADDRESS/api/v1/devices/$device/reports/last
}

