package se.kth.id2203.epfd;

import se.sics.kompics.PortType;

public final class EPFDPort extends PortType{
    public EPFDPort() {
        indication(Suspect.class);
        indication(Restore.class);
    }
}
