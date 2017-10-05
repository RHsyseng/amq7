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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/***
 * @author jary@redhat.com
 */
public enum Constants {

    USERNAME(null),
    PASSWORD(null),
    TIMEOUT(null),

    QUEUE_NAME(null),
    TOPIC_NAME(null),
    MULTICAST_QUEUE(null),
    BALANCED_QUEUE(null),
    CLOSEST_QUEUE(null),

    SINGLE_A("b1"),
    SINGLE_B("b2"),
    SINGLE_C("b3"),

    SYMMETRIC_1("s1"),
    SYMMETRIC_2("s2"),
    SYMMETRIC_3("s3"),

    REPLICATED_M1("m1"),
    REPLICATED_M2("m2"),
    REPLICATED_M3("m3"),

    INTERCONNECT_UK("uk"),
    INTERCONNECT_DE("de"),
    INTERCONNECT_ME("me"),
    INTERCONNECT_CA("ca"),
    INTERCONNECT_AU("au");

    private static final String PROPS_FILE = "servers.properties";

    private static final Logger logger = LoggerFactory.getLogger(Constants.class);

    private static java.util.Properties properties;

    Constants(String value) {
        this.value = value;
    }

    private String value;

    private void init() {
        if (properties == null) {
            properties = new java.util.Properties();
            try {
                properties.load(Constants.class.getClassLoader().getResourceAsStream(PROPS_FILE));
            } catch (IOException e) {
                logger.error("error initializing PROPS enum", e);
                System.exit(1);
            }
        }
        value = (String) properties.get(this.toString());
    }

    public String val() {
        if (value == null)
            init();
        return value;
    }
}