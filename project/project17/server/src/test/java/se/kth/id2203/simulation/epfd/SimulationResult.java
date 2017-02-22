package se.kth.id2203.simulation.epfd;

import se.kth.id2203.networking.NetAddress;

import java.util.HashSet;
import java.util.Set;

public class SimulationResult {

    private static Set<NetAddress> suspected;
    public static SimulationResult instance = null;

    public synchronized static SimulationResult getInstance() {

        if (instance == null) {
            instance =  new SimulationResult();
        }

        return instance;
    }

    private SimulationResult() {
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
