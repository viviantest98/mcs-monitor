FROM maven:3.6.0-jdk-8-alpine as Build

WORKDIR /usr/app
COPY . .

RUN mvn package

FROM openjdk:8-jre-alpine
WORKDIR /usr/app
COPY --from=Build /usr/app /usr/app

ENTRYPOINT [ "java", "-cp", "/usr/app/target/mcs-monitor-1.0-SNAPSHOT-jar-with-dependencies.jar", "com.mapr.qa.Monitor" ]
CMD []
