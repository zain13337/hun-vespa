# Copyright Vespa.ai. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.
vespa_add_executable(searchcore_metrics_engine_test_app TEST
    SOURCES
    metrics_engine_test.cpp
    DEPENDS
    searchcore_flushengine
    searchcore_proton_metrics
)
vespa_add_test(NAME searchcore_metrics_engine_test_app COMMAND searchcore_metrics_engine_test_app)
