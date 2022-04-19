// Copyright Yahoo. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.
package com.yahoo.vespa.hosted.controller.api.integration.deployment;

import com.yahoo.config.provision.Environment;
import com.yahoo.config.provision.SystemName;
import com.yahoo.config.provision.zone.ZoneId;
import com.yahoo.vespa.hosted.controller.api.integration.zone.ZoneRegistry;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author jonmv
 */
public class JobTypeTest {

    @Test
    public void test() {
        for (JobType type : JobType.values()) {
            if (type.isProduction()) {
                boolean match = false;
                for (JobType other : JobType.values())
                    match |=    type != other
                             && type.isTest() == other.isDeployment()
                             && type.zones.equals(other.zones);

                assertTrue(type + " should have matching job", match);
            }
        }

        assertEquals(JobType.testUsEast3, JobType.fromJobName("prod.us-east-3.test"));
        assertEquals(JobType.devAwsUsEast1c, JobType.fromJobName("dev.aws-us-east-1c"));

        assertFalse(JobType.dev("snohetta").isTest());
        assertTrue(JobType.dev("snohetta").isDeployment());
        assertFalse(JobType.dev("snohetta").isProduction());

        assertFalse(JobType.perf("snohetta").isTest());
        assertTrue(JobType.perf("snohetta").isDeployment());
        assertFalse(JobType.perf("snohetta").isProduction());

        assertTrue(JobType.deploymentTo(ZoneId.from("test", "snohetta")).isTest());
        assertTrue(JobType.deploymentTo(ZoneId.from("test", "snohetta")).isDeployment());
        assertFalse(JobType.deploymentTo(ZoneId.from("test", "snohetta")).isProduction());

        assertTrue(JobType.deploymentTo(ZoneId.from("staging", "snohetta")).isTest());
        assertTrue(JobType.deploymentTo(ZoneId.from("staging", "snohetta")).isDeployment());
        assertFalse(JobType.deploymentTo(ZoneId.from("staging", "snohetta")).isProduction());

        assertFalse(JobType.prod("snohetta").isTest());
        assertTrue(JobType.prod("snohetta").isDeployment());
        assertTrue(JobType.prod("snohetta").isProduction());

        assertTrue(JobType.test("snohetta").isTest());
        assertFalse(JobType.test("snohetta").isDeployment());
        assertTrue(JobType.test("snohetta").isProduction());
    }

}
