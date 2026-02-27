package org.openelisglobal.tenant;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Servlet filter that populates {@link TenantContext} for every HTTP request.
 *
 * <p>
 * Runs once per request, after Spring Security has processed the request (so
 * the HTTP session is already established). Always clears {@link TenantContext}
 * in a {@code finally} block to prevent thread-pool thread leakage.
 *
 * <p>
 * Logic:
 * <ol>
 * <li>No active session or no {@link UserSessionData} → skip (context stays
 * null, filter not applied by {@link TenantFilterAspect}).
 * <li>{@code usd.isAdmin() == true} → {@link TenantContext#bypass()} — admin
 * sees all data.
 * <li>{@code usd.getLoginLabUnit() == 0} → skip — user has not selected a lab
 * unit yet.
 * <li>Otherwise → {@link TenantContext#set(int)} with the active lab-unit ID.
 * </ol>
 *
 * <p>
 * Registered as a Spring bean in {@link MultitenancyConfig} and added to the
 * Spring Security filter chains in
 * {@link org.openelisglobal.security.SecurityConfig}.
 */
public class TenantContextFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            populateTenantContext(request);
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    private void populateTenantContext(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return;
        }

        UserSessionData usd = (UserSessionData) session.getAttribute(IActionConstants.USER_SESSION_DATA);
        if (usd == null) {
            return;
        }

        if (usd.isAdmin()) {
            TenantContext.bypass();
            return;
        }

        int loginLabUnit = usd.getLoginLabUnit();
        if (loginLabUnit != 0) {
            TenantContext.set(loginLabUnit);
        }
    }
}
