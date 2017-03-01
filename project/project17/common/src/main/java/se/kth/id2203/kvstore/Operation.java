package se.kth.id2203.kvstore;

import com.google.common.base.MoreObjects;
import se.sics.kompics.KompicsEvent;

import java.io.Serializable;
import java.util.UUID;

public class Operation implements KompicsEvent, Serializable
{
    public final UUID id;

    public Operation()
    {
        this.id = UUID.randomUUID();
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Operation operation = (Operation) o;

        return id != null ? id.equals(operation.id) : operation.id == null;
    }

    @Override
    public int hashCode()
    {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("id", id)
                .toString();
    }
}
