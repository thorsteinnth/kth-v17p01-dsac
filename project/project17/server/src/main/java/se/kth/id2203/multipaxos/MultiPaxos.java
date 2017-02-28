package se.kth.id2203.multipaxos;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private List<Object> av;

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
    private List<Object> pv;

    /**
     * Proposer: Length of learned sequence
     * */
    private int pl;

    /**
     * Proposer: Values proposed while preparing
     * */
    private List<Object> proposedValues;

    /**
     *
     */
    private Map<NetAddress, Object> readlist;

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

                // TODO Should we be clearing the maps here? So their size will be 0.

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
            // TODO is keySet.size() correct? Or should this be the number of non null values in the readlist.
            // TODO Probably the number of non null values.
            else if (readlist.keySet().size() <= Math.floor(getN()/2))
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
                        List<Object> valueSeq = new ArrayList<>();
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

    protected final ClassMatchedHandler<Accept, Message> acceptHandler = new ClassMatchedHandler<Accept, Message>()
    {
        @Override
        public void handle(Accept accept, Message message)
        {
            t = Math.max(t, accept.t_prime) + 1;

            if(accept.ts != prepts)
            {
                Nack nack = new Nack(accept.ts, t);
                trigger(new Message(self, message.getSource(), nack), net);
            }
            else
            {
                ats = accept.ts;

                // if length of proposer's proposed sequence is less then length of accepted sequence
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

    //endregion Accept phase

    //endregion Handlers

    //region Methods

    /**
     * Get prefix of list
     * @param v
     * @param l
     * @return list with first l elements of v
     */
    private List<Object> prefix(List<Object> v, int l)
    {
        List<Object> prefix = new ArrayList<>();

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
    private List<Object> suffix(List<Object> v, int l)
    {
        List<Object> suffix = new ArrayList<>();

        if (l < v.size())
        {
            for (int i = l-1; i < v.size(); i++)
                suffix.add(v.get(i));
        }

        return suffix;
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
        subscribe(proposeHandler, mpaxos);
        subscribe(prepareHandler, net);
        subscribe(nackHandler, net);
        subscribe(acceptHandler, net);
    }
}
