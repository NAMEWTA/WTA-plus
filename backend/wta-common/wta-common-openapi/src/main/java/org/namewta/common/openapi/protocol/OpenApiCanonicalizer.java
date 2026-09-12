package org.namewta.common.openapi.protocol;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;

/**
 * The single canonicalization implementation for the published NAMEWTA v1 wire format.
 */
public final class OpenApiCanonicalizer {

    public static final String ALGORITHM = "NAMEWTA-HMAC-SHA256";
    public static final String VERSION = "v1";

    public String canonicalize(OpenApiRequest request) {
        validateRequest(request);
        return String.join("\n",
            ALGORITHM,
            VERSION,
            request.appKey(),
            request.timestamp(),
            request.nonce(),
            request.method().toUpperCase(Locale.ROOT),
            canonicalizePath(request.rawPath()),
            canonicalizeQuery(request.rawQuery()),
            sha256Hex(request.body()));
    }

    public String canonicalizePath(String rawPath) {
        if (rawPath.isEmpty() || rawPath.charAt(0) != '/') {
            throw new OpenApiAuthenticationException();
        }
        StringBuilder result = new StringBuilder(rawPath.length());
        for (int index = 0; index < rawPath.length();) {
            char current = rawPath.charAt(index);
            if (current == '/') {
                result.append('/');
                index++;
                continue;
            }
            Segment segment = readSegment(rawPath, index, '/');
            result.append(percentEncode(segment.bytes()));
            index = segment.nextIndex();
        }
        return result.toString();
    }

    public String canonicalizeQuery(String rawQuery) {
        if (rawQuery == null || rawQuery.isEmpty()) {
            return "";
        }
        List<QueryParameter> parameters = new ArrayList<>();
        for (String pair : rawQuery.split("&", -1)) {
            int separator = pair.indexOf('=');
            String rawName = separator < 0 ? pair : pair.substring(0, separator);
            String rawValue = separator < 0 ? "" : pair.substring(separator + 1);
            parameters.add(new QueryParameter(
                percentEncode(decodeComponent(rawName)),
                percentEncode(decodeComponent(rawValue))));
        }
        parameters.sort(Comparator.comparing(QueryParameter::name).thenComparing(QueryParameter::value));
        return parameters.stream()
            .map(parameter -> parameter.name() + "=" + parameter.value())
            .reduce((left, right) -> left + "&" + right)
            .orElse("");
    }

    private static void validateRequest(OpenApiRequest request) {
        if (request.appKey().isBlank() || request.timestamp().isBlank() || request.nonce().isBlank()
            || request.method().isBlank()) {
            throw new OpenApiAuthenticationException();
        }
    }

    private static Segment readSegment(String value, int start, char terminator) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        int index = start;
        while (index < value.length() && value.charAt(index) != terminator) {
            char current = value.charAt(index);
            if (current == '%') {
                if (index + 2 >= value.length()) {
                    throw new OpenApiAuthenticationException();
                }
                int high = Character.digit(value.charAt(index + 1), 16);
                int low = Character.digit(value.charAt(index + 2), 16);
                if (high < 0 || low < 0) {
                    throw new OpenApiAuthenticationException();
                }
                bytes.write((high << 4) | low);
                index += 3;
            } else {
                int codePoint = value.codePointAt(index);
                bytes.writeBytes(new String(Character.toChars(codePoint)).getBytes(StandardCharsets.UTF_8));
                index += Character.charCount(codePoint);
            }
        }
        return new Segment(bytes.toByteArray(), index);
    }

    private static byte[] decodeComponent(String value) {
        return readSegment(value, 0, '\0').bytes();
    }

    private static String percentEncode(byte[] bytes) {
        StringBuilder result = new StringBuilder(bytes.length * 3);
        for (byte value : bytes) {
            int unsigned = Byte.toUnsignedInt(value);
            if (isUnreserved(unsigned)) {
                result.append((char) unsigned);
            } else {
                result.append('%');
                result.append(Character.toUpperCase(Character.forDigit(unsigned >>> 4, 16)));
                result.append(Character.toUpperCase(Character.forDigit(unsigned & 0x0F, 16)));
            }
        }
        return result.toString();
    }

    private static boolean isUnreserved(int value) {
        return value >= 'a' && value <= 'z'
            || value >= 'A' && value <= 'Z'
            || value >= '0' && value <= '9'
            || value == '-' || value == '.' || value == '_' || value == '~';
    }

    private static String sha256Hex(byte[] body) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(body));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    private record Segment(byte[] bytes, int nextIndex) {
    }

    private record QueryParameter(String name, String value) {
    }

}
