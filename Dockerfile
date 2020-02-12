FROM maven:3.6-jdk-8 as Build

WORKDIR /usr/app
COPY . .
RUN mvn install

ENTRYPOINT ["/usr/app/run.sh"]
CMD []