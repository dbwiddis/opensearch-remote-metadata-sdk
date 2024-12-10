/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.remote.metadata;

import java.util.Set;

public class CommonValue {
    public static final String TENANT_ID = "tenant_id";

    /** The value for remote metadata type for a remote OpenSearch cluster compatible with OpenSearch Java Client. */
    public static final String REMOTE_OPENSEARCH = "RemoteOpenSearch";
    /** The value for remote metadata type for a remote cluster on AWS OpenSearch Service using AWS SDK v2. */
    public static final String AWS_OPENSEARCH_SERVICE = "AWSOpenSearchService";
    private static final String AOS_SERVICE_NAME = "es";
    private static final String AOSS_SERVICE_NAME = "aoss";
    /** Service Names compatible with AWS SDK v2. */
    public static final Set<String> VALID_AWS_OPENSEARCH_SERVICE_NAMES = Set.of(AOS_SERVICE_NAME, AOSS_SERVICE_NAME);

    /** The value for remote metadata type for a remote cluster on AWS Dynamo DB and Zero-ETL replication to OpenSearch */
    public static final String AWS_DYNAMO_DB = "AWSDynamoDB";
}
