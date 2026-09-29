package com.sfs.reconstruction;

public interface SFSReconstructionModel {

    String modelId();

    ModelOutput reconstruct(ModelInput input);
}
