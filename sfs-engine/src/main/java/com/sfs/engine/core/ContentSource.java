package com.sfs.engine.core;

import java.util.Optional;

public interface ContentSource {

    Optional<byte[]> content(String objectId);
}
