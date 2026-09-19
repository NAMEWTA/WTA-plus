package org.namewta.profile.enterprise.service.impl;

import org.namewta.profile.enterprise.adapter.codec.EnterpriseVerificationEvidenceCodec;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag("dev")
class EnterpriseVerificationEvidenceCodecTest {

    @Test
    void roundTripsDigestAndProviderEvidenceWithoutChangingLegacyRows() {
        EnterpriseVerificationEvidenceCodec codec =
            new EnterpriseVerificationEvidenceCodec();

        String stored = codec.encode("digest-1", "{\"provider\":\"test-provider\"}");
        EnterpriseVerificationEvidenceCodec.DecodedEvidence decoded = codec.decode(stored);

        assertEquals("digest-1", decoded.callbackDigest());
        assertEquals("{\"provider\":\"test-provider\"}", decoded.providerEvidenceJson());
        assertEquals("legacy-evidence", codec.decode("legacy-evidence").providerEvidenceJson());
    }
}
