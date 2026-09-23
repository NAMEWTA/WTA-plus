package org.namewta.notify.service.runtime;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.namewta.notify.dao.NotifyNotificationDao;
import org.namewta.notify.domain.entity.NotifyDelivery;
import org.namewta.notify.domain.entity.NotifyIntent;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Tag("dev")
class NotificationMonitorServiceTest {
    @Test
    void batchesOnlyCurrentIntentIdsAndMasksMissingOrSensitiveMetadata() {
        NotifyNotificationDao dao = mock(NotifyNotificationDao.class);
        NotifyDelivery first = delivery(1L, 10L, "13812345678");
        NotifyDelivery second = delivery(2L, 10L, "1234");
        NotifyDelivery missing = delivery(3L, 20L, "1234");
        NotifyIntent intent = new NotifyIntent();
        intent.setIntentId(10L);
        intent.setMetadataJson("{\"audit\":\"REDACT_SENSITIVE\"}");
        when(dao.monitorDeliveries(null, null, null, 500)).thenReturn(List.of(first, second, missing));
        when(dao.intents(anyCollection())).thenReturn(List.of(intent));

        var views = new NotificationMonitorService(dao).listDeliveries(null, null, null);

        assertEquals(3, views.size());
        assertTrue(views.stream().allMatch(view -> view.providerMessageId() == null));
        verify(dao, times(1)).intents(List.of(10L, 20L));
        verify(dao, never()).intent(anyLong());
    }

    @Test
    void emptyMonitorPageNeverLoadsAllIntents() {
        NotifyNotificationDao dao = mock(NotifyNotificationDao.class);
        when(dao.monitorDeliveries(null, null, null, 500)).thenReturn(List.of());
        assertTrue(new NotificationMonitorService(dao).listDeliveries(null, null, null).isEmpty());
        verify(dao, never()).intents(anyCollection());
    }

    private NotifyDelivery delivery(long deliveryId, long intentId, String providerMessageId) {
        NotifyDelivery value = new NotifyDelivery();
        value.setDeliveryId(deliveryId);
        value.setIntentId(intentId);
        value.setProviderMessageId(providerMessageId);
        return value;
    }
}
