package com.sfs.security;

public interface KeyManager {

    String currentKeyId();

    byte[] currentKey();
}
