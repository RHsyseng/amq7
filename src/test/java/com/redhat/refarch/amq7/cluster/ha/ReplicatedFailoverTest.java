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
package com.redhat.refarch.amq7.cluster.ha;

import com.diffplug.common.base.Errors;
import com.google.common.collect.ImmutableMap;
import com.redhat.refarch.amq7.BrokerClient;
import com.redhat.refarch.amq7.cluster.ClusterBaseTest;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.Message;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static com.redhat.refarch.amq7.Constants.*;

public class ReplicatedFailoverTest extends ClusterBaseTest {

    private final static Logger logger = LoggerFactory.getLogger(ReplicatedFailoverTest.class);

    private static Map<String, BrokerClient> clients;

    @After
    public void after() {
        logger.warn("if using OpenShift, redeployment of replicated template required before re-running this test");
    }

    @Test
    public void testReplicatedFailback() throws Exception {

        try {
        logger.debug("creating clients...");
        clients = ImmutableMap.of(
                REPLICATED_M1.val(), new BrokerClient(initialContext, REPLICATED_M1.val(),
                        false, true, false, true, false),
                REPLICATED_M2.val(), new BrokerClient(initialContext, REPLICATED_M2.val(),
                        false, true, false, true, false),
                REPLICATED_M3.val(), new BrokerClient(initialContext, REPLICATED_M3.val(),
                        false, true, false, true, false)
        );

        Integer numMessages = 20;

        logger.debug("send " + numMessages + " messages via producer to all 3 master brokers...");
        clients.get(REPLICATED_M1.val()).sendToQueue(numMessages);
        clients.get(REPLICATED_M2.val()).sendToQueue(numMessages);
        clients.get(REPLICATED_M3.val()).sendToQueue(numMessages);

        Map<String, List<Message>> nonAckMessages = new HashMap<>();
        clients.keySet().forEach(brokerName -> nonAckMessages.put(brokerName, new ArrayList<>()));

        logger.debug("receiving all, but only acknowledging half of messages on all 3 master brokers...");
        for (int i = 1; i <= numMessages; i++) {

            if (i <= (numMessages / 2)) {
                clients.keySet().forEach(Errors.rethrow().wrap(ReplicatedFailoverTest::acknowledgeMessage));

            } else {
                clients.keySet().forEach(Errors.rethrow().wrap(brokerName -> {
                    nonAckMessages.get(brokerName).add(clients.get(brokerName).queueConsumer().receive(Long.valueOf(TIMEOUT.val())));
                }));
            }
        }

        logger.debug("shutting down master broker m2 via Mgmt API forceFailover()...");
        clients.get(REPLICATED_M2.val()).sendShutdown();

        logger.debug("verifying rec'd-only messages fail to ack if on a failover broker...");
        nonAckMessages.forEach((brokerName, messages) -> {
            try {
                logger.debug("attempting to ack " + messages.size() + " messages from broker " + brokerName + " post-shutdown...");
                messages.forEach(Errors.rethrow().wrap(Message::acknowledge));

                // fail if message that should have failed over to slave is able to ack
                if (brokerName.equals(REPLICATED_M2.val())) {
                    logger.error("failover message should not have been able to ack");
                    Assert.fail();
                }

            } catch (Exception e) {
                if (!brokerName.equals(REPLICATED_M2.val())) {
                    logger.error("non-failover message should have been able to ack");
                    Assert.fail();
                } else {
                    logger.debug("messages from m2 post-failover correctly failed to ack");
                }
            }
        });

        logger.debug("re-consuming the non-ack'd m2 messages with slave broker now promoted...");
        IntStream.rangeClosed(1, numMessages / 2).forEach(i -> {
            try {
                acknowledgeMessage(REPLICATED_M2.val());
            } catch (Exception e) {
                logger.error("error re-consuming messages", e);
                Assert.fail();
            }
        });

        } finally {
            logger.debug("terminating clients...");
            clients.values().forEach(ReplicatedFailoverTest::terminateClient);
        }
    }

    private static void acknowledgeMessage(String brokerName) throws Exception {
        clients.get(brokerName).ackFromQueue();
    }
}