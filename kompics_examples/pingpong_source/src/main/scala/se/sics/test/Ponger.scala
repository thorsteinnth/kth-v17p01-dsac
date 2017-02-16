package se.sics.test

import se.sics.kompics.sl._

import se.sics.kompics.Init
import se.sics.kompics.network.{Network, Transport}
import se.sics.kompics.network.netty.{ NettyNetwork, NettyInit }

import com.typesafe.scalalogging.StrictLogging

class PongerParent extends ComponentDefinition {
    val self = cfg.getValue[TAddress]("pingpong.self");
    val network = create(classOf[NettyNetwork], new NettyInit(self));
    val ponger = create(classOf[Ponger], Init.NONE);

    connect[Network](network -> ponger);
}

class Ponger extends ComponentDefinition with StrictLogging {

    val net = requires[Network];

    private var counter: Long = 0;
    private val self = cfg.getValue[TAddress]("pingpong.self");

    net uponEvent {
        case context@TMessage(_, Ping) => handle {
            counter += 1;
            logger.info(s"Got Ping #$counter!");
            trigger (TMessage(THeader(self, context.getSource, Transport.TCP), Pong) -> net)
        }
    }
}