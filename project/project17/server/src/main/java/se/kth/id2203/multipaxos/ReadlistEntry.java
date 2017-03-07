package se.kth.id2203.multipaxos;

import se.kth.id2203.kvstore.Operation;

import java.util.List;

public class ReadlistEntry
{
    /**
     * Timestamp
     */
    public int ts;

    /**
     * Value suffix
     */
    public List<Operation> vsuf;

    public ReadlistEntry(int ts, List<Operation> vsuf)
    {
        this.ts = ts;
        this.vsuf = vsuf;
    }

    /**
     * Checks if readlist entry is less then input entry
     * @param entry
     * @return true if entries ts is less then input's entry ts or if
     * vsuf length is less the input's entry vsuf
     */
    public boolean lessThan(ReadlistEntry entry)
    {
        if (ts < entry.ts)
            return true;

        if ((ts == entry.ts) && (vsuf.size() < entry.vsuf.size()))
            return true;

        return false;
    }


    @Override
    public String toString() {
        return "ReadlistEntry{" +
                "ts=" + ts +
                ", vsuf=" + vsuf +
                '}';
    }
}
