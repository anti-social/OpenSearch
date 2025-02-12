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

package org.opensearch.monitor.fs;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.common.settings.Setting;
import org.opensearch.common.settings.Setting.Property;
import org.opensearch.common.settings.Settings;
import org.opensearch.common.unit.TimeValue;
import org.opensearch.common.util.SingleObjectCache;
import org.opensearch.env.NodeEnvironment;

import java.io.IOException;
import java.util.function.Supplier;

public class FsService {

    private static final Logger logger = LogManager.getLogger(FsService.class);

    private final Supplier<FsInfo> fsInfoSupplier;

    public static final Setting<TimeValue> REFRESH_INTERVAL_SETTING =
        Setting.timeSetting(
            "monitor.fs.refresh_interval",
            TimeValue.timeValueSeconds(1),
            TimeValue.timeValueSeconds(1),
            Property.NodeScope);

    // permits tests to bypass the refresh interval on the cache; deliberately unregistered since it is only for use in tests
    public static final Setting<Boolean> ALWAYS_REFRESH_SETTING =
        Setting.boolSetting("monitor.fs.always_refresh", false, Property.NodeScope);

    public FsService(final Settings settings, final NodeEnvironment nodeEnvironment) {
        final FsProbe probe = new FsProbe(nodeEnvironment);
        final FsInfo initialValue = stats(probe, null);
        if (ALWAYS_REFRESH_SETTING.get(settings)) {
            assert REFRESH_INTERVAL_SETTING.exists(settings) == false;
            logger.debug("bypassing refresh_interval");
            fsInfoSupplier = () -> stats(probe, initialValue);
        } else {
            final TimeValue refreshInterval = REFRESH_INTERVAL_SETTING.get(settings);
            logger.debug("using refresh_interval [{}]", refreshInterval);
            fsInfoSupplier = new FsInfoCache(refreshInterval, initialValue, probe)::getOrRefresh;
        }
    }

    public FsInfo stats() {
        return fsInfoSupplier.get();
    }

    private static FsInfo stats(FsProbe probe, FsInfo initialValue) {
        try {
            return probe.stats(initialValue);
        } catch (IOException e) {
            logger.debug("unexpected exception reading filesystem info", e);
            return null;
        }
    }

    private static class FsInfoCache extends SingleObjectCache<FsInfo> {

        private final FsInfo initialValue;
        private final FsProbe probe;

        FsInfoCache(TimeValue interval, FsInfo initialValue, FsProbe probe) {
            super(interval, initialValue);
            this.initialValue = initialValue;
            this.probe = probe;
        }

        @Override
        protected FsInfo refresh() {
            return stats(probe, initialValue);
        }

    }

}
