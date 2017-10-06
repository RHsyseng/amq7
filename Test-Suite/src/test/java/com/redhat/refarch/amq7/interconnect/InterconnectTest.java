package com.redhat.refarch.amq7.interconnect;

import com.google.common.collect.ImmutableMap;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.InitialContext;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Map;
import java.util.Properties;

import static com.redhat.refarch.amq7.Constants.*;

public class InterconnectTest {

    private final static Logger logger = LoggerFactory.getLogger(InterconnectTest.class);

    static private Map<String, InterconnectClient> clients;

    private InitialContext initialContext;

    @Before
    public void before() {
        try {
            InputStream is = InterconnectTest.class.getClassLoader().getSystemResourceAsStream("interconnect.jndi.properties");
            Properties properties = new Properties();
            properties.load(is);

            initialContext = new InitialContext(properties);
        } catch (Exception e) {
            logger.error("error instantiating InitialContext before test", e);
            Assert.fail();
        }
    }

    @Test
    public void testSendDirect() throws Exception {

        try {
            logger.debug("routing path: CA -> US -> EU -> DE");
            logger.debug("creating CA and DE router connections...");
            clients = ImmutableMap.of(
                    INTERCONNECT_CA.val(), new InterconnectClient(initialContext, INTERCONNECT_CA.val(), QUEUE_NAME.val(), false, true),
                    INTERCONNECT_DE.val(), new InterconnectClient(initialContext, INTERCONNECT_DE.val(), QUEUE_NAME.val(), true, false)
            );

            // allow a bit of time for topology/routes to propagate
            Thread.sleep(1000);

            logger.debug("sending 10 messages via CA...");
            clients.get(INTERCONNECT_CA.val()).sendToQueue(10);

            logger.debug("receiving 10 messages from DE router...");
            clients.get(INTERCONNECT_DE.val()).receiveFromQueue(10);

        } finally {
            logger.debug("terminating clients...");
            clients.values().forEach(InterconnectTest::terminateClient);
        }
    }

    @Test
    public void testSendMulticast() throws Exception {

        try {
            logger.debug("routing path: AU -> EU -> UK,DE,US -> CA,ME");
            logger.debug("creating router connections...");
            clients = ImmutableMap.of(
                    INTERCONNECT_AU.val(), new InterconnectClient(initialContext, INTERCONNECT_AU.val(), MULTICAST_QUEUE.val(), false, true),
                    INTERCONNECT_CA.val(), new InterconnectClient(initialContext, INTERCONNECT_CA.val(), MULTICAST_QUEUE.val(), true, false),
                    INTERCONNECT_ME.val(), new InterconnectClient(initialContext, INTERCONNECT_ME.val(), MULTICAST_QUEUE.val(), true, false),
                    INTERCONNECT_UK.val(), new InterconnectClient(initialContext, INTERCONNECT_UK.val(), MULTICAST_QUEUE.val(), true, false),
                    INTERCONNECT_DE.val(), new InterconnectClient(initialContext, INTERCONNECT_DE.val(), MULTICAST_QUEUE.val(), true, false)
            );

            // allow a bit of time for topology/routes to propagate
            Thread.sleep(1000);

            logger.debug("sending 10 messages via CA...");
            clients.get(INTERCONNECT_AU.val()).sendToQueue(10);

            logger.debug("receiving 10 messages from all 4 multicast recipient clients...");
            clients.get(INTERCONNECT_CA.val()).receiveFromQueue(10);
            clients.get(INTERCONNECT_ME.val()).receiveFromQueue(10);
            clients.get(INTERCONNECT_UK.val()).receiveFromQueue(10);
            clients.get(INTERCONNECT_DE.val()).receiveFromQueue(10);

        } finally {
            logger.debug("terminating clients...");
            clients.values().forEach(InterconnectTest::terminateClient);
        }
    }

    @Test
    public void testSendBalancedSameHops() throws Exception {

        try {
            logger.debug("creating router connections...");
            clients = ImmutableMap.of(
                    INTERCONNECT_AU.val(), new InterconnectClient(initialContext, INTERCONNECT_AU.val(), BALANCED_QUEUE.val(), false, true),
                    INTERCONNECT_CA.val(), new InterconnectClient(initialContext, INTERCONNECT_CA.val(), BALANCED_QUEUE.val(), true, false),
                    INTERCONNECT_ME.val(), new InterconnectClient(initialContext, INTERCONNECT_ME.val(), BALANCED_QUEUE.val(), true, false)
            );

            // allow a bit of time for topology/routes to propagate
            Thread.sleep(1000);

            logger.debug("sending 20 messages via AU...");
            clients.get(INTERCONNECT_AU.val()).sendToQueue(20);

            logger.debug("receiving equal number of balanced messages from CA and ME clients...");
            clients.get(INTERCONNECT_CA.val()).receiveFromQueue(10);
            clients.get(INTERCONNECT_ME.val()).receiveFromQueue(10);

            logger.debug("verifying there are no more than half of the messages on one of the receivers...");
            Arrays.asList(INTERCONNECT_CA.val(), INTERCONNECT_ME.val()).forEach(brokerName -> {
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
            clients.values().forEach(InterconnectTest::terminateClient);
        }
    }

    @Test
    public void testSendBalancedDifferentHops() throws Exception {

        try {
            logger.debug("creating router connections...");
            clients = ImmutableMap.of(
                    INTERCONNECT_AU.val(), new InterconnectClient(initialContext, INTERCONNECT_AU.val(), BALANCED_QUEUE.val(), false, true),
                    INTERCONNECT_CA.val(), new InterconnectClient(initialContext, INTERCONNECT_CA.val(), BALANCED_QUEUE.val(), true, false),
                    INTERCONNECT_ME.val(), new InterconnectClient(initialContext, INTERCONNECT_ME.val(), BALANCED_QUEUE.val(), true, false),
                    INTERCONNECT_UK.val(), new InterconnectClient(initialContext, INTERCONNECT_UK.val(), BALANCED_QUEUE.val(), true, false),
                    INTERCONNECT_DE.val(), new InterconnectClient(initialContext, INTERCONNECT_DE.val(), BALANCED_QUEUE.val(), true, false)
            );

            // allow a bit of time for topology/routes to propagate
            Thread.sleep(1000);

            logger.debug("sending 20 messages via AU...");
            clients.get(INTERCONNECT_AU.val()).sendToQueue(20);

            logger.debug("due to routing weight, receiving less balanced messages from CA/ME than from UK/DE clients...");
            int countCA = clients.get(INTERCONNECT_CA.val()).receiveAllFromQueue();
            int countME = clients.get(INTERCONNECT_ME.val()).receiveAllFromQueue();
            int countUK = clients.get(INTERCONNECT_UK.val()).receiveAllFromQueue();
            int countDE = clients.get(INTERCONNECT_DE.val()).receiveAllFromQueue();

            logger.debug("asserting yield after weighted routing is 3 messages to CA/ME each, and 7 messages to UK/DE each...");
            Assert.assertEquals(3, countCA);
            Assert.assertEquals(3, countME);
            Assert.assertEquals(7, countUK);
            Assert.assertEquals(7, countDE);

        } finally {
            logger.debug("terminating clients...");
            clients.values().forEach(InterconnectTest::terminateClient);
        }
    }

    @Test
    public void testSendClosest() throws Exception {

        try {
            logger.debug("creating router connections...");
            clients = ImmutableMap.of(
                    INTERCONNECT_CA.val(), new InterconnectClient(initialContext, INTERCONNECT_CA.val(), CLOSEST_QUEUE.val(), false, true),
                    INTERCONNECT_DE.val(), new InterconnectClient(initialContext, INTERCONNECT_DE.val(), CLOSEST_QUEUE.val(), false, true),
                    INTERCONNECT_ME.val(), new InterconnectClient(initialContext, INTERCONNECT_ME.val(), CLOSEST_QUEUE.val(), true, false),
                    INTERCONNECT_UK.val(), new InterconnectClient(initialContext, INTERCONNECT_UK.val(), CLOSEST_QUEUE.val(), true, false)
            );

            // allow a bit of time for topology/routes to propagate
            Thread.sleep(1000);

            logger.debug("sending closest message from CA and asserting ME receives...");
            clients.get(INTERCONNECT_CA.val()).sendToQueue();
            clients.get(INTERCONNECT_ME.val()).receiveFromQueue();

            logger.debug("sending closest message from DE and asserting UK receives...");
            clients.get(INTERCONNECT_DE.val()).sendToQueue();
            clients.get(INTERCONNECT_UK.val()).receiveFromQueue();

        } finally {
            logger.debug("terminating clients...");
            clients.values().forEach(InterconnectTest::terminateClient);
        }
    }

    private static void terminateClient(InterconnectClient client) {
        if (client != null)
            client.terminateConnections();
    }
}