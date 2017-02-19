package se.kth.id2203;

import com.google.common.base.Optional;
import se.kth.id2203.bootstrapping.BootstrapClient;
import se.kth.id2203.bootstrapping.BootstrapServer;
import se.kth.id2203.bootstrapping.Bootstrapping;
import se.kth.id2203.broadcast.beb.BestEffortBroadcast;
import se.kth.id2203.broadcast.beb.BestEffortBroadcastPort;
import se.kth.id2203.broadcast.rb.ReliableBroadcast;
import se.kth.id2203.broadcast.rb.ReliableBroadcastPort;
import se.kth.id2203.epfd.EPFD;
import se.kth.id2203.epfd.EPFDPort;
import se.kth.id2203.kvstore.KVService;
import se.kth.id2203.networking.NetAddress;
import se.kth.id2203.overlay.Routing;
import se.kth.id2203.overlay.VSOverlayManager;
import se.sics.kompics.Channel;
import se.sics.kompics.Component;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Init;
import se.sics.kompics.Positive;
import se.sics.kompics.network.Network;
import se.sics.kompics.timer.Timer;

public class ParentComponent
        extends ComponentDefinition {

    //******* Ports ******
    protected final Positive<Network> net = requires(Network.class);
    protected final Positive<Timer> timer = requires(Timer.class);
    //******* Children ******
    protected final Component overlay = create(VSOverlayManager.class, Init.NONE);
    protected final Component kv = create(KVService.class, Init.NONE);
    protected final Component boot;
    protected final Component beb = create(BestEffortBroadcast.class, Init.NONE);
    protected final Component rb = create(ReliableBroadcast.class, Init.NONE);
    protected final Component epfd = create(EPFD.class, Init.NONE);

    {

        Optional<NetAddress> serverO = config().readValue("id2203.project.bootstrap-address", NetAddress.class);
        if (serverO.isPresent()) { // start in client mode
            boot = create(BootstrapClient.class, Init.NONE);
        } else { // start in server mode
            boot = create(BootstrapServer.class, Init.NONE);
        }
        connect(timer, boot.getNegative(Timer.class), Channel.TWO_WAY);
        connect(net, boot.getNegative(Network.class), Channel.TWO_WAY);
        // Overlay
        connect(boot.getPositive(Bootstrapping.class), overlay.getNegative(Bootstrapping.class), Channel.TWO_WAY);
        connect(beb.getPositive(BestEffortBroadcastPort.class), overlay.getNegative(BestEffortBroadcastPort.class), Channel.TWO_WAY);
        connect(rb.getPositive(ReliableBroadcastPort.class), overlay.getNegative(ReliableBroadcastPort.class), Channel.TWO_WAY);
        connect(epfd.getPositive(EPFDPort.class), overlay.getNegative(EPFDPort.class), Channel.TWO_WAY);
        connect(net, overlay.getNegative(Network.class), Channel.TWO_WAY);
        // KV
        connect(overlay.getPositive(Routing.class), kv.getNegative(Routing.class), Channel.TWO_WAY);
        connect(rb.getPositive(ReliableBroadcastPort.class), kv.getNegative(ReliableBroadcastPort.class), Channel.TWO_WAY);
        connect(net, kv.getNegative(Network.class), Channel.TWO_WAY);
        // Best effort broadcast
        connect(net, beb.getNegative(Network.class), Channel.TWO_WAY);
        // Reliable broadcast
        connect(beb.getPositive(BestEffortBroadcastPort.class), rb.getNegative(BestEffortBroadcastPort.class), Channel.TWO_WAY);
        // Eventually perfect failure detector
        connect(net, epfd.getNegative(Network.class), Channel.TWO_WAY);
        connect(timer, epfd.getNegative(Timer.class), Channel.TWO_WAY);
    }
}
