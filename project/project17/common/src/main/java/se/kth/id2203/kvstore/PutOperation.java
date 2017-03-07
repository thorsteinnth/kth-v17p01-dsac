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
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        PutOperation operation = (PutOperation) o;

        if (key != null ? !key.equals(operation.key) : operation.key != null) return false;
        return value != null ? value.equals(operation.value) : operation.value == null;
    }

    @Override
    public int hashCode()
    {
        int result = super.hashCode();
        result = 31 * result + (key != null ? key.hashCode() : 0);
        result = 31 * result + (value != null ? value.hashCode() : 0);
        return result;
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
