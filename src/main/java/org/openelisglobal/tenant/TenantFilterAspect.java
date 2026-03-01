package org.openelisglobal.tenant;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.hibernate.Filter;
import org.hibernate.Session;

/**
 * AOP aspect that automatically enables the {@code labUnitFilter} Hibernate
 * named filter on every DAO method call when a tenant context has been
 * established by {@link TenantContextFilter}.
 *
 * <p>
 * The pointcut intercepts all methods in any {@code daoimpl} package, matching
 * the existing OpenELIS DAO naming convention (e.g.
 * {@code org.openelisglobal.analysis.daoimpl.AnalysisDAOImpl}).
 *
 * <p>
 * The filter is defined in {@code Analysis.hbm.xml} (and any other HBM files
 * that declare it) as:
 *
 * <pre>{@code
 * <filter-def name="labUnitFilter">
 *     <filter-param name="labUnitId" type="integer"/>
 * </filter-def>
 * }</pre>
 *
 * <p>
 * The filter is <b>always disabled</b> in the {@code finally} block, even on
 * exception, so the Hibernate session is left in a clean state for the next
 * caller.
 *
 * <p>
 * Bypass / skip conditions (matching {@link TenantContextFilter}):
 * <ul>
 * <li>{@code TenantContext.get() == null} – no active lab unit (unauthenticated
 * request or call from a background thread).
 * <li>{@code TenantContext.isBypassed() == true} – global admin.
 * </ul>
 *
 * <p>
 * <b>Registration note:</b> This bean is declared explicitly via
 * {@link MultitenancyConfig#tenantFilterAspect()} and must NOT carry
 * {@code @Component}. {@code AppConfig} scans all of
 * {@code org.openelisglobal}, so adding {@code @Component} here would register
 * a second bean instance, causing every DAO call to be advised twice.
 */
@Aspect
public class TenantFilterAspect {

    static final String FILTER_NAME = "labUnitFilter";
    static final String PARAM_NAME = "labUnitId";

    @PersistenceContext
    private EntityManager entityManager;

    @Around("execution(* org.openelisglobal.*.daoimpl.*.*(..))")
    public Object applyLabUnitFilter(ProceedingJoinPoint pjp) throws Throwable {
        Integer labUnitId = TenantContext.get();

        if (labUnitId == null || TenantContext.isBypassed()) {
            return pjp.proceed();
        }

        Session session;
        try {
            session = entityManager.unwrap(Session.class);
        } catch (IllegalStateException e) {
            // No active persistence context (e.g. background thread) — skip filtering
            return pjp.proceed();
        }

        // Guard against nested DAO calls: if a DAO method (A) calls another injected
        // DAO (B) through a Spring proxy, this aspect fires again on B. Without this
        // guard, B's finally block would disable the filter before A finishes.
        // Current OpenELIS DAOs do not inject other DAOs, so this is safe today, but
        // the guard makes the pattern correct for future changes.
        boolean alreadyActive = session.getEnabledFilter(FILTER_NAME) != null;
        if (!alreadyActive) {
            Filter filter = session.enableFilter(FILTER_NAME);
            filter.setParameter(PARAM_NAME, labUnitId);
        }

        try {
            return pjp.proceed();
        } finally {
            if (!alreadyActive) {
                session.disableFilter(FILTER_NAME);
            }
        }
    }
}
