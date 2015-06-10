/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.ignite.internal.processors.cache;

import org.apache.ignite.*;
import org.apache.ignite.cache.*;
import org.apache.ignite.configuration.*;
import org.apache.ignite.spi.communication.tcp.*;
import org.apache.ignite.spi.discovery.tcp.*;
import org.apache.ignite.spi.discovery.tcp.ipfinder.*;
import org.apache.ignite.spi.discovery.tcp.ipfinder.vm.*;
import org.apache.ignite.testframework.*;
import org.apache.ignite.testframework.junits.common.*;

/**
 * Checks stop and destroy methods behavior.
 */
public class StopGridTest extends GridCommonAbstractTest {
    /** */
    private static TcpDiscoveryIpFinder ipFinder = new TcpDiscoveryVmIpFinder(true);

    /** key-value used at test. */
    protected static String KEY_VAL = "1";

    /** cache name 1. */
    protected static String CACHE_NAME_DHT = "cache";

    /** cache name 2. */

    /**
     * @return Grids count to start.
     */
    protected int gridCount() {
        return 3;
    }

    /** {@inheritDoc} */
    @Override protected IgniteConfiguration getConfiguration(String gridName) throws Exception {
        IgniteConfiguration iCfg = super.getConfiguration(gridName);

        if (getTestGridName(2).equals(gridName))
            iCfg.setClientMode(true);

        ((TcpDiscoverySpi)iCfg.getDiscoverySpi()).setIpFinder(ipFinder);
        ((TcpDiscoverySpi)iCfg.getDiscoverySpi()).setForceServerMode(true);

        iCfg.setCacheConfiguration();

        TcpCommunicationSpi commSpi = new TcpCommunicationSpi();

        commSpi.setLocalPort(GridTestUtils.getNextCommPort(getClass()));
        commSpi.setTcpNoDelay(true);

        iCfg.setCommunicationSpi(commSpi);

        return iCfg;
    }

    /**
     * @return dht config
     */
    private CacheConfiguration getDhtConfig() {
        CacheConfiguration cfg = defaultCacheConfiguration();
        cfg.setName(CACHE_NAME_DHT);
        cfg.setCacheMode(CacheMode.PARTITIONED);
        cfg.setNearConfiguration(null);
        return cfg;
    }

    /**
     * Test Dht close.
     *
     * @throws Exception If failed.
     */
    public void testDhtClose() throws Exception {
        IgniteCache<String, String> dhtCache0 = grid(0).getOrCreateCache(getDhtConfig());

        IgniteCache<String, String> dhtCache1 = grid(1).cache(CACHE_NAME_DHT);
        IgniteCache<String, String> dhtCache2 = grid(2).cache(CACHE_NAME_DHT);

        assert dhtCache0.get(KEY_VAL) == null;

        dhtCache2.put(KEY_VAL, KEY_VAL);

        assert dhtCache0.get(KEY_VAL).equals(KEY_VAL);

        stopGrid(1);//Stop another server node.

        try{
            dhtCache1.get(KEY_VAL);
            assert false;
        }catch (IllegalStateException e){
            // No-op
        }

        assert dhtCache0.get(KEY_VAL) != null;

        assert dhtCache0.get(KEY_VAL).equals(KEY_VAL);// Not affected.
    }

    /** {@inheritDoc} */
    @Override protected void beforeTest() throws Exception {
        super.beforeTest();

        startGridsMultiThreaded(gridCount());
    }

    /** {@inheritDoc} */
    @Override protected void afterTest() throws Exception {
        super.afterTest();

        stopAllGrids();
    }
}
