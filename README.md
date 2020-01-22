write stats to mapr json db

on client or cluster node as mapr user:
maprcli volume create -name mcs -path /user/mapr/mcs
maprcli table create -path /user/mapr/mcs/uptime -tabletype json

build jar
mvn clean package;scp target/mcs-uptime-1.0-SNAPSHOT-jar-with-dependencies.jar mapr@10.10.100.104:/home/mapr
log on to 10.10.100.104 and run as mapr user
java -cp `mapr classpath`:./mcs-uptime-1.0-SNAPSHOT-jar-with-dependencies.jar com.mapr.qa.Monitor  -host qa-node104.qa.lab -interval 60000

