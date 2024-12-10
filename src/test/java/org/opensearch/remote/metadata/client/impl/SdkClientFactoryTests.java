/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.remote.metadata.client.impl;

import com.carrotsearch.randomizedtesting.annotations.ThreadLeakScope;

import org.opensearch.client.Client;
import org.opensearch.core.xcontent.NamedXContentRegistry;
import org.opensearch.remote.metadata.client.SdkClient;
import org.opensearch.test.OpenSearchTestCase;

import java.util.Map;

import static org.mockito.Mockito.mock;

@ThreadLeakScope(ThreadLeakScope.Scope.NONE) // remote http client is never closed
public class SdkClientFactoryTests extends OpenSearchTestCase {

    public void testLocalBinding() {
        SdkClient sdkClient = SdkClientFactory.createSdkClient(mock(Client.class), NamedXContentRegistry.EMPTY, Map.of());
        assertTrue(sdkClient.getDelegate() instanceof LocalClusterIndicesClient);
    }

    public void testRemoteOpenSearchBinding() {
        /* TODO Change to a map
        Settings settings = Settings.builder()
            .put(SdkClientSettings.REMOTE_METADATA_TYPE_KEY, SdkClientSettings.REMOTE_OPENSEARCH)
            .put(SdkClientSettings.REMOTE_METADATA_ENDPOINT_KEY, "http://example.org")
            .put(SdkClientSettings.REMOTE_METADATA_REGION_KEY, "eu-west-3")
            .build();
        SdkClient sdkClient = SdkClientFactory.createSdkClient(mock(Client.class), NamedXContentRegistry.EMPTY, settings);
        assertTrue(sdkClient.getDelegate() instanceof RemoteClusterIndicesClient);
        */
    }

    public void testAwsOpenSearchServiceBinding() {
        /* TODO Change to a map
        Settings settings = Settings.builder()
            .put(SdkClientSettings.REMOTE_METADATA_TYPE_KEY, SdkClientSettings.AWS_OPENSEARCH_SERVICE)
            .put(SdkClientSettings.REMOTE_METADATA_ENDPOINT_KEY, "example.org")
            .put(SdkClientSettings.REMOTE_METADATA_REGION_KEY, "eu-west-3")
            .put(SdkClientSettings.REMOTE_METADATA_SERVICE_NAME_KEY, "es")
            .build();
        SdkClient sdkClient = SdkClientFactory.createSdkClient(mock(Client.class), NamedXContentRegistry.EMPTY, settings);
        assertTrue(sdkClient.getDelegate() instanceof RemoteClusterIndicesClient);
        */
    }

    public void testDDBBinding() {
        /* TODO Change to a map
        Settings settings = Settings.builder()
            .put(SdkClientSettings.REMOTE_METADATA_TYPE_KEY, SdkClientSettings.AWS_DYNAMO_DB)
            .put(SdkClientSettings.REMOTE_METADATA_ENDPOINT_KEY, "example.org")
            .put(SdkClientSettings.REMOTE_METADATA_REGION_KEY, "eu-west-3")
            .put(SdkClientSettings.REMOTE_METADATA_SERVICE_NAME_KEY, "aoss")
            .build();
        SdkClient sdkClient = SdkClientFactory.createSdkClient(mock(Client.class), NamedXContentRegistry.EMPTY, settings);
        assertTrue(sdkClient.getDelegate() instanceof DDBOpenSearchClient);
        */
    }

    public void testRemoteOpenSearchBindingException() {
        /* TODO Change to a map
        Settings settings = Settings.builder().put(SdkClientSettings.REMOTE_METADATA_TYPE_KEY, SdkClientSettings.REMOTE_OPENSEARCH).build();
        assertThrows(
            OpenSearchException.class,
            () -> SdkClientFactory.createSdkClient(mock(Client.class), NamedXContentRegistry.EMPTY, settings)
        );
        */
    }

    public void testAwsOpenSearchServiceBindingException() {
        /* TODO Change to a map
        Settings settings = Settings.builder()
            .put(SdkClientSettings.REMOTE_METADATA_TYPE_KEY, SdkClientSettings.AWS_OPENSEARCH_SERVICE)
            .build();
        assertThrows(
            OpenSearchException.class,
            () -> SdkClientFactory.createSdkClient(mock(Client.class), NamedXContentRegistry.EMPTY, settings)
        );
        Settings settings2 = Settings.builder()
            .put(SdkClientSettings.REMOTE_METADATA_TYPE_KEY, SdkClientSettings.AWS_OPENSEARCH_SERVICE)
            .put(SdkClientSettings.REMOTE_METADATA_ENDPOINT_KEY, "example.org")
            .put(SdkClientSettings.REMOTE_METADATA_REGION_KEY, "eu-west-3")
            .put(SdkClientSettings.REMOTE_METADATA_SERVICE_NAME_KEY, "invalid")
            .build();
        assertThrows(
            OpenSearchException.class,
            () -> SdkClientFactory.createSdkClient(mock(Client.class), NamedXContentRegistry.EMPTY, settings2)
        );
        */
    }

    public void testDDBBindingException() {
        /* TODO Change to a map
        Settings settings = Settings.builder().put(SdkClientSettings.REMOTE_METADATA_TYPE_KEY, SdkClientSettings.AWS_DYNAMO_DB).build();
        assertThrows(
            OpenSearchException.class,
            () -> SdkClientFactory.createSdkClient(mock(Client.class), NamedXContentRegistry.EMPTY, settings)
        );
        Settings settingss = Settings.builder()
            .put(SdkClientSettings.REMOTE_METADATA_TYPE_KEY, SdkClientSettings.AWS_DYNAMO_DB)
            .put(SdkClientSettings.REMOTE_METADATA_ENDPOINT_KEY, "example.org")
            .put(SdkClientSettings.REMOTE_METADATA_REGION_KEY, "eu-west-3")
            .put(SdkClientSettings.REMOTE_METADATA_SERVICE_NAME_KEY, "invalid")
            .build();
        assertThrows(
            OpenSearchException.class,
            () -> SdkClientFactory.createSdkClient(mock(Client.class), NamedXContentRegistry.EMPTY, settingss)
        );
        */
    }
}
