# Copyright Vespa.ai. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.
vespa_add_library(vespalib_vespalib_coro OBJECT
    SOURCES
    active_work.cpp
    async_crypto_socket.cpp
    async_io.cpp
    DEPENDS
)
