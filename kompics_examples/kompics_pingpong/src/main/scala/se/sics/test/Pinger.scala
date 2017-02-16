package se.sics.test

import se.sics.kompics.sl._

import se.sics.kompics.network.{ Network, Transport }
import se.sics.kompics.network.netty.{ NettyNetwork, NettyInit }
import se.sics.kompics.timer.{ Timer, SchedulePeriodicTimeout, Timeout, CancelPeriodicTimeout }
import se.sics.kompics.timer.java.JavaTimer
import se.sics.kompics.Start

import com.typesafe.scalalogging.StrictLogging
import java.util.UUID

class PingerParent extends ComponentDefinition {
    val self = cfg.getValue[TAddress]("pingpong.self");
    val ponger = cfg.getValue[TAddress]("pingpong.pinger.pongeraddr");
    
    val timer = create(classOf[JavaTimer], se.sics.kompics.Init.NONE);
    val network = create(classOf[NettyNetwork], new NettyInit(self));
    val pinger = create(classOf[Pinger], Init[Pinger](ponger));

    connect[Network](network -> pinger);
    connect[Timer](timer -> pinger);
}

case class PingTimeout(spt: SchedulePeriodicTimeout) extends Timeout(spt)

class Pinger(init: Init[Pinger]) extends ComponentDefinition with StrictLogging {
    val net = requires[Network];
    val timer = requires[Timer];

    private val self = cfg.getValue[TAddress]("pingpong.self");
    private val ponger = init match {
        case Init(pongerAddr: TAddress) => pongerAddr
    }
    private var counter: Long = 0;
    private var timerId: Option[UUID] = None;

    ctrl uponEvent {
        case _: Start => handle {
            val period = cfg.getValue[Long]("pingpong.pinger.timeout");
            val spt = new SchedulePeriodicTimeout(0, period);
            val timeout = PingTimeout(spt);
            spt.setTimeoutEvent(timeout);
            trigger(spt -> timer);
            timerId = Some(timeout.getTimeoutId());
        }
    }

    net uponEvent {
        case context @ TMessage(_, Pong) => handle {
            counter += 1;
            logger.info(s"Got Pong #$counter!");
        }
    }

    timer uponEvent {
        case PingTimeout(_) => handle {
            trigger(TMessage(THeader(self, ponger, Transport.TCP), Ping) -> net);
        }
    }

    override def tearDown(): Unit = {
        timerId match {
            case Some(id) =>
                trigger(new CancelPeriodicTimeout(id) -> timer);
            case None => // nothing
        }
    }
}