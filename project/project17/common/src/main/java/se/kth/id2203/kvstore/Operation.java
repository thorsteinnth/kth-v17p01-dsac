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
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("id", id)
                .toString();
    }
}
