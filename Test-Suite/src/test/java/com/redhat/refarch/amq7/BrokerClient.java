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
package com.redhat.refarch.amq7;

import org.apache.activemq.artemis.api.core.management.ResourceNames;
import org.apache.activemq.artemis.api.jms.ActiveMQJMSClient;
import org.apache.activemq.artemis.api.jms.management.JMSManagementHelper;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.*;
import javax.naming.InitialContext;
import java.lang.IllegalStateException;
import java.util.stream.IntStream;

import static com.redhat.refarch.amq7.Constants.*;

/***
 * @author jary@redhat.com
 */
public class BrokerClient {

    private static final Logger logger = LoggerFactory.getLogger(BrokerClient.class);

    private Session session;
    private Connection connection;

    private MessageConsumer queueConsumer;
    private MessageConsumer topicConsumer;

    private MessageProducer queueProducer;
    private MessageProducer topicProducer;

    public BrokerClient(InitialContext initialContext, String brokerName, boolean autoAck, boolean consumeQueue, boolean consumeTopic,
                        boolean produceToQueue, boolean produceToTopic) throws Exception {

        ConnectionFactory connectionFactory = (ConnectionFactory) initialContext.lookup(brokerName + "/ConnectionFactory");
        connection = connectionFactory.createConnection(USERNAME.val(), PASSWORD.val());
        session = connection.createSession(false, autoAck ? Session.AUTO_ACKNOWLEDGE : Session.CLIENT_ACKNOWLEDGE);
        connection.start();

        Queue queue = (Queue) initialContext.lookup("queue/" + QUEUE_NAME.val());
        Topic topic = (Topic) initialContext.lookup("topic/" + TOPIC_NAME.val());

        if (consumeQueue)
            queueConsumer = session.createConsumer(queue);

        if (consumeTopic)
            topicConsumer = session.createConsumer(topic);

        if (produceToQueue)
            queueProducer = session.createProducer(queue);

        if (produceToTopic)
            topicProducer = session.createProducer(topic);
    }

    public void sendToQueue(Integer count) throws Exception {

        if (queueProducer == null)
            throw new IllegalStateException("cannot publish to queue with null publisher");

        IntStream.rangeClosed(1, count).forEach(i -> {
            try {
                queueProducer.send(session.createTextMessage("test"));

            } catch (Exception e) {
                logger.error("error sending messages to queue", e);
                Assert.fail();
            }
        });
    }

    public void sendToTopic(Integer count) throws Exception {

        if (topicProducer == null)
            throw new IllegalStateException("cannot publish to topic with null publisher");

        IntStream.rangeClosed(1, count).forEach(i -> {
            try {
                topicProducer.send(session.createTextMessage("test"));

            } catch (Exception e) {
                logger.error("error sending messages", e);
                Assert.fail();
            }
        });
    }

    public void receiveFromQueue(Integer count) throws Exception {

        if (queueConsumer == null)
            throw new IllegalStateException("cannot receive from queue with null consumer");

        IntStream.rangeClosed(1, count).forEach(i -> {
            try {
                Assert.assertNotNull("received message is null", queueConsumer.receive(Long.valueOf(TIMEOUT.val())));
            } catch (Exception e) {
                logger.error("error sending messages", e);
                Assert.fail();
            }
        });
    }

    public void ackFromQueue(Integer count) throws Exception {

        if (queueConsumer == null)
            throw new IllegalStateException("cannot receive from queue with null consumer");

        IntStream.rangeClosed(1, count).forEach(i -> {
            try {
                Message message = queueConsumer.receive(Long.valueOf(TIMEOUT.val()));
                Assert.assertNotNull("received message is null", message);
                message.acknowledge();

            } catch (Exception e) {
                logger.error("error sending messages", e);
                Assert.fail();
            }
        });
    }

    public void ackFromQueue() throws Exception {
        ackFromQueue(1);
    }

    public void receiveFromTopic(Integer count) throws Exception {

        if (topicConsumer == null)
            throw new IllegalStateException("cannot receive from topic with null consumer");

        IntStream.rangeClosed(1, count).forEach(i -> {
            try {
                Assert.assertNotNull("rcvd message is null", topicConsumer.receive(Long.valueOf(TIMEOUT.val())));
            } catch (Exception e) {
                logger.error("error sending messages", e);
                Assert.fail();
            }
        });
    }

    public void sendShutdown() throws Exception {

        QueueSession queueSession = ((QueueConnection) connection).createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
        QueueRequestor requestor = new QueueRequestor(queueSession, ActiveMQJMSClient.createQueue("activemq.management"));

        Message message = queueSession.createMessage();
        JMSManagementHelper.putOperationInvocation(message, ResourceNames.BROKER, "forceFailover");
        try {
            requestor.request(message);
        } catch (JMSException e) {
            if (!e.getLocalizedMessage().startsWith("AMQ119016")) {
                // we killed the broker - a connection failure is expected, throw anything else
                throw e;
            }
        }
    }

    public void terminateConnections() {
        try {

            if (queueConsumer != null)
                queueConsumer.close();

            if (topicConsumer != null)
                topicConsumer.close();

            if (queueProducer != null)
                queueProducer.close();

            if (topicProducer != null)
                topicProducer.close();

            if (session != null)
                session.close();

            if (connection != null)
                connection.close();

        } catch (Exception e) {
            logger.error("error occurred while shutting down client", e);
        }
    }

    public MessageConsumer queueConsumer() {
        return queueConsumer;
    }
}