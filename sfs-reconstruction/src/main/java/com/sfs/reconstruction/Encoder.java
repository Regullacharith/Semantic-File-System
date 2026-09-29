package com.sfs.reconstruction;

public interface Encoder<I, O> {

    O encode(I source);
}
