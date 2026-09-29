package com.sfs.reconstruction;

import com.sfs.reconstruction.encode.UnifiedRepresentation;

public interface Decoder {

    String decoderId();

    String decode(UnifiedRepresentation representation);
}
