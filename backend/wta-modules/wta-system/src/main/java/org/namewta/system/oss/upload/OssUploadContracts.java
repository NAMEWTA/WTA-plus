package org.namewta.system.oss.upload;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.namewta.common.oss.model.OssPresignedRequest;

import java.time.Instant;
import java.util.List;

/**
 * 上传控制面的固定 JSON 合同。
 */
public final class OssUploadContracts {

    private OssUploadContracts() {
    }

    public record InitRequest(
        @NotBlank String policy,
        @NotBlank String fileName,
        @Positive long fileSize,
        @NotBlank String contentType,
        @NotBlank String fingerprint
    ) {
    }

    public record InitResponse(
        String uploadToken,
        OssUploadMode mode,
        Instant expiresAt,
        OssPresignedRequest presignedRequest,
        Long partSize,
        Integer partCount
    ) {
    }

    public record SignPartsRequest(@NotEmpty List<@NotNull Integer> partNumbers) {
    }

    public record SignedPart(
        int partNumber,
        String method,
        String url,
        java.util.Map<String, String> requiredHeaders,
        Instant expiresAt
    ) {
        public SignedPart(int partNumber, OssPresignedRequest request) {
            this(partNumber, request.method(), request.url(), request.requiredHeaders(), request.expiresAt());
        }

        public SignedPart {
            requiredHeaders = requiredHeaders == null ? java.util.Map.of() : java.util.Map.copyOf(requiredHeaders);
        }
    }

    public record SignPartsResponse(List<SignedPart> parts) {
        public SignPartsResponse {
            parts = parts == null ? List.of() : List.copyOf(parts);
        }
    }

    public record UploadedPart(int partNumber, String eTag, long size) {
    }

    public record ResumeResponse(
        String uploadToken,
        OssUploadMode mode,
        OssUploadState state,
        String completedOssId,
        String fileName,
        long fileSize,
        String contentType,
        long partSize,
        int partCount,
        Instant expiresAt,
        OssPresignedRequest presignedRequest,
        List<UploadedPart> uploadedParts
    ) {
        public ResumeResponse {
            uploadedParts = uploadedParts == null ? List.of() : List.copyOf(uploadedParts);
        }
    }

    public record CompleteRequest(List<CompletedPart> parts) {
        public CompleteRequest {
            parts = parts == null ? List.of() : List.copyOf(parts);
        }
    }

    public record CompletedPart(@Positive int partNumber, @NotBlank String eTag) {
    }

    public record ErrorResponse(String error) {
    }
}
