package se.kth.id2203.kvstore;

import com.google.common.base.MoreObjects;
import se.sics.kompics.KompicsEvent;

import java.io.Serializable;

public class CASOperation extends Operation implements KompicsEvent, Serializable
{
    public final String key;
    public final String referenceValue;
    public final String newValue;

    public CASOperation(String key, String referenceValue, String newValue)
    {
        this.key = key;
        this.referenceValue = referenceValue;
        this.newValue = newValue;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("id", id)
                .add("key", key)
                .add("referenceValue", referenceValue)
                .add("newValue", newValue)
                .toString();
    }
}