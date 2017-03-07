package se.kth.id2203.multipaxos;

import se.kth.id2203.kvstore.Operation;
import se.sics.kompics.KompicsEvent;

import java.io.Serializable;

public class Propose implements KompicsEvent, Serializable
{
    public Operation value;

    public Propose(Operation value)
    {
        this.value = value;
    }

    @Override
    public String toString()
    {
        return "Propose{" +
                "value=" + value +
                '}';
    }
}
