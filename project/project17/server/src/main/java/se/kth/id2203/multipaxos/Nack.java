package se.kth.id2203.multipaxos;

import se.sics.kompics.KompicsEvent;

import java.io.Serializable;

public class Nack implements KompicsEvent, Serializable
{
    /**
     * Timestamp (proposer)
     */
    public int pts_prime;

    /**
     * Logical clock
     */
    public int t_prime;

    public Nack(int pts_prime, int t_prime)
    {
        this.pts_prime = pts_prime;
        this.t_prime = t_prime;
    }

    @Override
    public String toString()
    {
        return "Nack{" +
                "pts_prime=" + pts_prime +
                ", t_prime=" + t_prime +
                '}';
    }
}
