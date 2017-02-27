package se.kth.id2203.nnar.event;

import se.kth.id2203.kvstore.GetOperation;
import se.sics.kompics.KompicsEvent;

import java.io.Serializable;

public class ARReadRequest implements KompicsEvent, Serializable
{
    public GetOperation operation;

    public ARReadRequest(GetOperation operation)
    {
        this.operation = operation;
    }
}
