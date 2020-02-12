#!/usr/bin/env bash
set -e

usage() {
  echo "
    $0
   -help
     display this help
   -email
     gmail for receiving alert
   -email-password
     your gmail account password
  -host
     hostname of apiserver to monitor
   -reset
     passing this flag to reset monitoring data if needed to clear out unneeded data and limit the disk usage.

   "
   exit 1
}

while [ $# -gt 0 ]
do
  case $1 in
  -host)      host=$2 ;;
  -email)       email=$2 ;;
  *)
     echo "****" Bad argument:  $1
     usage
     exit  ;;
  esac
  shift 2
done

java -cp target/mcs-monitor-1.0-SNAPSHOT-jar-with-dependencies.jar com.mapr.qa.Monitor -host ${host}