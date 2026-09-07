package com.sfs.lifecycle.store;

import java.util.Optional;

public interface RawContentStore {

    void store(String objectId, byte[] content);

    Optional<byte[]> retrieve(String objectId);

    boolean release(String objectId);

    boolean contains(String objectId);
}
