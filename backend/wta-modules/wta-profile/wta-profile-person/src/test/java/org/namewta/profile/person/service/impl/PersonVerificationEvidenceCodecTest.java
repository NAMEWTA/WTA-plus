package org.namewta.profile.person.service.impl;

import org.namewta.profile.person.adapter.codec.PersonVerificationEvidenceCodec;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag("dev")
class PersonVerificationEvidenceCodecTest {

    @Test
    void roundTripsDigestAndProviderEvidenceWithoutChangingLegacyRows() {
        PersonVerificationEvidenceCodec codec =
            new PersonVerificationEvidenceCodec(JsonMapper.builder().build());

        String stored = codec.encode("digest-1", "{\"provider\":\"test-provider\"}");
        PersonVerificationEvidenceCodec.DecodedEvidence decoded = codec.decode(stored);

        assertEquals("digest-1", decoded.callbackDigest());
        assertEquals("{\"provider\":\"test-provider\"}", decoded.providerEvidenceJson());
        assertEquals("legacy-evidence", codec.decode("legacy-evidence").providerEvidenceJson());
    }
}
