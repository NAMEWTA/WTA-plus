package org.namewta.common.oss.exception;

import java.io.Serial;

/**
 * S3对象存储异常
 */
public class S3StorageException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final OssErrorCode code;

    /**
     * 使用异常消息构造 S3 对象存储异常。
     *
     * @param message 异常消息
     */
    public S3StorageException(String message) {
        this(OssErrorCode.PROVIDER_ERROR, message);
    }

    /**
     * 使用异常消息和原因构造 S3 对象存储异常。
     *
     * @param message 异常消息
     * @param cause   异常原因
     */
    public S3StorageException(String message, Throwable cause) {
        this(OssErrorCode.PROVIDER_ERROR, message, cause);
    }

    /**
     * 使用异常原因构造 S3 对象存储异常。
     *
     * @param cause 异常原因
     */
    public S3StorageException(Throwable cause) {
        this(OssErrorCode.PROVIDER_ERROR, cause == null ? null : cause.getMessage(), cause);
    }

    /**
     * 使用完整异常参数构造 S3 对象存储异常。
     *
     * @param message            异常消息
     * @param cause              异常原因
     * @param enableSuppression  是否启用抑制
     * @param writableStackTrace 是否写入堆栈
     */
    public S3StorageException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
        this.code = OssErrorCode.PROVIDER_ERROR;
    }

    /**
     * 使用稳定错误类别构造异常。
     */
    public S3StorageException(OssErrorCode code, String message) {
        super(message);
        this.code = code;
    }

    /**
     * 使用稳定错误类别和原因构造异常。
     */
    public S3StorageException(OssErrorCode code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    /**
     * 获取稳定错误类别。
     */
    public OssErrorCode code() {
        return code;
    }

    /**
     * 创建 S3 对象存储异常。
     *
     * @param message 异常消息
     * @return S3 对象存储异常
     */
    public static S3StorageException form(String message) {
        return new S3StorageException(message);
    }

    /**
     * 创建带稳定错误类别的 S3 对象存储异常。
     */
    public static S3StorageException form(OssErrorCode code, String message) {
        return new S3StorageException(code, message);
    }

    /**
     * 创建 S3 对象存储异常。
     *
     * @param message 异常消息
     * @param cause   异常原因
     * @return S3 对象存储异常
     */
    public static S3StorageException form(String message, Throwable cause) {
        return new S3StorageException(message, cause);
    }

    /**
     * 创建 S3 对象存储异常。
     *
     * @param cause 异常原因
     * @return S3 对象存储异常
     */
    public static S3StorageException form(Throwable cause) {
        return new S3StorageException(cause);
    }

    /**
     * 创建 S3 对象存储异常。
     *
     * @param message            异常消息
     * @param cause              异常原因
     * @param enableSuppression  是否启用抑制
     * @param writableStackTrace 是否写入堆栈
     * @return S3 对象存储异常
     */
    public static S3StorageException form(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        return new S3StorageException(message, cause, enableSuppression, writableStackTrace);
    }

}
