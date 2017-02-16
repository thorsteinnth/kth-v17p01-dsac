package se.sics.test

import se.sics.kompics.network.netty.serialization.Serializers
import se.sics.kompics.config.Conversions
import se.sics.kompics.Kompics
import com.typesafe.scalalogging.StrictLogging

object Main extends StrictLogging {

    // register serializer
    Serializers.register(PickleSerializer, "pickleS");
    // map types to serializer
    Serializers.register(classOf[TAddress], "pickleS");
    Serializers.register(classOf[THeader], "pickleS");
    Serializers.register(classOf[TMessage[_]], "pickleS");
    // conversions
    Conversions.register(TAddressConverter);

    def main(args: Array[String]): Unit = {
        if (args.length == 1) {
            if (args(0).equalsIgnoreCase("ponger")) {
                Kompics.createAndStart(classOf[PongerParent], 2);
                System.out.println("Starting Ponger");
                // no shutdown this time...act like a server and keep running until externally exited
            } else if (args(0).equalsIgnoreCase("pinger")) {
                Kompics.createAndStart(classOf[PingerParent], 2);
                System.out.println("Starting Pinger");
                try {
                    Thread.sleep(10000);
                } catch {
                    case e: Throwable =>
                        logger.error("Error while pinging!", e);
                        System.exit(1);
                }
                Kompics.shutdown();
                System.exit(0);
            }
        } else {
            System.err.println("Invalid number of parameters");
            System.exit(1);
        }
    }
}