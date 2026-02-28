package org.openelisglobal.tenant;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.atomic.AtomicReference;
import org.junit.After;
import org.junit.Test;

/**
 * Unit tests for {@link TenantContext}. No Spring context — pure ThreadLocal
 * behaviour is verified in isolation.
 */
public class TenantContextTest {

    @After
    public void tearDown() {
        TenantContext.clear();
    }

    @Test
    public void get_returnsNull_whenNotSet() {
        assertNull("get() must return null before any set() call", TenantContext.get());
    }

    @Test
    public void set_andGet_roundtrip() {
        TenantContext.set(42);
        assertEquals("get() must return the value passed to set()", Integer.valueOf(42), TenantContext.get());
    }

    @Test
    public void isBypassed_returnsFalse_byDefault() {
        assertFalse("isBypassed() must be false before bypass() is called", TenantContext.isBypassed());
    }

    @Test
    public void bypass_setsBypassedFlag() {
        TenantContext.bypass();
        assertTrue("isBypassed() must be true after bypass() is called", TenantContext.isBypassed());
    }

    @Test
    public void clear_removesLabUnitId() {
        TenantContext.set(7);
        TenantContext.clear();
        assertNull("get() must return null after clear()", TenantContext.get());
    }

    @Test
    public void clear_removesBypassedFlag() {
        TenantContext.bypass();
        TenantContext.clear();
        assertFalse("isBypassed() must be false after clear()", TenantContext.isBypassed());
    }

    @Test
    public void clear_isIdempotent_whenNothingWasSet() {
        // Must not throw when called on a thread that never called set() or bypass().
        TenantContext.clear();
        assertNull(TenantContext.get());
        assertFalse(TenantContext.isBypassed());
    }

    @Test
    public void threadIsolation_contextDoesNotLeakBetweenThreads() throws InterruptedException {
        TenantContext.set(99);

        AtomicReference<Integer> otherThreadValue = new AtomicReference<>();
        Thread other = new Thread(() -> otherThreadValue.set(TenantContext.get()));
        other.start();
        other.join();

        assertNull("Another thread must not see the lab-unit ID set on this thread", otherThreadValue.get());
        assertEquals("Original thread value must be unchanged", Integer.valueOf(99), TenantContext.get());
    }
}
