package com.sfs.ui.config;

import com.sfs.app.service.ReconstructionApplicationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class JobRecoveryRunner implements ApplicationRunner {

    private static final Logger LOG = LoggerFactory.getLogger(JobRecoveryRunner.class);

    private final ReconstructionApplicationService reconstructionApplicationService;

    public JobRecoveryRunner(ReconstructionApplicationService reconstructionApplicationService) {
        this.reconstructionApplicationService = reconstructionApplicationService;
    }

    @Override
    public void run(ApplicationArguments args) {
        int failed = reconstructionApplicationService.jobRegistry().failOrphanedJobs();

        if (failed > 0) {
            LOG.warn("Marked {} non-terminal job(s) as FAILED at startup. "
                    + "Job state is held in memory and does not survive a restart.", failed);
        }
    }
}
