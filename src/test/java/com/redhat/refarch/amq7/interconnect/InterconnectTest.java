package com.redhat.refarch.amq7.interconnect;

import com.google.common.collect.ImmutableMap;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.InitialContext;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;

import static com.redhat.refarch.amq7.Constants.*;

public class InterconnectTest {

    private final static Logger logger = LoggerFactory.getLogger(InterconnectTest.class);

    static private Map<String, RouterDelegate> routers;

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

        logger.debug("routing path: CA -> US -> EU -> DE");
        logger.debug("creating CA and DE router connections...");
        routers = ImmutableMap.of(
                INTERCONNECT_CA.val(), new RouterDelegate(initialContext, INTERCONNECT_CA.val(), QUEUE_NAME.val(), false, true),
                INTERCONNECT_DE.val(), new RouterDelegate(initialContext, INTERCONNECT_DE.val(), QUEUE_NAME.val(), true, false)
        );

        // allow a bit of time for topology/routes to propagate
        Thread.sleep(2000);

        logger.debug("sending 10 messages via CA...");
        routers.get(INTERCONNECT_CA.val()).sendToQueue(10);

        logger.debug("receiving 10 messages from DE router...");
        routers.get(INTERCONNECT_DE.val()).receiveFromQueue(10);
    }

    @Test
    public void testSendMulticast() throws Exception {

        logger.debug("routing path: AU -> EU -> UK,DE,US -> CA,ME");
        logger.debug("creating router connections...");
        routers = ImmutableMap.of(
                INTERCONNECT_AU.val(), new RouterDelegate(initialContext, INTERCONNECT_AU.val(), MULTICAST_QUEUE.val(), false, true),
                INTERCONNECT_CA.val(), new RouterDelegate(initialContext, INTERCONNECT_CA.val(), MULTICAST_QUEUE.val(), true, false),
                INTERCONNECT_ME.val(), new RouterDelegate(initialContext, INTERCONNECT_ME.val(), MULTICAST_QUEUE.val(), true, false),
                INTERCONNECT_UK.val(), new RouterDelegate(initialContext, INTERCONNECT_UK.val(), MULTICAST_QUEUE.val(), true, false),
                INTERCONNECT_DE.val(), new RouterDelegate(initialContext, INTERCONNECT_DE.val(), MULTICAST_QUEUE.val(), true, false)
        );

        // allow a bit of time for topology/routes to propagate
        Thread.sleep(2000);

        logger.debug("sending 10 messages via CA...");
        routers.get(INTERCONNECT_AU.val()).sendToQueue(10);

        logger.debug("receiving 10 messages from all 4 multicast recipient routers...");
        routers.get(INTERCONNECT_CA.val()).receiveFromQueue(10);
        routers.get(INTERCONNECT_ME.val()).receiveFromQueue(10);
        routers.get(INTERCONNECT_UK.val()).receiveFromQueue(10);
        routers.get(INTERCONNECT_DE.val()).receiveFromQueue(10);
    }

    @Test
    public void testSendBalancedSameHops() throws Exception {

        logger.debug("creating router connections...");
        routers = ImmutableMap.of(
                INTERCONNECT_AU.val(), new RouterDelegate(initialContext, INTERCONNECT_AU.val(), BALANCED_QUEUE.val(), false, true),
                INTERCONNECT_CA.val(), new RouterDelegate(initialContext, INTERCONNECT_CA.val(), BALANCED_QUEUE.val(), true, false),
                INTERCONNECT_ME.val(), new RouterDelegate(initialContext, INTERCONNECT_ME.val(), BALANCED_QUEUE.val(), true, false)
        );

        // allow a bit of time for topology/routes to propagate
        Thread.sleep(2000);

        logger.debug("sending 20 messages via AU...");
        routers.get(INTERCONNECT_AU.val()).sendToQueue(20);

        logger.debug("receiving equal number of balanced messages from CA and ME routers...");
        routers.get(INTERCONNECT_CA.val()).receiveFromQueue(10);
        routers.get(INTERCONNECT_ME.val()).receiveFromQueue(10);
    }

    @Test
    public void testSendBalancedDifferentHops() throws Exception {

        logger.debug("creating router connections...");
        routers = ImmutableMap.of(
                INTERCONNECT_AU.val(), new RouterDelegate(initialContext, INTERCONNECT_AU.val(), BALANCED_QUEUE.val(), false, true),
                INTERCONNECT_CA.val(), new RouterDelegate(initialContext, INTERCONNECT_CA.val(), BALANCED_QUEUE.val(), true, false),
                INTERCONNECT_ME.val(), new RouterDelegate(initialContext, INTERCONNECT_ME.val(), BALANCED_QUEUE.val(), true, false),
                INTERCONNECT_UK.val(), new RouterDelegate(initialContext, INTERCONNECT_UK.val(), BALANCED_QUEUE.val(), true, false),
                INTERCONNECT_DE.val(), new RouterDelegate(initialContext, INTERCONNECT_DE.val(), BALANCED_QUEUE.val(), true, false)
        );

        // allow a bit of time for topology/routes to propagate
        Thread.sleep(2000);

        logger.debug("sending 20 messages via AU...");
        routers.get(INTERCONNECT_AU.val()).sendToQueue(20);

        logger.debug("due to routing weight, receiving less balanced messages from CA/ME than from UK/DE routers...");
        Integer countCA = routers.get(INTERCONNECT_CA.val()).receiveAllFromQueue();
        Integer countME = routers.get(INTERCONNECT_ME.val()).receiveAllFromQueue();
        Integer countUK = routers.get(INTERCONNECT_UK.val()).receiveAllFromQueue();
        Integer countDE = routers.get(INTERCONNECT_DE.val()).receiveAllFromQueue();

        Assert.assertEquals(countCA, countME);
        Assert.assertEquals(countUK, countDE);
        Assert.assertTrue(countUK > countCA);
    }

    @Test
    public void testSendClosest() throws Exception {

        logger.debug("creating router connections...");
        routers = ImmutableMap.of(
                INTERCONNECT_CA.val(), new RouterDelegate(initialContext, INTERCONNECT_CA.val(), CLOSEST_QUEUE.val(), false, true),
                INTERCONNECT_DE.val(), new RouterDelegate(initialContext, INTERCONNECT_DE.val(), CLOSEST_QUEUE.val(), false, true),
                INTERCONNECT_ME.val(), new RouterDelegate(initialContext, INTERCONNECT_ME.val(), CLOSEST_QUEUE.val(), true, false),
                INTERCONNECT_UK.val(), new RouterDelegate(initialContext, INTERCONNECT_UK.val(), CLOSEST_QUEUE.val(), true, false)
        );

        // allow a bit of time for topology/routes to propagate
        Thread.sleep(2000);

        logger.debug("sending closest message from CA and asserting ME receives...");
        routers.get(INTERCONNECT_CA.val()).sendToQueue();
        routers.get(INTERCONNECT_ME.val()).receiveFromQueue();

        logger.debug("sending closest message from DE and asserting UK receives...");
        routers.get(INTERCONNECT_DE.val()).sendToQueue();
        routers.get(INTERCONNECT_UK.val()).receiveFromQueue();
    }
}