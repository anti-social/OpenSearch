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

package org.opensearch.search.aggregations.bucket.terms.heuristic;


import java.io.IOException;

import org.opensearch.common.io.stream.StreamInput;
import org.opensearch.common.io.stream.StreamOutput;
import org.opensearch.common.xcontent.ObjectParser;
import org.opensearch.common.xcontent.XContentBuilder;

public class JLHScore extends SignificanceHeuristic {
    public static final String NAME = "jlh";
    public static final ObjectParser<JLHScore, Void> PARSER = new ObjectParser<>(NAME, JLHScore::new);

    public JLHScore() {
    }

    /**
     * Read from a stream.
     */
    public JLHScore(StreamInput in) {
        // Nothing to read.
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
    }

    @Override
    public String getWriteableName() {
        return NAME;
    }

    /**
     * Calculates the significance of a term in a sample against a background of
     * normal distributions by comparing the changes in frequency. This is the heart
     * of the significant terms feature.
     */
    @Override
    public double getScore(long subsetFreq, long subsetSize, long supersetFreq, long supersetSize) {
        checkFrequencyValidity(subsetFreq, subsetSize, supersetFreq, supersetSize, "JLHScore");
        if ((subsetSize == 0) || (supersetSize == 0)) {
            // avoid any divide by zero issues
            return 0;
        }
        if (supersetFreq == 0) {
            // If we are using a background context that is not a strict superset, a foreground
            // term may be missing from the background, so for the purposes of this calculation
            // we assume a value of 1 for our calculations which avoids returning an "infinity" result
            supersetFreq = 1;
        }
        double subsetProbability = (double) subsetFreq / (double) subsetSize;
        double supersetProbability = (double) supersetFreq / (double) supersetSize;

        // Using absoluteProbabilityChange alone favours very common words e.g. you, we etc
        // because a doubling in popularity of a common term is a big percent difference
        // whereas a rare term would have to achieve a hundred-fold increase in popularity to
        // achieve the same difference measure.
        // In favouring common words as suggested features for search we would get high
        // recall but low precision.
        double absoluteProbabilityChange = subsetProbability - supersetProbability;
        if (absoluteProbabilityChange <= 0) {
            return 0;
        }
        // Using relativeProbabilityChange tends to favour rarer terms e.g.mis-spellings or
        // unique URLs.
        // A very low-probability term can very easily double in popularity due to the low
        // numbers required to do so whereas a high-probability term would have to add many
        // extra individual sightings to achieve the same shift.
        // In favouring rare words as suggested features for search we would get high
        // precision but low recall.
        double relativeProbabilityChange = (subsetProbability / supersetProbability);

        // A blend of the above metrics - favours medium-rare terms to strike a useful
        // balance between precision and recall.
        return absoluteProbabilityChange * relativeProbabilityChange;
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject(NAME).endObject();
        return builder;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || obj.getClass() != getClass()) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    public static class JLHScoreBuilder implements SignificanceHeuristicBuilder {

        @Override
        public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
            builder.startObject(NAME).endObject();
            return builder;
        }
    }
}

