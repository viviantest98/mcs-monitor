# Server Monitor

## How it  works

The application will poll a server running on a node every 1 minute and log the status to elasticsearch which can be visualized and generate report 
later on so we can see the uptime and downtime. Also it will sends an alert email so someone can address the issue. The elasticsearch is installed and configured
on a host and index mapping is created prior to run the application. 

Data is collected for one week from the time dev-monet is rebuilt, report will be generated and exported from elasticsearch (manual for now)
We do not want to delete the index because we would have to build the dashboard each time. So we only remove the old data to prevent filling up the disk space.

## Steps
1. build jar 
 ```
   $ check out the code
   $ mvn clean package
 ```

## point to existing kibana and elasticsearch host that has the indexes created
    to create the index, run this in the kibana tool UI:
    PUT /monitor
    {
      "mappings": {
        
          "properties": {
            "time": {
             "type": "date",
             "format": "epoch_second"
           },
           "statusCode": {
             "type": "integer"
             },
           "msg": {
             "type": "text"
           }
          }    
      }
    }

To get usage:
    ```
    java -cp target/mcs-monitor-1.0-SNAPSHOT-jar-with-dependencies.jar com.mapr.qa.Monitor --help 
    ```
    
To run it:
    ```
    java -cp target/monitor-1.0-SNAPSHOT-jar-with-dependencies.jar com.mapr.qa.Monitor -eshost <ESHOST> -mcshost <HOST_IP> -email your-gmail  -email-password
    enter your password at the prompt.

    ```

## Docker container way, run this on a linux server 
    prerequists: docker that support docker-compose 3.x and git
    check out the source

    ./deployAndRun.sh -h
    ./deployAndRun.sh -mcshost <HOST_IP> -eshost <ESHOST> -esport 9292 -kibana_port 5611


