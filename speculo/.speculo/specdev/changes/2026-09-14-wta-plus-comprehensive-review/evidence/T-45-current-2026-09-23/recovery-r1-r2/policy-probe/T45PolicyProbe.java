import org.namewta.common.json.utils.JsonUtils;
import org.namewta.common.oss.client.DefaultOssClientImpl;
import org.namewta.common.oss.client.OssClient;
import org.namewta.common.oss.config.OssAsyncExecutorConfig;
import org.namewta.common.oss.config.OssClientConfig;
import org.namewta.common.oss.enums.AccessPolicy;
import org.namewta.common.oss.model.OssAccessDiagnostic;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import tools.jackson.databind.JsonNode;

import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;

/** One owned MinIO policy shape probe; stdout contains only fixed booleans, counts and enums. */
public final class T45PolicyProbe {
    private static final String KEY = "diagnostic/canary.txt";

    public static void main(String[] args) throws Exception {
        if (args.length != 0) throw new IllegalArgumentException("no arguments accepted");
        PrintStream safeOutput = System.out;
        System.setOut(new PrintStream(OutputStream.nullOutputStream()));
        URI endpoint = URI.create(required("T45_PROBE_ENDPOINT"));
        String accessKey = required("T45_PROBE_ACCESS_KEY");
        String secretKey = required("T45_PROBE_SECRET_KEY");
        String bucket = "t45-policy-probe-" + UUID.randomUUID().toString().replace("-", "");
        String policy = "{\"Version\":\"2012-10-17\",\"Statement\":[{"
            + "\"Effect\":\"Allow\",\"Principal\":{\"AWS\":[\"*\"]},"
            + "\"Action\":[\"s3:GetObject\"],"
            + "\"Resource\":[\"arn:aws:s3:::" + bucket + "/*\"]}]}";
        try (S3Client bootstrap = S3Client.builder().endpointOverride(endpoint)
            .region(Region.US_EAST_1)
            .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey)))
            .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build()).build()) {
            bootstrap.createBucket(request -> request.bucket(bucket));
            try {
                bootstrap.putObject(request -> request.bucket(bucket).key(KEY),
                    RequestBody.fromBytes("namewta-readiness-canary".getBytes(StandardCharsets.UTF_8)));
                bootstrap.putBucketPolicy(request -> request.bucket(bucket).policy(policy));
                String returned = bootstrap.getBucketPolicy(request -> request.bucket(bucket)).policy();
                JsonNode root = JsonUtils.getJsonMapper().readTree(returned);
                JsonNode statements = root.path("Statement");
                JsonNode statement = statements.isArray() && statements.size() == 1
                    ? statements.get(0) : statements;
                JsonNode principal = statement.path("Principal");
                JsonNode aws = principal.path("AWS");
                JsonNode actions = statement.path("Action");
                JsonNode resources = statement.path("Resource");
                boolean actionGet = actions.isArray() && actions.size() == 1
                    && "s3:GetObject".equals(actions.get(0).asText());
                boolean resourceMatch = resources.isArray() && resources.size() == 1
                    && ("arn:aws:s3:::" + bucket + "/*").equals(resources.get(0).asText());
                safeOutput.printf("SHAPE statement_array=%s statement_count=%d principal_aws_array=%s "
                    + "principal_aws_star=%s action_array=%s action_get=%s resource_array=%s "
                    + "resource_match=%s has_condition=%s has_not=%s submitted_equal=%s%n",
                    statements.isArray(), statements.isArray() ? statements.size() : 1,
                    aws.isArray(), aws.isArray() && aws.size() == 1 && "*".equals(aws.get(0).asText()),
                    actions.isArray(), actionGet, resources.isArray(), resourceMatch,
                    statement.has("Condition"), statement.has("NotAction") || statement.has("NotPrincipal")
                        || statement.has("NotResource"), policy.equals(returned));

                OssClientConfig config = OssClientConfig.builder()
                    .endpoint(endpoint.getAuthority()).useHttps("https".equalsIgnoreCase(endpoint.getScheme()))
                    .usePathStyleAccess(true).accessKey(accessKey).secretKey(secretKey).bucket(bucket)
                    .region(Region.US_EAST_1).prefix("")
                    .asyncExecutorConfig(OssAsyncExecutorConfig.DEFAULT).build();
                try (OssClient client = new DefaultOssClientImpl("policy-probe", config)) {
                    OssAccessDiagnostic result = client.diagnoseAccess(KEY, AccessPolicy.PUBLIC_READ,
                        Duration.ofSeconds(3));
                    safeOutput.println("SUMMARY verification=" + result.verification() + " reason=" + result.reason());
                    for (OssAccessDiagnostic.Fact fact : result.facts()) {
                        safeOutput.println("FACT subject=" + fact.subject() + " observation=" + fact.observation()
                            + " basis=" + fact.basis() + " scope=" + fact.scope());
                    }
                }
            } finally {
                bootstrap.deleteObject(request -> request.bucket(bucket).key(KEY));
                bootstrap.deleteBucketPolicy(request -> request.bucket(bucket));
                bootstrap.deleteBucket(request -> request.bucket(bucket));
            }
        }
    }

    private static String required(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) throw new IllegalStateException("missing private probe input");
        return value;
    }
}
