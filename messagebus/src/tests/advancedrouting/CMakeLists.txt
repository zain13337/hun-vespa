# Copyright Vespa.ai. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.
vespa_add_executable(messagebus_advancedrouting_test_app TEST
    SOURCES
    advancedrouting.cpp
    DEPENDS
    messagebus_messagebus-test
    messagebus
    GTest::gtest
)
vespa_add_test(NAME messagebus_advancedrouting_test_app COMMAND messagebus_advancedrouting_test_app)
