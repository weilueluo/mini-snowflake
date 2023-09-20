import java.time.Clock;
import java.time.Instant;
import java.time.InstantSource;
import java.time.ZoneOffset;

/*
 * Generate a 64 bits sequence number based on Twitter's snowflake.
 * - 1 bit: sign.
 * - 41 bits: timestamp since {@Value START_TIME} in milliseconds, 2^41/1000/60/60/24/365=69.73 years, starting 2023-01-01T00:00:00Z
 * - 4 bits: for workers, max 2^4=16 workers.
 * - 18 bits: same millis sequence, max 2^18=262144 concurrency.
 *
 * **Note**
 * Depending on your machine either sequence's 18 bits or cost of synchronized keyword will be the bottleneck.
 * */
public class Generator {

    private final static long TIMESTAMP_BITS = 41L;
    private final static long WORKER_BITS = 4L;
    private final static long SEQUENCE_BITS = 18L;
    private static final long START_MILLIS = Instant.parse("2023-01-01T00:00:00Z").toEpochMilli();
    private static final long MAX_SEQUENCE = 2 ^ SEQUENCE_BITS;
    private static final long TIMESTAMP_SHIFT = 63 - TIMESTAMP_BITS;
    private static final long WORKER_SHIFT = TIMESTAMP_SHIFT - WORKER_BITS;

    static {
        // sanity check
        if ((TIMESTAMP_BITS + WORKER_BITS + SEQUENCE_BITS + 1) != 64) {
            throw new IllegalArgumentException("timestamp + worker + sequence bits != 64 bits, please set the variable properly.");
        }
    }

    private final Clock clock = InstantSource.system().withZone(ZoneOffset.UTC);
    private final long workerId = 0;

    private long sequence = 0;
    private long lastTimestamp;

    public Generator() {
        // sanity check
        if (workerId >= (2 ^ WORKER_BITS)) {
            throw new IllegalArgumentException("worker id must be less than: " + (2 ^ WORKER_BITS));
        }
    }

    public synchronized Long next() {
        long timestamp = timeGen();
        if (timestamp == lastTimestamp) {
            sequence += 1;
            if (sequence == MAX_SEQUENCE) {
                // overflow, wait until next millis
                timestamp = untilNextTime(timestamp);
                sequence = 0;
                lastTimestamp = timestamp;
            }
        } else {
            sequence = 0;
            lastTimestamp = timestamp;
        }

        return (timestamp << TIMESTAMP_SHIFT) + (workerId << WORKER_SHIFT) + sequence;
    }

    private long untilNextTime(long currTimestamp) {
        long timestamp = timeGen();
        while (timestamp == currTimestamp) {
            timestamp = timeGen();
        }
        return timestamp;
    }

    private long timeGen() {
        return clock.millis() - START_MILLIS;
    }
}