/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.remote.metadata.client.impl;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProviderChain;
import software.amazon.awssdk.auth.credentials.ContainerCredentialsProvider;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.auth.credentials.InstanceProfileCredentialsProvider;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManager;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.ClientTlsStrategyBuilder;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.URIScheme;
import org.apache.hc.core5.http.nio.ssl.TlsStrategy;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.util.Strings;
import org.opensearch.OpenSearchException;
import org.opensearch.client.Client;
import org.opensearch.client.json.jackson.JacksonJsonpMapper;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.transport.aws.AwsSdk2Transport;
import org.opensearch.client.transport.aws.AwsSdk2TransportOptions;
import org.opensearch.client.transport.httpclient5.ApacheHttpClient5Transport;
import org.opensearch.client.transport.httpclient5.ApacheHttpClient5TransportBuilder;
import org.opensearch.core.xcontent.NamedXContentRegistry;
import org.opensearch.remote.metadata.client.SdkClient;
import org.opensearch.remote.metadata.client.SdkClientDelegate;

import javax.net.ssl.SSLContext;

import java.util.Map;

import static org.opensearch.common.util.concurrent.ThreadContextAccess.doPrivileged;
import static org.opensearch.remote.metadata.common.CommonValue.AWS_DYNAMO_DB;
import static org.opensearch.remote.metadata.common.CommonValue.AWS_OPENSEARCH_SERVICE;
import static org.opensearch.remote.metadata.common.CommonValue.REMOTE_METADATA_ENDPOINT_KEY;
import static org.opensearch.remote.metadata.common.CommonValue.REMOTE_METADATA_REGION_KEY;
import static org.opensearch.remote.metadata.common.CommonValue.REMOTE_METADATA_SERVICE_NAME_KEY;
import static org.opensearch.remote.metadata.common.CommonValue.REMOTE_METADATA_TYPE_KEY;
import static org.opensearch.remote.metadata.common.CommonValue.REMOTE_OPENSEARCH;
import static org.opensearch.remote.metadata.common.CommonValue.TENANT_AWARE_KEY;
import static org.opensearch.remote.metadata.common.CommonValue.TENANT_ID_FIELD_KEY;
import static org.opensearch.remote.metadata.common.CommonValue.VALID_AWS_OPENSEARCH_SERVICE_NAMES;

/**
 * A class to create a {@link SdkClient} with implementation based on settings
 */
public class SdkClientFactory {
    private static final Logger log = LogManager.getLogger(SdkClientFactory.class);

    /**
     * Create a new SdkClient with implementation determined by the value of the Remote Metadata Type setting
     * @param client The OpenSearch node client used as the default implementation
     * @param xContentRegistry The OpenSearch XContentRegistry
     * @param metadataSettings A map defining the remote metadata type and configuration
     * @return An instance of SdkClient which delegates to an implementation based on Remote Metadata Type
     */
    public static SdkClient createSdkClient(Client client, NamedXContentRegistry xContentRegistry, Map<String, String> metadataSettings) {
        String tenantIdField = metadataSettings.get(TENANT_ID_FIELD_KEY);
        Boolean multiTenancy = Boolean.parseBoolean(metadataSettings.get(TENANT_AWARE_KEY));

        String remoteMetadataType = metadataSettings.get(REMOTE_METADATA_TYPE_KEY);
        String remoteMetadataEndpoint = metadataSettings.get(REMOTE_METADATA_ENDPOINT_KEY);
        String region = metadataSettings.get(REMOTE_METADATA_REGION_KEY);
        String serviceName = metadataSettings.get(REMOTE_METADATA_SERVICE_NAME_KEY);

        if (Strings.isEmpty(remoteMetadataType)) {
            return createDefaultClient(client, xContentRegistry, tenantIdField, multiTenancy);
        }
        switch (remoteMetadataType) {
            case REMOTE_OPENSEARCH:
                if (Strings.isBlank(remoteMetadataEndpoint)) {
                    throw new OpenSearchException("Remote Opensearch client requires a metadata endpoint.");
                }
                log.info("Using remote opensearch cluster as metadata store");
                return new SdkClient(
                    new RemoteClusterIndicesClient(createOpenSearchClient(remoteMetadataEndpoint), tenantIdField),
                    multiTenancy
                );
            case AWS_OPENSEARCH_SERVICE:
                validateAwsParams(remoteMetadataType, remoteMetadataEndpoint, region, serviceName);
                log.info("Using remote AWS Opensearch Service cluster as metadata store");
                return new SdkClient(
                    new RemoteClusterIndicesClient(
                        createAwsOpenSearchServiceClient(remoteMetadataEndpoint, region, serviceName),
                        tenantIdField
                    ),
                    multiTenancy
                );
            case AWS_DYNAMO_DB:
                validateAwsParams(remoteMetadataType, remoteMetadataEndpoint, region, serviceName);
                log.info("Using dynamo DB as metadata store");
                return new SdkClient(
                    new DDBOpenSearchClient(
                        createDynamoDbClient(region),
                        new RemoteClusterIndicesClient(
                            createAwsOpenSearchServiceClient(remoteMetadataEndpoint, region, serviceName),
                            tenantIdField
                        ),
                        tenantIdField
                    ),
                    multiTenancy
                );
            default:
                return createDefaultClient(client, xContentRegistry, tenantIdField, multiTenancy);
        }
    }

    private static SdkClient createDefaultClient(
        Client client,
        NamedXContentRegistry xContentRegistry,
        String tenantIdField,
        Boolean multiTenancy
    ) {
        log.info("Using local opensearch cluster as metadata store");
        return new SdkClient(new LocalClusterIndicesClient(client, xContentRegistry, tenantIdField), multiTenancy);
    }

    private static void validateAwsParams(String clientType, String remoteMetadataEndpoint, String region, String serviceName) {
        if (Strings.isBlank(remoteMetadataEndpoint) || Strings.isBlank(region)) {
            throw new OpenSearchException(clientType + " client requires a metadata endpoint and region.");
        }
        if (!VALID_AWS_OPENSEARCH_SERVICE_NAMES.contains(serviceName)) {
            throw new OpenSearchException(clientType + " client only supports service names " + VALID_AWS_OPENSEARCH_SERVICE_NAMES);
        }
    }

    // Package private for testing
    static SdkClient wrapSdkClientDelegate(SdkClientDelegate delegate, Boolean multiTenancy) {
        return new SdkClient(delegate, multiTenancy);
    }

    private static DynamoDbClient createDynamoDbClient(String region) {
        if (region == null) {
            throw new IllegalStateException("REGION environment variable needs to be set!");
        }
        return doPrivileged(
            () -> DynamoDbClient.builder().region(Region.of(region)).credentialsProvider(createCredentialsProvider()).build()
        );
    }

    private static OpenSearchClient createOpenSearchClient(String remoteMetadataEndpoint) {
        try {
            Map<String, String> env = System.getenv();
            String user = env.getOrDefault("user", "admin");
            String pass = env.getOrDefault("password", "admin");
            // Endpoint syntax: https://127.0.0.1:9200
            HttpHost host = HttpHost.create(remoteMetadataEndpoint);
            SSLContext sslContext = SSLContextBuilder.create().loadTrustMaterial(null, (chain, authType) -> true).build();
            ApacheHttpClient5Transport transport = ApacheHttpClient5TransportBuilder.builder(host)
                .setMapper(
                    new JacksonJsonpMapper(
                        new ObjectMapper().setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
                            .setSerializationInclusion(JsonInclude.Include.NON_NULL)
                            .registerModule(new JavaTimeModule())
                            .configure(SerializationFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS, false)
                    )
                )
                .setHttpClientConfigCallback(httpClientBuilder -> {
                    BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
                    credentialsProvider.setCredentials(new AuthScope(host), new UsernamePasswordCredentials(user, pass.toCharArray()));
                    if (URIScheme.HTTP.getId().equalsIgnoreCase(host.getSchemeName())) {
                        // No SSL/TLS
                        return httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
                    }
                    // Disable SSL/TLS verification as our local testing clusters use self-signed certificates
                    final TlsStrategy tlsStrategy = ClientTlsStrategyBuilder.create()
                        .setSslContext(sslContext)
                        .setHostnameVerifier(NoopHostnameVerifier.INSTANCE)
                        .build();
                    final PoolingAsyncClientConnectionManager connectionManager = PoolingAsyncClientConnectionManagerBuilder.create()
                        .setTlsStrategy(tlsStrategy)
                        .build();
                    return httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider).setConnectionManager(connectionManager);
                })
                .build();
            return new OpenSearchClient(transport);
        } catch (Exception e) {
            throw new OpenSearchException(e);
        }
    }

    private static OpenSearchClient createAwsOpenSearchServiceClient(String remoteMetadataEndpoint, String region, String signingService) {
        // https://github.com/opensearch-project/opensearch-java/blob/main/guides/auth.md
        final SdkHttpClient httpClient = ApacheHttpClient.builder().build();
        return new OpenSearchClient(
            doPrivileged(
                () -> new AwsSdk2Transport(
                    httpClient,
                    remoteMetadataEndpoint.replaceAll("^https?://", ""), // OpenSearch endpoint, without https://
                    signingService, // signing service name, use "es" for OpenSearch, "aoss" for OpenSearch Serverless
                    Region.of(region), // signing service region
                    AwsSdk2TransportOptions.builder().setCredentials(createCredentialsProvider()).build()
                )
            )
        );
    }

    private static AwsCredentialsProvider createCredentialsProvider() {
        return AwsCredentialsProviderChain.builder()
            .addCredentialsProvider(EnvironmentVariableCredentialsProvider.create())
            .addCredentialsProvider(ContainerCredentialsProvider.builder().build())
            .addCredentialsProvider(InstanceProfileCredentialsProvider.create())
            .build();
    }
}
