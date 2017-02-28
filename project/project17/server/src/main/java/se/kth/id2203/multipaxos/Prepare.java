package se.kth.id2203.multipaxos;

import se.sics.kompics.KompicsEvent;

import java.io.Serializable;

public class Prepare implements KompicsEvent, Serializable
{
    /**
     * Timestamp (proposer)
     */
    public int ts;
    /**
     * Length of decided sequence (acceptor)
     */
    public int l;
    /**
     * Logical clock
     */
    public int t_prime;

    public Prepare(int ts, int l, int t_prime)
    {
        this.ts = ts;
        this.l = l;
        this.t_prime = t_prime;
    }

    @Override
    public String toString()
    {
        return "Prepare{" +
                "ts=" + ts +
                ", l=" + l +
                ", t_prime=" + t_prime +
                '}';
    }
}
