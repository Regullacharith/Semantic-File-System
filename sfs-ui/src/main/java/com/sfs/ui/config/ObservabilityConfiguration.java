package com.sfs.ui.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.util.Arrays;

@Configuration
public class ObservabilityConfiguration {

    @Bean
    public com.sfs.core.observe.ObservabilityRegistry observabilityRegistry() {
        return new com.sfs.core.observe.ObservabilityRegistry();
    }

    @Bean
    public FilterRegistrationBean<TraceIdFilter> traceIdFilter(
            com.sfs.core.observe.ObservabilityRegistry observabilityRegistry) {
        FilterRegistrationBean<TraceIdFilter> registration =
                new FilterRegistrationBean<>(new TraceIdFilter(observabilityRegistry));
        registration.addUrlPatterns("/*");
        registration.setOrder(1);
        return registration;
    }

    @Bean
    public EffectiveConfiguration effectiveConfiguration(
            Environment environment,
            @Value("${server.address}") String serverAddress,
            @Value("${server.port}") String serverPort,
            @Value("${sfs.memory.path}") String memoryPath,
            @Value("${sfs.security.keys-dir}") String keysDir,
            @Value("${sfs.security.secure-dir}") String secureDir,
            @Value("${SFS_ALLOW_NON_LOOPBACK:false}") String allowNonLoopback) {
        return EffectiveConfiguration.of(serverAddress, serverPort, memoryPath,
                keysDir, secureDir,
                String.join(",", Arrays.asList(environment.getActiveProfiles())),
                allowNonLoopback);
    }
}
