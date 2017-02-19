package se.kth.id2203.epfd;

import se.sics.kompics.timer.ScheduleTimeout;
import se.sics.kompics.timer.Timeout;

public class CheckTimeout extends Timeout{

    public CheckTimeout(ScheduleTimeout timeout) {
        super(timeout);
    }
}
