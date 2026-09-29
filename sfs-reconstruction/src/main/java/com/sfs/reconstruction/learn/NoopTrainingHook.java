package com.sfs.reconstruction.learn;

import com.sfs.reconstruction.ConstraintInterface;
import com.sfs.reconstruction.ModelInput;
import com.sfs.reconstruction.ModelOutput;

public final class NoopTrainingHook implements TrainingHook {

    public static final String HOOK_ID = "sfs-reconstruction/noop-training-hook/0.1";

    @Override
    public String hookId() {
        return HOOK_ID;
    }

    @Override
    public void onReconstruction(ModelInput input, ModelOutput output,
                                 ConstraintInterface.Result result) {
    }
}
