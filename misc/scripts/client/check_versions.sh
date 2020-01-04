#!/usr/bin/env bash
source source.sh
for i in $DEVICES_LIST
do 
  echo "## $i:"
  main4ino_get_summary_report $i | jq . | grep "version\|project\|platform\|utarget\|ufreq\|alias"
  main4ino_get_last_report $i | jq .dbId.creation | xargs -I% date -d @%
  main4ino_get_logs $i 0 200
  echo ""
  echo ""
done

for pl in $PLATFORMS_LIST
do
  for pr in $PROJECTS_LIST
  do
    
    echo "PLATFORM $pl PROJECT $pr"
    main4ino_get_firmwares $pr $pl | jq . | grep version | tail -2
  done
done
