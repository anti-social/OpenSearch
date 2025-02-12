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

package org.opensearch.example.customsuggester;

import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.util.CharsRefBuilder;
import org.opensearch.common.text.Text;
import org.opensearch.search.suggest.Suggest;
import org.opensearch.search.suggest.Suggester;

import java.io.IOException;
import java.util.Locale;

public class CustomSuggester extends Suggester<CustomSuggestionContext> {

    // This is a pretty dumb implementation which returns the original text + fieldName + custom config option + 12 or 123
    @Override
    public Suggest.Suggestion<? extends Suggest.Suggestion.Entry<? extends Suggest.Suggestion.Entry.Option>> innerExecute(
        String name,
        CustomSuggestionContext suggestion,
        IndexSearcher searcher,
        CharsRefBuilder spare) throws IOException {

        // create two suggestions with 12 and 123 appended
        CustomSuggestion response = emptySuggestion(name, suggestion, spare);
        CustomSuggestion.Entry entry = response.getEntries().get(0);
        String text = entry.getText().string();

        String firstOption =
            String.format(Locale.ROOT, "%s-%s-%s-%s", text, suggestion.getField(), suggestion.options.get("suffix"), "12");
        CustomSuggestion.Entry.Option option12 = new CustomSuggestion.Entry.Option(new Text(firstOption), 0.9f, "option-dummy-value-1");
        entry.addOption(option12);

        String secondOption =
            String.format(Locale.ROOT, "%s-%s-%s-%s", text, suggestion.getField(), suggestion.options.get("suffix"), "123");
        CustomSuggestion.Entry.Option option123 = new CustomSuggestion.Entry.Option(new Text(secondOption), 0.8f, "option-dummy-value-2");
        entry.addOption(option123);

        return response;
    }

    @Override
    protected CustomSuggestion emptySuggestion(String name, CustomSuggestionContext suggestion,
            CharsRefBuilder spare) throws IOException {
        String text = suggestion.getText().utf8ToString();
        CustomSuggestion response = new CustomSuggestion(name, suggestion.getSize(), "suggestion-dummy-value");
        CustomSuggestion.Entry entry = new CustomSuggestion.Entry(new Text(text), 0, text.length(), "entry-dummy-value");
        response.addTerm(entry);
        return response;
    }
}
