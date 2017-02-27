package se.kth.id2203.nnar;

public class Tuple
{
    private int ts;
    private int wr;
    private Object optionalValue;

    public Tuple(int ts, int wr)
    {
        this.ts = ts;
        this.wr = wr;
        this.optionalValue = null;
    }

    public Tuple(int ts, int wr, Object optionalValue)
    {
        this.ts = ts;
        this.wr = wr;
        this.optionalValue = optionalValue;
    }

    public int getTs()
    {
        return ts;
    }

    public void setTs(int ts)
    {
        this.ts = ts;
    }

    public int getWr()
    {
        return wr;
    }

    public void setWr(int wr)
    {
        this.wr = wr;
    }

    public Object getOptionalValue()
    {
        return optionalValue;
    }

    public void setOptionalValue(Object optionalValue)
    {
        this.optionalValue = optionalValue;
    }

    /**
     * Checks if tuple is bigger then input tuple
     * @param tuple
     * @return True if timestamp is bigger. If timestamp are equal, return true if rank is bigger. Otherwise false.
     */
    public boolean biggerThan(Tuple tuple)
    {
        if (ts > tuple.getTs())
            return true;

        if (ts == tuple.getTs() && wr > tuple.getWr())
            return true;

        return false;
    }

    /**
     * Checks if tuple is bigger then or equal to the input tuple
     * @param tuple
     * @return True if timestamp is bigger. If timestamps are equal, return true if rank is bigger or equal. Otherwise false.
     */
    public boolean biggerOrEqual(Tuple tuple)
    {
        // (2,2) <= (3,1) -> true
        // (2,2) <= (2,2) -> true
        // (2,2) <= (2,1) -> false

        if (ts > tuple.getTs())
            return true;

        if (ts == tuple.getTs() && wr >= tuple.getWr())
            return true;

        return false;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Tuple tuple = (Tuple) o;

        if (ts != tuple.ts) return false;
        if (wr != tuple.wr) return false;
        return optionalValue != null ? optionalValue.equals(tuple.optionalValue) : tuple.optionalValue == null;
    }

    @Override
    public int hashCode()
    {
        int result = ts;
        result = 31 * result + wr;
        result = 31 * result + (optionalValue != null ? optionalValue.hashCode() : 0);
        return result;
    }

    @Override
    public String toString()
    {
        return "Tuple{" +
                "ts=" + ts +
                ", wr=" + wr +
                ", optionalValue=" + optionalValue +
                '}';
    }
}
