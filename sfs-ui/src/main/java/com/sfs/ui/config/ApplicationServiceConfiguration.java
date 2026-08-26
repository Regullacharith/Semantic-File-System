package com.sfs.ui.config;

import com.sfs.app.service.EvaluationApplicationService;
import com.sfs.app.service.FileApplicationService;
import com.sfs.app.service.ReconstructionApplicationService;
import com.sfs.app.service.SearchApplicationService;
import com.sfs.app.service.SecurityApplicationService;
import com.sfs.contracts.evaluation.EvaluationService;
import com.sfs.contracts.security.AuthenticationService;
import com.sfs.contracts.security.AuthorizationService;
import com.sfs.contracts.file.FileService;
import com.sfs.contracts.reconstruction.ReconstructionService;
import com.sfs.contracts.search.SearchService;
import com.sfs.contracts.security.SecuritySettingsService;
import com.sfs.contracts.semantic.SemanticRecordService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApplicationServiceConfiguration {

    @Bean
    public FileApplicationService fileApplicationService(
            FileService fileService,
            SemanticRecordService semanticRecordService,
            AuthenticationService authenticationService,
            AuthorizationService authorizationService) {

        return new FileApplicationService(
                fileService, semanticRecordService, authenticationService, authorizationService);
    }

    @Bean
    public SearchApplicationService searchApplicationService(SearchService searchService) {
        return new SearchApplicationService(searchService);
    }

    @Bean
    public ReconstructionApplicationService reconstructionApplicationService(
            ReconstructionService reconstructionService) {
        return new ReconstructionApplicationService(reconstructionService);
    }

    @Bean
    public EvaluationApplicationService evaluationApplicationService(
            EvaluationService evaluationService) {
        return new EvaluationApplicationService(evaluationService);
    }

    @Bean
    public SecurityApplicationService securityApplicationService(
            SecuritySettingsService securitySettingsService) {
        return new SecurityApplicationService(securitySettingsService);
    }
}
