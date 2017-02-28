package se.kth.id2203.multipaxos;

import se.sics.kompics.KompicsEvent;

import java.io.Serializable;

public class AcceptAck implements KompicsEvent, Serializable
{
    /**
     * Timestamp (proposer)
     */
    public int pts_prime;

    /**
     * Length of accepted sequence
     */
    public int l;

    /**
     * Logical clock
     */
    public int t_prime;

    public AcceptAck(int pts_prime, int l, int t_prime)
    {
        this.pts_prime = pts_prime;
        this.l = l;
        this.t_prime = t_prime;
    }

    @Override
    public String toString()
    {
        return "AcceptAck{" +
                "pts_prime=" + pts_prime +
                ", l=" + l +
                ", t_prime=" + t_prime +
                '}';
    }
}
