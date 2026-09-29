package com.sfs.reconstruction.learn;

import com.sfs.reconstruction.ConstraintInterface;
import com.sfs.reconstruction.ModelInput;
import com.sfs.reconstruction.ModelOutput;

public interface TrainingHook {

    String hookId();

    void onReconstruction(ModelInput input, ModelOutput output,
                          ConstraintInterface.Result result);
}
