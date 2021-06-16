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
package de.openknowledge.showcase.mq.queue.consumer;

import io.smallrye.asyncapi.spec.annotations.channel.ChannelItem;
import io.smallrye.asyncapi.spec.annotations.message.Message;
import io.smallrye.asyncapi.spec.annotations.operation.Operation;
import io.smallrye.asyncapi.spec.annotations.schema.Schema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.MessageDriven;
import javax.interceptor.Interceptors;
import javax.jms.JMSException;
import javax.jms.MessageListener;
import javax.jms.TextMessage;
import javax.json.bind.JsonbBuilder;

import io.smallrye.asyncapi.spec.annotations.AsyncAPI;
import io.smallrye.asyncapi.spec.annotations.info.Info;
import io.smallrye.asyncapi.spec.annotations.info.License;
import io.smallrye.asyncapi.spec.annotations.server.Server;
import io.smallrye.asyncapi.spec.annotations.server.ServerVariable;

/**
 * JMS consumer that receives messages from a queue. The queue is configured in the server.xml.
 */
@AsyncAPI(
    asyncapi = "2.0.0",
    defaultContentType = "plain/text",
    info = @Info(
        title = "JMS Queue Consumer API",
        version = "0",
        description = "A Java EE App which receives messages via JMS from a MQ Broker",
        license = @License(
            name = "Apache 2.0",
            url = "https://www.apache.org/licenses/LICENSE-2.0"
        )
    ),
    id = "urn:de:openknowledge:queue:consumer:server",
    servers = {
        @Server(
            url = "localhost:{port}",
            protocol = "jms",
            description = "Test broker",
            name = "dev",
            variables = {
                @ServerVariable(
                    name = "port",
                    description = "Connection is available through port 1414.",
                    defaultValue = "1414"
                )
            }
        )
    }
)
@Interceptors(TracingInterceptor.class)
@MessageDriven
public class QueueConsumer implements MessageListener {

  private static final Logger LOG = LoggerFactory.getLogger(QueueConsumer.class);

  @ChannelItem(
      channel = "DEV.QUEUE.MESSAGES.1",
      subscribe = @Operation(
          description = "Receive a message from the MQ Broker",
          operationId = "onMessage",
          message = @Message(
              name = "Custom Message",
              payload = @Schema(ref = "#/components/schemas/CustomMessage")
          )
      )
  )
  @Override
  public void onMessage(final javax.jms.Message message) {
    try {
      String textMessage = ((TextMessage)message).getText();

      LOG.info("Received message {}", textMessage);

      CustomMessage customMessage = JsonbBuilder.create().fromJson(textMessage, CustomMessage.class);

      throwIllegalArgumentExceptionWhenIdEqualsFail(customMessage);
    } catch (JMSException e) {
      LOG.error(e.getMessage(), e);
      throw new IllegalStateException(e);
    }
  }

  private void throwIllegalArgumentExceptionWhenIdEqualsFail(final CustomMessage message) {
    if (message.getText().equals("fail")) {
      LOG.error("Message processing failed");
      throw new IllegalArgumentException();
    }
  }
}
