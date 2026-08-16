package com.sfs.ui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("SFS UI application")
class SfsUiApplicationTest {

    @Value("${spring.application.name}")
    private String applicationName;

    @Test
    @DisplayName("starts the Spring application context successfully")
    void applicationContextLoads(ApplicationContext context) {
        assertThat(context).isNotNull();
    }

    @Test
    @DisplayName("loads externalised configuration from application.properties")
    void externalisedConfigurationIsLoaded() {
        assertThat(applicationName).isEqualTo("sfs-ui");
    }

    @Test
    @DisplayName("registers the entry point as a Spring bean")
    void entryPointBeanIsRegistered(ApplicationContext context) {
        assertThat(context.getBean(SfsUiApplication.class)).isNotNull();
    }
}
