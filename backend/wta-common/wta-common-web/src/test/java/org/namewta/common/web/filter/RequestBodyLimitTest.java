package org.namewta.common.web.filter;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.namewta.common.core.exception.RequestBodyTooLargeException;
import org.namewta.common.core.http.CapturedRequestBody;
import org.namewta.common.web.logging.SysLogFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("dev")
class RequestBodyLimitTest {
    private static final int LIMIT = 2 * 1024 * 1024;

    @Test
    void declaredOversizeIsRejectedBeforeReadingTheBody() {
        AtomicInteger reads = new AtomicInteger();
        MockHttpServletRequest request = request(LIMIT + 1, LIMIT + 1, reads);
        assertThrows(RequestBodyTooLargeException.class, () -> new RepeatedlyRequestWrapper(request, new MockHttpServletResponse(), LIMIT));
        assertEquals(0, reads.get());
    }

    @Test
    void unknownLengthCannotConsumeBeyondTheBudgetPlusOne() {
        AtomicInteger reads = new AtomicInteger();
        MockHttpServletRequest request = request(-1, LIMIT + 32, reads);
        assertThrows(RequestBodyTooLargeException.class, () -> new RepeatedlyRequestWrapper(request, new MockHttpServletResponse(), LIMIT));
        assertTrue(reads.get() <= LIMIT + 1);
    }

    @Test
    void structuredJsonSuffixUsesTheSameIngressLimit() throws Exception {
        MockHttpServletRequest request = request(LIMIT + 1, LIMIT + 1, new AtomicInteger());
        request.setContentType("application/problem+json");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicInteger business = new AtomicInteger();
        new RepeatableFilter(2 * 1024 * 1024).doFilter(request, response, (req, res) -> business.incrementAndGet());
        assertEquals(413, response.getStatus());
        assertEquals(0, business.get());
    }

    @Test
    void knownAndUnknownLengthsAllowExactlyTheBudgetButNotOneMoreByte() throws Exception {
        for (long declared : new long[]{-1, 31, 32}) {
            for (int size : new int[]{0, 31, 32}) {
                var body = CapturedRequestBody.capture(request(declared, size, new AtomicInteger()), 32);
                assertEquals(size, body.length());
            }
        }
        assertThrows(RequestBodyTooLargeException.class,
            () -> CapturedRequestBody.capture(request(1, 33, new AtomicInteger()), 32));
    }

    @Test
    void repeatedObserversShareOriginalStorageAndCannotMutateItThroughCopies() throws Exception {
        AtomicInteger reads = new AtomicInteger();
        var request = request(-1, 32, reads);
        var response = new MockHttpServletResponse();
        var first = new RepeatedlyRequestWrapper(request, response, 32);
        var owner = CapturedRequestBody.find(request);
        for (int i = 0; i < 20; i++) {
            var observer = new RepeatedlyRequestWrapper(first, response, 32);
            assertSame(owner, CapturedRequestBody.find(observer));
            assertEquals(32, observer.getInputStream().readAllBytes().length);
            observer.getBodyPrefix(32)[0] = '!';
        }
        owner.copy()[0] = '!';
        assertEquals('x', owner.openStream().read());
        assertEquals(32, reads.get());
    }

    @Test
    void xssCreatesOneSeparateViewAndLeavesRawSignatureBytesUnchanged() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContentType("application/problem+json");
        byte[] raw = " {\"value\":\"<b>中</b>\"} ".getBytes(StandardCharsets.UTF_8);
        request.setContent(raw);
        var first = new RepeatedlyRequestWrapper(request, new MockHttpServletResponse(), 128);
        var view = new XssHttpServletRequestWrapper(first, 128);
        assertEquals("{\"value\":\"中\"}", view.getReader().readLine());
        assertArrayEquals(view.getInputStream().readAllBytes(), view.getInputStream().readAllBytes());
        assertArrayEquals(raw, CapturedRequestBody.find(view).copy());
        assertThrows(RequestBodyTooLargeException.class,
            () -> CapturedRequestBody.find(view).utf8View("中".repeat(128)));
    }

    @Test
    void loggingFallbackMustNotSwallowTheIngressRejection() throws Exception {
        var request = request(-1, 33, new AtomicInteger());
        request.setContentType("text/plain");
        var response = new MockHttpServletResponse();
        AtomicInteger business = new AtomicInteger();
        new SysLogFilter(8, 32, ignored -> { }).doFilter(request, response, (req, res) -> business.incrementAndGet());
        assertEquals(413, response.getStatus());
        assertEquals(0, business.get());
    }

    @Test
    void multipartAndSseDoNotEnterOrdinaryBodyCapture() throws Exception {
        for (String type : new String[]{"multipart/form-data; boundary=owned", "text/event-stream", "application/octet-stream"}) {
            MockHttpServletRequest request = new MockHttpServletRequest() {
                @Override public ServletInputStream getInputStream() { throw new AssertionError("stream was eagerly consumed"); }
            };
            request.setContentType(type);
            var response = new MockHttpServletResponse();
            AtomicInteger business = new AtomicInteger();
            new RepeatableFilter(32).doFilter(request, response,
                (req, res) -> new SysLogFilter(8, 32, ignored -> { }).doFilter(req, res,
                    (raw, out) -> business.incrementAndGet()));
            assertEquals(1, business.get());
        }
    }

    @Test
    void truncatedOrCancelledReadDoesNotInstallAPartialCache() {
        MockHttpServletRequest request = new MockHttpServletRequest() {
            @Override public ServletInputStream getInputStream() {
                return new ServletInputStream() {
                    @Override public int read() throws IOException { throw new IOException("owned disconnect"); }
                    @Override public boolean isFinished() { return false; }
                    @Override public boolean isReady() { return true; }
                    @Override public void setReadListener(ReadListener listener) { }
                };
            }
        };
        assertThrows(IOException.class, () -> CapturedRequestBody.capture(request, 32));
        assertEquals(null, CapturedRequestBody.find(request));
    }

    private static MockHttpServletRequest request(long declared, int actual, AtomicInteger reads) {
        return new MockHttpServletRequest() {
            @Override public long getContentLengthLong() { return declared; }
            @Override public int getContentLength() { return (int) declared; }
            @Override public ServletInputStream getInputStream() {
                return new ServletInputStream() {
                    private int remaining = actual;
                    @Override public int read() {
                        if (remaining == 0) return -1;
                        remaining--; reads.incrementAndGet(); return 'x';
                    }
                    @Override public int read(byte[] target, int offset, int length) {
                        if (length == 0) return 0;
                        if (remaining == 0) return -1;
                        int count = Math.min(length, remaining);
                        java.util.Arrays.fill(target, offset, offset + count, (byte) 'x');
                        remaining -= count; reads.addAndGet(count); return count;
                    }
                    @Override public boolean isFinished() { return remaining == 0; }
                    @Override public boolean isReady() { return true; }
                    @Override public void setReadListener(ReadListener listener) { }
                };
            }
        };
    }
}
