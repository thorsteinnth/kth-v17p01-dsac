package se.kth.id2203.epfd;

import se.sics.kompics.KompicsEvent;

public class HeartbeatRequest implements KompicsEvent{

    private int seq;

    public HeartbeatRequest(int seq) {
        this.seq = seq;
    }

    public int getSeq() {
        return this.seq;
    }
}
