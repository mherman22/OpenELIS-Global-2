package org.openelisglobal.tenant;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * Spring configuration for the multitenancy / lab-unit data-isolation layer.
 *
 * <p>
 * Enables AspectJ proxy support so that {@link TenantFilterAspect} can
 * intercept DAO method calls and activate the Hibernate {@code labUnitFilter}
 * per request.
 *
 * <p>
 * {@link TenantContextFilter} is exposed as a bean so it can be injected into
 * {@link org.openelisglobal.security.SecurityConfig} and added to the Spring
 * Security filter chains via {@code http.addFilterAfter(...)}. Because this is
 * a WAR deployment (no Spring Boot auto-configuration), a {@code @Bean} filter
 * is NOT automatically registered in the Servlet container — registration
 * happens only through the explicit {@code addFilterAfter} calls in
 * {@code SecurityConfig}.
 */
@Configuration
@EnableAspectJAutoProxy
public class MultitenancyConfig {

    @Bean
    public TenantContextFilter tenantContextFilter() {
        return new TenantContextFilter();
    }

    @Bean
    public TenantFilterAspect tenantFilterAspect() {
        return new TenantFilterAspect();
    }
}
