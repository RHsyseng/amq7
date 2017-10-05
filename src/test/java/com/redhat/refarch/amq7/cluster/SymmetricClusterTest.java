package com.redhat.refarch.amq7.cluster;

import com.google.common.collect.ImmutableMap;
import com.redhat.refarch.amq7.BrokerDelegate;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import static com.redhat.refarch.amq7.Constants.*;

public class SymmetricClusterTest extends ClusterBaseTest {

    private final static Logger logger = LoggerFactory.getLogger(SymmetricClusterTest.class);

    private static Map<String, BrokerDelegate> brokers;

    @Test
    public void testSymmetricCluster() throws Exception {

        logger.debug("instantiate brokers, all 3 sub to topic, symmetric-1 subs to queue, symmetric-2 writes to queue and topic...");
        brokers = ImmutableMap.of(
                SYMMETRIC_1.val(), new BrokerDelegate(initialContext, SYMMETRIC_1.val(),
                        true, true, true, false, false),
                SYMMETRIC_2.val(), new BrokerDelegate(initialContext, SYMMETRIC_2.val(),
                        true, false, true, true, true),
                SYMMETRIC_3.val(), new BrokerDelegate(initialContext, SYMMETRIC_3.val(),
                        true, false, true, false, false)
        );

        Integer numMessages = 25;
        logger.debug("sending " + numMessages + " messages via symmetric-2 producer to queue & topic...");

        brokers.get(SYMMETRIC_2.val()).sendToQueue(numMessages);
        brokers.get(SYMMETRIC_2.val()).sendToTopic(numMessages);

        logger.debug("verifying all 3 topic subscribers & the single queue consumer received all messages...");
        brokers.get(SYMMETRIC_1.val()).receiveFromTopic(numMessages);
        brokers.get(SYMMETRIC_2.val()).receiveFromTopic(numMessages);
        brokers.get(SYMMETRIC_3.val()).receiveFromTopic(numMessages);
        brokers.get(SYMMETRIC_1.val()).receiveFromQueue(numMessages);
    }
}