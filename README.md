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

## Usage
To get usage:
    ```
    java -cp target/mcs-monitor-1.0-SNAPSHOT-jar-with-dependencies.jar com.mapr.qa.Monitor --help 
    ```
    
To run it:
    ```
    java -cp target/mcs-monitor-1.0-SNAPSHOT-jar-with-dependencies.jar com.mapr.qa.Monitor -host 10.10.88.60 -email vsummers98@gmail.com  -email-password
    enter your password at the prompt.

    ```



