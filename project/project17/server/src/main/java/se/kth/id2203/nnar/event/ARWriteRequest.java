package se.kth.id2203.nnar.event;

import se.kth.id2203.kvstore.PutOperation;
import se.sics.kompics.KompicsEvent;

import java.io.Serializable;

public class ARWriteRequest implements KompicsEvent, Serializable
{
    public PutOperation operation;

    public ARWriteRequest(PutOperation operation)
    {
        this.operation = operation;
    }
}
