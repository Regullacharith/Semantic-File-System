package com.sfs.security;

import com.sfs.contracts.security.SecuritySettingsView.AuditEventView;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public final class SecurityAuditLog {

    private static final int CAPACITY = 100;
    private static final DateTimeFormatter TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneOffset.UTC);

    private final ArrayDeque<AuditEventView> events = new ArrayDeque<>(CAPACITY);
    private final AtomicLong total = new AtomicLong();

    public synchronized void record(String eventType, String detail, boolean permitted) {
        if (total.incrementAndGet() > CAPACITY) {
            events.pollFirst();
        }
        events.addLast(new AuditEventView(
                TIMESTAMP.format(Instant.now()), eventType, detail, permitted));
    }

    public synchronized List<AuditEventView> events() {
        return List.copyOf(events);
    }

    public long totalEvents() {
        return total.get();
    }
}
