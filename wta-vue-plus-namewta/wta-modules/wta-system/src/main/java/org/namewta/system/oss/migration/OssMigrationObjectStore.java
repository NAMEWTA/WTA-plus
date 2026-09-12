package org.namewta.system.oss.migration;

public interface OssMigrationObjectStore {

    Inspection inspect(String sourceService, String targetService, String objectKey, long maxVerifyBytes);

    Transfer transferAndVerify(String sourceService, String targetService, String objectKey, long maxVerifyBytes);

    boolean exists(String service, String objectKey);

    void delete(String service, String objectKey);

    record Inspection(boolean sourceExists, boolean targetExists, boolean conflict,
                      long sourceSize, String sourceEtag) {
    }

    record Transfer(long sourceSize, long targetSize, String sourceEtag, String targetEtag) {
    }
}
