package org.openelisglobal.tenant;

/**
 * Per-request tenant context backed by a ThreadLocal.
 *
 * <p>
 * Set once by {@link TenantContextFilter} at the start of every HTTP request
 * and always cleared in the filter's {@code finally} block so thread-pool
 * threads are never left with stale state.
 *
 * <p>
 * Semantics:
 * <ul>
 * <li>{@code get() == null} – no authenticated user / no active lab unit →
 * Hibernate filter is NOT applied (unauthenticated or service-layer calls
 * outside a web request).
 * <li>{@code isBypassed() == true} – global admin → Hibernate filter is NOT
 * applied (admin sees all data).
 * <li>{@code get() != null && !isBypassed()} – normal user with an active lab
 * unit → Hibernate filter IS applied with the stored lab-unit ID.
 * </ul>
 */
public final class TenantContext {

    private static final ThreadLocal<Integer> LAB_UNIT_ID = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> BYPASSED = new ThreadLocal<>();

    private TenantContext() {
    }

    /** Store the active lab-unit ID for the current request thread. */
    public static void set(int labUnitId) {
        LAB_UNIT_ID.set(labUnitId);
    }

    /** Return the active lab-unit ID, or {@code null} if not set. */
    public static Integer get() {
        return LAB_UNIT_ID.get();
    }

    /**
     * Mark the current thread as admin-bypassed. The Hibernate filter will not be
     * applied for the remainder of this request.
     */
    public static void bypass() {
        BYPASSED.set(Boolean.TRUE);
    }

    /**
     * Returns {@code true} when the current thread has been marked as
     * admin-bypassed.
     */
    public static boolean isBypassed() {
        return Boolean.TRUE.equals(BYPASSED.get());
    }

    /**
     * Remove all state for the current thread. <b>Must</b> be called in a
     * {@code finally} block at the end of every HTTP request (done by
     * {@link TenantContextFilter}).
     */
    public static void clear() {
        LAB_UNIT_ID.remove();
        BYPASSED.remove();
    }
}
