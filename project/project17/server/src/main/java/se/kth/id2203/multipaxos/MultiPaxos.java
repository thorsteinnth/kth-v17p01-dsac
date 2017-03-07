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
import java.util.concurrent.ThreadLocalRandom;

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

    /**
     * self rank
     */
    private int selfRank;

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
            selfRank = ThreadLocalRandom.current().nextInt(100000000);
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

            LOG.info(self + " - Got propose: " + propose);

            if (pts == 0)
            {
                // Generate unique sequence number
                pts = t * getN() + getSelfRank();
                // Proposers proposed sequence is the first length-of-decided sequence of the acceptor accepted sequence
                pv = prefix(av, al);
                // Length of learned sequence set to 0
                pl = 0;

                // Clear proposed values and add the new proposed value from the request to it
                proposedValues.clear();
                proposedValues.add(propose.value);

                // TODO New the readlist instead? Don't think that matters.
                // Clear readlist
                for (NetAddress key : readlist.keySet())
                    readlist.put(key, null);

                // Clear accepted
                for (NetAddress key : accepted.keySet())
                    accepted.put(key, 0);

                // Clear decided
                for (NetAddress key : decided.keySet())
                    decided.put(key, 0);

                // I am in the prepare phase
                // Send prepare message to all participants
                for (NetAddress processAddress : topology)
                {
                    Prepare prepare = new Prepare(pts, al, t);
                    trigger(new Message(self, processAddress, prepare), net);
                }
            }
            else if (nonNullReadlistValueSize() <= Math.floor(getN()/2))
            {
                // Seqnum != 0 and I have a read list value from less than half of all processes

                // Append this new propose request value to my proposed value sequence
                proposedValues.add(propose.value);
            }
            else if (!pv.contains(propose.value))
            {
                // Seqnum != 0 and I have read list values from more than half of all processes
                // and proposed sequence does not contain the new propose request value

                // Add the new propose request value to the proposed sequence
                pv.add(propose.value);

                // Send Accept message to all processes that have an entry in my readlist
                for (NetAddress processAddress : topology)
                {
                    if (readlist.get(processAddress) != null)
                    {
                        // The accept message contains the new propose request value
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

            // This is part of the prepare phase

            // Update my logical clock with the clock value from the prepare message
            t = Math.max(t, prepare.t_prime) + 1;

            if (prepare.ts < prepts)
            {
                // Proposer time stamp is less than my prepare timestamp
                // I have promised not to vote in this round
                // Send NACK to proposer
                Nack nack = new Nack(prepare.ts, t);
                trigger(new Message(self, message.getSource(), nack), net);
            }
            else
            {
                // Proposer time stamp is larger than or equal to my prepare time stamp.
                // I have not promised not to vote in this round
                // In the slides he sends a promise/prepareAck if (my prepare timestamp < proposer time stamp).
                // Here I send promise if (my prepare timestamp <= proposer time stamp.
                // Update my prepare timestamp to the proposer's timestamp
                prepts = prepare.ts;
                // Send a promise/prepareAck to the proposer
                // The message contains the proposer timestamp, my (acceptor timestamp),
                // the suffix of my accepted sequence (tail of my accepted seq after the length of decided sequence
                // I got from the proposer), the length of my decided sequence, and my logical clock.
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

            // Received promise/prepareACK

            // Update my logical clock w.r.t. the logical clock from the prepareAck message
            t = Math.max(t, prepareAck.t_prime) + 1;

            if (prepareAck.pts_prime == pts)
            {
                // This prepareAck message is part of my proposal round

                // Put the acceptor timestamp and the acceptors accepted sequence suffix in the readlist map
                readlist.put(message.getSource(), new ReadlistEntry(prepareAck.ts, prepareAck.vsuf));
                // Put the length of the acceptors decided sequence in the decided map
                decided.put(message.getSource(), prepareAck.l);

                // Find the readlist entry with highest TS, and if two TS are equal,
                // with longest value suffix
                // TODO Non null values in readlist or clear readlist?
                if (nonNullReadlistValueSize() == (Math.floor(getN()/2) + 1))
                {
                    // I have read list values from more than half of the processes in the system
                    // I have exactly as many read list values as I need

                    // Find the highest entry in the read list map
                    ReadlistEntry highestEntry = new ReadlistEntry(0, new ArrayList<Operation>());
                    for (ReadlistEntry entry : readlist.values())
                    {
                        if (entry != null && highestEntry.lessThan(entry))
                        {
                            highestEntry.ts = entry.ts;
                            highestEntry.vsuf = entry.vsuf;
                        }
                    }

                    // Add the highest readlist entry accepted sequence suffix to the proposed sequence
                    pv.addAll(highestEntry.vsuf);

                    // Add the incoming proposed values (that I am trying to get a consensus on)
                    // that are not already in the proposed sequence to the proposed sequence
                    for (Operation value : proposedValues)
                    {
                        if (!pv.contains(value))
                        {
                            pv.add(value);
                        }
                    }

                    // Send Accept message to all other processes that have sent me a readlist entry
                    // (i.e. to those that I have gotten a prepareAck/promise from)
                    // New length of decided sequence
                    int l_prime;
                    for (NetAddress p : readlist.keySet())
                    {
                        if (readlist.get(p) != null)
                        {
                            l_prime = decided.get(p);   // Length of this acceptors decided sequence
                            // Accepted message contains the current sequence number, the suffix of the proposed
                            // sequence (after the length of this acceptors decided sequence), this acceptors
                            // length of decided sequence (that I know of), and my logical clock
                            Accept accept = new Accept(pts, suffix(pv, l_prime), l_prime, t);
                            trigger(new Message(self, p, accept), net);
                        }
                    }
                }
                // TODO Non null values in readlist or clear readlist?
                else if (nonNullReadlistValueSize() > (Math.floor(getN()/2) + 1))
                {
                    // I have more than the minimum required read list values I need

                    // Reply to the prepareAck sender with an Accept message
                    // The message contains the current sequence number, the suffix of the proposed
                    // sequence (after the length of this acceptors decided sequence), this acceptors
                    // length of decided sequence, and my logical clock
                    Accept accept = new Accept(pts, suffix(pv, prepareAck.l), prepareAck.l, t);
                    trigger(new Message(self, message.getSource(), accept), net);

                    // TODO In the slides he is updating the decided(promise sender) here it seems

                    if (pl != 0)
                    {
                        // Length of learned sequence is not 0
                        // Send decide message to the prepareAck sender
                        // Decide message contains the sequence number, length of learned sequence, and
                        // my logical clock
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

            // Just got Accept message from proposer

            // Update my logical clock w.r.t. the proposer's logical clock
            t = Math.max(t, accept.t_prime) + 1;

            if (accept.ts != prepts)
            {
                // This accept message is for a round that I am not taking part in
                Nack nack = new Nack(accept.ts, t);
                trigger(new Message(self, message.getSource(), nack), net);
            }
            else
            {
                // This accept message is for a round that I am taking part in

                // Update my acceptor's time stamp
                ats = accept.ts;

                // If length of proposer's proposed sequence is less then length of accepted sequence
                // offs is the length of my decided sequence that the proposer knew of
                if (accept.offs < av.size())
                {
                    // Cut down my accepted sequence to what the proposer knew to be the length of my decided sequence
                    av = prefix(av, accept.offs);
                }

                // Add the proposed value sequence from the proposer (suffix, tail after the element that the proposer
                // knew that I already had in my decided sequence)
                av.addAll(accept.vsuf);

                // Send accept ACK to the proposer
                // The accept ACK contains the seqnum of the round that this accept is for, the size of
                // my decided sequence and my logical clock timestamp
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

            // Just got accepted message from acceptor

            // Update my logical clock w.r.t. the logical timestamp in the AcceptAck message
            t = Math.max(t, acceptAck.t_prime) + 1;

            if (acceptAck.pts_prime == pts)
            {
                // This message is part of the round that I am in

                // Put the size of this acceptors decided sequence in my accepted map
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
                    // Length of learned sequence is less than the size of this acceptors decided sequence
                    // and the prefix is supported
                    // (Sequence v is supported if majority of acceptors q: a[q] >= length(v) ...
                    // i.e. majority of acceptors have accepted sequence that is longer or equal to the sequence)

                    pl = acceptAck.l;

                    // Send decide to all processes that I have a readlist entry for (they have sent me
                    // a prepareAck/promise)
                    for (NetAddress p : readlist.keySet())
                    {
                        if (readlist.get(p) != null)
                        {
                            // Decide message contains sequence number, length of learned sequence and logical clock
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

            // Got decide message from proposer

            // Update logical clock w.r.t. logical clock in decide message
            t = Math.max(t, decide.t_prime) + 1;

            if (decide.ts == prepts)
            {
                // This decide message is part of my proposal round
                while (al < decide.l)
                {
                    // While my (acceptor) length of decided sequence is less than the length of proposer's
                    // learned sequence ...
                    // Send the elements in my accepted sequence, that are part of this new learned sequence
                    // to my application layer, and increase the length of my decided sequence up to the
                    // length of this new learned sequence
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
        return selfRank;
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
