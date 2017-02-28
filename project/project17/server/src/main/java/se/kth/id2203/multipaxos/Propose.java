package se.kth.id2203.multipaxos;

import se.sics.kompics.KompicsEvent;

import java.io.Serializable;

public class Propose implements KompicsEvent, Serializable
{
    public Object value;

    public Propose(Object value)
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
