package se.kth.id2203.nnar;

import se.kth.id2203.networking.NetAddress;

import java.util.HashMap;
import java.util.Map;

/**
 * Contains the state of an NN atomic register execution.
 * */
public class NNARState
{
    private int acks;
    private Object readVal;
    private Object writeVal;
    private Map<NetAddress, Tuple> readList;
    private boolean reading;

    public NNARState()
    {
        acks = 0;
        readVal = null;
        writeVal = null;
        readList = new HashMap<>();
        reading = false;
    }

    public int getAcks()
    {
        return acks;
    }

    public void setAcks(int acks)
    {
        this.acks = acks;
    }

    public Object getReadVal()
    {
        return readVal;
    }

    public void setReadVal(Object readVal)
    {
        this.readVal = readVal;
    }

    public Object getWriteVal()
    {
        return writeVal;
    }

    public void setWriteVal(Object writeVal)
    {
        this.writeVal = writeVal;
    }

    public Map<NetAddress, Tuple> getReadList()
    {
        return readList;
    }

    public boolean isReading()
    {
        return reading;
    }

    public void setReading(boolean reading)
    {
        this.reading = reading;
    }
}
