// Copyright Vespa.ai. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.
package com.yahoo.vespaxmlparser;

import com.yahoo.document.DocumentRemove;
import com.yahoo.document.DocumentPut;
import com.yahoo.document.DocumentUpdate;
import com.yahoo.document.TestAndSetCondition;

public class FeedOperation {

    public enum Type {DOCUMENT, REMOVE, UPDATE, INVALID}

    public static final FeedOperation INVALID = new FeedOperation(Type.INVALID);

    private Type type;
    protected FeedOperation(Type type) {
        this.type = type;
    }
    public final Type getType() { return type; }
    protected final void setType(Type type) {
        this.type = type;
    }

    public DocumentPut getDocumentPut() { return null; }
    public DocumentUpdate getDocumentUpdate() { return null; }
    public DocumentRemove getDocumentRemove() { return null; }

    public TestAndSetCondition getCondition() {
        return TestAndSetCondition.NOT_PRESENT_CONDITION;
    }
    @Override
    public String toString() {
        return "Operation{" +
                "type=" + getType() +
                ", doc=" + getDocumentPut() +
                ", remove=" + getDocumentRemove() +
                ", docUpdate=" + getDocumentUpdate() +
                " testandset=" + getCondition() +
                '}';
    }

}
