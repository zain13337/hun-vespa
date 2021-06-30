// Copyright 2017 Yahoo Holdings. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.

#pragma once

#include "value.h"
#include "memory.h"
#include <vespa/vespalib/util/traits.h>
#include <vespa/vespalib/util/stash.h>

namespace vespalib::slime {

/**
 * Classes representing a single basic value.
 **/
class BasicBoolValue : public Value {
    bool _value;
public:
    BasicBoolValue(bool bit) : _value(bit) {}
    bool asBool() const override { return _value; }
    Type type() const override { return BOOL::instance; }
};

class BasicLongValue : public Value {
    int64_t _value;
public:
    BasicLongValue(int64_t l) : _value(l) {}
    int64_t asLong() const override { return _value; }
    double asDouble() const override { return _value; }
    Type type() const override { return LONG::instance; }
};

class BasicDoubleValue : public Value {
    double _value;
public:
    BasicDoubleValue(double d) : _value(d) {}
    double asDouble() const override { return _value; }
    int64_t asLong() const override { return _value; }
    Type type() const override { return DOUBLE::instance; }
};

class BasicStringValue : public Value {
    Memory _value;
public:
    BasicStringValue(Memory str, Stash & stash);
    BasicStringValue(const BasicStringValue &) = delete;
    BasicStringValue & operator = (const BasicStringValue &) = delete;
    Memory asString() const override { return _value; }
    Type type() const override { return STRING::instance; }
};

class BasicDataValue : public Value {
    Memory _value;
public:
    BasicDataValue(Memory data, Stash & stash);
    BasicDataValue(const BasicDataValue &) = delete;
    BasicDataValue & operator = (const BasicDataValue &) = delete;
    Memory asData() const override { return _value; }
    Type type() const override { return DATA::instance; }
};

} // namespace vespalib::slime

VESPA_CAN_SKIP_DESTRUCTION(vespalib::slime::BasicBoolValue);
VESPA_CAN_SKIP_DESTRUCTION(vespalib::slime::BasicLongValue);
VESPA_CAN_SKIP_DESTRUCTION(vespalib::slime::BasicDoubleValue);
VESPA_CAN_SKIP_DESTRUCTION(vespalib::slime::BasicStringValue);
VESPA_CAN_SKIP_DESTRUCTION(vespalib::slime::BasicDataValue);
