package org.namewta.common.openapi.gateway;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.namewta.common.openapi.annotation.OpenApi;
import org.namewta.common.openapi.config.properties.OpenApiProperties;
import org.namewta.common.openapi.nonce.OpenApiNonceStore;
import org.namewta.common.openapi.protocol.OpenApiAuthenticationException;
import org.namewta.common.openapi.protocol.OpenApiCanonicalizer;
import org.namewta.common.openapi.protocol.OpenApiHeaders;
import org.namewta.common.openapi.protocol.OpenApiRequest;
import org.namewta.common.openapi.protocol.OpenApiSigner;
import org.namewta.common.openapi.ratelimit.OpenApiRateLimiter;
import org.namewta.common.openapi.registry.OpenApiAuthorizationMatcher;
import org.namewta.common.openapi.registry.OpenApiOperationDefinition;
import org.namewta.common.openapi.registry.OpenApiOperationRegistry;
import org.namewta.common.openapi.session.OpenApiMachineSessionBridge;
import org.namewta.common.openapi.session.OpenApiMachineSessionException;
import org.namewta.common.openapi.session.VerifiedOpenApiIdentity;
import org.namewta.common.openapi.spi.OpenApiCallEvent;
import org.namewta.common.openapi.spi.OpenApiCallEventPublisher;
import org.namewta.common.openapi.spi.OpenApiCredential;
import org.namewta.common.openapi.spi.OpenApiCredentialResolver;
import org.namewta.common.satoken.utils.LoginHelper;
import org.namewta.system.api.model.LoginUser;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Servlet boundary for fully signed NAMEWTA v1 machine requests.
 */
public final class OpenApiGatewayFilter extends OncePerRequestFilter {

    public static final String VERIFIED_REQUEST_ATTRIBUTE = "org.namewta.openapi.verifiedRequest";
    private static final String RATE_INTERVAL_APP_PREFIX = "app:";
    private static final String RATE_INTERVAL_INTERFACE_PREFIX = "interface:";
    private static final Duration RATE_INTERVAL = Duration.ofMinutes(1);
    private static final List<String> SIGNATURE_HEADERS = List.of(
        OpenApiHeaders.VERSION, OpenApiHeaders.APP_KEY, OpenApiHeaders.TIMESTAMP,
        OpenApiHeaders.NONCE, OpenApiHeaders.SIGNATURE);

    private final RequestMappingHandlerMapping handlerMapping;
    private final OpenApiOperationRegistry operationRegistry;
    private final OpenApiCredentialResolver credentialResolver;
    private final OpenApiSigner signer;
    private final OpenApiNonceStore nonceStore;
    private final OpenApiRateLimiter rateLimiter;
    private final OpenApiMachineSessionBridge sessionBridge;
    private final OpenApiAuthorizationMatcher authorizationMatcher;
    private final OpenApiCallEventPublisher eventPublisher;
    private final OpenApiProperties properties;
    private final Clock clock;
    private final Supplier<LoginUser> loginUserSupplier;

    public OpenApiGatewayFilter(RequestMappingHandlerMapping handlerMapping,
                                OpenApiOperationRegistry operationRegistry,
                                OpenApiCredentialResolver credentialResolver,
                                OpenApiSigner signer,
                                OpenApiNonceStore nonceStore,
                                OpenApiRateLimiter rateLimiter,
                                OpenApiMachineSessionBridge sessionBridge,
                                OpenApiAuthorizationMatcher authorizationMatcher,
                                OpenApiCallEventPublisher eventPublisher,
                                OpenApiProperties properties) {
        this(handlerMapping, operationRegistry, credentialResolver, signer, nonceStore, rateLimiter,
            sessionBridge, authorizationMatcher, eventPublisher, properties, Clock.systemUTC(),
            LoginHelper::getLoginUser);
    }

    public OpenApiGatewayFilter(RequestMappingHandlerMapping handlerMapping,
                                OpenApiOperationRegistry operationRegistry,
                                OpenApiCredentialResolver credentialResolver,
                                OpenApiSigner signer,
                                OpenApiNonceStore nonceStore,
                                OpenApiRateLimiter rateLimiter,
                                OpenApiMachineSessionBridge sessionBridge,
                                OpenApiAuthorizationMatcher authorizationMatcher,
                                OpenApiCallEventPublisher eventPublisher,
                                OpenApiProperties properties,
                                Clock clock,
                                Supplier<LoginUser> loginUserSupplier) {
        this.handlerMapping = Objects.requireNonNull(handlerMapping, "handlerMapping");
        this.operationRegistry = Objects.requireNonNull(operationRegistry, "operationRegistry");
        this.credentialResolver = Objects.requireNonNull(credentialResolver, "credentialResolver");
        this.signer = Objects.requireNonNull(signer, "signer");
        this.nonceStore = Objects.requireNonNull(nonceStore, "nonceStore");
        this.rateLimiter = Objects.requireNonNull(rateLimiter, "rateLimiter");
        this.sessionBridge = Objects.requireNonNull(sessionBridge, "sessionBridge");
        this.authorizationMatcher = Objects.requireNonNull(authorizationMatcher, "authorizationMatcher");
        this.eventPublisher = Objects.requireNonNull(eventPublisher, "eventPublisher");
        this.properties = Objects.requireNonNull(properties, "properties");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.loginUserSupplier = Objects.requireNonNull(loginUserSupplier, "loginUserSupplier");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {
        int presentHeaders = (int) SIGNATURE_HEADERS.stream().filter(name -> request.getHeader(name) != null).count();
        if (presentHeaders == 0) {
            filterChain.doFilter(request, response);
            return;
        }

        Invocation invocation = new Invocation(request.getMethod(), request.getRequestURI(), clock.instant());
        Throwable downstreamFailure = null;
        try {
            if (presentHeaders != SIGNATURE_HEADERS.size() || hasBrowserAuthentication(request)) {
                throw new OpenApiAuthenticationException();
            }
            ReplayableOpenApiRequest replayable = new ReplayableOpenApiRequest(request);
            invocation.operation = resolveOperation(replayable);
            invocation.credential = authenticate(replayable);
            enforceReplayAndRates(replayable, invocation.operation);
            VerifiedOpenApiIdentity identity = new VerifiedOpenApiIdentity(
                invocation.credential.credentialId(), invocation.credential.ownerUserId());
            try {
                sessionBridge.execute(identity, () -> {
                    LoginUser loginUser = loginUserSupplier.get();
                    if (!authorizationMatcher.matches(loginUser, invocation.operation.accessRule())) {
                        throw OpenApiGatewayException.forbidden();
                    }
                    Object previous = replayable.getAttribute(VERIFIED_REQUEST_ATTRIBUTE);
                    replayable.setAttribute(VERIFIED_REQUEST_ATTRIBUTE, Boolean.TRUE);
                    try {
                        filterChain.doFilter(replayable, response);
                    } catch (IOException | ServletException | RuntimeException exception) {
                        throw new DownstreamException(exception);
                    } finally {
                        restoreAttribute(replayable, previous);
                    }
                    return null;
                });
            } catch (OpenApiMachineSessionException exception) {
                throw OpenApiGatewayException.unavailable(exception);
            }
        } catch (DownstreamException exception) {
            downstreamFailure = exception.getCause();
        } catch (OpenApiAuthenticationException exception) {
            writeError(response, 401, OpenApiAuthenticationException.ERROR_CODE);
        } catch (OpenApiGatewayException exception) {
            writeError(response, exception.status(), exception.errorCode());
        } catch (OpenApiStateStoreException exception) {
            writeError(response, 503, OpenApiGatewayException.UNAVAILABLE);
        } catch (RuntimeException exception) {
            writeError(response, 503, OpenApiGatewayException.UNAVAILABLE);
        } finally {
            publishEvent(invocation, response, downstreamFailure);
        }
        if (downstreamFailure instanceof IOException exception) {
            throw exception;
        }
        if (downstreamFailure instanceof ServletException exception) {
            throw exception;
        }
        if (downstreamFailure instanceof RuntimeException exception) {
            throw exception;
        }
    }

    private OpenApiOperationDefinition resolveOperation(HttpServletRequest request) {
        try {
            HandlerExecutionChain chain = handlerMapping.getHandler(request);
            if (chain == null || !(chain.getHandler() instanceof HandlerMethod handler)
                || handler.getMethod().getDeclaredAnnotation(OpenApi.class) == null) {
                throw OpenApiGatewayException.forbidden();
            }
            String requestMethod = request.getMethod().toUpperCase(Locale.ROOT);
            String matchedPath = Objects.toString(
                request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE), "");
            if (matchedPath.isBlank()) {
                throw OpenApiGatewayException.forbidden();
            }
            return handlerMapping.getHandlerMethods().entrySet().stream()
                .filter(entry -> sameHandler(entry.getValue(), handler))
                .filter(entry -> methodMatches(entry.getKey(), requestMethod))
                .flatMap(entry -> paths(entry.getKey()).stream())
                .filter(matchedPath::equals)
                .flatMap(path -> operationRegistry.all().stream()
                    .filter(item -> requestMethod.equals(item.method()) && path.equals(item.path())))
                .distinct()
                .reduce((left, right) -> {
                    throw OpenApiGatewayException.forbidden();
                })
                .orElseThrow(OpenApiGatewayException::forbidden);
        } catch (OpenApiGatewayException exception) {
            throw exception;
        } catch (Exception exception) {
            throw OpenApiGatewayException.unavailable(exception);
        }
    }

    private OpenApiCredential authenticate(ReplayableOpenApiRequest request) {
        String version = requiredHeader(request, OpenApiHeaders.VERSION);
        String appKey = requiredHeader(request, OpenApiHeaders.APP_KEY);
        String timestamp = requiredHeader(request, OpenApiHeaders.TIMESTAMP);
        String nonce = requiredHeader(request, OpenApiHeaders.NONCE);
        String signature = requiredHeader(request, OpenApiHeaders.SIGNATURE);
        if (!OpenApiCanonicalizer.VERSION.equals(version)
            || !validBase64Url(appKey, 16)
            || !validBase64Url(nonce, 16)
            || !validBase64Url(signature, 32)) {
            throw new OpenApiAuthenticationException();
        }
        Instant requestedAt = parseTimestamp(timestamp);
        Duration skew = requirePositive(properties.getClockSkew());
        if (requestedAt.isBefore(clock.instant().minus(skew)) || requestedAt.isAfter(clock.instant().plus(skew))) {
            throw new OpenApiAuthenticationException();
        }
        OpenApiCredential credential = credentialResolver.resolve(appKey);
        if (credential == null || !appKey.equals(credential.appKey())
            || credential.credentialId() == null || credential.ownerUserId() == null
            || credential.appSecret() == null
            || credential.expiresAt() != null && !credential.expiresAt().isAfter(clock.instant())) {
            throw new OpenApiAuthenticationException();
        }
        OpenApiRequest signedRequest = new OpenApiRequest(appKey, timestamp, nonce, request.getMethod(),
            request.getRequestURI(), request.getQueryString(), request.body());
        if (!signer.verify(signedRequest, credential.appSecret(), signature)) {
            throw new OpenApiAuthenticationException();
        }
        return credential;
    }

    private void enforceReplayAndRates(HttpServletRequest request, OpenApiOperationDefinition operation) {
        String appKey = request.getHeader(OpenApiHeaders.APP_KEY);
        String nonce = request.getHeader(OpenApiHeaders.NONCE);
        try {
            if (!nonceStore.register(appKey, nonce, requirePositive(properties.getNonceTtl()))) {
                throw new OpenApiAuthenticationException();
            }
            boolean appAllowed = rateLimiter.acquire(RATE_INTERVAL_APP_PREFIX + appKey,
                properties.getAppRateLimitPerMinute(), RATE_INTERVAL);
            boolean interfaceAllowed = rateLimiter.acquire(
                RATE_INTERVAL_INTERFACE_PREFIX + appKey + ':' + operation.interfaceId(),
                properties.getInterfaceRateLimitPerMinute(), RATE_INTERVAL);
            if (!appAllowed || !interfaceAllowed) {
                throw OpenApiGatewayException.rateLimited();
            }
        } catch (OpenApiAuthenticationException | OpenApiGatewayException | OpenApiStateStoreException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new OpenApiStateStoreException(exception);
        }
    }

    private void publishEvent(Invocation invocation, HttpServletResponse response, Throwable downstreamFailure) {
        if (invocation.credential == null || invocation.operation == null) {
            return;
        }
        int status = downstreamFailure == null ? response.getStatus() : 500;
        long durationMillis = Math.max(0, Duration.between(invocation.startedAt, clock.instant()).toMillis());
        OpenApiCallEvent event = new OpenApiCallEvent(
            invocation.credential.credentialId(), invocation.credential.ownerUserId(),
            invocation.operation.interfaceId(), invocation.method, invocation.path,
            status, durationMillis, clock.instant());
        try {
            eventPublisher.publish(event);
        } catch (RuntimeException ignored) {
            // Metering is optional and cannot alter the determined HTTP response.
        }
    }

    private static boolean hasBrowserAuthentication(HttpServletRequest request) {
        if (request.getHeader("Authorization") != null || request.getHeader("Token") != null) {
            return true;
        }
        Cookie[] cookies = request.getCookies();
        if (cookies != null && java.util.Arrays.stream(cookies)
            .anyMatch(cookie -> isBrowserTokenName(cookie.getName()))) {
            return true;
        }
        String query = request.getQueryString();
        if (query == null || query.isEmpty()) {
            return false;
        }
        return java.util.Arrays.stream(query.split("&", -1))
            .map(pair -> pair.contains("=") ? pair.substring(0, pair.indexOf('=')) : pair)
            .map(OpenApiGatewayFilter::decodeQueryName)
            .anyMatch(OpenApiGatewayFilter::isBrowserTokenName);
    }

    private static String decodeQueryName(String value) {
        try {
            return URLDecoder.decode(value, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException exception) {
            return value;
        }
    }

    private static boolean isBrowserTokenName(String value) {
        return "Authorization".equals(value) || "Token".equals(value);
    }

    private static String requiredHeader(HttpServletRequest request, String name) {
        String value = request.getHeader(name);
        if (value == null || value.isBlank()) {
            throw new OpenApiAuthenticationException();
        }
        return value;
    }

    private static Instant parseTimestamp(String value) {
        try {
            if (!value.matches("0|[1-9][0-9]{0,18}")) {
                throw new OpenApiAuthenticationException();
            }
            return Instant.ofEpochSecond(Long.parseLong(value));
        } catch (RuntimeException exception) {
            throw new OpenApiAuthenticationException();
        }
    }

    private static boolean validBase64Url(String value, int minimumBytes) {
        if (!value.matches("[A-Za-z0-9_-]+") || value.contains("=")) {
            return false;
        }
        try {
            byte[] decoded = Base64.getUrlDecoder().decode(value);
            return decoded.length >= minimumBytes
                && Base64.getUrlEncoder().withoutPadding().encodeToString(decoded).equals(value);
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private static Duration requirePositive(Duration value) {
        if (value == null || value.isZero() || value.isNegative()) {
            throw new OpenApiAuthenticationException();
        }
        return value;
    }

    private static boolean methodMatches(RequestMappingInfo mapping, String method) {
        return mapping.getMethodsCondition().getMethods().stream().anyMatch(item -> item.name().equals(method));
    }

    private static boolean sameHandler(HandlerMethod left, HandlerMethod right) {
        return left.getBeanType().equals(right.getBeanType()) && left.getMethod().equals(right.getMethod());
    }

    private static List<String> paths(RequestMappingInfo mapping) {
        return mapping.getPathPatternsCondition() == null
            ? List.of()
            : mapping.getPathPatternsCondition().getPatternValues().stream().sorted().toList();
    }

    private static void restoreAttribute(HttpServletRequest request, Object previous) {
        if (previous == null) {
            request.removeAttribute(VERIFIED_REQUEST_ATTRIBUTE);
        } else {
            request.setAttribute(VERIFIED_REQUEST_ATTRIBUTE, previous);
        }
    }

    private static void writeError(HttpServletResponse response, int status, String errorCode) throws IOException {
        if (response.isCommitted()) {
            return;
        }
        response.resetBuffer();
        response.setStatus(status);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/json");
        response.getWriter().write("{\"code\":" + status + ",\"msg\":\"" + errorCode + "\",\"data\":null}");
    }

    private static final class Invocation {
        private final String method;
        private final String path;
        private final Instant startedAt;
        private OpenApiOperationDefinition operation;
        private OpenApiCredential credential;

        private Invocation(String method, String path, Instant startedAt) {
            this.method = method;
            this.path = path;
            this.startedAt = startedAt;
        }
    }

    private static final class DownstreamException extends RuntimeException {
        private DownstreamException(Throwable cause) {
            super(cause);
        }
    }
}
