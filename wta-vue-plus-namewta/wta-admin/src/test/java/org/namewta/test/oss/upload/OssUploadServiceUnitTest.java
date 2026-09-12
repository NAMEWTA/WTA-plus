package org.namewta.test.oss.upload;

import org.namewta.common.oss.enums.AccessPolicy;
import org.namewta.common.oss.model.*;
import org.namewta.system.oss.readiness.OssStorageReadinessEntry;
import org.namewta.system.oss.readiness.OssStorageReadinessProperties;
import org.namewta.system.oss.readiness.OssStorageReadinessRegistry;
import org.namewta.system.oss.upload.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.namewta.system.oss.upload.OssUploadContracts.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * SINGLE/Multipart 上传控制面状态机测试。
 */
@Tag("dev")
class OssUploadServiceUnitTest {

    private OssUploadProperties properties;
    private MutableIdentity identity;
    private MemoryTicketStore tickets;
    private FakeObjectStore objects;
    private MemoryMetadataStore metadata;
    private OssStorageReadinessRegistry readiness;
    private OssUploadService service;

    @BeforeEach
    void setUp() {
        properties = OssUploadPropertiesUnitTest.validProperties();
        properties.validate();
        identity = new MutableIdentity(7L, 100L, true);
        tickets = new MemoryTicketStore();
        objects = new FakeObjectStore();
        metadata = new MemoryMetadataStore();
        readiness = new OssStorageReadinessRegistry(new OssStorageReadinessProperties());
        replaceReadiness("minio", AccessPolicy.PRIVATE, OssStorageReadinessEntry.Status.SERVING);
        service = new OssUploadService(properties, identity, tickets, objects, metadata, readiness);
    }

    @Test
    void shouldRouteInitUsingServerPolicyStorageBinding() {
        service.init(new InitRequest("general", "private.bin", 8,
            "application/octet-stream", "fp-private-route"));
        assertEquals("minio", objects.preparedStorageConfigKey);
        assertEquals(AccessPolicy.PRIVATE, objects.preparedExpectedAccessPolicy);

        properties.requirePolicy("general").setStorageConfigKey("portal");
        properties.requirePolicy("general").setExpectedAccessPolicy(AccessPolicy.PUBLIC_READ);
        replaceReadiness("portal", AccessPolicy.PUBLIC_READ, OssStorageReadinessEntry.Status.SERVING);
        service.init(new InitRequest("general", "public.bin", 8,
            "application/octet-stream", "fp-public-route"));

        assertEquals("portal", objects.preparedStorageConfigKey);
        assertEquals(AccessPolicy.PUBLIC_READ, objects.preparedExpectedAccessPolicy);
        assertEquals(2, objects.prepareCalls.get());
    }

    @Test
    void shouldRejectUnreadyOrMismatchedStorageBeforeProviderAndTicketSideEffects() {
        replaceReadiness("minio", AccessPolicy.PRIVATE, OssStorageReadinessEntry.Status.NOT_SERVING);
        OssUploadException unavailable = assertThrows(OssUploadException.class,
            () -> service.init(new InitRequest("general", "file.bin", 8,
                "application/octet-stream", "fp-unready")));
        assertEquals(OssUploadError.STORAGE_NOT_SERVING, unavailable.error());
        assertEquals(0, objects.prepareCalls.get());
        assertTrue(tickets.tickets.isEmpty());

        replaceReadiness("minio", AccessPolicy.PUBLIC_READ, OssStorageReadinessEntry.Status.SERVING);
        OssUploadException mismatch = assertThrows(OssUploadException.class,
            () -> service.init(new InitRequest("general", "file.bin", 8,
                "application/octet-stream", "fp-mismatch")));
        assertEquals(OssUploadError.STORAGE_ACCESS_POLICY_MISMATCH, mismatch.error());
        assertEquals(0, objects.prepareCalls.get());
        assertTrue(tickets.tickets.isEmpty());
    }

    @Test
    void shouldKeepTicketStorageRouteFrozenAfterPolicyAndReadinessChange() {
        InitResponse init = service.init(new InitRequest("general", "file.bin", 8,
            "application/octet-stream", "fp-frozen"));
        assertEquals("minio", tickets.get(init.uploadToken()).service());

        properties.requirePolicy("general").setStorageConfigKey("portal");
        properties.requirePolicy("general").setExpectedAccessPolicy(AccessPolicy.PUBLIC_READ);
        replaceReadiness("portal", AccessPolicy.PUBLIC_READ, OssStorageReadinessEntry.Status.SERVING);
        service.resume(init.uploadToken(), "fp-frozen");
        service.abort(init.uploadToken());

        assertEquals("minio", objects.lastTicketService);
        assertEquals("minio", objects.lastCleanupService);
    }

    @Test
    void shouldCompleteSingleOnceWithinOwningClient() {
        InitResponse init = service.init(new InitRequest("general", "avatar.png", 8, "image/png", "fp-1"));
        assertEquals(OssUploadMode.SINGLE, init.mode());
        assertEquals("PUT", init.presignedRequest().method());
        assertTrue(init.presignedRequest().requiredHeaders().containsKey("x-amz-meta-upload-fingerprint"));
        objects.objectPresent = true;
        objects.objectSize = 8;
        objects.contentType = "image/png";
        objects.prefix = HexFormat.of().parseHex("89504e470d0a1a0a");

        String first = service.complete(init.uploadToken(), new CompleteRequest(List.of()));
        String duplicate = service.complete(init.uploadToken(), new CompleteRequest(List.of()));

        assertEquals("1001", first);
        assertEquals(first, duplicate);
        assertEquals(1, metadata.registerCalls.get());
        assertEquals(0, objects.completeCalls.get());
        assertNull(tickets.cleanup.get(init.uploadToken()));
    }

    @Test
    void shouldSignResumeAndCompleteMultipartWithProviderParts() {
        properties.requirePolicy("general").setMode(OssUploadMode.MULTIPART);
        long partSize = properties.requirePolicy("general").getPartSize();
        long fileSize = partSize + 7;
        InitResponse init = service.init(new InitRequest("general", "archive.bin", fileSize,
            "application/octet-stream", "fp-large"));
        assertEquals(OssUploadMode.MULTIPART, init.mode());
        assertNull(init.presignedRequest());
        SignPartsResponse signed = service.signParts(init.uploadToken(), new SignPartsRequest(List.of(2, 1)));
        assertEquals(List.of(1, 2), signed.parts().stream().map(SignedPart::partNumber).toList());
        assertTrue(signed.parts().getFirst().url().contains("part=1"));

        objects.parts = List.of(part(1, "etag-1", partSize), part(2, "etag-2", 7));
        ResumeResponse resumed = service.resume(init.uploadToken(), "fp-large");
        assertEquals(2, resumed.uploadedParts().size());
        assertEquals(OssUploadError.FINGERPRINT_MISMATCH,
            assertThrows(OssUploadException.class, () -> service.resume(init.uploadToken(), "other")).error());

        objects.objectSize = fileSize;
        objects.contentType = "application/octet-stream";
        String ossId = service.complete(init.uploadToken(), new CompleteRequest(List.of(
            new CompletedPart(1, "\"etag-1\""), new CompletedPart(2, "etag-2"))));
        assertEquals("1001", ossId);
        assertEquals(1, objects.completeCalls.get());
        assertEquals(1, metadata.registerCalls.get());
    }

    @Test
    void shouldRejectOtherUserAndOtherClient() {
        InitResponse init = service.init(new InitRequest("general", "file.bin", 8,
            "application/octet-stream", "fp-owner"));
        identity.userId = 8L;
        OssUploadException error = assertThrows(OssUploadException.class,
            () -> service.resume(init.uploadToken(), "fp-owner"));
        assertEquals(OssUploadError.SESSION_OWNER_MISMATCH, error.error());

        identity.userId = 7L;
        identity.clientPk = 999L;
        assertEquals(OssUploadError.SESSION_OWNER_MISMATCH, assertThrows(OssUploadException.class,
            () -> service.resume(init.uploadToken(), "fp-owner")).error());
    }

    @Test
    void shouldRenewSinglePutAuthorizationWhenResuming() {
        InitResponse init = service.init(new InitRequest("general", "file.bin", 8,
            "application/octet-stream", "fp-single-resume"));

        ResumeResponse resumed = service.resume(init.uploadToken(), "fp-single-resume");

        assertNotNull(resumed.presignedRequest());
        assertEquals("PUT", resumed.presignedRequest().method());
    }

    @Test
    void shouldResumeCompletedTicketWithoutRenewingUploadAuthorization() throws ReflectiveOperationException {
        InitResponse init = service.init(new InitRequest("general", "file.bin", 8,
            "application/octet-stream", "fp-completed-resume"));
        objects.objectPresent = true;
        objects.objectSize = 8;
        objects.contentType = "application/octet-stream";
        assertEquals("1001", service.complete(init.uploadToken(), new CompleteRequest(List.of())));

        ResumeResponse resumed = service.resume(init.uploadToken(), "fp-completed-resume");
        Map<String, Object> values = new HashMap<>();
        for (java.lang.reflect.RecordComponent component : ResumeResponse.class.getRecordComponents()) {
            values.put(component.getName(), component.getAccessor().invoke(resumed));
        }

        assertEquals(OssUploadState.COMPLETED, values.get("state"));
        assertEquals("1001", values.get("completedOssId"));
        assertNull(resumed.presignedRequest());
        assertEquals(0, objects.presignSingleCalls.get());
    }

    @Test
    void shouldUseClientOnlyForOptionalPolicyAdmissionAtInit() {
        properties.requirePolicy("general").setAllowedClientPks(Set.of(100L));
        identity.clientPk = 200L;
        assertEquals(OssUploadError.ACCESS_DENIED, assertThrows(OssUploadException.class,
            () -> service.init(new InitRequest("general", "file.bin", 8,
                "application/octet-stream", "fp-client"))).error());
        assertTrue(tickets.tickets.isEmpty());
    }

    @Test
    void shouldDeleteBadMagicAndNeverRegisterMetadata() {
        InitResponse init = service.init(new InitRequest("general", "avatar.png", 8, "image/png", "fp-bad"));
        objects.objectPresent = true;
        objects.objectSize = 8;
        objects.contentType = "image/png";
        objects.prefix = "not-png!".getBytes();

        OssUploadException error = assertThrows(OssUploadException.class,
            () -> service.complete(init.uploadToken(), new CompleteRequest(List.of())));
        assertEquals(OssUploadError.COMPLETE_VALIDATION_FAILED, error.error());
        assertEquals(1, objects.deleteCalls.get());
        assertEquals(0, metadata.registerCalls.get());
        assertNull(tickets.get(init.uploadToken()));
    }

    @Test
    void shouldReturnExistingMetadataAfterCompletedStateWriteFailure() {
        InitResponse init = service.init(new InitRequest("general", "file.bin", 8,
            "application/octet-stream", "fp-recover"));
        objects.objectPresent = true;
        objects.objectSize = 8;
        objects.contentType = "application/octet-stream";
        tickets.failCompletedSave = true;

        assertEquals(OssUploadError.STATE_STORE_FAILURE, assertThrows(OssUploadException.class,
            () -> service.complete(init.uploadToken(), new CompleteRequest(List.of()))).error());
        tickets.failCompletedSave = false;
        assertEquals("1001", service.complete(init.uploadToken(), new CompleteRequest(List.of())));
        assertEquals(1, metadata.registerCalls.get());
    }

    @Test
    void cleanupMustNeverDeleteCompletedObjectEvenIfCleanupIndexRemains() {
        InitResponse init = service.init(new InitRequest("general", "file.bin", 8,
            "application/octet-stream", "fp-clean"));
        OssUploadTicket ticket = tickets.get(init.uploadToken());
        tickets.save(ticket.completed(1001L), Duration.ofHours(1));
        OssUploadCleanupRecord cleanup = tickets.cleanup.get(init.uploadToken());
        tickets.cleanup.put(init.uploadToken(), new OssUploadCleanupRecord(cleanup.token(), cleanup.mode(),
            cleanup.service(), cleanup.objectKey(), cleanup.uploadId(), System.currentTimeMillis() - 1));

        assertFalse(service.cleanupExpired(init.uploadToken(), false));
        assertEquals(0, objects.deleteCalls.get());
        assertNull(tickets.cleanup.get(init.uploadToken()));
    }

    @Test
    void cleanupMustPreserveRegisteredObjectWhenCompletedTicketWriteWasLost() {
        InitResponse init = service.init(new InitRequest("general", "file.bin", 8,
            "application/octet-stream", "fp-clean-recover"));
        objects.objectPresent = true;
        objects.objectSize = 8;
        objects.contentType = "application/octet-stream";
        tickets.failCompletedSave = true;
        assertEquals(OssUploadError.STATE_STORE_FAILURE, assertThrows(OssUploadException.class,
            () -> service.complete(init.uploadToken(), new CompleteRequest(List.of()))).error());
        OssUploadCleanupRecord cleanup = tickets.cleanup.get(init.uploadToken());
        tickets.cleanup.put(init.uploadToken(), new OssUploadCleanupRecord(cleanup.token(), cleanup.mode(),
            cleanup.service(), cleanup.objectKey(), cleanup.uploadId(), System.currentTimeMillis() - 1));

        assertFalse(service.cleanupExpired(init.uploadToken(), false));
        assertEquals(0, objects.deleteCalls.get());
        assertNull(tickets.get(init.uploadToken()));
        assertNull(tickets.cleanup.get(init.uploadToken()));
    }

    private static OssMultipartPart part(int number, String etag, long size) {
        return new OssMultipartPart(number, etag, size, Instant.now(), Map.of());
    }

    private void replaceReadiness(String configKey, AccessPolicy policy,
                                  OssStorageReadinessEntry.Status status) {
        Instant now = Instant.now();
        readiness.replace(Map.of(configKey, new OssStorageReadinessEntry(configKey, policy, true,
            Set.of("UPLOAD_POLICY:general"), status,
            status == OssStorageReadinessEntry.Status.SERVING
                ? OssStorageReadinessEntry.Reason.READY
                : OssStorageReadinessEntry.Reason.DIAGNOSTIC_UNVERIFIED, now)), Set.of(configKey), true);
    }

    private static final class MutableIdentity implements OssUploadIdentityResolver {
        private Long userId;
        private Long clientPk;
        private final boolean permission;

        private MutableIdentity(Long userId, Long clientPk, boolean permission) {
            this.userId = userId;
            this.clientPk = clientPk;
            this.permission = permission;
        }

        @Override
        public Identity resolve() {
            return new Identity(userId, clientPk);
        }

        @Override
        public boolean hasPermission(String ignored) {
            return permission;
        }
    }

    private static final class MemoryTicketStore implements OssUploadTicketStore {
        private final Map<String, OssUploadTicket> tickets = new ConcurrentHashMap<>();
        private final Map<String, OssUploadCleanupRecord> cleanup = new ConcurrentHashMap<>();
        private boolean failCompletedSave;

        @Override
        public void create(OssUploadTicket ticket, OssUploadCleanupRecord record, Duration ticketTtl,
                           Duration cleanupTtl) {
            tickets.put(ticket.token(), ticket);
            cleanup.put(ticket.token(), record);
        }

        @Override
        public OssUploadTicket get(String token) {
            return tickets.get(token);
        }

        @Override
        public void save(OssUploadTicket ticket, Duration ttl) {
            if (failCompletedSave && ticket.state() == OssUploadState.COMPLETED) {
                throw new IllegalStateException("redis down");
            }
            tickets.put(ticket.token(), ticket);
        }

        @Override
        public void removeCompletedCleanup(String token) {
            cleanup.remove(token);
        }

        @Override
        public void removeSession(String token) {
            tickets.remove(token);
            cleanup.remove(token);
        }

        @Override
        public List<String> findExpired(long nowEpochMilli, int limit) {
            return cleanup.values().stream().filter(value -> value.expiresAt() <= nowEpochMilli)
                .limit(limit).map(OssUploadCleanupRecord::token).toList();
        }

        @Override
        public OssUploadCleanupRecord getCleanup(String token) {
            return cleanup.get(token);
        }

        @Override
        public void scheduleCleanup(String token, long whenEpochMilli) {
        }

        @Override
        public synchronized <T> T locked(String token, Supplier<T> action) {
            return action.get();
        }
    }

    private static final class FakeObjectStore implements OssUploadObjectStore {
        private final AtomicInteger completeCalls = new AtomicInteger();
        private final AtomicInteger deleteCalls = new AtomicInteger();
        private final AtomicInteger presignSingleCalls = new AtomicInteger();
        private final AtomicInteger prepareCalls = new AtomicInteger();
        private String preparedStorageConfigKey;
        private AccessPolicy preparedExpectedAccessPolicy;
        private String lastTicketService;
        private String lastCleanupService;
        private boolean objectPresent;
        private long objectSize;
        private String contentType;
        private String fingerprintDigest;
        private byte[] prefix = new byte[0];
        private List<OssMultipartPart> parts = List.of();

        @Override
        public PreparedUpload prepare(String storageConfigKey, AccessPolicy expectedAccessPolicy,
                                      String objectPrefix, String fileName, String contentType,
                                      String fingerprintDigest, OssUploadMode mode, Duration presignTtl) {
            prepareCalls.incrementAndGet();
            preparedStorageConfigKey = storageConfigKey;
            preparedExpectedAccessPolicy = expectedAccessPolicy;
            this.contentType = contentType;
            this.fingerprintDigest = fingerprintDigest;
            OssPresignedRequest request = mode == OssUploadMode.SINGLE
                ? new OssPresignedRequest("PUT", "https://oss.example/object",
                    Map.of("x-amz-meta-upload-fingerprint", fingerprintDigest), Instant.now().plus(presignTtl)) : null;
            return new PreparedUpload(storageConfigKey, storageConfigKey + "-bucket", objectPrefix + "/key.bin",
                mode == OssUploadMode.MULTIPART ? "upload-1" : null, request);
        }

        @Override
        public List<SignedPart> signParts(OssUploadTicket ticket, List<Integer> numbers, Duration ttl) {
            return numbers.stream().map(number -> new SignedPart(number, "PUT",
                "https://oss.example/object?part=" + number, Map.of(), Instant.now().plus(ttl))).toList();
        }

        @Override
        public OssPresignedRequest presignSingle(OssUploadTicket ticket, Duration ttl) {
            presignSingleCalls.incrementAndGet();
            lastTicketService = ticket.service();
            return new OssPresignedRequest("PUT", "https://oss.example/object",
                Map.of("x-amz-meta-upload-fingerprint", ticket.fingerprintDigest()), Instant.now().plus(ttl));
        }

        @Override
        public List<OssMultipartPart> listParts(OssUploadTicket ticket) {
            return parts;
        }

        @Override
        public void completeMultipart(OssUploadTicket ticket, List<OssCompletedPart> completedParts) {
            completeCalls.incrementAndGet();
            objectPresent = true;
        }

        @Override
        public Optional<OssObjectStat> headIfPresent(OssUploadTicket ticket) {
            return objectPresent ? Optional.of(new OssObjectStat("bucket-a", ticket.objectKey(), objectSize,
                contentType, "etag", Instant.now(), Map.of("upload-fingerprint", fingerprintDigest), Map.of()))
                : Optional.empty();
        }

        @Override
        public byte[] readPrefix(OssUploadTicket ticket, int length) {
            return Arrays.copyOf(prefix, Math.min(prefix.length, length));
        }

        @Override
        public void abort(OssUploadCleanupRecord cleanup) {
            lastCleanupService = cleanup.service();
        }

        @Override
        public void deleteObject(OssUploadCleanupRecord cleanup) {
            deleteCalls.incrementAndGet();
            objectPresent = false;
        }
    }

    private static final class MemoryMetadataStore implements OssUploadMetadataStore {
        private final AtomicInteger registerCalls = new AtomicInteger();
        private Long ossId;

        @Override
        public Long findByObject(String service, String objectKey) {
            return ossId;
        }

        @Override
        public Long registerTemporary(OssUploadTicket ticket) {
            registerCalls.incrementAndGet();
            ossId = 1001L;
            return ossId;
        }
    }
}
