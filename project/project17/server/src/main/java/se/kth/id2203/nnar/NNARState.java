package se.kth.id2203.nnar;

import se.kth.id2203.networking.NetAddress;

import java.util.HashMap;
import java.util.Map;

/**
 * Contains the state of an NN atomic register execution.
 * */
public class NNARState
{
    private Tuple tuple;
    private Object value;
    private int acks;
    private Object readVal;
    private Object writeVal;
    private Map<NetAddress, Tuple> readList;
    private boolean reading;

    public NNARState()
    {
        tuple = new Tuple(0, 0);
        value = null;
        acks = 0;
        readVal = null;
        writeVal = null;
        readList = new HashMap<>();
        reading = false;
    }

    public Tuple getTuple()
    {
        return tuple;
    }

    public void setTuple(Tuple tuple)
    {
        this.tuple = tuple;
    }

    public Object getValue()
    {
        return value;
    }

    public void setValue(Object value)
    {
        this.value = value;
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

    public void setReadList(Map<NetAddress, Tuple> readList)
    {
        this.readList = readList;
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
