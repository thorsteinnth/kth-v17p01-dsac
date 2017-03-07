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
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        CASOperation that = (CASOperation) o;

        if (key != null ? !key.equals(that.key) : that.key != null) return false;
        if (referenceValue != null ? !referenceValue.equals(that.referenceValue) : that.referenceValue != null)
            return false;
        return newValue != null ? newValue.equals(that.newValue) : that.newValue == null;
    }

    @Override
    public int hashCode()
    {
        int result = super.hashCode();
        result = 31 * result + (key != null ? key.hashCode() : 0);
        result = 31 * result + (referenceValue != null ? referenceValue.hashCode() : 0);
        result = 31 * result + (newValue != null ? newValue.hashCode() : 0);
        return result;
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