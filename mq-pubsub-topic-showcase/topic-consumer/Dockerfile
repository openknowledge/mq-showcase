FROM openjdk:8-jre-alpine

ARG JAR_FILE
COPY target/${JAR_FILE} /opt/topic-consumer.jar

ENV JAVA_OPTS="-Djava.net.preferIPv4Stack=true -Djava.net.preferIPv4Addresses=true"

EXPOSE 9080
ENTRYPOINT exec java $JAVA_OPTS -jar /opt/topic-consumer.jar