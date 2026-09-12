package org.namewta.common.web.logging;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * 将响应字节直接写给下游响应，同时复制有界日志前缀。
 */
final class SysLogResponseWrapper extends HttpServletResponseWrapper {

    private final SysLogResponseCapture capture;
    private CapturingServletOutputStream outputStream;
    private PrintWriter writer;
    private boolean outputStreamRequested;

    SysLogResponseWrapper(HttpServletResponse response, SysLogResponseCapture capture) {
        super(response);
        this.capture = capture;
    }

    boolean uses(SysLogResponseCapture expectedCapture) {
        return capture == expectedCapture;
    }

    void flushForLogging() {
        if (writer != null) {
            writer.flush();
        }
    }

    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        if (writer != null) {
            throw new IllegalStateException("getWriter() has already been called for this response");
        }
        outputStreamRequested = true;
        return outputStream();
    }

    @Override
    public PrintWriter getWriter() throws IOException {
        if (outputStreamRequested) {
            throw new IllegalStateException("getOutputStream() has already been called for this response");
        }
        if (writer == null) {
            String encoding = getCharacterEncoding();
            Charset charset = encoding == null ? StandardCharsets.UTF_8 : Charset.forName(encoding);
            writer = new PrintWriter(new OutputStreamWriter(outputStream(), charset));
        }
        return writer;
    }

    @Override
    public void flushBuffer() throws IOException {
        if (writer != null) {
            writer.flush();
        } else if (outputStream != null) {
            outputStream.flush();
        }
        super.flushBuffer();
    }

    @Override
    public void reset() {
        super.reset();
        capture.reset();
    }

    @Override
    public void resetBuffer() {
        super.resetBuffer();
        capture.reset();
    }

    private CapturingServletOutputStream outputStream() throws IOException {
        if (outputStream == null) {
            outputStream = new CapturingServletOutputStream(super.getOutputStream());
        }
        return outputStream;
    }

    private final class CapturingServletOutputStream extends ServletOutputStream {

        private final ServletOutputStream delegate;

        private CapturingServletOutputStream(ServletOutputStream delegate) {
            this.delegate = delegate;
        }

        @Override
        public boolean isReady() {
            return delegate.isReady();
        }

        @Override
        public void setWriteListener(WriteListener writeListener) {
            delegate.setWriteListener(writeListener);
        }

        @Override
        public void write(int value) throws IOException {
            delegate.write(value);
            capture.capture(value, SysLogResponseWrapper.this);
        }

        @Override
        public void write(byte[] bytes, int offset, int length) throws IOException {
            delegate.write(bytes, offset, length);
            capture.capture(bytes, offset, length, SysLogResponseWrapper.this);
        }

        @Override
        public void flush() throws IOException {
            delegate.flush();
        }

        @Override
        public void close() throws IOException {
            delegate.close();
        }
    }
}
