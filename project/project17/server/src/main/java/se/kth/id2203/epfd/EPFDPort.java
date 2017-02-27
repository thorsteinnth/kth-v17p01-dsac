package se.kth.id2203.epfd;

import se.kth.id2203.epfd.event.Restore;
import se.kth.id2203.epfd.event.Suspect;
import se.kth.id2203.epfd.event.SystemStable;
import se.kth.id2203.overlay.Topology;
import se.sics.kompics.PortType;

public final class EPFDPort extends PortType{
    public EPFDPort() {
        indication(Suspect.class);
        indication(Restore.class);
        indication(SystemStable.class);
        request(Topology.class);
    }
}
