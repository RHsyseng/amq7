package com.redhat.refarch.amq7.single;

import com.google.common.collect.ImmutableMap;
import com.redhat.refarch.amq7.BrokerDelegate;
import com.redhat.refarch.amq7.cluster.ClusterBaseTest;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import static com.redhat.refarch.amq7.Constants.SINGLE;

public class SingleBrokerTest extends ClusterBaseTest {

    private final static Logger logger = LoggerFactory.getLogger(SingleBrokerTest.class);

    private Map<String, BrokerDelegate> brokers;

    @Test
    public void testSingleBroker() throws Exception {

        logger.debug("instantiating broker...");
        brokers = ImmutableMap.of(
                SINGLE.val(), new BrokerDelegate(initialContext, SINGLE.val(),
                        true, true, true, true, true)
        );

        Integer numMessages = 25;
        logger.debug("sending " + numMessages + " messages via producer to queue & topic...");
        brokers.get(SINGLE.val()).sendToTopic(numMessages);
        brokers.get(SINGLE.val()).sendToQueue(numMessages);

        logger.debug("verifying all 3 topic subscribers & single queue consumer received all messages...");
        brokers.get(SINGLE.val()).receiveFromQueue(25);
        brokers.get(SINGLE.val()).receiveFromTopic(25);
    }
}