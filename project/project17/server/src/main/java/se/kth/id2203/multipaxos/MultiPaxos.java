package se.kth.id2203.multipaxos;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kth.id2203.kvstore.Operation;
import se.kth.id2203.networking.Message;
import se.kth.id2203.networking.NetAddress;
import se.kth.id2203.overlay.Topology;
import se.sics.kompics.*;
import se.sics.kompics.network.Network;

import java.util.*;

/**
 * Implementation of Multi Paxos. Abortable sequence consensus.
 * Should be using FIFO perfect point to point links, but this implementation just uses network directly.
 * */
public class MultiPaxos extends ComponentDefinition
{
    //region Ports

    private final Positive<Network> net = requires(Network.class);
    private final Negative<MultiPaxosPort> mpaxos = provides(MultiPaxosPort.class);

    //endregion

    //region Fields

    /**
     * My address
     * */
    private final NetAddress self = config().getValue("id2203.project.address", NetAddress.class);

    /**
     * Logger instance
     * */
    private final static Logger LOG = LoggerFactory.getLogger(MultiPaxos.class);

    /**
     * The topology. Addresses of processes that are taking part in the algorithm.
     * */
    private Set<NetAddress> topology = new HashSet<>();

    /**
     * Logical clock
     * */
    private int t;

    /**
     * Acceptor: Prepared timestamp
     * */
    private int prepts;

    /**
     * Acceptor: Timestamp
     * */
    private int ats;

    /**
     * Acceptor: Accepted sequence
     * */
    private List<Operation> av;

    /**
     * Acceptor: Length of decided sequence
     * */
    private int al;

    /**
     * Proposer: Timestamp
     * */
    private int pts;

    /**
     * Proposer: Proposed sequence
     * */
    private List<Operation> pv;

    /**
     * Proposer: Length of learned sequence
     * */
    private int pl;

    /**
     * Proposer: Values proposed while preparing
     * */
    private List<Operation> proposedValues;

    /**
     *
     */
    private Map<NetAddress, ReadlistEntry> readlist;

    /**
     * Proposer's knowledge about length of acceptor's longest accepted sequence
     * */
    private Map<NetAddress, Integer> accepted;

    /**
     * Proposer's knowledge about length of acceptor's longest decided sequence
     * */
    private Map<NetAddress, Integer> decided;

    // endregion

    //region Handlers

    protected final Handler<Start> startHandler = new Handler<Start>()
    {
        @Override
        public void handle(Start event)
        {
            LOG.info("Starting Multi Paxos on {}", self);

            t = 0;
            prepts = 0;
            ats = 0;
            av = new ArrayList<>();
            al = 0;
            pts = 0;
            pv = new ArrayList<>();
            pl = 0;
            proposedValues = new ArrayList<>();
            readlist = new HashMap<>();
            accepted = new HashMap<>();
            decided = new HashMap<>();
        }
    };

    private final Handler<Topology> topologyHandler = new Handler<Topology>()
    {
        @Override
        public void handle(Topology e)
        {
            LOG.info(self + " - Received topology: " + e.nodes);
            topology = e.nodes;
        }
    };

    // region Prepare phase

    private final Handler<Propose> proposeHandler = new Handler<Propose>()
    {
        @Override
        public void handle(Propose propose)
        {
            t++;

            if (pts == 0)
            {
                // Generate unique sequence number
                pts = t * getN() + getSelfRank();
                pv = prefix(av, al);
                pl = 0;

                proposedValues.clear();
                proposedValues.add(propose.value);

                // TODO New the readlist instead? Don't think that matters.
                for (NetAddress key : readlist.keySet())
                    readlist.put(key, null);

                for (NetAddress key : accepted.keySet())
                    accepted.put(key, 0);

                for (NetAddress key : decided.keySet())
                    decided.put(key, 0);

                for (NetAddress processAddress : topology)
                {
                    Prepare prepare = new Prepare(pts, al, t);
                    trigger(new Message(self, processAddress, prepare), net);
                }
            }
            else if (nonNullReadlistValueSize() <= Math.floor(getN()/2))
            {
                // Append to sequence
                proposedValues.add(propose.value);
            }
            else if (!pv.contains(propose.value))
            {
                pv.add(propose.value);

                for (NetAddress processAddress : topology)
                {
                    if (readlist.get(processAddress) != null)
                    {
                        List<Operation> valueSeq = new ArrayList<>();
                        valueSeq.add(propose.value);
                        Accept accept = new Accept(pts, valueSeq, pv.size()-1, t);
                        trigger(new Message(self, processAddress, accept), net);
                    }

                }
            }
        }
    };

    private final ClassMatchedHandler<Prepare, Message> prepareHandler = new ClassMatchedHandler<Prepare, Message>()
    {
        @Override
        public void handle(Prepare prepare, Message message)
        {
            LOG.info(self + " - Got prepare {}", prepare);

            t = Math.max(t, prepare.t_prime) + 1;

            if (prepare.ts < prepts)
            {
                Nack nack = new Nack(prepare.ts, t);
                trigger(new Message(self, message.getSource(), nack), net);
            }
            else
            {
                prepts = prepare.ts;
                PrepareAck prepareAck = new PrepareAck(prepare.ts, ats, suffix(av, prepare.l), al, t);
                trigger(new Message(self, message.getSource(), prepareAck), net);
            }
        }
    };

    private final ClassMatchedHandler<Nack, Message> nackHandler = new ClassMatchedHandler<Nack, Message>()
    {
        @Override
        public void handle(Nack nack, Message message)
        {
            LOG.info(self + " - Got NACK {}", nack);

            t = Math.max(t, nack.t_prime) + 1;

            if (nack.pts_prime == pts)
            {
                pts = 0;
                trigger(new Abort(), mpaxos);
            }
        }
    };

    // endregion Prepare phase

    //region Accept phase

    private final ClassMatchedHandler<PrepareAck, Message> prepareAckHandler = new ClassMatchedHandler<PrepareAck, Message>()
    {
        @Override
        public void handle(PrepareAck prepareAck, Message message)
        {
            LOG.info(self + " - Got prepare ack {}", prepareAck);

            t = Math.max(t, prepareAck.t_prime) + 1;

            if (prepareAck.pts_prime == pts)
            {
                readlist.put(message.getSource(), new ReadlistEntry(prepareAck.ts, prepareAck.vsuf));
                decided.put(message.getSource(), prepareAck.l);

                // Find the readlist entry with highest TS, and if two TS are equal,
                // with longest value suffix
                // TODO Non null values in readlist or clear readlist?
                if (nonNullReadlistValueSize() == (Math.floor(getN()/2) + 1))
                {
                    ReadlistEntry highestEntry = new ReadlistEntry(0, new ArrayList<Operation>());

                    for (ReadlistEntry entry : readlist.values())
                    {
                        if (entry != null && highestEntry.lessThan(entry))
                        {
                            highestEntry.ts = entry.ts;
                            highestEntry.vsuf = entry.vsuf;
                        }
                    }

                    pv.addAll(highestEntry.vsuf);

                    for (Operation value : proposedValues)
                    {
                        if (!pv.contains(value))
                        {
                            pv.add(value);
                        }
                    }

                    // New length of decided sequence
                    int newl = 0;

                    for (NetAddress p : readlist.keySet())
                    {
                        if (readlist.get(p) != null)
                        {
                            newl = decided.get(p);
                            Accept accept = new Accept(pts, suffix(pv, newl), newl, t);
                            trigger(new Message(self, p, accept), net);
                        }
                    }
                }
                // TODO Non null values in readlist or clear readlist?
                else if (nonNullReadlistValueSize() > (Math.floor(getN()/2) + 1))
                {
                    Accept accept = new Accept(pts, suffix(pv, prepareAck.l), prepareAck.l, t);
                    trigger(new Message(self, message.getSource(), accept), net);

                    if (pl != 0)
                    {
                        Decide decide = new Decide(pts, pl, t);
                        trigger(new Message(self, message.getSource(), decide), net);
                    }
                }
            }
        }
    };

    private final ClassMatchedHandler<Accept, Message> acceptHandler = new ClassMatchedHandler<Accept, Message>()
    {
        @Override
        public void handle(Accept accept, Message message)
        {
            LOG.info(self + " - Got accept {}", accept);

            t = Math.max(t, accept.t_prime) + 1;

            if (accept.ts != prepts)
            {
                Nack nack = new Nack(accept.ts, t);
                trigger(new Message(self, message.getSource(), nack), net);
            }
            else
            {
                ats = accept.ts;

                // If length of proposer's proposed sequence is less then length of accepted sequence
                if (accept.offs < av.size())
                {
                    av = prefix(av, accept.offs);
                }

                // add sequence with proposed value to accepted sequence
                av.addAll(accept.vsuf);

                AcceptAck acceptAck = new AcceptAck(accept.ts, av.size(), t);
                trigger(new Message(self, message.getSource(), acceptAck), net);
            }
        }
    };

    private final ClassMatchedHandler<AcceptAck, Message> acceptAckHandler = new ClassMatchedHandler<AcceptAck, Message>()
    {
        @Override
        public void handle(AcceptAck acceptAck, Message message)
        {
            LOG.info(self + " - Got accept ack {}", acceptAck);

            t = Math.max(t, acceptAck.t_prime) + 1;

            if (acceptAck.pts_prime == pts)
            {
                accepted.put(message.getSource(), acceptAck.l);

                // Number of processes that the proposer knows that have longer or equal accepted sequence then
                // the accepted ack accepted sequence
                int numberOfProcessesWithLongerOrEqAcceptedSeq = 0;
                for (int length : accepted.values())
                {
                    if (length >= acceptAck.l)
                        numberOfProcessesWithLongerOrEqAcceptedSeq++;
                }

                if ((pl < acceptAck.l) && (numberOfProcessesWithLongerOrEqAcceptedSeq > Math.floor(getN()/2)))
                {
                    pl = acceptAck.l;

                    for (NetAddress p : readlist.keySet())
                    {
                        if (readlist.get(p) != null)
                        {
                            Decide decide = new Decide(pts, pl, t);
                            trigger(new Message(self, p, decide), net);
                        }
                    }
                }
            }
        }
    };

    public final ClassMatchedHandler<Decide, Message> decideHandler = new ClassMatchedHandler<Decide, Message>()
    {
        @Override
        public void handle(Decide decide, Message message)
        {
            LOG.info(self + " - Got decide {}", decide);

            t = Math.max(t, decide.t_prime) + 1;

            if (decide.ts == prepts)
            {
                while (al < decide.l)
                {
                    trigger(new DecideResult(av.get(al)), mpaxos);
                    al++;
                }
            }
        }
    };

    //endregion Accept phase

    //endregion Handlers

    //region Methods

    /**
     * Get prefix of list
     * @param v
     * @param l
     * @return list with first l elements of v
     */
    private List<Operation> prefix(List<Operation> v, int l)
    {
        List<Operation> prefix = new ArrayList<>();

        int i = 0;
        while (i < v.size() && i < l)
            prefix.add(v.get(i++));

        return prefix;
    }

    /**
     * Get suffix of list
     * @param v
     * @param l
     * @return list with all elements of v after the first l elements
     */
    private List<Operation> suffix(List<Operation> v, int l)
    {
        System.out.println(self + " VALUE OF l: " + l);

        if (l == 0)
            return v;

        List<Operation> suffix = new ArrayList<>();

        if (l < v.size())
        {
            for (int i = l; i < v.size(); i++)
                suffix.add(v.get(i));
        }

        return suffix;
    }

    /**
     * Get count of values in readlist (map) that are not null.
     * @return Count of values in readlist that are not null.
     */
    private int nonNullReadlistValueSize()
    {
        int count = 0;
        for (ReadlistEntry entry : readlist.values())
        {
            if (entry != null)
                count++;
        }

        return count;
    }

    private int getN()
    {
        return topology.size();
    }

    private int getSelfRank()
    {
        return self.hashCode();
    }

    //endregion

    {
        subscribe(startHandler, control);

        subscribe(proposeHandler, mpaxos);
        subscribe(topologyHandler, mpaxos);

        subscribe(prepareHandler, net);
        subscribe(nackHandler, net);
        subscribe(prepareAckHandler, net);
        subscribe(acceptHandler, net);
        subscribe(acceptAckHandler, net);
        subscribe(decideHandler, net);
    }
}
