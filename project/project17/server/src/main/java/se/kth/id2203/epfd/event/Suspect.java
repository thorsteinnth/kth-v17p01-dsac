package se.kth.id2203.epfd.event;

import se.kth.id2203.networking.NetAddress;
import se.sics.kompics.KompicsEvent;

import java.io.Serializable;

public class Suspect implements KompicsEvent, Serializable {

    private NetAddress address;

    public Suspect(NetAddress address) {
        this.address = address;
    }

    public NetAddress getAddress() {
        return this.address;
    }
}
