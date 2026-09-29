package com.sfs.reconstruction;

import java.util.List;

public interface ConstraintInterface {

    Result judge(ModelInput input, String candidateDraft);

    record Result(List<String> violations, List<String> warnings) {

        public Result {
            violations = List.copyOf(violations);
            warnings = List.copyOf(warnings);
        }

        public boolean satisfied() {
            return violations.isEmpty();
        }
    }
}
