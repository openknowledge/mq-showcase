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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.containers.output.WaitingConsumer;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.ws.rs.core.Response;

import io.restassured.RestAssured;

/**
 * A test class that verifies that a consumer cannot process a message which is moved to the dead letter queue
 */
public class DLQConsumerIT extends AbstractIntegrationTest {

  private static final Logger LOG = LoggerFactory.getLogger(DLQConsumerIT.class);

  private WaitingConsumer waitingConsumer = new WaitingConsumer();
  private WaitingConsumer waitingConsumerDLQ = new WaitingConsumer();

  @BeforeEach
  void setUp() {
    assertThat(TOPIC_CONSUMER_A.isRunning()).isFalse();
    assertThat(DLQ_CONSUMER.isRunning()).isFalse();

    LOG.info("Start TopicConsumer and DLQConsumer");

    TOPIC_CONSUMER_A.start();
    DLQ_CONSUMER.start();

    TOPIC_CONSUMER_A.followOutput(waitingConsumer, OutputFrame.OutputType.STDOUT);
    DLQ_CONSUMER.followOutput(waitingConsumerDLQ, OutputFrame.OutputType.STDOUT);

    assertThat(TOPIC_CONSUMER_A.isRunning()).isTrue();
    assertThat(DLQ_CONSUMER.isRunning()).isTrue();

    LOG.info("TopicConsumer and DLQConsumer started");
  }

  @AfterEach
  void tearDown() {
    assertThat(TOPIC_CONSUMER_A.isRunning()).isTrue();
    assertThat(DLQ_CONSUMER.isRunning()).isTrue();

    LOG.info("Stop TopicConsumer and DLQConsumer");

    TOPIC_CONSUMER_A.stop();
    DLQ_CONSUMER.stop();

    assertThat(TOPIC_CONSUMER_A.isRunning()).isFalse();
    assertThat(DLQ_CONSUMER.isRunning()).isFalse();

    LOG.info("TopicConsumer and DLQConsumer stopped");
  }

  @Test
  void sendAndReceiveMessageFromDLQ() throws TimeoutException {
    LOG.info("Send message");

    RestAssured.given(requestSpecification)
        .queryParam("msg", "fail")
        .when()
        .get("/api/messages")
        .then()
        .statusCode(Response.Status.ACCEPTED.getStatusCode());

    String producerLogs = TOPIC_PRODUCER.getLogs();
    assertThat(producerLogs).contains("Send message {\"text\":\"fail\"} to topic");

    LOG.info("Message send");

    waitingConsumer.waitUntil(frame -> frame.getUtf8String().contains("Message processing failed"), 2, TimeUnit.MINUTES);
    waitingConsumerDLQ.waitUntil(frame -> frame.getUtf8String().contains("Received message {\"text\":\"fail\"}"), 2, TimeUnit.MINUTES);

    LOG.info("Message received");
  }
}
