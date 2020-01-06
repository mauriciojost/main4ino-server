#!/usr/bin/env bash
source source.sh
for i in $DEVICES_LIST
do 
  echo "## $i:"
  main4ino_post_target $i settings '{"+utarget":"SKIP"}'
  echo ""
  echo ""
done

