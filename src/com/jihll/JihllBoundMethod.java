package com.jihll;

class JihllBoundMethod {
    final JihllInstance receiver;
    final JihllFunction method;

    JihllBoundMethod(JihllInstance receiver, JihllFunction method) {
        this.receiver = receiver;
        this.method = method;
    }

    @Override public String toString() { return "<bound " + method.name + ">"; }
}
