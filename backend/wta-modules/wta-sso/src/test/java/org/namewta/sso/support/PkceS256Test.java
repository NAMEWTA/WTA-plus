package org.namewta.sso.support;

import org.namewta.common.core.exception.ServiceException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Tag("local")
@Tag("dev")
class PkceS256Test {

    @Test
    void rfc7636AppendixBChallenge() {
        String verifier = "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk";
        assertEquals("E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM", PkceS256.challenge(verifier));
    }

    @Test
    void rejectsMissingOrNonS256Method() {
        assertThrows(ServiceException.class, () -> PkceS256.requireChallenge(null));
        assertThrows(ServiceException.class, () -> PkceS256.requireS256("plain"));
        assertThrows(ServiceException.class, () -> PkceS256.requireS256(null));
    }

    @Test
    void rejectsWrongVerifier() {
        String verifier = "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk";
        String challenge = PkceS256.challenge(verifier);
        assertThrows(ServiceException.class, () -> PkceS256.verify(verifier + "x", challenge));
    }
}
