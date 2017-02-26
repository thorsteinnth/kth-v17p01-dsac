package se.kth.id2203.nnar.event;

import se.sics.kompics.KompicsEvent;

import java.io.Serializable;

public class ARWriteRequest implements KompicsEvent, Serializable
{
    private Object value;

    public ARWriteRequest(Object value) {
        this.value = value;
    }

    public Object getValue() {
        return this.value;
    }
}
