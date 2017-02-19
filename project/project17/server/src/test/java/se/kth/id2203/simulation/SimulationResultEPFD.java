package se.kth.id2203.simulation;

import se.kth.id2203.epfd.Suspect;
import se.kth.id2203.networking.NetAddress;

import java.util.HashSet;
import java.util.Set;

public class SimulationResultEPFD {

    private static Set<NetAddress> suspected;
    public static SimulationResultEPFD instance = null;

    public synchronized static SimulationResultEPFD getInstance() {

        if (instance == null) {
            instance =  new SimulationResultEPFD();
        }

        return instance;
    }

    private SimulationResultEPFD() {
        this.suspected = new HashSet<>();
    }

    public void addSuspect(NetAddress suspect) {
        suspected.add(suspect);
    }

    public void removeSuspect(NetAddress suspect) {
        suspected.remove(suspect);
    }

    public Set<NetAddress> getSuspected() {
        return suspected;
    }
}
