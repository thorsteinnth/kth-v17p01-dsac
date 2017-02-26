package se.kth.id2203.kvstore;

import com.google.common.base.MoreObjects;
import se.sics.kompics.KompicsEvent;

import java.io.Serializable;
import java.util.UUID;

public class PutOperation extends Operation implements KompicsEvent, Serializable
{
    // TODO Do we want to have to put the key in here too?
    // Or do we want to hash the data value itself to find keys?
    public final String key;
    public final String value;

    public PutOperation(String key, String value)
    {
        this.key = key;
        this.value = value;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("id", id)
                .add("key", key)
                .add("value", value)
                .toString();
    }
}
