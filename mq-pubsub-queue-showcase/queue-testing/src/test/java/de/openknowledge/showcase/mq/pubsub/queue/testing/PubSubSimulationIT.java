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
package de.openknowledge.showcase.mq.pubsub.queue.testing;

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

  @BeforeAll
  public static void registerLogConsumer() {
    QUEUE_PRODUCER.withLogConsumer(new Slf4jLogConsumer(LOG));
    QUEUE_CONSUMER_A.withLogConsumer(new Slf4jLogConsumer(LOG));
    QUEUE_CONSUMER_B.withLogConsumer(new Slf4jLogConsumer(LOG));
    QUEUE_CONSUMER_C.withLogConsumer(new Slf4jLogConsumer(LOG));
  }

  /**
   * |            | Message 1 | Message 2 | Message 3 | Message 4 | Message 5 | Message 6 |
   * |------------|-----------|-----------|-----------|-----------|-----------|-----------|
   * | Consumer A |     1     |     2     |     3     |     4     |     5     |     6     |
   * | Consumer B |     1     |     2     |           |           |   3,4,5   |     6     |
   * | Consumer C |           |           |   1,2,3   |     4     |           |           |
   */
  @Test
  void sendAndReceiveUpToSixMessages() throws TimeoutException {
    assertThat(QUEUE_CONSUMER_A.isRunning()).isFalse();
    assertThat(QUEUE_CONSUMER_B.isRunning()).isFalse();
    assertThat(QUEUE_CONSUMER_C.isRunning()).isFalse();

    LOG.info("Start QueueConsumerA and QueueConsumerB");

    QUEUE_CONSUMER_A.start();
    QUEUE_CONSUMER_B.start();

    assertThat(QUEUE_CONSUMER_A.isRunning()).isTrue();
    assertThat(QUEUE_CONSUMER_B.isRunning()).isTrue();
    assertThat(QUEUE_CONSUMER_C.isRunning()).isFalse();

    QUEUE_CONSUMER_A.followOutput(waitingConsumerA, OutputFrame.OutputType.STDOUT);
    QUEUE_CONSUMER_B.followOutput(waitingConsumerB, OutputFrame.OutputType.STDOUT);

    LOG.info("QueueConsumerA and QueueConsumerB started");

    sendAndReceiveMessage1();
    sendAndReceiveMessage2();

    LOG.info("Stop QueueConsumerB and start QueueConsumerC");

    QUEUE_CONSUMER_B.stop();
    QUEUE_CONSUMER_C.start();

    QUEUE_CONSUMER_C.followOutput(waitingConsumerC, OutputFrame.OutputType.STDOUT);

    assertThat(QUEUE_CONSUMER_A.isRunning()).isTrue();
    assertThat(QUEUE_CONSUMER_B.isRunning()).isFalse();
    assertThat(QUEUE_CONSUMER_C.isRunning()).isTrue();

    LOG.info("QueueConsumerB stopped and QueueConsumerC started");

    sendAndReceiveMessage3();
    sendAndReceiveMessage4();

    LOG.info("Start QueueConsumerB and stop QueueConsumerC");

    QUEUE_CONSUMER_B.start();
    QUEUE_CONSUMER_C.stop();

    assertThat(QUEUE_CONSUMER_A.isRunning()).isTrue();
    assertThat(QUEUE_CONSUMER_B.isRunning()).isTrue();
    assertThat(QUEUE_CONSUMER_C.isRunning()).isFalse();

    LOG.info("QueueConsumerB started and QueueConsumerC stopped");

    sendAndReceiveMessage5();
    sendAndReceiveMessage6();

    LOG.info("Stop QueueConsumerA and QueueConsumerB");

    QUEUE_CONSUMER_A.stop();
    QUEUE_CONSUMER_B.stop();

    assertThat(QUEUE_CONSUMER_A.isRunning()).isFalse();
    assertThat(QUEUE_CONSUMER_B.isRunning()).isFalse();
    assertThat(QUEUE_CONSUMER_C.isRunning()).isFalse();

    LOG.info("QueueConsumerA and QueueConsumerB stopped");
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

    String consumerALogs = QUEUE_CONSUMER_A.getLogs();
    assertThat(consumerALogs).contains("Received message {\"text\":\"Message 1\"}");

    String consumerBLogs = QUEUE_CONSUMER_B.getLogs();
    assertThat(consumerBLogs).contains("Received message {\"text\":\"Message 1\"}");

    String consumerCLogs = QUEUE_CONSUMER_C.getLogs();
    assertThat(consumerCLogs).isEmpty();

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

    String consumerALogs = QUEUE_CONSUMER_A.getLogs();
    assertThat(consumerALogs).contains("Received message {\"text\":\"Message 2\"}");

    String consumerBLogs = QUEUE_CONSUMER_B.getLogs();
    assertThat(consumerBLogs).contains("Received message {\"text\":\"Message 2\"}");

    String consumerCLogs = QUEUE_CONSUMER_C.getLogs();
    assertThat(consumerCLogs).isEmpty();

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

    LOG.info("Message 3 send");

    waitingConsumerA.waitUntil(frame -> frame.getUtf8String().contains("Received message {\"text\":\"Message 3\"}"), 15, TimeUnit.SECONDS);
    waitingConsumerC.waitUntil(frame -> frame.getUtf8String().contains("Received message {\"text\":\"Message 3\"}"), 30, TimeUnit.SECONDS);

    String consumerALogs = QUEUE_CONSUMER_A.getLogs();
    assertThat(consumerALogs).contains("Received message {\"text\":\"Message 3\"}");

    String consumerBLogs = QUEUE_CONSUMER_B.getLogs();
    assertThat(consumerBLogs).isEmpty();

    String consumerCLogs = QUEUE_CONSUMER_C.getLogs();
    assertThat(consumerCLogs).contains("Received message {\"text\":\"Message 1\"}");
    assertThat(consumerCLogs).contains("Received message {\"text\":\"Message 2\"}");
    assertThat(consumerCLogs).contains("Received message {\"text\":\"Message 3\"}");

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

    String consumerALogs = QUEUE_CONSUMER_A.getLogs();
    assertThat(consumerALogs).contains("Received message {\"text\":\"Message 4\"}");

    String consumerBLogs = QUEUE_CONSUMER_B.getLogs();
    assertThat(consumerBLogs).isEmpty();

    String consumerCLogs = QUEUE_CONSUMER_C.getLogs();
    assertThat(consumerCLogs).contains("Received message {\"text\":\"Message 4\"}");

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

    String consumerALogs = QUEUE_CONSUMER_A.getLogs();
    assertThat(consumerALogs).contains("Received message {\"text\":\"Message 5\"}");

    String consumerBLogs = QUEUE_CONSUMER_B.getLogs();
    assertThat(consumerBLogs).contains("Received message {\"text\":\"Message 3\"}");
    assertThat(consumerBLogs).contains("Received message {\"text\":\"Message 4\"}");
    assertThat(consumerBLogs).contains("Received message {\"text\":\"Message 5\"}");

    String consumerCLogs = QUEUE_CONSUMER_C.getLogs();
    assertThat(consumerCLogs).isEmpty();

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

    String consumerALogs = QUEUE_CONSUMER_A.getLogs();
    assertThat(consumerALogs).contains("Received message {\"text\":\"Message 6\"}");

    String consumerBLogs = QUEUE_CONSUMER_B.getLogs();
    assertThat(consumerBLogs).contains("Received message {\"text\":\"Message 6\"}");

    String consumerCLogs = QUEUE_CONSUMER_C.getLogs();
    assertThat(consumerCLogs).isEmpty();

    LOG.info("Message 6 received");
  }
}