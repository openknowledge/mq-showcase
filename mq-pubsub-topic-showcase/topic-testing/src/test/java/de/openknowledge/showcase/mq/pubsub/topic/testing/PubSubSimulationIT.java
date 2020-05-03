/*
 * Copyright (C) open knowledge GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions
 * and limitations under the License.
 */
package de.openknowledge.showcase.mq.pubsub.topic.testing;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.output.WaitingConsumer;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.ws.rs.core.Response;

import io.restassured.RestAssured;

/**
 * A test class that verifies that a consumer can receive a message send by a producer.
 */
public class PubSubSimulationIT extends AbstractIntegrationTest {

  private static final Logger LOG = LoggerFactory.getLogger(PubSubSimulationIT.class);

  private WaitingConsumer waitingConsumerA = new WaitingConsumer();
  private WaitingConsumer waitingConsumerB = new WaitingConsumer();
  private WaitingConsumer waitingConsumerC = new WaitingConsumer();
  private WaitingConsumer waitingConsumerD = new WaitingConsumer();

  @BeforeAll
  public static void registerLogConsumer() {
    TOPIC_PRODUCER.withLogConsumer(new Slf4jLogConsumer(LOG));
    TOPIC_CONSUMER_A.withLogConsumer(new Slf4jLogConsumer(LOG));
    TOPIC_CONSUMER_B.withLogConsumer(new Slf4jLogConsumer(LOG));
    TOPIC_CONSUMER_C.withLogConsumer(new Slf4jLogConsumer(LOG));
    TOPIC_CONSUMER_D.withLogConsumer(new Slf4jLogConsumer(LOG));
  }

  /**
   * |            | Message 1 | Message 2 | Message 3 | Message 4 | Message 5 | Message 6 |
   * |------------|-----------|-----------|-----------|-----------|-----------|-----------|
   * | Consumer A |     x     |     x     |     x     |     x     |     x     |     x     | (non-durable) // receives all messages
   * | Consumer B |     x     |     x     |           |           |     x     |     x     | (non-durable) // receives messages 1, 2, 5, 6
   * | Consumer C |           |           |     x     |     x     |           |           | (non-durable) // receives messages 3, 4
   * | Consumer D |     x     |     x     |     x     |     x     |     x     |     x     | (durable)     // receives all messages
   */
  @Test
  void sendAndReceiveUpToSixMessages() throws TimeoutException {
    assertThat(TOPIC_CONSUMER_A.isRunning()).isFalse();
    assertThat(TOPIC_CONSUMER_B.isRunning()).isFalse();
    assertThat(TOPIC_CONSUMER_C.isRunning()).isFalse();
    assertThat(TOPIC_CONSUMER_D.isRunning()).isFalse();

    LOG.info("Start TopicConsumerA and TopicConsumerB");

    TOPIC_CONSUMER_A.start();
    TOPIC_CONSUMER_B.start();

    assertThat(TOPIC_CONSUMER_A.isRunning()).isTrue();
    assertThat(TOPIC_CONSUMER_B.isRunning()).isTrue();
    assertThat(TOPIC_CONSUMER_C.isRunning()).isFalse();
    assertThat(TOPIC_CONSUMER_D.isRunning()).isFalse();

    TOPIC_CONSUMER_A.followOutput(waitingConsumerA, OutputFrame.OutputType.STDOUT);
    TOPIC_CONSUMER_B.followOutput(waitingConsumerB, OutputFrame.OutputType.STDOUT);

    LOG.info("TopicConsumerA and TopicConsumerB started");

    sendAndReceiveMessage1();

    LOG.info("Start TopicConsumerD");

    TOPIC_CONSUMER_D.start();

    TOPIC_CONSUMER_D.followOutput(waitingConsumerC, OutputFrame.OutputType.STDOUT);

    assertThat(TOPIC_CONSUMER_A.isRunning()).isTrue();
    assertThat(TOPIC_CONSUMER_B.isRunning()).isTrue();
    assertThat(TOPIC_CONSUMER_C.isRunning()).isFalse();
    assertThat(TOPIC_CONSUMER_D.isRunning()).isTrue();

    LOG.info("TopicConsumerD started");

    sendAndReceiveMessage2();

    LOG.info("Stop TopicConsumerB and start TopicConsumerC");

    TOPIC_CONSUMER_B.stop();
    TOPIC_CONSUMER_C.start();

    TOPIC_CONSUMER_C.followOutput(waitingConsumerC, OutputFrame.OutputType.STDOUT);

    assertThat(TOPIC_CONSUMER_A.isRunning()).isTrue();
    assertThat(TOPIC_CONSUMER_B.isRunning()).isFalse();
    assertThat(TOPIC_CONSUMER_C.isRunning()).isTrue();
    assertThat(TOPIC_CONSUMER_D.isRunning()).isTrue();

    LOG.info("TopicConsumerB stopped and TopicConsumerC started");

    sendAndReceiveMessage3();
    sendAndReceiveMessage4();

    LOG.info("Start TopicConsumerB and stop TopicConsumerC");

    TOPIC_CONSUMER_B.start();
    TOPIC_CONSUMER_C.stop();

    assertThat(TOPIC_CONSUMER_A.isRunning()).isTrue();
    assertThat(TOPIC_CONSUMER_B.isRunning()).isTrue();
    assertThat(TOPIC_CONSUMER_C.isRunning()).isFalse();
    assertThat(TOPIC_CONSUMER_D.isRunning()).isTrue();

    LOG.info("TopicConsumerB started and TopicConsumerC stopped");

    sendAndReceiveMessage5();
    sendAndReceiveMessage6();

    LOG.info("Stop TopicConsumerA, TopicConsumerB and TopicConsumerD");

    TOPIC_CONSUMER_A.stop();
    TOPIC_CONSUMER_B.stop();

    assertThat(TOPIC_CONSUMER_A.isRunning()).isFalse();
    assertThat(TOPIC_CONSUMER_B.isRunning()).isFalse();
    assertThat(TOPIC_CONSUMER_C.isRunning()).isFalse();
    assertThat(TOPIC_CONSUMER_D.isRunning()).isFalse();

    LOG.info("TopicConsumerA, TopicConsumerB and TopicConsumerD stopped");
  }

  private void sendAndReceiveMessage1() throws TimeoutException {
    LOG.info("Send message 1");

    RestAssured.given(requestSpecification)
        .queryParam("msg", "Message 1")
        .when()
        .get("/api/messages")
        .then()
        .statusCode(Response.Status.ACCEPTED.getStatusCode());

    LOG.info("Message 1 send");

    waitingConsumerA.waitUntil(frame -> frame.getUtf8String().contains("Received message {\"text\":\"Message 1\"}"), 15, TimeUnit.SECONDS);
    waitingConsumerB.waitUntil(frame -> frame.getUtf8String().contains("Received message {\"text\":\"Message 1\"}"), 15, TimeUnit.SECONDS);

    String consumerALogs = TOPIC_CONSUMER_A.getLogs();
    assertThat(consumerALogs).contains("Received message {\"text\":\"Message 1\"}");

    String consumerBLogs = TOPIC_CONSUMER_B.getLogs();
    assertThat(consumerBLogs).contains("Received message {\"text\":\"Message 1\"}");

    String consumerCLogs = TOPIC_CONSUMER_C.getLogs();
    assertThat(consumerCLogs).isEmpty();

    String consumerDLogs = TOPIC_CONSUMER_D.getLogs();
    assertThat(consumerDLogs).isEmpty();

    LOG.info("Message 1 received");
  }

  private void sendAndReceiveMessage2() throws TimeoutException {
    LOG.info("Send message 2");

    RestAssured.given(requestSpecification)
        .queryParam("msg", "Message 2")
        .when()
        .get("/api/messages")
        .then()
        .statusCode(Response.Status.ACCEPTED.getStatusCode());

    LOG.info("Message 2 send");

    waitingConsumerA.waitUntil(frame -> frame.getUtf8String().contains("Received message {\"text\":\"Message 2\"}"), 15, TimeUnit.SECONDS);
    waitingConsumerB.waitUntil(frame -> frame.getUtf8String().contains("Received message {\"text\":\"Message 2\"}"), 15, TimeUnit.SECONDS);
    waitingConsumerD.waitUntil(frame -> frame.getUtf8String().contains("Received message {\"text\":\"Message 2\"}"), 15, TimeUnit.SECONDS);

    String consumerALogs = TOPIC_CONSUMER_A.getLogs();
    assertThat(consumerALogs).contains("Received message {\"text\":\"Message 2\"}");

    String consumerBLogs = TOPIC_CONSUMER_B.getLogs();
    assertThat(consumerBLogs).contains("Received message {\"text\":\"Message 2\"}");

    String consumerCLogs = TOPIC_CONSUMER_C.getLogs();
    assertThat(consumerCLogs).isEmpty();

    String consumerDLogs = TOPIC_CONSUMER_D.getLogs();
    assertThat(consumerDLogs).contains("Received message {\"text\":\"Message 2\"}");

    LOG.info("Message 2 received");
  }

  private void sendAndReceiveMessage3() throws TimeoutException {
    LOG.info("Send message 3");

    RestAssured.given(requestSpecification)
        .queryParam("msg", "Message 3")
        .when()
        .get("/api/messages")
        .then()
        .statusCode(Response.Status.ACCEPTED.getStatusCode());

    String producerLogs = TOPIC_PRODUCER.getLogs();
    assertThat(producerLogs).contains("Send message {\"text\":\"Message 3\"} to queue");

    LOG.info("Message 3 send");

    waitingConsumerA.waitUntil(frame -> frame.getUtf8String().contains("Received message {\"text\":\"Message 3\"}"), 15, TimeUnit.SECONDS);
    waitingConsumerC.waitUntil(frame -> frame.getUtf8String().contains("Received message {\"text\":\"Message 3\"}"), 30, TimeUnit.SECONDS);
    waitingConsumerD.waitUntil(frame -> frame.getUtf8String().contains("Received message {\"text\":\"Message 3\"}"), 30, TimeUnit.SECONDS);

    String consumerALogs = TOPIC_CONSUMER_A.getLogs();
    assertThat(consumerALogs).contains("Received message {\"text\":\"Message 3\"}");

    String consumerBLogs = TOPIC_CONSUMER_B.getLogs();
    assertThat(consumerBLogs).isEmpty();

    String consumerCLogs = TOPIC_CONSUMER_C.getLogs();
    assertThat(consumerCLogs).doesNotContain("Received message {\"text\":\"Message 1\"}");
    assertThat(consumerCLogs).doesNotContain("Received message {\"text\":\"Message 2\"}");
    assertThat(consumerCLogs).contains("Received message {\"text\":\"Message 3\"}");

    String consumerDLogs = TOPIC_CONSUMER_D.getLogs();
    assertThat(consumerDLogs).contains("Received message {\"text\":\"Message 3\"}");

    LOG.info("Message 3 received");
  }

  private void sendAndReceiveMessage4() throws TimeoutException {
    LOG.info("Send message 4");

    RestAssured.given(requestSpecification)
        .queryParam("msg", "Message 4")
        .when()
        .get("/api/messages")
        .then()
        .statusCode(Response.Status.ACCEPTED.getStatusCode());

    LOG.info("Message 4 send");

    waitingConsumerA.waitUntil(frame -> frame.getUtf8String().contains("Received message {\"text\":\"Message 4\"}"), 15, TimeUnit.SECONDS);
    waitingConsumerC.waitUntil(frame -> frame.getUtf8String().contains("Received message {\"text\":\"Message 4\"}"), 15, TimeUnit.SECONDS);
    waitingConsumerD.waitUntil(frame -> frame.getUtf8String().contains("Received message {\"text\":\"Message 4\"}"), 15, TimeUnit.SECONDS);

    String consumerALogs = TOPIC_CONSUMER_A.getLogs();
    assertThat(consumerALogs).contains("Received message {\"text\":\"Message 4\"}");

    String consumerBLogs = TOPIC_CONSUMER_B.getLogs();
    assertThat(consumerBLogs).isEmpty();

    String consumerCLogs = TOPIC_CONSUMER_C.getLogs();
    assertThat(consumerCLogs).contains("Received message {\"text\":\"Message 4\"}");

    String consumerDLogs = TOPIC_CONSUMER_D.getLogs();
    assertThat(consumerDLogs).contains("Received message {\"text\":\"Message 4\"}");

    LOG.info("Message 4 received");
  }

  private void sendAndReceiveMessage5() throws TimeoutException {
    LOG.info("Send 5 message");

    RestAssured.given(requestSpecification)
        .queryParam("msg", "Message 5")
        .when()
        .get("/api/messages")
        .then()
        .statusCode(Response.Status.ACCEPTED.getStatusCode());

    LOG.info("Message 5 send");

    waitingConsumerA.waitUntil(frame -> frame.getUtf8String().contains("Received message {\"text\":\"Message 5\"}"), 15, TimeUnit.SECONDS);
    waitingConsumerB.waitUntil(frame -> frame.getUtf8String().contains("Received message {\"text\":\"Message 5\"}"), 30, TimeUnit.SECONDS);
    waitingConsumerD.waitUntil(frame -> frame.getUtf8String().contains("Received message {\"text\":\"Message 5\"}"), 30, TimeUnit.SECONDS);

    String consumerALogs = TOPIC_CONSUMER_A.getLogs();
    assertThat(consumerALogs).contains("Received message {\"text\":\"Message 5\"}");

    String consumerBLogs = TOPIC_CONSUMER_B.getLogs();
    assertThat(consumerBLogs).doesNotContain("Received message {\"text\":\"Message 3\"}");
    assertThat(consumerBLogs).doesNotContain("Received message {\"text\":\"Message 4\"}");
    assertThat(consumerBLogs).contains("Received message {\"text\":\"Message 5\"}");

    String consumerCLogs = TOPIC_CONSUMER_C.getLogs();
    assertThat(consumerCLogs).isEmpty();

    String consumerDLogs = TOPIC_CONSUMER_D.getLogs();
    assertThat(consumerDLogs).contains("Received message {\"text\":\"Message 5\"}");

    LOG.info("Message 5 received");
  }

  private void sendAndReceiveMessage6() throws TimeoutException {
    LOG.info("Send message 6");

    RestAssured.given(requestSpecification)
        .queryParam("msg", "Message 6")
        .when()
        .get("/api/messages")
        .then()
        .statusCode(Response.Status.ACCEPTED.getStatusCode());

    LOG.info("Message 6 send");

    waitingConsumerA.waitUntil(frame -> frame.getUtf8String().contains("Received message {\"text\":\"Message 6\"}"), 15, TimeUnit.SECONDS);
    waitingConsumerB.waitUntil(frame -> frame.getUtf8String().contains("Received message {\"text\":\"Message 6\"}"), 15, TimeUnit.SECONDS);
    waitingConsumerD.waitUntil(frame -> frame.getUtf8String().contains("Received message {\"text\":\"Message 6\"}"), 15, TimeUnit.SECONDS);

    String consumerALogs = TOPIC_CONSUMER_A.getLogs();
    assertThat(consumerALogs).contains("Received message {\"text\":\"Message 6\"}");

    String consumerBLogs = TOPIC_CONSUMER_B.getLogs();
    assertThat(consumerBLogs).contains("Received message {\"text\":\"Message 6\"}");

    String consumerCLogs = TOPIC_CONSUMER_C.getLogs();
    assertThat(consumerCLogs).isEmpty();

    String consumerDLogs = TOPIC_CONSUMER_D.getLogs();
    assertThat(consumerDLogs).contains("Received message {\"text\":\"Message 6\"}");

    LOG.info("Message 6 received");
  }
}