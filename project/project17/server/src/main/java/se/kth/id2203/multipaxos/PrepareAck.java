package se.kth.id2203.multipaxos;

import se.sics.kompics.KompicsEvent;

import java.io.Serializable;
import java.util.List;

public class PrepareAck implements KompicsEvent, Serializable
{
    /**
     * Timestamp (proposer)
     */
    public int pts_prime;

    /**
     * Timestamp (acceptor)
     */
    public int ts;

    /**
     * Value sequence suffix (suffix(av,l))
     */
    public List<Object> vsuf;

    /**
     * Length of decided sequence (acceptor)
     */
    public int l;

    /**
     * Logical clock
     */
    public int t_prime;

    public PrepareAck(int pts_prime, int ts, List<Object> vsuf, int l, int t_prime)
    {
        this.pts_prime = pts_prime;
        this.ts = ts;
        this.vsuf = vsuf;
        this.l = l;
        this.t_prime = t_prime;
    }

    @Override
    public String toString()
    {
        return "PrepareAck{" +
                "pts_prime=" + pts_prime +
                ", ts=" + ts +
                ", vsuf=" + vsuf +
                ", l=" + l +
                ", t_prime=" + t_prime +
                '}';
    }
}
