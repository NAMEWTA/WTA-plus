package org.namewta.third.support;

public interface ThirdLimitLease extends AutoCloseable {
    @Override
    void close();
}
