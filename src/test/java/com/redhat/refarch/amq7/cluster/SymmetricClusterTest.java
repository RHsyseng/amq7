package com.redhat.refarch.amq7.cluster;

import com.google.common.collect.ImmutableMap;
import com.redhat.refarch.amq7.BrokerClient;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Map;

import static com.redhat.refarch.amq7.Constants.*;

public class SymmetricClusterTest extends ClusterBaseTest {

    private final static Logger logger = LoggerFactory.getLogger(SymmetricClusterTest.class);

    private static Map<String, BrokerClient> clients;

    @Test
    public void testSymmetricCluster() throws Exception {

        try {
            logger.debug("instantiate clients, all 3 sub to topic, symmetric-1 subs to queue, symmetric-2 writes to queue and topic...");
            clients = ImmutableMap.of(
                    SYMMETRIC_1.val(), new BrokerClient(initialContext, SYMMETRIC_1.val(),
                            true, true, true, false, false),
                    SYMMETRIC_2.val(), new BrokerClient(initialContext, SYMMETRIC_2.val(),
                            true, false, true, true, true),
                    SYMMETRIC_3.val(), new BrokerClient(initialContext, SYMMETRIC_3.val(),
                            true, false, true, false, false)
            );

            Integer numMessages = 25;
            logger.debug("sending " + numMessages + " messages via symmetric-2 producer to queue & topic...");

            clients.get(SYMMETRIC_2.val()).sendToQueue(numMessages);
            clients.get(SYMMETRIC_2.val()).sendToTopic(numMessages);

            logger.debug("verifying all 3 topic subscribers & the single queue consumer received all messages...");
            clients.get(SYMMETRIC_1.val()).receiveFromTopic(numMessages);
            clients.get(SYMMETRIC_2.val()).receiveFromTopic(numMessages);
            clients.get(SYMMETRIC_3.val()).receiveFromTopic(numMessages);
            clients.get(SYMMETRIC_1.val()).receiveFromQueue(numMessages);

        } finally {
            logger.debug("terminating clients...");
            clients.values().forEach(SymmetricClusterTest::terminateClient);
        }
    }

    @Test
    public void testSymmetricDistribution() throws Exception {

        try {
            logger.debug("instantiate clients, symmetric-2 & symmetric-3 sub to queue, symmetric-1 writes to queue...");
            clients = ImmutableMap.of(
                    SYMMETRIC_1.val(), new BrokerClient(initialContext, SYMMETRIC_1.val(),
                            true, false, false, true, false),
                    SYMMETRIC_2.val(), new BrokerClient(initialContext, SYMMETRIC_2.val(),
                            true, true, false, false, false),
                    SYMMETRIC_3.val(), new BrokerClient(initialContext, SYMMETRIC_3.val(),
                            true, true, false, false, false)
            );

            Integer numMessages = 20;
            logger.debug("sending " + numMessages + " messages via symmetric-1 producer to queue...");

            clients.get(SYMMETRIC_1.val()).sendToQueue(numMessages);

            // allow messages a second to propagate
            Thread.sleep(1000);

            logger.debug("verifying both queue subscribers received half of the messages...");

            clients.get(SYMMETRIC_2.val()).receiveFromQueue(numMessages / 2);
            clients.get(SYMMETRIC_3.val()).receiveFromQueue(numMessages / 2);

            logger.debug("verifying there are no more than half of the messages on one of the receivers...");
            Arrays.asList(SYMMETRIC_2.val(), SYMMETRIC_3.val()).forEach(brokerName -> {
                try {
                    Assert.assertNull("11th message on " + brokerName + " not null",
                            clients.get(brokerName).queueConsumer().receive(Long.valueOf(TIMEOUT.val())));

                } catch (Exception e) {
                    logger.error("error fetching 11th (null) message from " + brokerName, e);
                    Assert.fail();
                }
            });

        } finally {
            logger.debug("terminating clients...");
            clients.values().forEach(SymmetricClusterTest::terminateClient);
        }
    }
}