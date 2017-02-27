package se.kth.id2203.nnar.event;

import se.sics.kompics.KompicsEvent;

import java.io.Serializable;

public class ARCASRequest  implements KompicsEvent, Serializable {

    private String key;
    private Object referenceValue;
    private Object newValue;

    public ARCASRequest(String key, Object referenceValue, Object newValue) {
        this.key = key;
        this.referenceValue = referenceValue;
        this.newValue = newValue;
    }

    public String getKey() {
        return key;
    }

    public Object getReferenceValue() {
        return referenceValue;
    }

    public Object getNewValue() {
        return newValue;
    }
}
