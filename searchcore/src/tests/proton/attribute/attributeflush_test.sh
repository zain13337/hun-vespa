#!/bin/bash
# Copyright Vespa.ai. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.
set -e
rm -rf flush
$VALGRIND ./searchcore_attributeflush_test_app
