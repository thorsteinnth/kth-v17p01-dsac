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
}
