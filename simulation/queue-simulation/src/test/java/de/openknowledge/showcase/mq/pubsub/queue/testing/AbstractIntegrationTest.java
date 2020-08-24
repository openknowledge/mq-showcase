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

import org.junit.jupiter.api.BeforeAll;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.containers.wait.strategy.WaitAllStrategy;
import org.testcontainers.images.builder.ImageFromDockerfile;

import java.nio.file.Paths;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

import io.restassured.builder.RequestSpecBuilder;
import io.restassured.specification.RequestSpecification;

/**
 * Provides testcontainers for integration tests.
 */
public abstract class AbstractIntegrationTest {

  protected static final Network NETWORK = Network.newNetwork();

  private static final GenericContainer MQ_BROKER = new GenericContainer("mq-showcase/mq-broker:0")
          .withNetwork(NETWORK)
          .withNetworkAliases("mq")
          .withExposedPorts(1414)
          .withEnv("LICENSE", "accept")
          .withEnv("MQ_QMGR_NAME", "QM1")
          .waitingFor(new WaitAllStrategy()
                          .withStrategy(Wait.forLogMessage(".*Started web server*.", 1))
                          .withStartupTimeout(Duration.ofMinutes(1)));

  private static final Map<String, String> env = new LinkedHashMap<>();

  static {
    env.put("MQ_CHANNEL", "DEV.ADMIN.SVRCONN");
    env.put("MQ_QMANAGER", "QM1");
    env.put("MQ_PORT", "1414");
    env.put("MQ_HOST", "mq");
    env.put("MQ_USER", "admin");
    env.put("MQ_PASSWORD", "passw0rd");
    env.put("WLP_LOGGING_CONSOLE_LOGLEVEL", "audit");
  }

  protected static final GenericContainer QUEUE_PRODUCER = new GenericContainer("mq-showcase/queue-producer:0")
      .withNetwork(NETWORK)
      .withNetworkAliases("queue-producer")
      .dependsOn(MQ_BROKER)
      .withExposedPorts(9080)
      .withEnv(env)
      .withEnv("MQ_QUEUE", "DEV.QUEUE.MESSAGES.ALIAS")
      .withEnv("MQ_CLIENT_ID", "queue-producer")
      .waitingFor(new WaitAllStrategy()
                      .withStrategy(Wait.forHttp("/health/live"))
                      .withStartupTimeout(Duration.ofMinutes(2)));

  protected static final GenericContainer QUEUE_CONSUMER_A = new GenericContainer("mq-showcase/queue-consumer:0")
      .withNetwork(NETWORK)
      .withNetworkAliases("queue-consumer-a")
      .dependsOn(MQ_BROKER)
      .withExposedPorts(9080)
      .withEnv(env)
      .withEnv("MQ_QUEUE", "DEV.QUEUE.MESSAGES.1")
      .withEnv("MQ_CLIENT_ID", "queue-consumer-1")
      .waitingFor(new WaitAllStrategy()
                      .withStrategy(Wait.forHttp("/health/live"))
                      .withStartupTimeout(Duration.ofMinutes(2)));

  protected static final GenericContainer QUEUE_CONSUMER_B = new GenericContainer("mq-showcase/queue-consumer:0")
      .withNetwork(NETWORK)
      .withNetworkAliases("queue-consumer-b")
      .dependsOn(MQ_BROKER)
      .withExposedPorts(9080)
      .withEnv(env)
      .withEnv("MQ_QUEUE", "DEV.QUEUE.MESSAGES.2")
      .withEnv("MQ_CLIENT_ID", "queue-consumer-2")
      .waitingFor(new WaitAllStrategy()
                      .withStrategy(Wait.forHttp("/health/live"))
                      .withStartupTimeout(Duration.ofMinutes(2)));

  protected static final GenericContainer QUEUE_CONSUMER_C = new GenericContainer("mq-showcase/queue-consumer:0")
      .withNetwork(NETWORK)
      .withNetworkAliases("queue-consumer-c")
      .dependsOn(MQ_BROKER)
      .withExposedPorts(9080)
      .withEnv(env)
      .withEnv("MQ_QUEUE", "DEV.QUEUE.MESSAGES.3")
      .withEnv("MQ_CLIENT_ID", "queue-consumer-3")
      .waitingFor(new WaitAllStrategy()
                      .withStrategy(Wait.forHttp("/health/live"))
                      .withStartupTimeout(Duration.ofMinutes(2)));

  protected static final GenericContainer DLQ_CONSUMER = new GenericContainer("mq-showcase/dlq-consumer:0")
      .withNetwork(NETWORK)
      .withNetworkAliases("dlq-consumer")
      .dependsOn(MQ_BROKER)
      .withExposedPorts(9080)
      .withEnv(env)
      .withEnv("MQ_QUEUE", "DEV.QUEUE.BO")
      .withEnv("MQ_CLIENT_ID", "dlq-consumer")
      .waitingFor(new WaitAllStrategy()
                      .withStrategy(Wait.forHttp("/health/live"))
                      .withStartupTimeout(Duration.ofMinutes(2)));

  static {
    MQ_BROKER.start();
    QUEUE_PRODUCER.start();
  }

  protected static RequestSpecification requestSpecification;

  @BeforeAll
  public static void setUpUri() {
    requestSpecification = new RequestSpecBuilder()
        .setPort(QUEUE_PRODUCER.getFirstMappedPort())
        .setBasePath("queue-producer")
        .build();
  }
}
