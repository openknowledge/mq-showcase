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

import io.smallrye.asyncapi.spec.annotations.AsyncAPI;
import io.smallrye.asyncapi.spec.annotations.info.Info;
import io.smallrye.asyncapi.spec.annotations.info.License;
import io.smallrye.asyncapi.spec.annotations.server.Server;
import io.smallrye.asyncapi.spec.annotations.server.ServerVariable;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

/**
 * JAX-RS Activator
 */
@AsyncAPI(
    asyncapi = "2.0.0",
    defaultContentType = "plain/text",
    info = @Info(
        title = "JMS Topic Producer API",
        version = "0",
        description = "A REST API to send messages via JMS to a MQ Broker",
        license = @License(
            name = "Apache 2.0",
            url = "https://www.apache.org/licenses/LICENSE-2.0"
        )
    ),
    id = "urn:de:openknowledge:topic:producer:server",
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
@ApplicationPath("api")
public class JaxRsActivator extends Application {

}
