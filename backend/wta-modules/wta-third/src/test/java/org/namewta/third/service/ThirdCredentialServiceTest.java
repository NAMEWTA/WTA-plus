package org.namewta.third.service;

import org.namewta.common.core.exception.ServiceException;
import org.namewta.third.dao.ThirdCredentialDao;
import org.namewta.third.dao.ThirdEndpointDao;
import org.namewta.third.dao.ThirdProviderDao;
import org.namewta.third.domain.ThirdCredential;
import org.namewta.third.domain.ThirdProvider;
import org.namewta.third.domain.bo.ThirdCredentialBo;
import org.namewta.third.port.ThirdConfigSnapshotPort;
import org.namewta.third.port.ThirdCredentialCryptoPort;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("local")
class ThirdCredentialServiceTest {

    @Test
    void cannotReviveDeletedCredentialThroughSave() {
        ThirdCredentialDao credentialDao = mock(ThirdCredentialDao.class);
        ThirdProviderDao providerDao = mock(ThirdProviderDao.class);
        ThirdEndpointDao endpointDao = mock(ThirdEndpointDao.class);
        ThirdCredentialCryptoPort crypto = mock(ThirdCredentialCryptoPort.class);
        ThirdConfigSnapshotPort configCache = mock(ThirdConfigSnapshotPort.class);
        ThirdProvider provider = new ThirdProvider();
        provider.setProviderId(5L);
        provider.setProviderCode("qichacha");
        when(providerDao.findActiveByCode("qichacha")).thenReturn(provider);
        ThirdCredential deleted = new ThirdCredential();
        deleted.setCredentialId(9L);
        deleted.setProviderId(5L);
        deleted.setDelFlag("1");
        when(credentialDao.findById(9L)).thenReturn(deleted);

        ThirdCredentialBo update = new ThirdCredentialBo();
        update.setCredentialId(9L);
        update.setProviderCode("qichacha");
        update.setCredentialType("API_KEY");
        update.setSecretJson("{}");
        update.setEnabled(true);

        ThirdCredentialService service = new ThirdCredentialService(credentialDao, providerDao, endpointDao, crypto, configCache);

        assertThrows(ServiceException.class, () -> service.save(update));
        verify(crypto, never()).encrypt("PROVIDER", "API_KEY", "{}");
        verify(credentialDao, never()).update(deleted);
    }
}
