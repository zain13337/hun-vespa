// Copyright Vespa.ai. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.
package com.yahoo.schema.derived;

import com.yahoo.schema.parser.ParseException;
import org.junit.jupiter.api.Test;

import java.io.IOException;

/**
 * @author bratseh
 */
public class TokenizationTestCase extends AbstractExportingTestCase {

    @Test
    void testTokenizationScripts() throws IOException, ParseException {
        assertCorrectDeriving("tokenization");
    }

}
