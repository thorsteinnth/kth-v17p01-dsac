package se.kth.id2203.epfd;

import se.kth.id2203.networking.NetAddress;
import se.sics.kompics.KompicsEvent;

public class Restore implements KompicsEvent{

    private NetAddress address;

    public Restore(NetAddress address) {
        this.address = address;
    }

    public NetAddress getAddress() {
        return this.address;
    }
}
