package se.kth.id2203.broadcast;

import se.sics.kompics.KompicsEvent;

import java.io.Serializable;

public class BroadcastMessage implements KompicsEvent, Serializable
{
    public String message;

    public BroadcastMessage(String message)
    {
        this.message = message;
    }

    @Override
    public String toString()
    {
        return "BroadcastMessage{" +
                "message='" + message + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BroadcastMessage that = (BroadcastMessage) o;

        return message != null ? message.equals(that.message) : that.message == null;
    }

    @Override
    public int hashCode()
    {
        return message != null ? message.hashCode() : 0;
    }
}
