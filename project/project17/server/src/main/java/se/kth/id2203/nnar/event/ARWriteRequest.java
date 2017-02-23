package se.kth.id2203.nnar.event;

import se.sics.kompics.KompicsEvent;

public class ARWriteRequest implements KompicsEvent {

    private Object value;

    public ARWriteRequest(Object value) {
        this.value = value;
    }

    public Object getValue() {
        return this.value;
    }
}
