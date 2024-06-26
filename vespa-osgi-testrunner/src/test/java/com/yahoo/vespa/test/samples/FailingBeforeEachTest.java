// Copyright Vespa.ai. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.
package com.yahoo.vespa.test.samples;

import com.yahoo.vespa.testrunner.Expect;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@Expect(error = 1, status = 5)
public class FailingBeforeEachTest {

    @BeforeEach
    void fail() { throw new RuntimeException(); }

    @Test
    void test() { }

}
