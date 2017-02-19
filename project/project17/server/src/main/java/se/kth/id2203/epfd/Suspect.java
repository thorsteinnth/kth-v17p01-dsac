package se.kth.id2203.epfd;

import se.kth.id2203.networking.NetAddress;
import se.sics.kompics.KompicsEvent;

public class Suspect implements KompicsEvent{

    private NetAddress address;

    public Suspect(NetAddress address) {
        this.address = address;
    }

    public NetAddress getAddress() {
        return this.address;
    }
}
