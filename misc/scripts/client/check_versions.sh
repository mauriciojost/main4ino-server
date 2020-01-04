#!/usr/bin/env bash
source source.sh
for i in $DEVICES_LIST
do 
  echo "## $i:"
  main4ino_get_summary_report $i | jq . | grep "version\|project\|platform"
done

for pl in $PLATFORMS_LIST
do
  for pr in $PROJECTS_LIST
  do
    
    echo "PLATFORM $pl PROJECT $pr"
    main4ino_get_firmwares $pr $pl | jq . | grep version | tail -2
  done
done
