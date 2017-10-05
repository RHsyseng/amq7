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
package com.redhat.refarch.amq7.cluster;

import com.redhat.refarch.amq7.BrokerClient;
import org.junit.Assert;
import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.InitialContext;
import java.io.InputStream;
import java.util.Properties;

/***
 * @author jary@redhat.com
 */
public class ClusterBaseTest {

    private static final Logger logger = LoggerFactory.getLogger(ClusterBaseTest.class);

    protected InitialContext initialContext;

    @Before
    public void before() {
        try {
            InputStream is = ClusterBaseTest.class.getClassLoader().getSystemResourceAsStream("cluster.jndi.properties");
            Properties properties = new Properties();
            properties.load(is);

            initialContext = new InitialContext(properties);
        } catch (Exception e) {
            logger.error("error instantiating InitialContext before test", e);
            Assert.fail();
        }
    }

    protected static void terminateClient(BrokerClient client) {
        if (client != null)
            client.terminateConnections();
    }
}
