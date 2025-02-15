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

package org.opensearch.script.expression;

import java.io.IOException;
import org.apache.lucene.expressions.Bindings;
import org.apache.lucene.expressions.Expression;
import org.apache.lucene.expressions.SimpleBindings;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.search.DoubleValues;
import org.apache.lucene.search.DoubleValuesSource;
import org.opensearch.script.AggregationScript;
import org.opensearch.script.GeneralScriptException;

/**
 * A bridge to evaluate an {@link Expression} against {@link Bindings} in the context
 * of a {@link AggregationScript}.
 */
class ExpressionAggregationScript implements AggregationScript.LeafFactory {

    final Expression exprScript;
    final SimpleBindings bindings;
    final DoubleValuesSource source;
    final boolean needsScore;
    final ReplaceableConstDoubleValueSource specialValue; // _value

    ExpressionAggregationScript(Expression e, SimpleBindings b, boolean n, ReplaceableConstDoubleValueSource v) {
        exprScript = e;
        bindings = b;
        source = exprScript.getDoubleValuesSource(bindings);
        needsScore = n;
        specialValue = v;
    }

    @Override
    public boolean needs_score() {
        return needsScore;
    }

    @Override
    public AggregationScript newInstance(final LeafReaderContext leaf) throws IOException {
        return new AggregationScript() {
            // Fake the scorer until setScorer is called.
            DoubleValues values = source.getValues(leaf, new DoubleValues() {
                @Override
                public double doubleValue() throws IOException {
                    return get_score().doubleValue();
                }

                @Override
                public boolean advanceExact(int doc) throws IOException {
                    return true;
                }
            });

            @Override
            public Object execute() {
                try {
                    return values.doubleValue();
                } catch (Exception exception) {
                    throw new GeneralScriptException("Error evaluating " + exprScript, exception);
                }
            }

            @Override
            public void setDocument(int d) {
                try {
                    values.advanceExact(d);
                } catch (IOException e) {
                    throw new IllegalStateException("Can't advance to doc using " + exprScript, e);
                }
            }

            @Override
            public void setNextAggregationValue(Object value) {
                // _value isn't used in script if specialValue == null
                if (specialValue != null) {
                    if (value instanceof Number) {
                        specialValue.setValue(((Number)value).doubleValue());
                    } else {
                        throw new GeneralScriptException("Cannot use expression with text variable using " + exprScript);
                    }
                }
            }
        };
    }
}
