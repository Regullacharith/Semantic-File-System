package com.sfs.contracts.lifecycle;

import java.util.List;

public interface LifecycleAuditService {

    List<LifecycleAuditEntry> eventsFor(String objectId);

    LifecycleStatistics statistics();
}
