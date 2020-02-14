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
  *)
     echo "****" Bad argument:  $1
     usage
     exit  ;;
  esac
  shift 2
done

java -jar mcs-monitor-1.0-SNAPSHOT-jar-with-dependencies.jar com.mapr.qa.Monitor -mcshost ${mcshost} -eshost ${eshost} -esport ${esport} -email vsummers98@gmail.com -email-password
