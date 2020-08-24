FROM openjdk:8-jre-alpine

ARG JAR_FILE
COPY target/${JAR_FILE} /opt/queue-producer.jar

EXPOSE 9080

ENTRYPOINT exec java -Djava.net.preferIPv4Stack=true -Djava.net.preferIPv4Addresses=true -jar /opt/queue-producer.jar