/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

/*
 * Modifications Copyright OpenSearch Contributors. See
 * GitHub history for details.
 */

package org.opensearch.search.aggregations.bucket.adjacency;

import org.opensearch.common.xcontent.ObjectParser;
import org.opensearch.common.xcontent.XContentParser;
import org.opensearch.search.aggregations.ParsedMultiBucketAggregation;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ParsedAdjacencyMatrix extends ParsedMultiBucketAggregation<ParsedAdjacencyMatrix.ParsedBucket> implements AdjacencyMatrix {

    private Map<String, ParsedBucket> bucketMap;

    @Override
    public String getType() {
        return AdjacencyMatrixAggregationBuilder.NAME;
    }

    @Override
    public List<? extends AdjacencyMatrix.Bucket> getBuckets() {
        return buckets;
    }

    @Override
    public ParsedBucket getBucketByKey(String key) {
        if (bucketMap == null) {
            bucketMap = new HashMap<>(buckets.size());
            for (ParsedBucket bucket : buckets) {
                bucketMap.put(bucket.getKey(), bucket);
            }
        }
        return bucketMap.get(key);
    }

    private static final ObjectParser<ParsedAdjacencyMatrix, Void> PARSER =
            new ObjectParser<>(ParsedAdjacencyMatrix.class.getSimpleName(), true, ParsedAdjacencyMatrix::new);
    static {
        declareMultiBucketAggregationFields(PARSER,
                parser -> ParsedBucket.fromXContent(parser),
                parser -> ParsedBucket.fromXContent(parser));
    }

    public static ParsedAdjacencyMatrix fromXContent(XContentParser parser, String name) throws IOException {
        ParsedAdjacencyMatrix aggregation = PARSER.parse(parser, null);
        aggregation.setName(name);
        return aggregation;
    }

    public static class ParsedBucket extends ParsedMultiBucketAggregation.ParsedBucket implements AdjacencyMatrix.Bucket {

        private String key;

        @Override
        public String getKey() {
            return key;
        }

        @Override
        public String getKeyAsString() {
            return key;
        }

        static ParsedBucket fromXContent(XContentParser parser) throws IOException {
            return parseXContent(parser, false, ParsedBucket::new, (p, bucket) -> bucket.key = p.text());
        }
    }
}
