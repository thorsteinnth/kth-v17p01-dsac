package se.kth.id2203.multipaxos;

import se.kth.id2203.kvstore.Operation;
import se.sics.kompics.KompicsEvent;

import java.io.Serializable;

public class DecideResult implements KompicsEvent, Serializable
{
    public Operation operation;

    public DecideResult(Operation operation)
    {
        this.operation = operation;
    }

    @Override
    public String toString()
    {
        return "DecideResult{" +
                "operation=" + operation +
                '}';
    }
}
