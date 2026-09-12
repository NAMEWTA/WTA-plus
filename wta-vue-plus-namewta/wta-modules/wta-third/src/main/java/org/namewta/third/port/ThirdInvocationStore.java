package org.namewta.third.port;

import org.namewta.third.domain.ThirdInvocation;

/** Invocation persistence boundary consumed by the observability adapter. */
public interface ThirdInvocationStore {
    int upsert(ThirdInvocation invocation);
}
