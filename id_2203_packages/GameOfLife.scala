
import se.sics.kompics.sl._;
import se.sics.kompics.{ Kompics, KompicsEvent, Start };
import spray.json._

object State {
    type Initializer = ((Int, Int) => State);
}
trait State

case object Alive extends State {
    override def toString(): String = "x";
}
case object Dead extends State {
    override def toString(): String = " ";
}
case object Unknown extends State {
    override def toString(): String = "?";
}
case class BroadcastState(val generation: Long, val x: Int, val y: Int, val state: State) extends KompicsEvent
case class Progress(val generation: Long) extends KompicsEvent

object EnvironmentPort extends Port {
    indication[BroadcastState];
    indication[Progress];
}

case class GameOfLifeInit(
    initializer: State.Initializer,
    cellType: Class[Cell], cellInit: Cell.Initializer,
    numGenerations: Int = 100,
    size: Int = 20) extends se.sics.kompics.Init[ParentC]

object Cell {
    type Initializer = Tuple3[Int, Int, State.Initializer] => se.sics.kompics.Init[Cell]
}

abstract class Cell extends ComponentDefinition {

}

class ParentC(init: GameOfLifeInit) extends ComponentDefinition {
    import StateJsonProtocol._
    
    val envOut = provides(EnvironmentPort);
    val envIn = requires(EnvironmentPort);

    val size = init.size;

    val grid = Array.tabulate(size, size)((i, j) => {
        create(init.cellType, init.cellInit((i, j, init.initializer)));
    });
    val wrap = stateWrap(size);
    for (i <- 0 until size) {
        for (j <- 0 until size) {
            val centre = grid(i)(j);
            connect(EnvironmentPort)(this.getComponentCore -> centre);
            connect(EnvironmentPort)(centre -> this.getComponentCore);
            connect(EnvironmentPort)(grid(wrap(i - 1))(wrap(j - 1)) -> centre);
            connect(EnvironmentPort)(grid(wrap(i - 1))(j) -> centre);
            connect(EnvironmentPort)(grid(wrap(i - 1))(wrap(j + 1)) -> centre);
            connect(EnvironmentPort)(grid(i)(wrap(j - 1)) -> centre);
            connect(EnvironmentPort)(grid(i)(wrap(j + 1)) -> centre);
            connect(EnvironmentPort)(grid(wrap(i + 1))(wrap(j - 1)) -> centre);
            connect(EnvironmentPort)(grid(wrap(i + 1))(j) -> centre);
            connect(EnvironmentPort)(grid(wrap(i + 1))(wrap(j + 1)) -> centre);
        }
    }
    val stateGrid = Array.fill[State](size, size) { Unknown }

    val stateHistory = collection.mutable.ArrayBuffer.empty[Array[Array[State]]];
    val gridSize = size * size;
    var broadcastCount = 0l;

    private var generation = -1l

    ctrl uponEvent {
        case _: Start => handle {
            println("Starting generation 0...");
            generation = 0;
            trigger(Progress(generation) -> envOut);
        }
    }
    envIn uponEvent {
        case BroadcastState(gen, x, y, state) => handle {
            if (x >= 0 && y >= 0 && x < size && y < size) {
                stateGrid(x)(y) = state;
                broadcastCount += 1;
            }
            if (broadcastCount >= gridSize) {
                val sgc = stateGrid.map(_.clone);
                stateHistory += sgc;
                broadcastCount = 0l;
                if (generation < init.numGenerations) {
                    generation += 1;
                    trigger(Progress(generation) -> envOut);
                } else {
                    val hist = stateHistory.toList;
                    val js = hist.toJson.compactPrint;
                    HTMLRenderer.render(js);
                    Kompics.asyncShutdown();
                }
            }
        }
    }
}