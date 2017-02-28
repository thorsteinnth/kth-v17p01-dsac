package se.kth.id2203.multipaxos;

import se.sics.kompics.KompicsEvent;

import java.io.Serializable;

public class AcceptAck implements KompicsEvent, Serializable
{
    /**
     * Timestamp (proposer)
     */
    public int ts;

    /**
     * Length of accepted sequence
     */
    public int l;

    /**
     * Logical clock
     */
    public int t_prime;

    public AcceptAck(int ts, int l, int t_prime)
    {
        this.ts = ts;
        this.l = l;
        this.t_prime = t_prime;
    }

    @Override
    public String toString()
    {
        return "AcceptAck{" +
                "ts=" + ts +
                ", l=" + l +
                ", t_prime=" + t_prime +
                '}';
    }
}
