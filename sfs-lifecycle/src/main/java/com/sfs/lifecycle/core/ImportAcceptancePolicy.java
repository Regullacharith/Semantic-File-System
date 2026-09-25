package com.sfs.lifecycle.core;

import java.util.Optional;

public interface ImportAcceptancePolicy {

    Optional<String> rejectionReason(String fileName, String contentType);
}
