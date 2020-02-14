# API Server Monitor

## How it works

The application will poll the apiserver running on dev-monet every 1 minute and log the status to elasticsearch which can be visualized and generate report 
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
    PUT /mcsmonitor
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
    Usage: <main class> [options]
      Options:
     -email
      gmail for receiving alert
     -email-password
      your gmail account password
     -eshost
      ip of the elasticsearch host
     -esport
      port of the elasticsearch host
      Default: 9200
     -help
      display this help
     -mcshost
      hostname of apiserver to monitor
     -reset
      passing this flag to reset monitoring data at beginning of weekly run
      Default: false
    
To run it:
    ```
    java -cp target/mcs-monitor-1.0-SNAPSHOT-jar-with-dependencies.jar com.mapr.qa.Monitor -eshost 10.10.100.104 -mcshost 10.10.88.60 -email your-gmail  -email-password
    enter your password at the prompt.

    ```

## Docker container way, run this on a linux server 
    prerequists: docker that support docker-compose 3.x and git
    git clone https://github.com/mapr/private-mcs-tools

    ./deployAndRun.sh -h
    ./deployAndRun.sh -mcshost 10.10.88.60 -eshost 10.10.30.150 -esport 9292 -kibana_port 5611


