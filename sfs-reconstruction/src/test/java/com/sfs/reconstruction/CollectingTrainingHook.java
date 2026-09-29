package com.sfs.reconstruction;

import com.sfs.reconstruction.learn.TrainingHook;

import java.util.ArrayList;
import java.util.List;

public final class CollectingTrainingHook implements TrainingHook {

    private final List<String> received = new ArrayList<>();

    @Override
    public String hookId() {
        return "collecting-training-hook/0.1";
    }

    @Override
    public synchronized void onReconstruction(ModelInput input, ModelOutput output,
                                              ConstraintInterface.Result result) {
        received.add(input.objectId() + ":" + output.modelId() + ":"
                + result.satisfied());
    }

    public synchronized List<String> received() {
        return List.copyOf(received);
    }
}
