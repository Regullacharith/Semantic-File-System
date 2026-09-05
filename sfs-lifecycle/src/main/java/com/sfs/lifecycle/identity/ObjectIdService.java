package com.sfs.lifecycle.identity;

import com.sfs.core.identity.ObjectId;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;
import java.util.random.RandomGenerator;

public final class ObjectIdService {

    private final AtomicLong sequence = new AtomicLong();
    private final RandomGenerator random;

    public ObjectIdService() {
        this(RandomGenerator.of("L64X128MixRandom"));
    }

    public ObjectIdService(RandomGenerator random) {
        this.random = random;
    }

    public ObjectId next() {
        return nextUnique(ignored -> false);
    }

    public ObjectId nextUnique(Predicate<String> taken) {
        String candidate;
        do {
            candidate = "sfs-obj-%04d-%08x".formatted(
                    sequence.incrementAndGet(),
                    random.nextLong() & 0xFFFFFFFFL);
        } while (taken.test(candidate));
        return ObjectId.of(candidate);
    }

    public long allocatedCount() {
        return sequence.get();
    }
}
