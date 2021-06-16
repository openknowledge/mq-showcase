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

import io.smallrye.asyncapi.spec.annotations.schema.Schema;

/**
 * A DTO that represents a custom message.
 */
@Schema
public class CustomMessage {

  @Schema(required = true)
  private String text;

  @Schema(required = true)
  private String sender;

  public CustomMessage() {
  }

  public CustomMessage(final String text) {
    this(text, "Topic Producer");
  }

  private CustomMessage(final String text, final String sender) {
    this.text = text;
    this.sender = sender;
  }

  public String getText() {
    return text;
  }

  public void setText(final String text) {
    this.text = text;
  }

  public String getSender() {
    return sender;
  }

  public void setSender(final String sender) {
    this.sender = sender;
  }

  @Override
  public String toString() {
    return "CustomMessage{" + "text='" + text + '\'' + ", sender='" + sender + '\'' + '}';
  }
}
