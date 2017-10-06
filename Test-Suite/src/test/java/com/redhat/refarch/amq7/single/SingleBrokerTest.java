package com.redhat.refarch.amq7.single;

import com.google.common.collect.ImmutableMap;
import com.redhat.refarch.amq7.BrokerClient;
import com.redhat.refarch.amq7.cluster.ClusterBaseTest;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import static com.redhat.refarch.amq7.Constants.*;

public class SingleBrokerTest extends ClusterBaseTest {

    private final static Logger logger = LoggerFactory.getLogger(SingleBrokerTest.class);

    private Map<String, BrokerClient> clients;

    @Test
    public void testPointToPoint() throws Exception {

        try {
            logger.debug("instantiating clients...");
            clients = ImmutableMap.of(
                    SINGLE_A.val(), new BrokerClient(initialContext, SINGLE_A.val(),
                            true, false, false, true, true),
                    SINGLE_B.val(), new BrokerClient(initialContext, SINGLE_A.val(),
                            true, true, false, false, false)
            );

            Integer numMessages = 25;
            logger.debug("sending " + numMessages + " messages to queue...");
            clients.get(SINGLE_A.val()).sendToQueue(numMessages);

            logger.debug("verifying single queue consumer received all messages...");
            clients.get(SINGLE_B.val()).receiveFromQueue(25);

        } finally {
            logger.debug("terminating clients...");
            clients.values().forEach(SingleBrokerTest::terminateClient);
        }
    }

    @Test
    public void testPublishSubscribe() throws Exception {

        try {
            logger.debug("instantiating clients...");
            clients = ImmutableMap.of(
                    SINGLE_A.val(), new BrokerClient(initialContext, SINGLE_A.val(),
                            true, false, false, true, true),
                    SINGLE_B.val(), new BrokerClient(initialContext, SINGLE_A.val(),
                            true, true, true, false, false),
                    SINGLE_C.val(), new BrokerClient(initialContext, SINGLE_A.val(),
                            true, false, true, false, false)
            );

            Integer numMessages = 25;
            logger.debug("sending " + numMessages + " messages to topic...");
            clients.get(SINGLE_A.val()).sendToTopic(numMessages);

            logger.debug("verifying both topic consumers received all messages...");
            clients.get(SINGLE_B.val()).receiveFromTopic(25);
            clients.get(SINGLE_C.val()).receiveFromTopic(25);

        } finally {
            logger.debug("terminating clients...");
            clients.values().forEach(SingleBrokerTest::terminateClient);
        }
    }
}