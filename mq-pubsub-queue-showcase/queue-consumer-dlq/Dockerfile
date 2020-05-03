FROM openjdk:8-jre-alpine

ARG JAR_FILE
COPY target/${JAR_FILE} /opt/queue-consumer-dlq.jar

HEALTHCHECK --start-period=60s CMD curl -f http://localhost:9080/queue-consumer-dlq/health/live || exit 1

ENV JAVA_OPTS="-Djava.net.preferIPv4Stack=true -Djava.net.preferIPv4Addresses=true"

EXPOSE 9080
ENTRYPOINT exec java $JAVA_OPTS -jar /opt/queue-consumer-dlq.jar