package se.kth.id2203.multipaxos;

import se.kth.id2203.kvstore.Operation;
import se.sics.kompics.KompicsEvent;

import java.io.Serializable;
import java.util.List;

public class Accept implements KompicsEvent, Serializable
{
    // TODO Figure out what variable names to use here. This object can carry different things (but of the same type).

    /**
     * Timestamp (proposer)
     */
    public int ts;
    /**
     * Sequence with proposed value (only)
     */
    public List<Operation> vsuf;
    /**
     * Length of proposer's proposed sequence - 1
     */
    public int offs;
    /**
     * Logical clock
     */
    public int t_prime;

    public Accept(int ts, List<Operation> vsuf, int offs, int t_prime)
    {
        this.ts = ts;
        this.vsuf = vsuf;
        this.offs = offs;
        this.t_prime = t_prime;
    }

    @Override
    public String toString()
    {
        return "Accept{" +
                "ts=" + ts +
                ", vsuf=" + vsuf +
                ", offs=" + offs +
                ", t_prime=" + t_prime +
                '}';
    }
}
