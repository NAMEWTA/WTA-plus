package org.namewta.common.oss.client;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.IdUtil;
import org.namewta.common.core.utils.DateUtils;
import org.namewta.common.core.utils.StringUtils;
import org.namewta.common.json.utils.JsonUtils;
import org.namewta.common.oss.config.OssClientConfig;
import org.namewta.common.oss.enums.AccessPolicy;
import org.namewta.common.oss.exception.S3StorageException;
import org.namewta.common.oss.exception.OssErrorCode;
import org.namewta.common.oss.io.OutputStreamDownloadSubscriber;
import org.namewta.common.oss.model.GetObjectResult;
import org.namewta.common.oss.model.HandleAsyncResult;
import org.namewta.common.oss.model.Options;
import org.namewta.common.oss.model.OssAccessDiagnostic;
import org.namewta.common.oss.model.OssChecksumAlgorithm;
import org.namewta.common.oss.model.OssClientCapabilities;
import org.namewta.common.oss.model.OssBucketConfiguration;
import org.namewta.common.oss.model.OssCompletedPart;
import org.namewta.common.oss.model.OssCopyResult;
import org.namewta.common.oss.model.OssMultipartCompleteResult;
import org.namewta.common.oss.model.OssMultipartPart;
import org.namewta.common.oss.model.OssMultipartUpload;
import org.namewta.common.oss.model.OssObjectOptions;
import org.namewta.common.oss.model.OssObjectStat;
import org.namewta.common.oss.model.OssPresignedRequest;
import org.namewta.common.oss.model.PutObjectResult;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.awscore.presigner.PresignedRequest;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.async.ResponsePublisher;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.auth.aws.signer.AwsV4FamilyHttpSigner;
import software.amazon.awssdk.http.auth.aws.signer.AwsV4HttpSigner;
import software.amazon.awssdk.http.auth.spi.signer.HttpSigner;
import software.amazon.awssdk.http.auth.spi.signer.SignRequest;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.AbortMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompletedMultipartUpload;
import software.amazon.awssdk.services.s3.model.CompletedPart;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.ListPartsRequest;
import software.amazon.awssdk.services.s3.model.Part;
import software.amazon.awssdk.services.s3.model.Permission;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.transfer.s3.model.CompletedUpload;
import software.amazon.awssdk.transfer.s3.model.DownloadRequest;
import software.amazon.awssdk.transfer.s3.progress.TransferListener;
import software.amazon.awssdk.utils.http.SdkHttpUtils;

import java.io.*;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * 抽象S3存储客户端实现类。
 */
public abstract class AbstractOssClientImpl implements OssClient {

    private final AtomicBoolean initialized = new AtomicBoolean(false);

    /**
     * S3 存储客户端ID
     * <p>
     * 用于标识客户端，初始化后不允许更改
     */
    protected final String clientId;

    /**
     * S3 存储客户端配置。
     */
    protected OssClientConfig config;

    /**
     * Amazon S3 异步客户端。
     */
    protected S3AsyncClient s3AsyncClient;

    /**
     * 用于管理 S3 数据传输的高级工具。
     */
    protected S3TransferManager s3TransferManager;

    /**
     * AWS S3 预签名 URL 生成器。
     */
    protected S3Presigner s3Presigner;

    /**
     * 异步调度线程池。
     */
    protected ExecutorService asyncExecutor;

    /**
     * 构造 S3 存储客户端基础实现。
     *
     * @param clientId 客户端 ID
     * @param config   S3 存储客户端配置
     */
    public AbstractOssClientImpl(String clientId, OssClientConfig config) {
        Assert.notNull(config, () -> S3StorageException.form("S3StorageClientConfig must not be null"));
        // 如果没有设置存储客户端ID，则随机生成一个
        this.clientId = StringUtils.isBlank(clientId) ? IdUtil.fastSimpleUUID() : clientId;
        this.config = config;
        this.initialize();
    }

    /**
     * 获取客户端 ID。
     *
     * @return 客户端 ID
     */
    @Override
    public String clientId() {
        return this.clientId;
    }

    /**
     * 获取客户端配置副本。
     *
     * @return 客户端配置副本
     */
    @Override
    public OssClientConfig config() {
        // 仅返回copy副本，防篡改
        return this.config.copy();
    }

    /**
     * 判断客户端是否已经初始化。
     *
     * @return 是否已初始化
     */
    @Override
    public boolean isInitialized() {
        return initialized.get();
    }

    /**
     * 初始化底层 S3 客户端资源。
     */
    @Override
    public void initialize() {
        // 如果已经是初始化状态，则直接返回
        if (isInitialized()) {
            return;
        }
        try {
            doInitialize();
            // 将状态转为已初始化
            initialized.compareAndSet(false, true);
        } catch (Exception e) {
            throw toStorageException(e);
        }
    }

    /**
     * 执行客户端具体初始化逻辑。
     */
    abstract void doInitialize();

    /**
     * 使用回调校验当前客户端配置。
     *
     * @param verifyConfigAction 配置校验回调
     * @return 是否校验通过
     */
    @Override
    public boolean verifyConfig(Function<OssClientConfig, Boolean> verifyConfigAction) {
        OssClientConfig config = config();
        return Boolean.TRUE.equals(verifyConfigAction.apply(config));
    }

    /**
     * 校验当前客户端配置是否与指定配置一致。
     *
     * @param verifyConfig 待校验配置
     * @return 是否一致
     */
    @Override
    public boolean verifyConfig(OssClientConfig verifyConfig) {
        return verifyConfig((config) -> Objects.equals(config, verifyConfig));
    }

    /**
     * 根据文件名构建对象键。
     *
     * @param fileName 原始文件名
     * @return 对象键
     */
    @Override
    public String buildPathKey(String fileName) {
        return buildPathKey(null, fileName);
    }

    /**
     * 根据业务前缀和文件名构建对象键。
     *
     * @param businessPrefix 业务前缀
     * @param fileName       原始文件名
     * @return 对象键
     */
    @Override
    public String buildPathKey(String businessPrefix, String fileName) {
        String defaultPrefix = config.prefix()
            .orElse("");
        String mergedPrefix = mergePrefix(defaultPrefix, businessPrefix);
        String suffix = suffix(fileName);
        String datePath = DateUtils.format(new Date(), "yyyy/MM/dd");
        String uuid = IdUtil.fastSimpleUUID();
        String path = mergedPrefix.isEmpty() ? datePath + StringUtils.SLASH + uuid : mergedPrefix + StringUtils.SLASH + datePath + StringUtils.SLASH + uuid;
        return path + suffix;
    }

    /**
     * 执行自定义上传请求。
     *
     * @param body                            上传请求体
     * @param putObjectRequestBuilderConsumer PutObject 请求构建回调
     * @param transferListeners               传输监听器集合
     * @param handleAsyncAction               上传完成处理函数
     * @param <T>                             返回值类型
     * @return 上传处理结果
     */
    @Override
    public <T> T doCustomUpload(AsyncRequestBody body, Consumer<PutObjectRequest.Builder> putObjectRequestBuilderConsumer, Collection<TransferListener> transferListeners, BiFunction<CompletedUpload, Throwable, T> handleAsyncAction) {
        try {
            return s3TransferManager.upload(uploadRequestBuilder -> {
                    uploadRequestBuilder.requestBody(body)
                        .putObjectRequest(putObjectRequestBuilderConsumer)
                        .transferListeners(transferListeners);
                })
                .completionFuture()
                .handleAsync(handleAsyncAction)
                .join();
        } catch (Exception e) {
            throw toStorageException(e);
        }
    }

    /**
     * 执行自定义上传请求。
     *
     * @param body                            上传请求体
     * @param putObjectRequestBuilderConsumer PutObject 请求构建回调
     * @param handleAsyncAction               上传完成处理函数
     * @param <T>                             返回值类型
     * @return 上传处理结果
     */
    @Override
    public <T> T doCustomUpload(AsyncRequestBody body, Consumer<PutObjectRequest.Builder> putObjectRequestBuilderConsumer, BiFunction<CompletedUpload, Throwable, T> handleAsyncAction) {
        return doCustomUpload(body, putObjectRequestBuilderConsumer, null, handleAsyncAction);
    }

    /**
     * 执行自定义上传请求并返回统一异步结果。
     *
     * @param body                            上传请求体
     * @param putObjectRequestBuilderConsumer PutObject 请求构建回调
     * @param transferListeners               传输监听器集合
     * @return 上传结果
     */
    @Override
    public HandleAsyncResult<PutObjectResponse> doCustomUpload(AsyncRequestBody body, Consumer<PutObjectRequest.Builder> putObjectRequestBuilderConsumer, Collection<TransferListener> transferListeners) {
        return doCustomUpload(body, putObjectRequestBuilderConsumer, transferListeners, (completedUpload, throwable) -> {
            if (completedUpload == null) {
                return HandleAsyncResult.of(null, throwable);
            }
            return HandleAsyncResult.of(completedUpload.response(), throwable);
        });
    }

    /**
     * 执行自定义上传请求并返回统一异步结果。
     *
     * @param body                            上传请求体
     * @param putObjectRequestBuilderConsumer PutObject 请求构建回调
     * @return 上传结果
     */
    @Override
    public HandleAsyncResult<PutObjectResponse> doCustomUpload(AsyncRequestBody body, Consumer<PutObjectRequest.Builder> putObjectRequestBuilderConsumer) {
        return doCustomUpload(body, putObjectRequestBuilderConsumer, null, (completedUpload, throwable) -> {
            if (completedUpload == null) {
                return HandleAsyncResult.of(null, throwable);
            }
            return HandleAsyncResult.of(completedUpload.response(), throwable);
        });
    }

    /**
     * 上传本地路径文件到指定存储桶。
     *
     * @param bucket  存储桶名称
     * @param key     对象键
     * @param path    文件路径
     * @param options 上传选项
     * @return 上传结果
     */
    @Override
    public PutObjectResult bucketUpload(String bucket, String key, Path path, Options options) {
        AsyncRequestBody body = AsyncRequestBody.fromFile(path);
        return bucketUpload(bucket, key, body, options);
    }

    /**
     * 上传本地路径文件到指定存储桶。
     *
     * @param bucket 存储桶名称
     * @param key    对象键
     * @param path   文件路径
     * @return 上传结果
     */
    @Override
    public PutObjectResult bucketUpload(String bucket, String key, Path path) {
        return bucketUpload(bucket, key, path, Options.builder());
    }

    /**
     * 上传文件到指定存储桶。
     *
     * @param bucket  存储桶名称
     * @param key     对象键
     * @param file    文件对象
     * @param options 上传选项
     * @return 上传结果
     */
    @Override
    public PutObjectResult bucketUpload(String bucket, String key, File file, Options options) {
        AsyncRequestBody body = AsyncRequestBody.fromFile(file);
        return bucketUpload(bucket, key, body, options);
    }

    /**
     * 上传文件到指定存储桶。
     *
     * @param bucket 存储桶名称
     * @param key    对象键
     * @param file   文件对象
     * @return 上传结果
     */
    @Override
    public PutObjectResult bucketUpload(String bucket, String key, File file) {
        return bucketUpload(bucket, key, file, Options.builder());
    }

    /**
     * 上传随机访问文件到指定存储桶。
     *
     * @param bucket  存储桶名称
     * @param key     对象键
     * @param file    随机访问文件
     * @param options 上传选项
     * @return 上传结果
     */
    @Override
    public PutObjectResult bucketUpload(String bucket, String key, RandomAccessFile file, Options options) {
        try {
            // 以文件的大小为准
            options.setLength(file.length());
            return bucketUpload(bucket, key, file.getChannel(), -1L, options);
        } catch (Exception e) {
            throw toStorageException(e);
        }
    }

    /**
     * 上传随机访问文件到指定存储桶。
     *
     * @param bucket 存储桶名称
     * @param key    对象键
     * @param file   随机访问文件
     * @return 上传结果
     */
    @Override
    public PutObjectResult bucketUpload(String bucket, String key, RandomAccessFile file) {
        return bucketUpload(bucket, key, file, Options.builder());
    }

    /**
     * 上传可读通道数据到指定存储桶。
     *
     * @param bucket        存储桶名称
     * @param key           对象键
     * @param channel       可读通道
     * @param contentLength 内容长度
     * @param options       上传选项
     * @return 上传结果
     */
    @Override
    public PutObjectResult bucketUpload(String bucket, String key, ReadableByteChannel channel, long contentLength, Options options) {
        // 让调用者自行处理通道的关闭
        InputStream in = Channels.newInputStream(channel);
        try {
            // 如果可以实时获取文件大小，则优先是有实时获取的
            long size = contentLength;
            if (channel instanceof SeekableByteChannel byteChannel) {
                size = byteChannel.size();
            }
            return bucketUpload(bucket, key, in, size, options);
        } catch (Exception e) {
            throw toStorageException(e);
        }
    }

    /**
     * 上传可读通道数据到指定存储桶。
     *
     * @param bucket        存储桶名称
     * @param key           对象键
     * @param channel       可读通道
     * @param contentLength 内容长度
     * @return 上传结果
     */
    @Override
    public PutObjectResult bucketUpload(String bucket, String key, ReadableByteChannel channel, long contentLength) {
        return bucketUpload(bucket, key, channel, contentLength, Options.builder());
    }

    /**
     * 上传输入流数据到指定存储桶。
     *
     * @param bucket        存储桶名称
     * @param key           对象键
     * @param in            输入流
     * @param contentLength 内容长度
     * @param options       上传选项
     * @return 上传结果
     */
    @Override
    public PutObjectResult bucketUpload(String bucket, String key, InputStream in, long contentLength, Options options) {
        Path tempFile = null;
        try {
            tempFile = Files.createTempFile("wta-oss-upload-", ".tmp");
            try (OutputStream out = Files.newOutputStream(tempFile)) {
                in.transferTo(out);
            }
            options.setLength(Files.size(tempFile));
            return bucketUpload(bucket, key, tempFile, options);
        } catch (Exception e) {
            throw toStorageException(e);
        } finally {
            if (tempFile != null) {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (IOException ignored) {
                    // 临时文件清理失败不影响上传结果。
                }
            }
        }
    }

    /**
     * 上传输入流数据到指定存储桶。
     *
     * @param bucket        存储桶名称
     * @param key           对象键
     * @param in            输入流
     * @param contentLength 内容长度
     * @return 上传结果
     */
    @Override
    public PutObjectResult bucketUpload(String bucket, String key, InputStream in, long contentLength) {
        return bucketUpload(bucket, key, in, contentLength, Options.builder());
    }

    /**
     * 上传字节数组到指定存储桶。
     *
     * @param bucket  存储桶名称
     * @param key     对象键
     * @param data    字节数组
     * @param options 上传选项
     * @return 上传结果
     */
    @Override
    public PutObjectResult bucketUpload(String bucket, String key, byte[] data, Options options) {
        options.setLength((long) data.length);
        AsyncRequestBody body = AsyncRequestBody.fromBytes(data);
        return bucketUpload(bucket, key, body, options);
    }

    /**
     * 上传字节数组到指定存储桶。
     *
     * @param bucket 存储桶名称
     * @param key    对象键
     * @param data   字节数组
     * @return 上传结果
     */
    @Override
    public PutObjectResult bucketUpload(String bucket, String key, byte[] data) {
        return bucketUpload(bucket, key, data, Options.builder());
    }

    /**
     * 执行指定存储桶的底层上传。
     *
     * @param bucket  存储桶名称
     * @param key     对象键
     * @param body    上传请求体
     * @param options 上传选项
     * @return 上传结果
     */
    private PutObjectResult bucketUpload(String bucket, String key, AsyncRequestBody body, Options options) {
        // 优先使用body中的内容大小，如果不存在，再获取可选项中的
        Long contentLength = body.contentLength().orElse(options.getLength());
        // 优先使用body中的内容类型，如果不存在，再获取可选项中的
        String contentType = StringUtils.isBlank(options.getContentType()) ? body.contentType() : options.getContentType();
        String md5Digest = options.getMd5Digest();
        Map<String, String> metadata = options.getMetadata();
        Collection<TransferListener> transferListeners = options.getTransferListeners();
        HandleAsyncResult<PutObjectResponse> result = doCustomUpload(body, builder -> {
            builder.bucket(bucket)
                .key(key)
                .contentMD5(md5Digest)
                .contentType(contentType)
                .contentLength(contentLength)
                .metadata(metadata);
        }, transferListeners);
        if (result.isFailure()) {
            throw toStorageException(result.error());
        }
        Optional<PutObjectResponse> opt = result.getResult();
        if (opt.isEmpty()) {
            throw S3StorageException.form("response is empty.");
        }
        PutObjectResponse response = opt.get();
        // 不知道什么原因导致 response.size() 返回了一个 null size ，此处做一个适配...
        Long size = response.size();
        if (size == null) {
            size = contentLength == null ? 0 : contentLength;
        }
        String bucketUrl = config.getBucketUrl(bucket);
        return PutObjectResult.form("%s/%s".formatted(bucketUrl, key), key, response.eTag(), size);
    }

    /**
     * 执行自定义下载请求。
     *
     * @param getObjectRequestBuilderConsumer GetObject 请求构建回调
     * @param responseTransformer             下载响应转换器
     * @param transferListeners               传输监听器集合
     * @param <T>                             下载结果类型
     * @return 下载结果
     */
    @Override
    public <T> T doCustomDownload(Consumer<GetObjectRequest.Builder> getObjectRequestBuilderConsumer, AsyncResponseTransformer<GetObjectResponse, T> responseTransformer, Collection<TransferListener> transferListeners) {
        try {
            DownloadRequest<T> downloadRequest = DownloadRequest.builder()
                .responseTransformer(responseTransformer)
                .getObjectRequest(getObjectRequestBuilderConsumer)
                .transferListeners(transferListeners)
                .build();
            return s3TransferManager.download(downloadRequest)
                .completionFuture()
                .join()
                .result();
        } catch (Exception e) {
            throw toStorageException(e);
        }
    }

    /**
     * 将指定存储桶对象下载到订阅器。
     *
     * @param bucket             存储桶名称
     * @param key                对象键
     * @param downloadSubscriber 下载订阅器
     * @return 下载结果
     */
    @Override
    public GetObjectResult bucketDownload(String bucket, String key, OutputStreamDownloadSubscriber downloadSubscriber) {
        try {
            ResponsePublisher<GetObjectResponse> publisher = doCustomDownload(builder -> builder.bucket(bucket).key(key), AsyncResponseTransformer.toPublisher(), null);
            GetObjectResult getObjectResult = buildGetObjectResult(key, publisher.response());
            publisher.subscribe(downloadSubscriber).join();
            return getObjectResult;
        } catch (Exception e) {
            throw toStorageException(e);
        }
    }

    /**
     * 将指定存储桶对象下载到转换器。
     *
     * @param bucket              存储桶名称
     * @param key                 对象键
     * @param downloadTransformer 下载转换器
     * @param <T>                 下载结果类型
     * @return 下载结果
     */
    @Override
    public <T> T bucketDownload(String bucket, String key, BiFunction<GetObjectResult, InputStream, T> downloadTransformer) {
        try (ResponseInputStream<GetObjectResponse> responseInputStream = doCustomDownload(builder -> builder.bucket(bucket).key(key), AsyncResponseTransformer.toBlockingInputStream(), null)) {
            GetObjectResponse response = responseInputStream.response();
            GetObjectResult getObjectResult = buildGetObjectResult(key, response);
            return downloadTransformer.apply(getObjectResult, responseInputStream);
        } catch (Exception e) {
            throw toStorageException(e);
        }
    }

    /**
     * 将指定存储桶对象下载到本地路径。
     *
     * @param bucket 存储桶名称
     * @param key    对象键
     * @param path   本地路径
     * @return 下载结果
     */
    @Override
    public GetObjectResult bucketDownload(String bucket, String key, Path path) {
        try (OutputStream out = Files.newOutputStream(path)) {
            return bucketDownload(bucket, key, out);
        } catch (Exception e) {
            throw toStorageException(e);
        }
    }

    /**
     * 将指定存储桶对象下载到文件。
     *
     * @param bucket 存储桶名称
     * @param key    对象键
     * @param file   本地文件
     * @return 下载结果
     */
    @Override
    public GetObjectResult bucketDownload(String bucket, String key, File file) {
        try (FileOutputStream out = new FileOutputStream(file)) {
            return bucketDownload(bucket, key, out);
        } catch (Exception e) {
            throw toStorageException(e);
        }
    }

    /**
     * 将指定存储桶对象下载到随机访问文件。
     *
     * @param bucket 存储桶名称
     * @param key    对象键
     * @param file   随机访问文件
     * @return 下载结果
     */
    @Override
    public GetObjectResult bucketDownload(String bucket, String key, RandomAccessFile file) {
        return bucketDownload(bucket, key, file.getChannel());
    }

    /**
     * 将指定存储桶对象下载到可写通道。
     *
     * @param bucket  存储桶名称
     * @param key     对象键
     * @param channel 可写通道
     * @return 下载结果
     */
    @Override
    public GetObjectResult bucketDownload(String bucket, String key, WritableByteChannel channel) {
        return bucketDownload(bucket, key, OutputStreamDownloadSubscriber.create(channel));
    }

    /**
     * 将指定存储桶对象下载到输出流。
     *
     * @param bucket 存储桶名称
     * @param key    对象键
     * @param out    输出流
     * @return 下载结果
     */
    @Override
    public GetObjectResult bucketDownload(String bucket, String key, OutputStream out) {
        return bucketDownload(bucket, key, OutputStreamDownloadSubscriber.create(out));
    }

    /**
     * 根据 S3 响应构建下载结果。
     *
     * @param key      对象键
     * @param response S3 下载响应
     * @return 下载结果
     */
    private GetObjectResult buildGetObjectResult(String key, GetObjectResponse response) {
        return GetObjectResult.form(
            key,
            response.eTag(),
            response.lastModified().atOffset(ZoneOffset.UTC).toLocalDateTime(),
            response.contentLength(),
            response.contentType(),
            response.contentDisposition(),
            response.contentRange(),
            response.contentEncoding(),
            response.contentLanguage(),
            response.metadata()
        );
    }

    /**
     * 删除指定存储桶中的对象。
     *
     * @param bucket 存储桶名称
     * @param key    对象键
     * @return 是否删除成功
     */
    @Override
    public boolean bucketDelete(String bucket, String key) {
        try {
            s3AsyncClient.deleteObject(builder -> builder.bucket(bucket).key(key)).join();
            return true;
        } catch (Exception e) {
            throw toStorageException(e);
        }
    }

    @Override
    public OssObjectStat bucketHeadObject(String bucket, String key) {
        validateObjectIdentity(bucket, key);
        try {
            var response = s3AsyncClient.headObject(HeadObjectRequest.builder().bucket(bucket).key(key).build()).join();
            return new OssObjectStat(
                bucket,
                key,
                Optional.ofNullable(response.contentLength()).orElse(0L),
                response.contentType(),
                response.eTag(),
                response.lastModified(),
                response.metadata(),
                checksumMap(response.checksumCRC32(), response.checksumCRC32C(), response.checksumSHA1(), response.checksumSHA256())
            );
        } catch (Exception e) {
            throw toStorageException(e);
        }
    }

    @Override
    public OssMultipartUpload bucketCreateMultipartUpload(String bucket, String key, OssObjectOptions options) {
        validateObjectIdentity(bucket, key);
        OssObjectOptions actualOptions = options == null ? OssObjectOptions.empty() : options;
        requireSupportedChecksum(actualOptions);
        try {
            CreateMultipartUploadRequest request = CreateMultipartUploadRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(actualOptions.contentType())
                .metadata(actualOptions.metadata())
                .build();
            var response = s3AsyncClient.createMultipartUpload(request).join();
            return new OssMultipartUpload(bucket, key, response.uploadId());
        } catch (Exception e) {
            throw toStorageException(e);
        }
    }

    @Override
    public List<OssMultipartPart> bucketListParts(String bucket, String key, String uploadId) {
        validateMultipartIdentity(bucket, key, uploadId);
        List<OssMultipartPart> result = new ArrayList<>();
        Integer partNumberMarker = null;
        try {
            do {
                ListPartsRequest request = ListPartsRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .uploadId(uploadId)
                    .partNumberMarker(partNumberMarker)
                    .build();
                var response = s3AsyncClient.listParts(request).join();
                for (Part part : response.parts()) {
                    result.add(new OssMultipartPart(
                        part.partNumber(),
                        part.eTag(),
                        Optional.ofNullable(part.size()).orElse(0L),
                        part.lastModified(),
                        checksumMap(part.checksumCRC32(), part.checksumCRC32C(), part.checksumSHA1(), part.checksumSHA256())
                    ));
                }
                if (!Boolean.TRUE.equals(response.isTruncated())) {
                    break;
                }
                Integer nextMarker = response.nextPartNumberMarker();
                if (nextMarker == null || Objects.equals(nextMarker, partNumberMarker)) {
                    throw S3StorageException.form(
                        OssErrorCode.PROVIDER_ERROR,
                        "Provider returned a truncated ListParts page without a forward marker."
                    );
                }
                partNumberMarker = nextMarker;
            } while (true);
            return List.copyOf(result);
        } catch (Exception e) {
            throw toStorageException(e);
        }
    }

    @Override
    public OssMultipartCompleteResult bucketCompleteMultipartUpload(
        String bucket,
        String key,
        String uploadId,
        List<OssCompletedPart> parts
    ) {
        validateMultipartIdentity(bucket, key, uploadId);
        List<CompletedPart> providerParts = normalizeCompletedParts(parts);
        try {
            CompleteMultipartUploadRequest request = CompleteMultipartUploadRequest.builder()
                .bucket(bucket)
                .key(key)
                .uploadId(uploadId)
                .multipartUpload(CompletedMultipartUpload.builder().parts(providerParts).build())
                .build();
            var response = s3AsyncClient.completeMultipartUpload(request).join();
            return new OssMultipartCompleteResult(
                bucket,
                key,
                response.eTag(),
                checksumMap(response.checksumCRC32(), response.checksumCRC32C(), response.checksumSHA1(), response.checksumSHA256())
            );
        } catch (Exception e) {
            throw toStorageException(e);
        }
    }

    @Override
    public boolean bucketAbortMultipartUpload(String bucket, String key, String uploadId) {
        validateMultipartIdentity(bucket, key, uploadId);
        try {
            AbortMultipartUploadRequest request = AbortMultipartUploadRequest.builder()
                .bucket(bucket)
                .key(key)
                .uploadId(uploadId)
                .build();
            s3AsyncClient.abortMultipartUpload(request).join();
            return true;
        } catch (Exception e) {
            Throwable cause = unwrapAsyncException(e);
            if (cause instanceof S3Exception s3Exception && s3Exception.statusCode() == 404) {
                return true;
            }
            throw toStorageException(e);
        }
    }

    @Override
    public OssCopyResult bucketCopyObject(String sourceBucket, String sourceKey, String targetBucket, String targetKey) {
        if (StringUtils.isAnyBlank(sourceBucket, sourceKey, targetBucket, targetKey)) {
            throw invalidRequest("source and target bucket/key must not be blank.");
        }
        try {
            String copySource = SdkHttpUtils.urlEncodeIgnoreSlashes(sourceBucket + StringUtils.SLASH + sourceKey);
            CopyObjectRequest request = CopyObjectRequest.builder()
                .copySource(copySource)
                .bucket(targetBucket)
                .key(targetKey)
                .build();
            var response = s3AsyncClient.copyObject(request).join();
            var result = response.copyObjectResult();
            return new OssCopyResult(
                sourceBucket,
                sourceKey,
                targetBucket,
                targetKey,
                result == null ? null : result.eTag(),
                result == null ? null : result.lastModified()
            );
        } catch (Exception e) {
            throw toStorageException(e);
        }
    }

    private List<CompletedPart> normalizeCompletedParts(List<OssCompletedPart> parts) {
        if (parts == null || parts.isEmpty()) {
            throw invalidRequest("completed parts must not be empty.");
        }
        List<OssCompletedPart> ordered = parts.stream()
            .sorted(Comparator.comparingInt(OssCompletedPart::partNumber))
            .toList();
        Set<Integer> partNumbers = new HashSet<>();
        List<CompletedPart> result = new ArrayList<>(ordered.size());
        for (OssCompletedPart part : ordered) {
            if (part.partNumber() < 1 || part.partNumber() > 10_000 || !partNumbers.add(part.partNumber())) {
                throw invalidRequest("completed parts contain an invalid or duplicate partNumber.");
            }
            if (StringUtils.isBlank(part.eTag())) {
                throw invalidRequest("completed part ETag must not be blank.");
            }
            CompletedPart.Builder builder = CompletedPart.builder()
                .partNumber(part.partNumber())
                .eTag(part.eTag());
            part.checksums().forEach((algorithm, checksum) -> applyChecksum(builder, algorithm, checksum));
            result.add(builder.build());
        }
        return result;
    }

    private void applyChecksum(CompletedPart.Builder builder, OssChecksumAlgorithm algorithm, String checksum) {
        switch (algorithm) {
            case CRC32 -> builder.checksumCRC32(checksum);
            case CRC32C -> builder.checksumCRC32C(checksum);
            case SHA1 -> builder.checksumSHA1(checksum);
            case SHA256 -> builder.checksumSHA256(checksum);
        }
    }

    private Map<OssChecksumAlgorithm, String> checksumMap(String crc32, String crc32c, String sha1, String sha256) {
        EnumMap<OssChecksumAlgorithm, String> checksums = new EnumMap<>(OssChecksumAlgorithm.class);
        putChecksum(checksums, OssChecksumAlgorithm.CRC32, crc32);
        putChecksum(checksums, OssChecksumAlgorithm.CRC32C, crc32c);
        putChecksum(checksums, OssChecksumAlgorithm.SHA1, sha1);
        putChecksum(checksums, OssChecksumAlgorithm.SHA256, sha256);
        return Map.copyOf(checksums);
    }

    private void putChecksum(Map<OssChecksumAlgorithm, String> checksums, OssChecksumAlgorithm algorithm, String value) {
        if (StringUtils.isNotBlank(value)) {
            checksums.put(algorithm, value);
        }
    }

    private void validateMultipartIdentity(String bucket, String key, String uploadId) {
        validateObjectIdentity(bucket, key);
        if (StringUtils.isBlank(uploadId)) {
            throw invalidRequest("uploadId must not be blank.");
        }
    }

    private void validateObjectIdentity(String bucket, String key) {
        if (StringUtils.isAnyBlank(bucket, key)) {
            throw invalidRequest("bucket and key must not be blank.");
        }
    }

    /**
     * 生成指定存储桶对象的下载预签名 URL。
     *
     * @param bucket      存储桶名称
     * @param key         对象键
     * @param expiredTime 过期时间
     * @return 预签名下载 URL
     */
    @Override
    public String bucketPresignGetUrl(String bucket, String key, Duration expiredTime) {
        return bucketPresignGet(bucket, key, expiredTime).url();
    }

    /**
     * 生成指定存储桶对象的上传预签名 URL。
     *
     * @param bucket      存储桶名称
     * @param key         对象键
     * @param expiredTime 过期时间
     * @param metadata    对象元数据
     * @return 预签名上传 URL
     */
    @Override
    public String bucketPresignPutUrl(String bucket, String key, Duration expiredTime, Map<String, String> metadata) {
        return bucketPresignPut(bucket, key, expiredTime, new OssObjectOptions(null, metadata, null)).url();
    }

    @Override
    public OssClientCapabilities capabilities() {
        return OssClientCapabilities.s3CompatibleBaseline();
    }

    @Override
    public OssAccessDiagnostic diagnoseAccess(String diagnosticObjectKey, AccessPolicy expectedPolicy,
                                               Duration timeout) {
        Instant checkedAt = Instant.now();
        if (!capabilities().readOnlyAccessDiagnostic()) {
            return unknownDiagnostic(expectedPolicy, checkedAt, OssAccessDiagnostic.Reason.UNSUPPORTED,
                OssAccessDiagnostic.Basis.UNSUPPORTED);
        }
        if (StringUtils.isBlank(diagnosticObjectKey) || expectedPolicy == null || timeout == null
            || timeout.isZero() || timeout.isNegative()) {
            return unknownDiagnostic(expectedPolicy, checkedAt, OssAccessDiagnostic.Reason.INVALID_REQUEST,
                OssAccessDiagnostic.Basis.NOT_EVALUATED);
        }
        String bucket;
        try {
            bucket = config.bucket().filter(StringUtils::isNotBlank).orElseThrow();
        } catch (RuntimeException ex) {
            return unknownDiagnostic(expectedPolicy, checkedAt,
                diagnosticFailure(ex, OssAccessDiagnostic.Reason.DIAGNOSTIC_OBJECT_MISSING), failureBasis(ex));
        }
        OssAccessDiagnostic.Reason prerequisiteFailure = null;
        try {
            await(s3AsyncClient.headObject(builder -> builder.bucket(bucket).key(diagnosticObjectKey)), timeout);
        } catch (RuntimeException ex) {
            prerequisiteFailure = diagnosticFailure(ex, OssAccessDiagnostic.Reason.DIAGNOSTIC_OBJECT_MISSING);
            if (Thread.currentThread().isInterrupted()) {
                return unknownDiagnostic(expectedPolicy, checkedAt, prerequisiteFailure,
                    OssAccessDiagnostic.Basis.INTERRUPTED);
            }
        }

        List<OssAccessDiagnostic.Fact> facts = new ArrayList<>(6);
        try {
            String policy = await(s3AsyncClient.getBucketPolicy(builder -> builder.bucket(bucket)), timeout).policy();
            facts.addAll(policyFacts(policy, bucket, diagnosticObjectKey));
        } catch (RuntimeException ex) {
            OssAccessDiagnostic.Basis basis = isNoSuchBucketPolicy(ex)
                ? OssAccessDiagnostic.Basis.NO_SUCH_POLICY
                : isPolicyReadDenied(ex) ? OssAccessDiagnostic.Basis.POLICY_UNREADABLE : failureBasis(ex);
            facts.add(fact(OssAccessDiagnostic.Subject.POLICY_READ, OssAccessDiagnostic.Observation.UNKNOWN,
                OssAccessDiagnostic.Source.BUCKET_POLICY, OssAccessDiagnostic.Scope.BUCKET, basis));
            facts.add(fact(OssAccessDiagnostic.Subject.POLICY_WRITE, OssAccessDiagnostic.Observation.UNKNOWN,
                OssAccessDiagnostic.Source.BUCKET_POLICY, OssAccessDiagnostic.Scope.BUCKET, basis));
        }
        if (Thread.currentThread().isInterrupted()) {
            appendUnknown(facts, OssAccessDiagnostic.Basis.INTERRUPTED);
            return diagnostic(OssAccessDiagnostic.Verification.UNVERIFIED, OssAccessDiagnostic.Reason.PROVIDER_ERROR,
                expectedPolicy, facts, checkedAt);
        }
        try {
            var acl = await(s3AsyncClient.getBucketAcl(builder -> builder.bucket(bucket)), timeout);
            List<software.amazon.awssdk.services.s3.model.Grant> grants = acl.grants() == null ? List.of() : acl.grants();
            boolean list = grants.stream().anyMatch(grant -> isAllUsers(grant)
                && (grant.permission() == Permission.READ || grant.permission() == Permission.FULL_CONTROL));
            boolean writeRisk = grants.stream().anyMatch(grant -> isAllUsers(grant)
                && (grant.permission() == Permission.WRITE || grant.permission() == Permission.WRITE_ACP
                || grant.permission() == Permission.FULL_CONTROL));
            facts.add(fact(OssAccessDiagnostic.Subject.ACL_LIST,
                list ? OssAccessDiagnostic.Observation.ALLOWED : OssAccessDiagnostic.Observation.UNKNOWN,
                OssAccessDiagnostic.Source.BUCKET_ACL, OssAccessDiagnostic.Scope.BUCKET,
                list ? OssAccessDiagnostic.Basis.ACL_GRANT : OssAccessDiagnostic.Basis.ACL_NO_GRANT));
            facts.add(fact(OssAccessDiagnostic.Subject.ACL_WRITE_RISK,
                writeRisk ? OssAccessDiagnostic.Observation.ALLOWED : OssAccessDiagnostic.Observation.UNKNOWN,
                OssAccessDiagnostic.Source.BUCKET_ACL, OssAccessDiagnostic.Scope.BUCKET,
                writeRisk ? OssAccessDiagnostic.Basis.ACL_GRANT : OssAccessDiagnostic.Basis.ACL_NO_GRANT));
        } catch (RuntimeException ex) {
            OssAccessDiagnostic.Basis basis = isPolicyReadDenied(ex)
                ? OssAccessDiagnostic.Basis.ACL_UNREADABLE : failureBasis(ex);
            facts.add(fact(OssAccessDiagnostic.Subject.ACL_LIST, OssAccessDiagnostic.Observation.UNKNOWN,
                OssAccessDiagnostic.Source.BUCKET_ACL, OssAccessDiagnostic.Scope.BUCKET, basis));
            facts.add(fact(OssAccessDiagnostic.Subject.ACL_WRITE_RISK, OssAccessDiagnostic.Observation.UNKNOWN,
                OssAccessDiagnostic.Source.BUCKET_ACL, OssAccessDiagnostic.Scope.BUCKET, basis));
        }
        if (Thread.currentThread().isInterrupted()) {
            appendUnknown(facts, OssAccessDiagnostic.Basis.INTERRUPTED);
            return diagnostic(OssAccessDiagnostic.Verification.UNVERIFIED, OssAccessDiagnostic.Reason.PROVIDER_ERROR,
                expectedPolicy, facts, checkedAt);
        }
        facts.addAll(anonymousReadFacts(bucket, diagnosticObjectKey, timeout));
        OssAccessDiagnostic summary = summarize(expectedPolicy, facts, checkedAt);
        return prerequisiteFailure != null && summary.verification() != OssAccessDiagnostic.Verification.MISMATCH
            ? diagnostic(OssAccessDiagnostic.Verification.UNVERIFIED, prerequisiteFailure, expectedPolicy,
                facts, checkedAt) : summary;
    }

    private OssAccessDiagnostic unknownDiagnostic(AccessPolicy policy, Instant checkedAt,
                                                  OssAccessDiagnostic.Reason reason,
                                                  OssAccessDiagnostic.Basis basis) {
        List<OssAccessDiagnostic.Fact> facts = new ArrayList<>(6);
        appendUnknown(facts, basis);
        return diagnostic(OssAccessDiagnostic.Verification.UNVERIFIED, reason, policy, facts, checkedAt);
    }

    private void appendUnknown(List<OssAccessDiagnostic.Fact> facts, OssAccessDiagnostic.Basis basis) {
        for (OssAccessDiagnostic.Subject subject : OssAccessDiagnostic.Subject.values()) {
            if (facts.stream().noneMatch(existing -> existing.subject() == subject)) {
                OssAccessDiagnostic.Source source = switch (subject) {
                    case POLICY_READ, POLICY_WRITE -> OssAccessDiagnostic.Source.BUCKET_POLICY;
                    case ACL_LIST, ACL_WRITE_RISK -> OssAccessDiagnostic.Source.BUCKET_ACL;
                    case OBJECT_HEAD -> OssAccessDiagnostic.Source.ANONYMOUS_HEAD;
                    case OBJECT_GET -> OssAccessDiagnostic.Source.ANONYMOUS_GET;
                };
                OssAccessDiagnostic.Scope scope = switch (subject) {
                    case OBJECT_HEAD, OBJECT_GET -> OssAccessDiagnostic.Scope.OBJECT;
                    default -> OssAccessDiagnostic.Scope.BUCKET;
                };
                facts.add(fact(subject, OssAccessDiagnostic.Observation.UNKNOWN, source, scope, basis));
            }
        }
    }

    private OssAccessDiagnostic summarize(AccessPolicy expectedPolicy, List<OssAccessDiagnostic.Fact> facts,
                                          Instant checkedAt) {
        OssAccessDiagnostic.Fact policyRead = find(facts, OssAccessDiagnostic.Subject.POLICY_READ);
        OssAccessDiagnostic.Fact policyWrite = find(facts, OssAccessDiagnostic.Subject.POLICY_WRITE);
        OssAccessDiagnostic.Fact aclWrite = find(facts, OssAccessDiagnostic.Subject.ACL_WRITE_RISK);
        OssAccessDiagnostic.Fact head = find(facts, OssAccessDiagnostic.Subject.OBJECT_HEAD);
        OssAccessDiagnostic.Fact get = find(facts, OssAccessDiagnostic.Subject.OBJECT_GET);
        if (policyWrite.observation() == OssAccessDiagnostic.Observation.ALLOWED
            || aclWrite.observation() == OssAccessDiagnostic.Observation.ALLOWED) {
            return diagnostic(OssAccessDiagnostic.Verification.MISMATCH,
                OssAccessDiagnostic.Reason.ANONYMOUS_WRITE_ALLOWED, expectedPolicy, facts, checkedAt);
        }
        if (expectedPolicy == AccessPolicy.PRIVATE) {
            if (head.observation() == OssAccessDiagnostic.Observation.ALLOWED
                || get.observation() == OssAccessDiagnostic.Observation.ALLOWED) {
                return diagnostic(OssAccessDiagnostic.Verification.MISMATCH,
                    OssAccessDiagnostic.Reason.ANONYMOUS_READ_MISMATCH, expectedPolicy, facts, checkedAt);
            }
        } else if (expectedPolicy == AccessPolicy.PUBLIC_READ) {
            if (policyRead.observation() == OssAccessDiagnostic.Observation.DENIED) {
                return diagnostic(OssAccessDiagnostic.Verification.MISMATCH,
                    OssAccessDiagnostic.Reason.POLICY_MISMATCH, expectedPolicy, facts, checkedAt);
            }
            if (policyRead.observation() == OssAccessDiagnostic.Observation.ALLOWED
                && (head.observation() == OssAccessDiagnostic.Observation.DENIED
                || get.observation() == OssAccessDiagnostic.Observation.DENIED)) {
                return diagnostic(OssAccessDiagnostic.Verification.MISMATCH,
                    OssAccessDiagnostic.Reason.ANONYMOUS_READ_MISMATCH, expectedPolicy, facts, checkedAt);
            }
        }
        OssAccessDiagnostic.Reason reason = facts.stream().anyMatch(fact ->
            fact.basis() == OssAccessDiagnostic.Basis.TIMEOUT) ? OssAccessDiagnostic.Reason.TIMEOUT
            : facts.stream().anyMatch(fact -> fact.basis() == OssAccessDiagnostic.Basis.POLICY_UNREADABLE
            || fact.basis() == OssAccessDiagnostic.Basis.ACL_UNREADABLE)
                ? OssAccessDiagnostic.Reason.POLICY_UNREADABLE : OssAccessDiagnostic.Reason.INSUFFICIENT_EVIDENCE;
        return diagnostic(OssAccessDiagnostic.Verification.UNVERIFIED, reason, expectedPolicy, facts, checkedAt);
    }

    private OssAccessDiagnostic.Fact find(List<OssAccessDiagnostic.Fact> facts, OssAccessDiagnostic.Subject subject) {
        return facts.stream().filter(fact -> fact.subject() == subject).findFirst().orElseThrow();
    }

    private List<OssAccessDiagnostic.Fact> policyFacts(String policy, String bucket, String key) {
        if (StringUtils.isBlank(policy)) {
            return unknownPolicyFacts(OssAccessDiagnostic.Basis.INVALID_POLICY);
        }
        try {
            tools.jackson.databind.JsonNode root = JsonUtils.getJsonMapper().readTree(policy);
            if (root == null || !root.isObject() || root.get("Statement") == null) {
                return unknownPolicyFacts(OssAccessDiagnostic.Basis.INVALID_POLICY);
            }
            tools.jackson.databind.JsonNode statements = root.get("Statement");
            Iterable<tools.jackson.databind.JsonNode> nodes = statements.isArray() ? statements : List.of(statements);
            boolean readAllow = false;
            boolean readDeny = false;
            Set<String> writeAllows = new HashSet<>();
            Set<String> writeDenies = new HashSet<>();
            OssAccessDiagnostic.Scope scope = null;
            for (tools.jackson.databind.JsonNode statement : nodes) {
                if (!statement.isObject() || statement.has("Condition") || statement.has("NotAction")
                    || statement.has("NotPrincipal") || statement.has("NotResource")
                    || !publicPrincipal(statement.get("Principal"))) {
                    return unknownPolicyFacts(OssAccessDiagnostic.Basis.COMPLEX_POLICY);
                }
                String effect = statement.path("Effect").asText();
                if (!"Allow".equals(effect) && !"Deny".equals(effect)) {
                    return unknownPolicyFacts(OssAccessDiagnostic.Basis.COMPLEX_POLICY);
                }
                tools.jackson.databind.JsonNode resource = statement.get("Resource");
                if (resource != null && resource.isArray() && resource.size() == 1) {
                    resource = resource.get(0);
                }
                if (resource == null || !resource.isTextual()) {
                    return unknownPolicyFacts(OssAccessDiagnostic.Basis.COMPLEX_POLICY);
                }
                OssAccessDiagnostic.Scope nextScope;
                if (("arn:aws:s3:::" + bucket + "/*").equals(resource.asText())) {
                    nextScope = OssAccessDiagnostic.Scope.BUCKET;
                } else if (("arn:aws:s3:::" + bucket + "/" + key).equals(resource.asText())) {
                    nextScope = OssAccessDiagnostic.Scope.OBJECT;
                } else {
                    return unknownPolicyFacts(OssAccessDiagnostic.Basis.COMPLEX_POLICY);
                }
                if (scope != null && scope != nextScope) {
                    return unknownPolicyFacts(OssAccessDiagnostic.Basis.COMPLEX_POLICY);
                }
                scope = nextScope;
                tools.jackson.databind.JsonNode actions = statement.get("Action");
                if (actions == null) {
                    return unknownPolicyFacts(OssAccessDiagnostic.Basis.COMPLEX_POLICY);
                }
                Iterable<tools.jackson.databind.JsonNode> actionNodes = actions.isArray() ? actions : List.of(actions);
                boolean reads = false;
                Set<String> writes = new HashSet<>();
                for (tools.jackson.databind.JsonNode action : actionNodes) {
                    if (!action.isTextual()) {
                        return unknownPolicyFacts(OssAccessDiagnostic.Basis.COMPLEX_POLICY);
                    }
                    String name = action.asText().toLowerCase(Locale.ROOT);
                    if ("*".equals(name) || "s3:*".equals(name)) {
                        reads = true;
                        writes.addAll(Set.of("s3:putobject", "s3:deleteobject", "s3:abortmultipartupload"));
                    } else if ("s3:getobject".equals(name)) {
                        reads = true;
                    } else if (Set.of("s3:putobject", "s3:deleteobject", "s3:abortmultipartupload").contains(name)) {
                        writes.add(name);
                    } else {
                        return unknownPolicyFacts(OssAccessDiagnostic.Basis.COMPLEX_POLICY);
                    }
                }
                if (reads && "Allow".equals(effect)) {
                    readAllow = true;
                }
                if (reads && "Deny".equals(effect)) {
                    readDeny = true;
                }
                if ("Allow".equals(effect)) {
                    writeAllows.addAll(writes);
                }
                if ("Deny".equals(effect)) {
                    writeDenies.addAll(writes);
                }
            }
            if (scope == null) {
                return unknownPolicyFacts(OssAccessDiagnostic.Basis.COMPLEX_POLICY);
            }
            writeAllows.removeAll(writeDenies);
            return List.of(policyFact(OssAccessDiagnostic.Subject.POLICY_READ, readAllow, readDeny, scope),
                policyFact(OssAccessDiagnostic.Subject.POLICY_WRITE, !writeAllows.isEmpty(), false, scope));
        } catch (Exception invalidPolicy) {
            return unknownPolicyFacts(OssAccessDiagnostic.Basis.INVALID_POLICY);
        }
    }

    private boolean publicPrincipal(tools.jackson.databind.JsonNode principal) {
        if (principal == null) {
            return false;
        }
        if (principal.isTextual()) {
            return "*".equals(principal.asText());
        }
        if (!principal.isObject() || principal.size() != 1) {
            return false;
        }
        tools.jackson.databind.JsonNode aws = principal.get("AWS");
        if (aws != null && aws.isArray()) {
            if (aws.size() != 1) {
                return false;
            }
            aws = aws.get(0);
        }
        return aws != null && aws.isTextual() && "*".equals(aws.asText());
    }

    private OssAccessDiagnostic.Fact policyFact(OssAccessDiagnostic.Subject subject, boolean allow,
                                                boolean deny, OssAccessDiagnostic.Scope scope) {
        return fact(subject, deny ? OssAccessDiagnostic.Observation.DENIED
            : allow ? OssAccessDiagnostic.Observation.ALLOWED : OssAccessDiagnostic.Observation.UNKNOWN,
            OssAccessDiagnostic.Source.BUCKET_POLICY, scope,
            deny ? OssAccessDiagnostic.Basis.POLICY_DENY : allow ? OssAccessDiagnostic.Basis.POLICY_ALLOW
                : OssAccessDiagnostic.Basis.NOT_EVALUATED);
    }

    private List<OssAccessDiagnostic.Fact> unknownPolicyFacts(OssAccessDiagnostic.Basis basis) {
        return List.of(
            fact(OssAccessDiagnostic.Subject.POLICY_READ, OssAccessDiagnostic.Observation.UNKNOWN,
                OssAccessDiagnostic.Source.BUCKET_POLICY, OssAccessDiagnostic.Scope.BUCKET, basis),
            fact(OssAccessDiagnostic.Subject.POLICY_WRITE, OssAccessDiagnostic.Observation.UNKNOWN,
                OssAccessDiagnostic.Source.BUCKET_POLICY, OssAccessDiagnostic.Scope.BUCKET, basis));
    }

    private List<OssAccessDiagnostic.Fact> anonymousReadFacts(String bucket, String key, Duration timeout) {
        List<OssAccessDiagnostic.Fact> facts = new ArrayList<>(2);
        try {
            // 自定义域名已经绑定默认桶，与 DefaultOssObjectStore.publicUrl 使用相同的基础地址。
            String base = config.domain().filter(StringUtils::isNotBlank)
                .map(ignored -> config.getDomainUrl()).orElseGet(() -> config.getBucketUrl(bucket));
            URI baseUri = URI.create(base);
            if (baseUri.getHost() == null || baseUri.getUserInfo() != null
                || baseUri.getRawQuery() != null || baseUri.getRawFragment() != null) {
                throw new IllegalArgumentException("invalid diagnostic URL base");
            }
            String normalized = base;
            while (normalized.endsWith(StringUtils.SLASH)) {
                normalized = normalized.substring(0, normalized.length() - 1);
            }
            String encodedKey = SdkHttpUtils.urlEncodeIgnoreSlashes(key);
            URI objectUri = URI.create(normalized + StringUtils.SLASH + encodedKey);
            try (HttpClient client = HttpClient.newBuilder().connectTimeout(timeout)
                .followRedirects(HttpClient.Redirect.NEVER).build()) {
                HttpRequest head = HttpRequest.newBuilder(objectUri).timeout(timeout)
                    .method("HEAD", HttpRequest.BodyPublishers.noBody()).build();
                facts.add(anonymousProbe(client, head, OssAccessDiagnostic.Subject.OBJECT_HEAD,
                    OssAccessDiagnostic.Source.ANONYMOUS_HEAD));
                if (Thread.currentThread().isInterrupted()) {
                    facts.add(fact(OssAccessDiagnostic.Subject.OBJECT_GET, OssAccessDiagnostic.Observation.UNKNOWN,
                        OssAccessDiagnostic.Source.ANONYMOUS_GET, OssAccessDiagnostic.Scope.OBJECT,
                        OssAccessDiagnostic.Basis.INTERRUPTED));
                } else {
                    HttpRequest get = HttpRequest.newBuilder(objectUri).timeout(timeout)
                        .header("Range", "bytes=0-0").GET().build();
                    facts.add(anonymousProbe(client, get, OssAccessDiagnostic.Subject.OBJECT_GET,
                        OssAccessDiagnostic.Source.ANONYMOUS_GET));
                }
            }
        } catch (RuntimeException invalidUriOrClient) {
            for (OssAccessDiagnostic.Subject subject : List.of(OssAccessDiagnostic.Subject.OBJECT_HEAD,
                OssAccessDiagnostic.Subject.OBJECT_GET)) {
                if (facts.stream().noneMatch(existing -> existing.subject() == subject)) {
                    facts.add(fact(subject, OssAccessDiagnostic.Observation.UNKNOWN,
                        subject == OssAccessDiagnostic.Subject.OBJECT_HEAD
                            ? OssAccessDiagnostic.Source.ANONYMOUS_HEAD : OssAccessDiagnostic.Source.ANONYMOUS_GET,
                        OssAccessDiagnostic.Scope.OBJECT, OssAccessDiagnostic.Basis.NETWORK_ERROR));
                }
            }
        }
        return facts;
    }

    private OssAccessDiagnostic.Fact anonymousProbe(HttpClient client, HttpRequest request,
                                                    OssAccessDiagnostic.Subject subject,
                                                    OssAccessDiagnostic.Source source) {
        try {
            int status;
            HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
            try (InputStream body = response.body()) {
                status = response.statusCode();
            }
            OssAccessDiagnostic.Observation observation = status >= 200 && status < 300
                ? OssAccessDiagnostic.Observation.ALLOWED
                : status == 401 || status == 403 ? OssAccessDiagnostic.Observation.DENIED
                : OssAccessDiagnostic.Observation.UNKNOWN;
            OssAccessDiagnostic.Basis basis = status >= 200 && status < 300
                ? OssAccessDiagnostic.Basis.HTTP_SUCCESS
                : status == 401 || status == 403 ? OssAccessDiagnostic.Basis.HTTP_DENIED
                : status == 404 ? OssAccessDiagnostic.Basis.HTTP_NOT_FOUND
                : status >= 300 && status < 400 ? OssAccessDiagnostic.Basis.REDIRECT
                : OssAccessDiagnostic.Basis.HTTP_ERROR;
            return fact(subject, observation, source, OssAccessDiagnostic.Scope.OBJECT, basis);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            return fact(subject, OssAccessDiagnostic.Observation.UNKNOWN, source,
                OssAccessDiagnostic.Scope.OBJECT, OssAccessDiagnostic.Basis.INTERRUPTED);
        } catch (java.net.http.HttpTimeoutException timedOut) {
            return fact(subject, OssAccessDiagnostic.Observation.UNKNOWN, source,
                OssAccessDiagnostic.Scope.OBJECT, OssAccessDiagnostic.Basis.TIMEOUT);
        } catch (IOException networkFailure) {
            return fact(subject, OssAccessDiagnostic.Observation.UNKNOWN, source,
                OssAccessDiagnostic.Scope.OBJECT, OssAccessDiagnostic.Basis.NETWORK_ERROR);
        }
    }

    private OssAccessDiagnostic.Fact fact(OssAccessDiagnostic.Subject subject,
                                          OssAccessDiagnostic.Observation observation,
                                          OssAccessDiagnostic.Source source, OssAccessDiagnostic.Scope scope,
                                          OssAccessDiagnostic.Basis basis) {
        return new OssAccessDiagnostic.Fact(subject, observation, source, scope, basis, Instant.now());
    }

    private boolean isAllUsers(software.amazon.awssdk.services.s3.model.Grant grant) {
        return grant.grantee() != null
            && "http://acs.amazonaws.com/groups/global/AllUsers".equals(grant.grantee().uri());
    }

    private boolean isNoSuchBucketPolicy(RuntimeException ex) {
        Throwable cause = unwrapAsyncException(ex);
        return cause instanceof S3Exception error && error.awsErrorDetails() != null
            && "NoSuchBucketPolicy".equals(error.awsErrorDetails().errorCode());
    }

    private boolean isPolicyReadDenied(RuntimeException ex) {
        Throwable cause = unwrapAsyncException(ex);
        if (!(cause instanceof AwsServiceException serviceException)) {
            return false;
        }
        return serviceException.statusCode() == 403 || serviceException.awsErrorDetails() != null
            && "AccessDenied".equals(serviceException.awsErrorDetails().errorCode());
    }

    private OssAccessDiagnostic.Basis failureBasis(RuntimeException ex) {
        Throwable cause = unwrapAsyncException(ex);
        if (cause instanceof TimeoutException || cause instanceof java.net.http.HttpTimeoutException) {
            return OssAccessDiagnostic.Basis.TIMEOUT;
        }
        if (cause instanceof InterruptedException) {
            return OssAccessDiagnostic.Basis.INTERRUPTED;
        }
        if (cause instanceof AwsServiceException response) {
            int status = response.statusCode();
            if (status == 401 || status == 403) {
                return OssAccessDiagnostic.Basis.HTTP_DENIED;
            }
            if (status == 404) {
                return OssAccessDiagnostic.Basis.HTTP_NOT_FOUND;
            }
            if (status >= 300 && status < 400) {
                return OssAccessDiagnostic.Basis.REDIRECT;
            }
            return OssAccessDiagnostic.Basis.HTTP_ERROR;
        }
        return OssAccessDiagnostic.Basis.NETWORK_ERROR;
    }

    private OssAccessDiagnostic.Reason diagnosticFailure(RuntimeException ex,
                                                          OssAccessDiagnostic.Reason fallback) {
        Throwable cause = unwrapAsyncException(ex);
        if (cause instanceof TimeoutException || cause instanceof java.net.http.HttpTimeoutException) {
            return OssAccessDiagnostic.Reason.TIMEOUT;
        }
        if (fallback == OssAccessDiagnostic.Reason.DIAGNOSTIC_OBJECT_MISSING
            && cause instanceof S3Exception s3Exception && s3Exception.statusCode() == 404) {
            return fallback;
        }
        return fallback == OssAccessDiagnostic.Reason.DIAGNOSTIC_OBJECT_MISSING
            ? OssAccessDiagnostic.Reason.PROVIDER_ERROR : fallback;
    }

    private <T> T await(java.util.concurrent.CompletableFuture<T> future, Duration timeout) {
        try {
            return future.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException timedOut) {
            future.cancel(true);
            throw new CompletionException(timedOut);
        } catch (InterruptedException interrupted) {
            future.cancel(true);
            Thread.currentThread().interrupt();
            throw new CompletionException(interrupted);
        } catch (ExecutionException failed) {
            throw new CompletionException(failed.getCause());
        }
    }

    private OssAccessDiagnostic diagnostic(OssAccessDiagnostic.Verification verification,
                                           OssAccessDiagnostic.Reason reason, AccessPolicy policy,
                                           List<OssAccessDiagnostic.Fact> facts, Instant checkedAt) {
        return new OssAccessDiagnostic(verification, reason, policy, facts, checkedAt);
    }

    @Override
    public OssBucketConfiguration bucketConfiguration() {
        String bucket = config.bucket()
            .filter(value -> !value.isBlank())
            .orElseThrow(() -> S3StorageException.form("bucket is not configured."));
        List<String> issues = new ArrayList<>();
        boolean explicitOrigins = false;
        boolean allowsPut = false;
        boolean exposesEtag = false;
        boolean abortIncomplete = false;
        try {
            var cors = s3AsyncClient.getBucketCors(builder -> builder.bucket(bucket)).join();
            explicitOrigins = cors.corsRules().stream()
                .flatMap(rule -> rule.allowedOrigins().stream())
                .anyMatch(origin -> !"*".equals(origin));
            allowsPut = cors.corsRules().stream()
                .flatMap(rule -> rule.allowedMethods().stream())
                .anyMatch("PUT"::equalsIgnoreCase);
            exposesEtag = cors.corsRules().stream()
                .flatMap(rule -> rule.exposeHeaders().stream())
                .anyMatch("ETag"::equalsIgnoreCase);
        } catch (RuntimeException ex) {
            issues.add("无法读取 Bucket CORS: " + rootMessage(ex));
        }
        try {
            var lifecycle = s3AsyncClient.getBucketLifecycleConfiguration(builder -> builder.bucket(bucket)).join();
            abortIncomplete = lifecycle.rules().stream()
                .anyMatch(rule -> rule.status() == software.amazon.awssdk.services.s3.model.ExpirationStatus.ENABLED
                    && rule.abortIncompleteMultipartUpload() != null);
        } catch (RuntimeException ex) {
            issues.add("无法读取 Bucket Lifecycle: " + rootMessage(ex));
        }
        return new OssBucketConfiguration(bucket, explicitOrigins, allowsPut, exposesEtag, abortIncomplete, issues);
    }

    private String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return StringUtils.defaultIfBlank(current.getMessage(), current.getClass().getSimpleName());
    }

    @Override
    public OssPresignedRequest bucketPresignGet(String bucket, String key, Duration expiredTime) {
        validateObjectIdentity(bucket, key);
        validatePresignInput(key, expiredTime);
        if (useBucketBoundDomain(bucket)) {
            return bucketBoundDomainPresignRequest(SdkHttpMethod.GET, key, expiredTime, Map.of(), Map.of());
        }
        try {
            PresignedRequest request = s3Presigner.presignGetObject(builder -> builder
                .signatureDuration(expiredTime)
                .getObjectRequest(getBuilder -> getBuilder.bucket(bucket).key(key)));
            return toPresignedRequest(request);
        } catch (Exception e) {
            throw toStorageException(e);
        }
    }

    @Override
    public OssPresignedRequest bucketPresignPut(String bucket, String key, Duration expiredTime, OssObjectOptions options) {
        validateObjectIdentity(bucket, key);
        validatePresignInput(key, expiredTime);
        OssObjectOptions actualOptions = options == null ? OssObjectOptions.empty() : options;
        requireSupportedChecksum(actualOptions);
        Map<String, String> requiredHeaders = objectHeaders(actualOptions);
        if (useBucketBoundDomain(bucket)) {
            return bucketBoundDomainPresignRequest(SdkHttpMethod.PUT, key, expiredTime, requiredHeaders, Map.of());
        }
        try {
            PresignedRequest request = s3Presigner.presignPutObject(builder -> builder
                .signatureDuration(expiredTime)
                .putObjectRequest(putBuilder -> {
                    putBuilder.bucket(bucket).key(key).metadata(actualOptions.metadata());
                    if (StringUtils.isNotBlank(actualOptions.contentType())) {
                        putBuilder.contentType(actualOptions.contentType());
                    }
                }));
            return toPresignedRequest(request);
        } catch (Exception e) {
            throw toStorageException(e);
        }
    }

    @Override
    public OssPresignedRequest bucketPresignUploadPart(String bucket, String key, String uploadId, int partNumber, Duration expiredTime) {
        validateObjectIdentity(bucket, key);
        validatePresignInput(key, expiredTime);
        if (StringUtils.isBlank(uploadId)) {
            throw invalidRequest("uploadId must not be blank.");
        }
        if (partNumber < 1 || partNumber > 10_000) {
            throw invalidRequest("partNumber must be between 1 and 10000.");
        }
        if (useBucketBoundDomain(bucket)) {
            return bucketBoundDomainPresignRequest(
                SdkHttpMethod.PUT,
                key,
                expiredTime,
                Map.of(),
                Map.of("partNumber", Integer.toString(partNumber), "uploadId", uploadId)
            );
        }
        try {
            UploadPartRequest uploadPartRequest = UploadPartRequest.builder()
                .bucket(bucket)
                .key(key)
                .uploadId(uploadId)
                .partNumber(partNumber)
                .build();
            PresignedRequest request = s3Presigner.presignUploadPart(builder -> builder
                .signatureDuration(expiredTime)
                .uploadPartRequest(uploadPartRequest));
            return toPresignedRequest(request);
        } catch (Exception e) {
            throw toStorageException(e);
        }
    }

    /**
     * 是否使用已绑定默认桶的自定义域名生成无桶名预签名 URL。
     *
     * @param bucket 存储桶名称
     * @return 是否使用自定义域名签名
     */
    private boolean useBucketBoundDomain(String bucket) {
        return config.domain()
            .filter(StringUtils::isNotBlank)
            .isPresent() && config.bucket()
            .filter(defaultBucket -> Objects.equals(defaultBucket, bucket))
            .isPresent();
    }

    /**
     * 使用绑定默认桶的自定义域名生成预签名 URL，避免 S3 SDK 自动拼接桶名。
     *
     * @param method      HTTP 方法
     * @param key         对象键
     * @param expiredTime 过期时间
     * @param metadata    对象元数据
     * @return 预签名 URL
     */
    private OssPresignedRequest bucketBoundDomainPresignRequest(
        SdkHttpMethod method,
        String key,
        Duration expiredTime,
        Map<String, String> requiredHeaders,
        Map<String, String> queryParameters
    ) {
        try {
            URI domainUri = URI.create(config.getDomainUrl());
            SdkHttpFullRequest.Builder requestBuilder = SdkHttpFullRequest.builder()
                .method(method)
                .uri(domainUri)
                .encodedPath(bucketBoundDomainPath(domainUri, key));
            requiredHeaders.forEach(requestBuilder::putHeader);
            queryParameters.forEach(requestBuilder::putRawQueryParameter);
            AwsCredentialsIdentity credentials = AwsCredentialsIdentity.create(
                config.accessKey()
                    .filter(StringUtils::isNotBlank)
                    .orElseThrow(() -> S3StorageException.form(OssErrorCode.CONFIGURATION, "accessKey is not configured.")),
                config.secretKey()
                    .filter(StringUtils::isNotBlank)
                    .orElseThrow(() -> S3StorageException.form(OssErrorCode.CONFIGURATION, "secretKey is not configured."))
            );
            Instant signedAt = Instant.now();
            Clock signingClock = Clock.fixed(signedAt, ZoneOffset.UTC);
            SdkHttpRequest signedRequest = AwsV4HttpSigner.create()
                .sign(SignRequest.builder(credentials)
                    .request(requestBuilder.build())
                    .putProperty(AwsV4HttpSigner.REGION_NAME, config.region().orElse(Region.US_EAST_1).id())
                    .putProperty(AwsV4FamilyHttpSigner.SERVICE_SIGNING_NAME, "s3")
                    .putProperty(AwsV4FamilyHttpSigner.AUTH_LOCATION, AwsV4FamilyHttpSigner.AuthLocation.QUERY_STRING)
                    .putProperty(AwsV4FamilyHttpSigner.PAYLOAD_SIGNING_ENABLED, false)
                    .putProperty(AwsV4FamilyHttpSigner.EXPIRATION_DURATION, expiredTime)
                    .putProperty(HttpSigner.SIGNING_CLOCK, signingClock)
                    .putProperty(AwsV4FamilyHttpSigner.DOUBLE_URL_ENCODE, false)
                    .putProperty(AwsV4FamilyHttpSigner.NORMALIZE_PATH, false)
                    .build())
                .request();
            return new OssPresignedRequest(
                method.name(),
                signedRequest.getUri().toString(),
                requiredHeaders,
                signedAt.plus(expiredTime)
            );
        } catch (Exception e) {
            throw toStorageException(e);
        }
    }

    private OssPresignedRequest toPresignedRequest(PresignedRequest request) {
        Map<String, String> requiredHeaders = new TreeMap<>();
        request.signedHeaders().forEach((name, values) -> {
            if (!"host".equalsIgnoreCase(name)) {
                requiredHeaders.put(name.toLowerCase(Locale.ROOT), String.join(",", values));
            }
        });
        return new OssPresignedRequest(
            request.httpRequest().method().name(),
            request.url().toExternalForm(),
            requiredHeaders,
            request.expiration()
        );
    }

    private Map<String, String> objectHeaders(OssObjectOptions options) {
        Map<String, String> headers = new TreeMap<>();
        if (StringUtils.isNotBlank(options.contentType())) {
            headers.put("content-type", options.contentType());
        }
        options.metadata().forEach((name, value) -> {
            if (StringUtils.isNotBlank(name)) {
                headers.put("x-amz-meta-" + name.toLowerCase(Locale.ROOT), String.valueOf(value));
            }
        });
        return headers;
    }

    private void validatePresignInput(String key, Duration expiredTime) {
        if (StringUtils.isBlank(key)) {
            throw invalidRequest("key must not be blank.");
        }
        if (expiredTime == null || expiredTime.isZero() || expiredTime.isNegative() || expiredTime.compareTo(Duration.ofDays(7)) > 0) {
            throw invalidRequest("expiredTime must be between 1 nanosecond and 7 days.");
        }
    }

    private void requireSupportedChecksum(OssObjectOptions options) {
        if (options.checksumAlgorithm() != null && !capabilities().supportsChecksum(options.checksumAlgorithm())) {
            throw S3StorageException.form(
                OssErrorCode.UNSUPPORTED_CAPABILITY,
                "checksum algorithm is not supported by the configured provider: " + options.checksumAlgorithm()
            );
        }
    }

    private S3StorageException invalidRequest(String message) {
        return S3StorageException.form(OssErrorCode.INVALID_REQUEST, message);
    }

    /**
     * 构建绑定桶域名下的对象访问路径。
     *
     * @param domainUri 自定义域名 URI
     * @param key       对象键
     * @return 编码后的访问路径
     */
    private String bucketBoundDomainPath(URI domainUri, String key) {
        String basePath = Optional.ofNullable(domainUri.getRawPath())
            .filter(StringUtils::isNotBlank)
            .filter(path -> !"/".equals(path))
            .orElse("");
        String objectPath = SdkHttpUtils.urlEncodeIgnoreSlashes(key);
        if (!basePath.startsWith("/")) {
            basePath = "/" + basePath;
        }
        if (!basePath.endsWith("/")) {
            basePath = basePath + "/";
        }
        return basePath + objectPath;
    }

    /**
     * 上传本地路径文件到默认存储桶。
     *
     * @param key     对象键
     * @param path    文件路径
     * @param options 上传选项
     * @return 上传结果
     */
    @Override
    public PutObjectResult upload(String key, Path path, Options options) {
        return bucketUpload(defaultBucket(), key, path, options);
    }

    /**
     * 上传本地路径文件到默认存储桶。
     *
     * @param key  对象键
     * @param path 文件路径
     * @return 上传结果
     */
    @Override
    public PutObjectResult upload(String key, Path path) {
        return bucketUpload(defaultBucket(), key, path);
    }

    /**
     * 上传文件到默认存储桶。
     *
     * @param key     对象键
     * @param file    文件对象
     * @param options 上传选项
     * @return 上传结果
     */
    @Override
    public PutObjectResult upload(String key, File file, Options options) {
        return bucketUpload(defaultBucket(), key, file, options);
    }

    /**
     * 上传文件到默认存储桶。
     *
     * @param key  对象键
     * @param file 文件对象
     * @return 上传结果
     */
    @Override
    public PutObjectResult upload(String key, File file) {
        return bucketUpload(defaultBucket(), key, file);
    }

    /**
     * 上传随机访问文件到默认存储桶。
     *
     * @param key     对象键
     * @param file    随机访问文件
     * @param options 上传选项
     * @return 上传结果
     */
    @Override
    public PutObjectResult upload(String key, RandomAccessFile file, Options options) {
        return bucketUpload(defaultBucket(), key, file, options);
    }

    /**
     * 上传随机访问文件到默认存储桶。
     *
     * @param key  对象键
     * @param file 随机访问文件
     * @return 上传结果
     */
    @Override
    public PutObjectResult upload(String key, RandomAccessFile file) {
        return bucketUpload(defaultBucket(), key, file);
    }

    /**
     * 上传可读通道数据到默认存储桶。
     *
     * @param key           对象键
     * @param channel       可读通道
     * @param contentLength 内容长度
     * @param options       上传选项
     * @return 上传结果
     */
    @Override
    public PutObjectResult upload(String key, ReadableByteChannel channel, long contentLength, Options options) {
        return bucketUpload(defaultBucket(), key, channel, contentLength, options);
    }

    /**
     * 上传可读通道数据到默认存储桶。
     *
     * @param key           对象键
     * @param channel       可读通道
     * @param contentLength 内容长度
     * @return 上传结果
     */
    @Override
    public PutObjectResult upload(String key, ReadableByteChannel channel, long contentLength) {
        return bucketUpload(defaultBucket(), key, channel, contentLength);
    }

    /**
     * 上传输入流数据到默认存储桶。
     *
     * @param key           对象键
     * @param in            输入流
     * @param contentLength 内容长度
     * @param options       上传选项
     * @return 上传结果
     */
    @Override
    public PutObjectResult upload(String key, InputStream in, long contentLength, Options options) {
        return bucketUpload(defaultBucket(), key, in, contentLength, options);
    }

    /**
     * 上传输入流数据到默认存储桶。
     *
     * @param key           对象键
     * @param in            输入流
     * @param contentLength 内容长度
     * @return 上传结果
     */
    @Override
    public PutObjectResult upload(String key, InputStream in, long contentLength) {
        return bucketUpload(defaultBucket(), key, in, contentLength);
    }

    /**
     * 上传字节数组到默认存储桶。
     *
     * @param key     对象键
     * @param data    字节数组
     * @param options 上传选项
     * @return 上传结果
     */
    @Override
    public PutObjectResult upload(String key, byte[] data, Options options) {
        return bucketUpload(defaultBucket(), key, data, options);
    }

    /**
     * 上传字节数组到默认存储桶。
     *
     * @param key  对象键
     * @param data 字节数组
     * @return 上传结果
     */
    @Override
    public PutObjectResult upload(String key, byte[] data) {
        return bucketUpload(defaultBucket(), key, data);
    }

    /**
     * 将默认存储桶对象下载到订阅器。
     *
     * @param key                对象键
     * @param downloadSubscriber 下载订阅器
     * @return 下载结果
     */
    @Override
    public GetObjectResult download(String key, OutputStreamDownloadSubscriber downloadSubscriber) {
        return bucketDownload(defaultBucket(), key, downloadSubscriber);
    }

    /**
     * 将默认存储桶对象下载到转换器。
     *
     * @param key                 对象键
     * @param downloadTransformer 下载转换器
     * @param <T>                 下载结果类型
     * @return 下载结果
     */
    @Override
    public <T> T download(String key, BiFunction<GetObjectResult, InputStream, T> downloadTransformer) {
        return bucketDownload(defaultBucket(), key, downloadTransformer);
    }

    /**
     * 将默认存储桶对象下载到本地路径。
     *
     * @param key  对象键
     * @param path 本地路径
     * @return 下载结果
     */
    @Override
    public GetObjectResult download(String key, Path path) {
        return bucketDownload(defaultBucket(), key, path);
    }

    /**
     * 将默认存储桶对象下载到文件。
     *
     * @param key  对象键
     * @param file 本地文件
     * @return 下载结果
     */
    @Override
    public GetObjectResult download(String key, File file) {
        return bucketDownload(defaultBucket(), key, file);
    }

    /**
     * 将默认存储桶对象下载到随机访问文件。
     *
     * @param key  对象键
     * @param file 随机访问文件
     * @return 下载结果
     */
    @Override
    public GetObjectResult download(String key, RandomAccessFile file) {
        return bucketDownload(defaultBucket(), key, file);
    }

    /**
     * 将默认存储桶对象下载到可写通道。
     *
     * @param key     对象键
     * @param channel 可写通道
     * @return 下载结果
     */
    @Override
    public GetObjectResult download(String key, WritableByteChannel channel) {
        return bucketDownload(defaultBucket(), key, channel);
    }

    /**
     * 将默认存储桶对象下载到输出流。
     *
     * @param key 对象键
     * @param out 输出流
     * @return 下载结果
     */
    @Override
    public GetObjectResult download(String key, OutputStream out) {
        return bucketDownload(defaultBucket(), key, out);
    }

    /**
     * 删除默认存储桶中的对象。
     *
     * @param key 对象键
     * @return 是否删除成功
     */
    @Override
    public boolean delete(String key) {
        return bucketDelete(defaultBucket(), key);
    }

    /**
     * 生成默认存储桶对象的下载预签名 URL。
     *
     * @param key         对象键
     * @param expiredTime 过期时间
     * @return 预签名下载 URL
     */
    @Override
    public String presignGetUrl(String key, Duration expiredTime) {
        return bucketPresignGetUrl(defaultBucket(), key, expiredTime);
    }

    /**
     * 生成默认存储桶对象的上传预签名 URL。
     *
     * @param key         对象键
     * @param expiredTime 过期时间
     * @param metadata    对象元数据
     * @return 预签名上传 URL
     */
    @Override
    public String presignPutUrl(String key, Duration expiredTime, Map<String, String> metadata) {
        return bucketPresignPutUrl(defaultBucket(), key, expiredTime, metadata);
    }

    @Override
    public OssPresignedRequest presignGet(String key, Duration expiredTime) {
        return bucketPresignGet(defaultBucket(), key, expiredTime);
    }

    @Override
    public OssPresignedRequest presignPut(String key, Duration expiredTime, OssObjectOptions options) {
        return bucketPresignPut(defaultBucket(), key, expiredTime, options);
    }

    @Override
    public OssPresignedRequest presignUploadPart(String key, String uploadId, int partNumber, Duration expiredTime) {
        return bucketPresignUploadPart(defaultBucket(), key, uploadId, partNumber, expiredTime);
    }

    @Override
    public OssObjectStat headObject(String key) {
        return bucketHeadObject(defaultBucket(), key);
    }

    @Override
    public OssObjectStat headObject(String key, Duration timeout) {
        validateObjectIdentity(defaultBucket(), key);
        Duration budget = requireMigrationTimeout(timeout);
        try {
            var request = HeadObjectRequest.builder().bucket(defaultBucket()).key(key)
                .overrideConfiguration(builder -> builder.apiCallTimeout(budget).apiCallAttemptTimeout(budget))
                .build();
            var response = await(s3AsyncClient.headObject(request), budget);
            return new OssObjectStat(defaultBucket(), key,
                Optional.ofNullable(response.contentLength()).orElse(0L), response.contentType(),
                response.eTag(), response.lastModified(), response.metadata(),
                checksumMap(response.checksumCRC32(), response.checksumCRC32C(), response.checksumSHA1(), response.checksumSHA256()));
        } catch (RuntimeException ex) {
            throw toStorageException(ex);
        }
    }

    @Override
    public boolean delete(String key, Duration timeout) {
        validateObjectIdentity(defaultBucket(), key);
        Duration budget = requireMigrationTimeout(timeout);
        try {
            var request = software.amazon.awssdk.services.s3.model.DeleteObjectRequest.builder()
                .bucket(defaultBucket()).key(key)
                .overrideConfiguration(builder -> builder.apiCallTimeout(budget).apiCallAttemptTimeout(budget))
                .build();
            await(s3AsyncClient.deleteObject(request), budget);
            return true;
        } catch (RuntimeException ex) {
            throw toStorageException(ex);
        }
    }

    private Duration requireMigrationTimeout(Duration timeout) {
        if (timeout == null || timeout.isNegative() || timeout.isZero() || timeout.toMillis() < 1) {
            throw new IllegalArgumentException("OSS migration timeout must be positive");
        }
        return timeout;
    }

    @Override
    public OssMultipartUpload createMultipartUpload(String key, OssObjectOptions options) {
        return bucketCreateMultipartUpload(defaultBucket(), key, options);
    }

    @Override
    public List<OssMultipartPart> listParts(String key, String uploadId) {
        return bucketListParts(defaultBucket(), key, uploadId);
    }

    @Override
    public OssMultipartCompleteResult completeMultipartUpload(String key, String uploadId, List<OssCompletedPart> parts) {
        return bucketCompleteMultipartUpload(defaultBucket(), key, uploadId, parts);
    }

    @Override
    public boolean abortMultipartUpload(String key, String uploadId) {
        return bucketAbortMultipartUpload(defaultBucket(), key, uploadId);
    }

    @Override
    public OssCopyResult copyObject(String sourceKey, String targetKey) {
        String bucket = defaultBucket();
        return bucketCopyObject(bucket, sourceKey, bucket, targetKey);
    }

    /**
     * 获取默认存储桶名称。
     *
     * @return 默认存储桶名称
     */
    private String defaultBucket() {
        return config.bucket()
            .filter(bucket -> !bucket.isBlank())
            .orElseThrow(() -> S3StorageException.form(OssErrorCode.CONFIGURATION, "bucket is not configured."));
    }

    /**
     * 合并默认前缀与业务前缀。
     *
     * @param defaultPrefix  默认前缀
     * @param businessPrefix 业务前缀
     * @return 合并后的前缀
     */
    private String mergePrefix(String defaultPrefix, String businessPrefix) {
        String left = normalizePrefix(defaultPrefix);
        String right = normalizePrefix(businessPrefix);
        if (left.isEmpty()) {
            return right;
        }
        if (right.isEmpty()) {
            return left;
        }
        return left + StringUtils.SLASH + right;
    }

    /**
     * 规范化对象键前缀。
     *
     * @param prefix 原始前缀
     * @return 规范化后的前缀
     */
    private String normalizePrefix(String prefix) {
        if (prefix == null) {
            return "";
        }
        String normalized = prefix.trim();
        while (normalized.startsWith(StringUtils.SLASH)) {
            normalized = normalized.substring(1);
        }
        while (normalized.endsWith(StringUtils.SLASH)) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    /**
     * 提取文件扩展名。
     *
     * @param fileName 文件名
     * @return 文件扩展名
     */
    private String suffix(String fileName) {
        if (fileName == null) {
            return "";
        }
        int index = fileName.lastIndexOf('.');
        if (index < 0) {
            return "";
        }
        return fileName.substring(index);
    }

    /**
     * 转换为统一的 S3 存储异常。
     *
     * @param e 原始异常
     * @return S3 存储异常
     */
    private S3StorageException toStorageException(Throwable e) {
        Throwable cause = unwrapAsyncException(e);
        if (cause instanceof S3StorageException ex) {
            return ex;
        }
        if (cause instanceof S3Exception s3Exception) {
            OssErrorCode code = s3Exception.statusCode() == 404
                ? OssErrorCode.OBJECT_NOT_FOUND
                : OssErrorCode.PROVIDER_ERROR;
            String providerCode = Optional.ofNullable(s3Exception.awsErrorDetails())
                .map(details -> details.errorCode())
                .filter(StringUtils::isNotBlank)
                .map(value -> value.replaceAll("[^A-Za-z0-9_.-]", ""))
                .filter(StringUtils::isNotBlank)
                .orElse("unknown");
            return S3StorageException.form(
                code,
                "S3 provider request failed (status=" + s3Exception.statusCode() + ", code=" + providerCode + ")."
            );
        }
        return S3StorageException.form(OssErrorCode.PROVIDER_ERROR, "S3 provider operation failed.");
    }

    /**
     * 解包异步执行异常。
     *
     * @param e 原始异常
     * @return 根因异常
     */
    private Throwable unwrapAsyncException(Throwable e) {
        Throwable cause = e;
        while ((cause instanceof CompletionException || cause instanceof ExecutionException) && cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause;
    }

    /**
     * 关闭底层 S3 客户端资源。
     *
     * @throws Exception 关闭资源异常
     */
    @Override
    public void close() throws Exception {
        if (s3TransferManager != null) {
            s3TransferManager.close();
        }
        if (s3AsyncClient != null) {
            s3AsyncClient.close();
        }
        if (s3Presigner != null) {
            s3Presigner.close();
        }
        if (asyncExecutor != null) {
            asyncExecutor.close();
        }
        // 重置初始化状态为 false
        initialized.compareAndSet(true, false);
    }
}
