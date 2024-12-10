/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.remote.metadata.client;

import java.security.PrivilegedAction;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;

import static org.opensearch.common.util.concurrent.ThreadContextAccess.doPrivileged;

/**
 * Superclass abstracting privileged action execution
 */
public abstract class AbstractSdkClient implements SdkClientDelegate {

    /**
     * Execute this priveleged action asynchronously
     * @param <T> The return type of the completable future to be returned
     * @param action the action to execute
     * @param executor the executor for the action
     * @return A {@link CompletionStage} encapsulating the completable future for the action
     */
    protected <T> CompletionStage<T> executePrivilegedAsync(PrivilegedAction<T> action, Executor executor) {
        return CompletableFuture.supplyAsync(() -> doPrivileged(action), executor);
    }
}
