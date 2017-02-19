package se.kth.id2203.epfd;

import se.sics.kompics.KompicsEvent;

public class HeartbeatReply implements KompicsEvent{

    private int seq;

    public HeartbeatReply(int seq) {

    }

    public int getSeq() {
        return this.seq;
    }
}
