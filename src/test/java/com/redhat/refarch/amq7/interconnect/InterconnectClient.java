/*
 * Copyright 2005-2017 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version
 * 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */
package com.redhat.refarch.amq7.interconnect;

import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.*;
import javax.naming.InitialContext;
import java.lang.IllegalStateException;
import java.util.stream.IntStream;

import static com.redhat.refarch.amq7.Constants.TIMEOUT;

/***
 * @author jary@redhat.com
 */
class InterconnectClient {

    private static final Logger logger = LoggerFactory.getLogger(InterconnectClient.class);

    private Session session;
    private Connection connection;

    private MessageConsumer queueConsumer;
    private MessageProducer queueProducer;

    InterconnectClient(InitialContext initialContext, String routerName, String queueName, Boolean consumeQueue, Boolean produceToQueue) throws Exception {

        ConnectionFactory connectionFactory = (ConnectionFactory) initialContext.lookup(routerName + "/ConnectionFactory");
        connection = connectionFactory.createConnection();

        connection.setExceptionListener((JMSException e) -> {
            logger.error("[CLIENT] connectionExceptionListener triggered, exiting connection", e);
            System.exit(1);
        });

        connection.start();
        session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

        Queue queue = (Queue) initialContext.lookup("queue/" + queueName);

        if (consumeQueue)
            queueConsumer = session.createConsumer(queue);

        if (produceToQueue)
            queueProducer = session.createProducer(queue);
    }

    void sendToQueue(Integer count) throws Exception {

        if (queueProducer == null)
            throw new IllegalStateException("cannot publish to queue with null publisher");

        IntStream.rangeClosed(1, count).forEach(i -> {
            try {
                queueProducer.send(session.createTextMessage("test"), DeliveryMode.NON_PERSISTENT, Message.DEFAULT_PRIORITY, Message.DEFAULT_TIME_TO_LIVE);

            } catch (Exception e) {
                logger.error("error sending messages", e);
                Assert.fail();
            }
        });
    }

    void sendToQueue() throws Exception {
        sendToQueue(1);
    }

    void receiveFromQueue(Integer count) throws Exception {

        if (queueConsumer == null)
            throw new IllegalStateException("cannot receive from queue with null consumer");

        IntStream.rangeClosed(1, count).forEach(i -> {
            try {
                Assert.assertNotNull("rec'd message is null", queueConsumer.receive(Long.valueOf(TIMEOUT.val())));
            } catch (Exception e) {
                logger.error("error sending messages", e);
                Assert.fail();
            }
        });
    }

    Integer receiveAllFromQueue() throws Exception {

        if (queueConsumer == null)
            throw new IllegalStateException("cannot receive from queue with null consumer");

        Message message = null;
        Integer count = 0;
        do {
            message = queueConsumer.receive(Long.valueOf(TIMEOUT.val()));
            count++;

        } while (message != null);
        return count - 1;
    }

    void receiveFromQueue() throws Exception {
        receiveFromQueue(1);
    }

    void terminateConnections() {
        try {

            if (queueConsumer != null)
                queueConsumer.close();

            if (queueProducer != null)
                queueProducer.close();

            if (session != null)
                session.close();

            if (connection != null)
                connection.close();

        } catch (Exception e) {
            logger.error("error occurred while shutting down client", e);
        }
    }

    MessageConsumer queueConsumer() {
        return queueConsumer;
    }
}