# Copyright Vespa.ai. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.
vespa_add_library(searchlib_predicate OBJECT
    SOURCES
    document_features_store.cpp
    document_features_store_saver.cpp
    predicate_index.cpp
    predicate_index_saver.cpp
    predicate_interval.cpp
    predicate_interval_store.cpp
    predicate_range_expander.cpp
    predicate_tree_analyzer.cpp
    predicate_tree_annotator.cpp
    predicate_zero_constraint_posting_list.cpp
    simple_index.cpp
    simple_index_saver.cpp
    common.cpp
    DEPENDS
)
