/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ignite.internal.processors.cache;

import org.apache.ignite.*;
import org.apache.ignite.cache.*;
import org.apache.ignite.configuration.*;
import org.apache.ignite.testframework.*;
import org.apache.ignite.testframework.junits.common.*;
import org.apache.ignite.transactions.*;

import java.util.concurrent.*;

/**
 * Check starting cache in transaction.
 */
public class StartCacheInTransactionSelfTest extends GridCommonAbstractTest {
    /** {@inheritDoc} */
    @Override protected IgniteConfiguration getConfiguration(String gridName) throws Exception {
        IgniteConfiguration cfg =  super.getConfiguration(gridName);

        CacheConfiguration ccfg = new CacheConfiguration();

        ccfg.setAtomicityMode(CacheAtomicityMode.TRANSACTIONAL);
        ccfg.setBackups(1);

        cfg.setCacheConfiguration(ccfg);

        return cfg;
    }

    /** {@inheritDoc} */
    @Override protected void beforeTestsStarted() throws Exception {
        startGrids(2);
    }

    /** {@inheritDoc} */
    @Override protected void afterTestsStopped() throws Exception {
        stopAllGrids();
    }

    /**
     * @throws Exception If failed.
     */
    public void testStartCache() throws Exception {
        final Ignite ignite = grid(0);

        final String key = "key";
        final String val = "val";

        try (Transaction tx = ignite.transactions().txStart(
            TransactionConcurrency.PESSIMISTIC, TransactionIsolation.REPEATABLE_READ)){
            ignite.cache(null).put(key, val);

            GridTestUtils.assertThrows(log, new Callable<Object>() {
                @Override public Object call() throws Exception {
                    IgniteCache<String, String> cache = ignite.createCache("NEW_CACHE");

                    cache.put(key, val);

                    return null;
                }
            }, IgniteException.class, "Cannot start/stop cache within transaction.");

            tx.commit();
        }
    }

    /**
     * @throws Exception If failed.
     */
    public void testStopCache() throws Exception {
        final Ignite ignite = grid(0);

        final String key = "key";
        final String val = "val";

        try (Transaction tx = ignite.transactions().txStart(
            TransactionConcurrency.PESSIMISTIC, TransactionIsolation.REPEATABLE_READ)){
            ignite.cache(null).put(key, val);

            GridTestUtils.assertThrows(log, new Callable<Object>() {
                @Override public Object call() throws Exception {
                    ignite.destroyCache(null);

                    return null;
                }
            }, IgniteException.class, "Cannot start/stop cache within transaction.");

            tx.commit();
        }
    }
}
