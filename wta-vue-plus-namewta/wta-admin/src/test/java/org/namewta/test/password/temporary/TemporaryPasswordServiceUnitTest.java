package org.namewta.test.password.temporary;

import cn.hutool.crypto.digest.BCrypt;
import org.namewta.system.password.PasswordPolicyService;
import org.namewta.system.temporarypassword.TemporaryPasswordService;
import org.namewta.system.temporarypassword.TemporaryPasswordStore;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Tag("dev")
class TemporaryPasswordServiceUnitTest {

    @Test
    void issueStoresOnlyBcryptForExactlyOneMinute() {
        PasswordPolicyService policy = mock(PasswordPolicyService.class);
        when(policy.generateTemporaryPassword()).thenReturn("Temporary1!");
        RecordingStore store = new RecordingStore();
        TemporaryPasswordService service = new TemporaryPasswordService(policy, store);

        TemporaryPasswordService.IssuedPassword issued = service.issue(42L);

        assertEquals("Temporary1!", issued.password());
        assertEquals(60, issued.expiresInSeconds());
        assertEquals(Duration.ofSeconds(60), store.ttl);
        assertNotEquals(issued.password(), store.hash);
        assertTrue(BCrypt.checkpw(issued.password(), store.hash));
        assertFalse(issued.toString().contains(issued.password()));
    }

    @Test
    void wrongValueDoesNotConsumeWhileVerifiedValueUsesCompareAndDelete() {
        PasswordPolicyService policy = mock(PasswordPolicyService.class);
        RecordingStore store = new RecordingStore();
        store.hash = BCrypt.hashpw("Temporary1!");
        TemporaryPasswordService service = new TemporaryPasswordService(policy, store);

        assertTrue(service.verify(42L, "wrong-value").isEmpty());
        assertEquals(0, store.compareAndDeleteCalls);

        TemporaryPasswordService.VerifiedPassword verified =
            service.verify(42L, "Temporary1!").orElseThrow();
        assertFalse(verified.toString().contains(store.hash));
        assertTrue(service.consume(verified));
        assertEquals(1, store.compareAndDeleteCalls);
        assertNull(store.hash);
    }

    @Test
    void policyFailureBlocksIssueButExistingHashStillVerifies() {
        PasswordPolicyService policy = mock(PasswordPolicyService.class);
        doThrow(new org.namewta.common.core.exception.ServiceException("PASSWORD_POLICY_UNAVAILABLE"))
            .when(policy).generateTemporaryPassword();
        RecordingStore store = new RecordingStore();
        store.hash = BCrypt.hashpw("Temporary1!");
        TemporaryPasswordService service = new TemporaryPasswordService(policy, store);

        assertThrows(org.namewta.common.core.exception.ServiceException.class, () -> service.issue(42L));
        assertTrue(service.verify(42L, "Temporary1!").isPresent());
    }

    @Test
    void redisFailureFailsVerificationClosedWithoutProcessFallback() {
        PasswordPolicyService policy = mock(PasswordPolicyService.class);
        TemporaryPasswordStore store = mock(TemporaryPasswordStore.class);
        when(store.read(42L)).thenThrow(new IllegalStateException("redis unavailable"));
        TemporaryPasswordService service = new TemporaryPasswordService(policy, store);

        assertTrue(service.verify(42L, "Temporary1!").isEmpty());
    }

    private static final class RecordingStore implements TemporaryPasswordStore {
        private String hash;
        private Duration ttl;
        private int compareAndDeleteCalls;

        @Override
        public void store(Long userId, String passwordHash, Duration timeToLive) {
            this.hash = passwordHash;
            this.ttl = timeToLive;
        }

        @Override
        public String read(Long userId) {
            return hash;
        }

        @Override
        public boolean compareAndDelete(Long userId, String expectedHash) {
            compareAndDeleteCalls++;
            if (!expectedHash.equals(hash)) {
                return false;
            }
            hash = null;
            return true;
        }
    }
}
