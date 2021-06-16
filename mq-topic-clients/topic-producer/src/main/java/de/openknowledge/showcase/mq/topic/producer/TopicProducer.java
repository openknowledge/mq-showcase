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
package de.openknowledge.showcase.mq.topic.producer;

import io.opentracing.Tracer;
import io.opentracing.contrib.jms2.TracingMessageProducer;
import io.smallrye.asyncapi.spec.annotations.channel.ChannelItem;
import io.smallrye.asyncapi.spec.annotations.message.Message;
import io.smallrye.asyncapi.spec.annotations.operation.Operation;
import io.smallrye.asyncapi.spec.annotations.parameter.Parameter;
import io.smallrye.asyncapi.spec.annotations.parameter.Parameters;
import io.smallrye.asyncapi.spec.annotations.schema.Schema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.Topic;
import javax.jms.TopicConnectionFactory;
import javax.json.bind.JsonbBuilder;

/**
 * JMS producer that sends messages to a topic. The topic is configured in the server.xml.
 */
@ApplicationScoped
public class TopicProducer {

  private static final Logger LOG = LoggerFactory.getLogger(TopicProducer.class);

  @Resource(lookup = "JMSTopicFactory")
  private TopicConnectionFactory jmsFactory;

  @Resource(lookup = "JMSTopic")
  private Topic topic;

  @Inject
  Tracer tracer;

  @ChannelItem(
      channel = "DEV.TOPIC.MESSAGES",
      publish = @Operation(
          description = "Publish messages to the MQ Broker",
          operationId = "sendMessage",
          message = @Message(
            name = "Custom Message",
            payload = @Schema(ref = "#/components/schemas/CustomMessage")
          )
      )
  )
  public void send(final CustomMessage message) {
    try (Connection connection = jmsFactory.createConnection();
        Session session = connection.createSession();
        MessageProducer messageProducer = session.createProducer(topic);
        TracingMessageProducer producer = new TracingMessageProducer(messageProducer, tracer)) {

      String json = JsonbBuilder.create().toJson(message);

      TextMessage textMessage = session.createTextMessage();
      textMessage.setText(json);

      LOG.info("Send message {} to {}", textMessage.getText(), topic.getTopicName());

      producer.send(textMessage);
    } catch (JMSException e) {
      LOG.error(e.getMessage(), e);
    }
  }
}
