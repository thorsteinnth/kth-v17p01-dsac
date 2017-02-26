package se.kth.id2203.nnar;

import se.kth.id2203.nnar.event.*;
import se.kth.id2203.overlay.Topology;
import se.sics.kompics.PortType;

public final class AtomicRegisterPort extends PortType {

    public AtomicRegisterPort() {
        request(ARReadRequest.class);
        request(ARWriteRequest.class);
        request(ARCASRequest.class);
        indication(ARReadResponse.class);
        indication(ARWriteResponse.class);
        indication(ARCASResponse.class);
        request(Topology.class);
    }
}
