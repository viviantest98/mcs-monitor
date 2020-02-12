#!/usr/bin/env bash

# below is needed to run the container on linux
sysctl -w vm.max_map_count=262144

# note be sure to upgrade docker to the latest 19, because we are using 3.7 docker composer version
docker-compose up
# output is on console, may want to store it in logs

git clone
cd mcs-monitor
mvn clean install

java -cp ./target/mcs-monitor-1.0-SNAPSHOT-jar-with-dependencies.jar com.mapr.qa.Monitor -host 10.10.88.60 -email vsummers98@gmail.com -email-password
