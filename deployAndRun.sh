#!/usr/bin/env bash
set -e

usage() {
  echo "
    $0
   -help
     display this help. Run this script on a node where you want to setup the elasticsearch and kibana containers. Docker that support docker-compose 3.x  is required.
   -mcshost
     mcs server to monitor
   -eshost
     elasticsearch ip to index into
   -esport
     host esport to map to from container
   -kibana_port
     kibana port to map to from container
   -rebuild-monitor-jar
      1 to rebuild or 0 not (default)
   "
   exit 1
}

while [ $# -gt 0 ]
do
  case $1 in
  -mcshost)    mcshost=$2 ;;
  -eshost)      eshost=$2 ;;
  -esport)      esport=$2 ;;
  -kibana_port)       kport=$2 ;;
  -rebuild-monitor-jar) rebuildjar=$2;;
  *)
     echo "****" Bad argument:  $1
     usage
     exit  ;;
  esac
  shift 2
done
# below is needed to run the container on linux
sysctl -w vm.max_map_count=262144

esport=${esport:-9200}
kport=${kport:-5601}
# note be sure to upgrade docker to the latest 19, because we are using 3.7 docker composer version
ESPORT=${esport} KIBANA_PORT=${kport} docker-compose up -d
# wait for es and kibana service ready
sleep 300

# import index and dashboard if it's new setup
curl -X GET localhost:5611/api/saved_objects/_find?type=index-pattern |grep mcsmonitor > /dev/null 2>&1
ret=$?
if [ ${ret} -ne 0 ]
then
  curl -X POST "localhost:${esport}/api/saved_objects/_import" -H "kbn-xsrf: true" --form file=@mcsmonitor.ndjson
fi

rebuildjar=${rebuildjar:-0}
if [ ${rebuildjar} -eq 1 ]
then
  docker build -t mcsmonitor .
fi

cmd="docker run -it mcsmonitor -mcshost 10.10.88.60 -eshost 10.10.30.150 -esport 9292 -email vsummers98@gmail.com -email-password"
screen -S mcsmonitor bash -c "${cmd}"
