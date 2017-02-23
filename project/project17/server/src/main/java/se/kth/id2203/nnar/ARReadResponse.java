package se.kth.id2203.nnar;

import se.sics.kompics.KompicsEvent;

public class ARReadResponse implements KompicsEvent {

    // TODO : This is supposed to be an optional parameter, might need to change this
    private Object value;

    public ARReadResponse() {
    }

    public ARReadResponse(Object value) {
        this.value = value;
    }

    public Object getValue() {
        return this.value;
    }
}
