package se.kth.id2203.multipaxos;

import se.sics.kompics.KompicsEvent;

import java.io.Serializable;

public class Decide implements KompicsEvent, Serializable
{
    /**
     * Timestamp (proposer)
     */
    public int ts;

    /**
     * Length of learned seq (proposer)
     */
    public int l;

    /**
     * Logical clock
     */
    public int t_prime;

    public Decide(int ts, int l, int t_prime)
    {
        this.ts = ts;
        this.l = l;
        this.t_prime = t_prime;
    }
}
