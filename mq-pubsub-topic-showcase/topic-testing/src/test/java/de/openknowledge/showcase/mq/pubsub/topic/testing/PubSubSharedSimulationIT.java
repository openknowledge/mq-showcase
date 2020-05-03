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
public class PubSubSharedSimulationIT extends AbstractIntegrationTest {

  private static final Logger LOG = LoggerFactory.getLogger(PubSubSharedSimulationIT.class);

  private WaitingConsumer waitingConsumer = new WaitingConsumer();

  @BeforeAll
  public static void registerLogConsumer() {
    TOPIC_PRODUCER.withLogConsumer(new Slf4jLogConsumer(LOG));
    TOPIC_SHARED_CONSUMER_1.withLogConsumer(new Slf4jLogConsumer(LOG));
    TOPIC_SHARED_CONSUMER_2.withLogConsumer(new Slf4jLogConsumer(LOG));
  }

  @Test
  void sendAndReceiveUpToSixMessages() throws TimeoutException {
    assertThat(TOPIC_SHARED_CONSUMER_1.isRunning()).isFalse();
    assertThat(TOPIC_SHARED_CONSUMER_2.isRunning()).isFalse();

    LOG.info("Start TopicSharedConsumer1 and TopicSharedConsumer2");

    TOPIC_SHARED_CONSUMER_1.start();
    TOPIC_SHARED_CONSUMER_2.start();

    TOPIC_SHARED_CONSUMER_1.followOutput(waitingConsumer, OutputFrame.OutputType.STDOUT);
    TOPIC_SHARED_CONSUMER_2.followOutput(waitingConsumer, OutputFrame.OutputType.STDOUT);

    assertThat(TOPIC_SHARED_CONSUMER_1.isRunning()).isTrue();
    assertThat(TOPIC_SHARED_CONSUMER_2.isRunning()).isTrue();

    LOG.info("TopicSharedConsumer1 and TopicSharedConsumer2 started");

    sendAndReceiveMessage("1");
    sendAndReceiveMessage("2");
    sendAndReceiveMessage("3");
    sendAndReceiveMessage("4");
    sendAndReceiveMessage("5");
    sendAndReceiveMessage("6");

    TOPIC_SHARED_CONSUMER_1.stop();
    TOPIC_SHARED_CONSUMER_2.stop();

    assertThat(TOPIC_SHARED_CONSUMER_1.isRunning()).isFalse();
    assertThat(TOPIC_SHARED_CONSUMER_2.isRunning()).isFalse();

    LOG.info("TopicSharedConsumer1 and TopicSharedConsumer2 stopped");
  }

  private void sendAndReceiveMessage(final String n) throws TimeoutException {
    LOG.info("Send message " + n);

    RestAssured.given(requestSpecification)
        .queryParam("msg", "Message " + n)
        .when()
        .get("/api/messages")
        .then()
        .statusCode(Response.Status.ACCEPTED.getStatusCode());

    LOG.info("Message " + n + " send");

    waitingConsumer.waitUntil(frame -> frame.getUtf8String().contains("Received message {\"text\":\"Message " + n + "\"}"), 2, TimeUnit.MINUTES);

    String sharedConsumer1Logs = TOPIC_SHARED_CONSUMER_1.getLogs();
    boolean receivedBySharedConsumer1 = sharedConsumer1Logs.contains("Received message {\"text\":\"Message " + n + "\"}");

    String sharedConsumer2Logs = TOPIC_SHARED_CONSUMER_2.getLogs();
    boolean receivedBySharedConsumer2 = sharedConsumer2Logs.contains("Received message {\"text\":\"Message " + n + "\"}");

    assertThat(receivedBySharedConsumer1 ^ receivedBySharedConsumer2).isTrue();
    
    LOG.info("Message " + n + " received");
  }
}
