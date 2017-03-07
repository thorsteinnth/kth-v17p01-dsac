package se.kth.id2203.kvstore;

import se.kth.id2203.networking.NetAddress;

public class PendingOperation
{
    public NetAddress clientAddress;
    public Operation operation;

    public PendingOperation(NetAddress clientAddress, Operation operation)
    {
        this.clientAddress = clientAddress;
        this.operation = operation;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PendingOperation that = (PendingOperation) o;

        if (clientAddress != null ? !clientAddress.equals(that.clientAddress) : that.clientAddress != null)
            return false;
        return operation != null ? operation.equals(that.operation) : that.operation == null;
    }

    @Override
    public int hashCode()
    {
        int result = clientAddress != null ? clientAddress.hashCode() : 0;
        result = 31 * result + (operation != null ? operation.hashCode() : 0);
        return result;
    }

    @Override
    public String toString()
    {
        return "PendingOperation{" +
                "clientAddress=" + clientAddress +
                ", operation=" + operation +
                '}';
    }
}