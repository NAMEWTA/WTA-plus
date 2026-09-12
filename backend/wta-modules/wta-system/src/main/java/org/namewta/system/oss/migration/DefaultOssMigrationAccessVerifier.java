package org.namewta.system.oss.migration;

import lombok.RequiredArgsConstructor;
import org.namewta.system.api.OssService;
import org.namewta.system.oss.service.OssLifecycleManager;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Component
@RequiredArgsConstructor
public class DefaultOssMigrationAccessVerifier implements OssMigrationAccessVerifier {

    private final OssLifecycleManager lifecycleManager;
    private final HttpClient httpClient = HttpClient.newBuilder()
        .followRedirects(HttpClient.Redirect.NORMAL).build();

    @Override
    public void verifyPublic(Long ossId) {
        OssService.OssAccessUrl access = lifecycleManager.resolveAccessUrl(ossId);
        if (!"PUBLIC".equals(access.accessType()) || access.expiresAt() != null) {
            throw new OssMigrationException(OssMigrationError.ACCESS_VERIFICATION_FAILED,
                "迁移后访问类型不是公共稳定 URL");
        }
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(access.url()))
                .timeout(Duration.ofSeconds(5)).header("Range", "bytes=0-0").GET().build();
            int status = httpClient.send(request,
                HttpResponse.BodyHandlers.discarding()).statusCode();
            if (status != 200 && status != 206) {
                throw new OssMigrationException(OssMigrationError.ACCESS_VERIFICATION_FAILED,
                    "迁移后公共匿名访问验收失败");
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new OssMigrationException(OssMigrationError.ACCESS_VERIFICATION_FAILED,
                "迁移后公共匿名访问验收中断", ex);
        } catch (OssMigrationException ex) {
            throw ex;
        } catch (RuntimeException | java.io.IOException ex) {
            throw new OssMigrationException(OssMigrationError.ACCESS_VERIFICATION_FAILED,
                "迁移后公共匿名访问验收失败", ex);
        }
    }
}
