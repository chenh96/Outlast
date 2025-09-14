package tech.chenh.outlast.core;

import java.util.concurrent.locks.LockSupport;

public class Parker {

    private static final long INIT = 1_000_000;

    private final long maximum;
    private final double multiplier ;

    private long time = INIT;

    public Parker(int maximum, double multiplier) {
        this.maximum = maximum * 1_000_000L;
        this.multiplier = multiplier;
    }

    public void increaseIfAbsent(boolean absent) {
        if (absent) {
            increase();
        } else {
            reset();
        }
    }

    public void increase() {
        if (time > maximum) {
            return;
        }
        time = (long) (time * multiplier);
    }

    public void reset() {
        time = INIT;
    }

    public void park() {
        LockSupport.parkNanos(time);
    }

}