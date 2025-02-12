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
 *     http://www.apache.org/licenses/LICENSE-2.0
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

package org.opensearch.test.rest.yaml.section;

import org.opensearch.Version;
import org.opensearch.common.ParsingException;
import org.opensearch.common.Strings;
import org.opensearch.common.xcontent.XContentParser;
import org.opensearch.test.VersionUtils;
import org.opensearch.test.rest.yaml.Features;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a skip section that tells whether a specific test section or suite needs to be skipped
 * based on:
 * - the opensearch version the tests are running against
 * - a specific test feature required that might not be implemented yet by the runner
 */
public class SkipSection {
    /**
     * Parse a {@link SkipSection} if the next field is {@code skip}, otherwise returns {@link SkipSection#EMPTY}.
     */
    public static SkipSection parseIfNext(XContentParser parser) throws IOException {
        ParserUtils.advanceToFieldName(parser);

        if ("skip".equals(parser.currentName())) {
            SkipSection section = parse(parser);
            parser.nextToken();
            return section;
        }

        return EMPTY;
    }

    public static SkipSection parse(XContentParser parser) throws IOException {
        if (parser.nextToken() != XContentParser.Token.START_OBJECT) {
            throw new IllegalArgumentException("Expected [" + XContentParser.Token.START_OBJECT +
                    ", found [" + parser.currentToken() + "], the skip section is not properly indented");
        }
        String currentFieldName = null;
        XContentParser.Token token;
        String version = null;
        String reason = null;
        List<String> features = new ArrayList<>();
        while ((token = parser.nextToken()) != XContentParser.Token.END_OBJECT) {
            if (token == XContentParser.Token.FIELD_NAME) {
                currentFieldName = parser.currentName();
            } else if (token.isValue()) {
                if ("version".equals(currentFieldName)) {
                    version = parser.text();
                } else if ("reason".equals(currentFieldName)) {
                    reason = parser.text();
                } else if ("features".equals(currentFieldName)) {
                    String f = parser.text();
                    // split on ','
                    String[] fs = f.split(",");
                    if (fs != null) {
                        // add each feature, separately:
                        for (String feature : fs) {
                            features.add(feature.trim());
                        }
                    } else {
                        features.add(f);
                    }
                }
                else {
                    throw new ParsingException(parser.getTokenLocation(),
                            "field " + currentFieldName + " not supported within skip section");
                }
            } else if (token == XContentParser.Token.START_ARRAY) {
                if ("features".equals(currentFieldName)) {
                    while(parser.nextToken() != XContentParser.Token.END_ARRAY) {
                        features.add(parser.text());
                    }
                }
            }
        }

        parser.nextToken();

        if (!Strings.hasLength(version) && features.isEmpty()) {
            throw new ParsingException(parser.getTokenLocation(), "version or features is mandatory within skip section");
        }
        if (Strings.hasLength(version) && !Strings.hasLength(reason)) {
            throw new ParsingException(parser.getTokenLocation(), "reason is mandatory within skip version section");
        }
        return new SkipSection(version, features, reason);
    }

    public static final SkipSection EMPTY = new SkipSection();

    private final List<VersionRange> versionRanges;
    private final List<String> features;
    private final String reason;

    private SkipSection() {
        this.versionRanges = new ArrayList<>();
        this.features = new ArrayList<>();
        this.reason = null;
    }

    public SkipSection(String versionRange, List<String> features, String reason) {
        assert features != null;
        this.versionRanges = parseVersionRanges(versionRange);
        assert versionRanges.isEmpty() == false;
        this.features = features;
        this.reason = reason;
    }

    public Version getLowerVersion() {
        return versionRanges.get(0).getLower();
    }

    public Version getUpperVersion() {
        return versionRanges.get(versionRanges.size() - 1).getUpper();
    }

    public List<String> getFeatures() {
        return features;
    }

    public String getReason() {
        return reason;
    }

    public boolean skip(Version currentVersion) {
        if (isEmpty()) {
            return false;
        }
        boolean skip = versionRanges.stream().anyMatch(range -> range.contains(currentVersion));
        return skip || Features.areAllSupported(features) == false;
    }

    public boolean isVersionCheck() {
        return features.isEmpty();
    }

    public boolean isEmpty() {
        return EMPTY.equals(this);
    }

    static List<VersionRange> parseVersionRanges(String rawRanges) {
        if (rawRanges == null) {
            return Collections.singletonList(new VersionRange(null, null));
        }
        if (rawRanges.trim().equals("all")) {
            return Collections.singletonList(new VersionRange(VersionUtils.getFirstVersion(), Version.CURRENT));
        }
        String[] ranges = rawRanges.split(",");
        List<VersionRange> versionRanges = new ArrayList<>();
        for (String rawRange : ranges) {
            String[] skipVersions = rawRange.split("-", -1);
            if (skipVersions.length > 2) {
                throw new IllegalArgumentException("version range malformed: " + rawRanges);
            }

            String lower = skipVersions[0].trim();
            String upper = skipVersions[1].trim();
            VersionRange versionRange = new VersionRange(
                lower.isEmpty() ? VersionUtils.getFirstVersion() : Version.fromString(lower),
                upper.isEmpty() ? Version.CURRENT : Version.fromString(upper)
            );
            versionRanges.add(versionRange);
        }
        return versionRanges;
    }

    public String getSkipMessage(String description) {
        StringBuilder messageBuilder = new StringBuilder();
        messageBuilder.append("[").append(description).append("] skipped,");
        if (reason != null) {
             messageBuilder.append(" reason: [").append(getReason()).append("]");
        }
        if (features.isEmpty() == false) {
            messageBuilder.append(" unsupported features ").append(getFeatures());
        }
        return messageBuilder.toString();
    }
}
