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
import org.apache.ignite.cluster.*;
import org.apache.ignite.configuration.*;
import org.apache.ignite.internal.managers.communication.*;
import org.apache.ignite.internal.processors.cache.distributed.dht.*;
import org.apache.ignite.internal.util.typedef.internal.*;
import org.apache.ignite.plugin.extensions.communication.*;
import org.apache.ignite.spi.*;
import org.apache.ignite.spi.communication.tcp.*;
import org.apache.ignite.spi.discovery.tcp.*;
import org.apache.ignite.spi.discovery.tcp.ipfinder.*;
import org.apache.ignite.spi.discovery.tcp.ipfinder.vm.*;
import org.apache.ignite.testframework.*;
import org.apache.ignite.testframework.junits.common.*;

import java.util.*;
import java.util.concurrent.atomic.*;

/**
 * Checks stop and destroy methods behavior.
 */
public class CacheStopAndDestroySelfTest extends GridCommonAbstractTest {
    /** */
    private static TcpDiscoveryIpFinder ipFinder = new TcpDiscoveryVmIpFinder(true);

    /** key-value used at test. */
    protected static String KEY_VAL = "1";

    /** cache name 1. */
    protected static String CACHE_NAME_DHT = "cache";

    /** cache name 2. */
    protected static String CACHE_NAME_CLIENT = "cache_client";

    /** near cache name. */
    protected static String CACHE_NAME_NEAR = "cache_near";

    /** local cache name. */
    protected static String CACHE_NAME_LOC = "cache_local";

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

        TcpCommunicationSpi commSpi = new CountingTxRequestsToClientNodeTcpCommunicationSpi();

        commSpi.setLocalPort(GridTestUtils.getNextCommPort(getClass()));
        commSpi.setTcpNoDelay(true);

        iCfg.setCommunicationSpi(commSpi);

        return iCfg;
    }

    /**
     * Helps to count messages.
     */
    public static class CountingTxRequestsToClientNodeTcpCommunicationSpi extends TcpCommunicationSpi {
        /** Counter. */
        public static AtomicInteger cnt = new AtomicInteger();

        /** Node filter. */
        public static UUID nodeFilter;

        /** {@inheritDoc} */
        @Override public void sendMessage(ClusterNode node, Message msg) throws IgniteSpiException {
            super.sendMessage(node, msg);

            if (nodeFilter != null &&
                node.id().equals(nodeFilter) &&
                msg instanceof GridIoMessage &&
                ((GridIoMessage)msg).message() instanceof GridDhtTxPrepareRequest)
                cnt.incrementAndGet();
        }
    }

    /**
     * @return dht config
     */
    private CacheConfiguration getDhtConfig() {
        CacheConfiguration cfg = defaultCacheConfiguration();
        cfg.setName(CACHE_NAME_DHT);
        cfg.setCacheMode(CacheMode.PARTITIONED);
        return cfg;
    }

    /**
     * @return client config
     */
    private CacheConfiguration getClientConfig() {
        CacheConfiguration cfg = defaultCacheConfiguration();
        cfg.setName(CACHE_NAME_CLIENT);
        cfg.setCacheMode(CacheMode.PARTITIONED);
        return cfg;
    }

    /**
     * @return near config
     */
    private CacheConfiguration getNearConfig() {
        CacheConfiguration cfg = defaultCacheConfiguration();
        cfg.setName(CACHE_NAME_NEAR);
        cfg.setCacheMode(CacheMode.PARTITIONED);
        cfg.setNearConfiguration(new NearCacheConfiguration());
        return cfg;
    }

    /**
     * @return local config
     */
    private CacheConfiguration getLocalConfig() {
        CacheConfiguration cfg = defaultCacheConfiguration();
        cfg.setName(CACHE_NAME_LOC);
        cfg.setCacheMode(CacheMode.LOCAL);
        return cfg;
    }

    /**
     * Test Double Destroy.
     *
     * @throws Exception If failed.
     */
    public void testDhtDoubleDestroy() throws Exception {
        dhtDestroy();
        dhtDestroy();
    }

    /**
     * Test DHT Destroy.
     *
     * @throws Exception If failed.
     */
    private void dhtDestroy() throws Exception {
        grid(0).getOrCreateCache(getDhtConfig());

        assert grid(0).cache(CACHE_NAME_DHT).get(KEY_VAL) == null;

        grid(0).cache(CACHE_NAME_DHT).put(KEY_VAL, KEY_VAL);

        assert grid(0).cache(CACHE_NAME_DHT).get(KEY_VAL).equals(KEY_VAL);

        //DHT Destroy. Cache should be removed from each node.

        grid(0).cache(CACHE_NAME_DHT).destroy();

        try {
            grid(0).cache(CACHE_NAME_DHT).get(KEY_VAL);
            assert false;
        }
        catch (IllegalArgumentException | IllegalStateException ignored0) {
            try {
                grid(1).cache(CACHE_NAME_DHT).get(KEY_VAL);
                assert false;
            }
            catch (IllegalArgumentException | IllegalStateException ignored1) {
                try {
                    grid(2).cache(CACHE_NAME_DHT).get(KEY_VAL);
                    assert false;
                }
                catch (IllegalArgumentException | IllegalStateException ignored2) {
                    // No-op
                }
            }
        }
    }

    /**
     * Test Double Destroy.
     *
     * @throws Exception If failed.
     */
    public void testClientDoubleDestroy() throws Exception {
        clientDestroy();
        clientDestroy();
    }

    /**
     * Test Client Destroy.
     *
     * @throws Exception If failed.
     */
    private void clientDestroy() throws Exception {
        grid(0).getOrCreateCache(getClientConfig());

        assert grid(0).cache(CACHE_NAME_CLIENT).get(KEY_VAL) == null;

        grid(0).cache(CACHE_NAME_CLIENT).put(KEY_VAL, KEY_VAL);

        assert grid(0).cache(CACHE_NAME_CLIENT).get(KEY_VAL).equals(KEY_VAL);

        //DHT Destroy from client node. Cache should be removed from each node.

        grid(2).cache(CACHE_NAME_CLIENT).destroy();// Client node.

        try {
            grid(0).cache(CACHE_NAME_CLIENT).get(KEY_VAL);
            assert false;
        }
        catch (IllegalArgumentException | IllegalStateException ignored0) {
            try {
                grid(1).cache(CACHE_NAME_CLIENT).get(KEY_VAL);
                assert false;
            }
            catch (IllegalArgumentException | IllegalStateException ignored1) {
                try {
                    grid(2).cache(CACHE_NAME_CLIENT).get(KEY_VAL);
                    assert false;
                }
                catch (IllegalArgumentException | IllegalStateException ignored2) {
                    // No-op
                }
            }
        }
    }

    /**
     * Test Double Destroy.
     *
     * @throws Exception If failed.
     */
    public void testNearDoubleDestroy() throws Exception {
        nearDestroy();
        nearDestroy();
    }

    /**
     * Test Near Destroy.
     *
     * @throws Exception If failed.
     */
    private void nearDestroy() throws Exception {
        grid(0).getOrCreateCache(getNearConfig());

        grid(2).getOrCreateNearCache(CACHE_NAME_NEAR, new NearCacheConfiguration());

        assert grid(0).cache(CACHE_NAME_NEAR).get(KEY_VAL) == null;
        assert grid(2).cache(CACHE_NAME_NEAR).get(KEY_VAL) == null;

        grid(2).cache(CACHE_NAME_NEAR).put(KEY_VAL, KEY_VAL);
        grid(0).cache(CACHE_NAME_NEAR).put(KEY_VAL, "near-test");

        assert grid(2).cache(CACHE_NAME_NEAR).localPeek(KEY_VAL).equals("near-test");

        //Local destroy. Cache should be removed from each node.

        grid(2).cache(CACHE_NAME_NEAR).destroy();

        try {
            grid(0).cache(CACHE_NAME_NEAR).get(KEY_VAL);
            assert false;
        }
        catch (IllegalArgumentException | IllegalStateException ignored0) {
            try {
                grid(1).cache(CACHE_NAME_NEAR).get(KEY_VAL);
                assert false;
            }
            catch (IllegalArgumentException | IllegalStateException ignored1) {
                try {
                    grid(2).cache(CACHE_NAME_NEAR).get(KEY_VAL);
                    assert false;
                }
                catch (IllegalArgumentException | IllegalStateException ignored2) {
                    // No-op
                }
            }
        }
    }

    /**
     * Test Double Destroy.
     *
     * @throws Exception If failed.
     */
    public void testNearLocalDestroy() throws Exception {
        localDestroy();
        localDestroy();
    }

    /**
     * Test Local Destroy.
     *
     * @throws Exception If failed.
     */
    private void localDestroy() throws Exception {
        grid(0).getOrCreateCache(getLocalConfig());

        assert grid(0).cache(CACHE_NAME_LOC).get(KEY_VAL) == null;
        assert grid(1).cache(CACHE_NAME_LOC).get(KEY_VAL) == null;

        grid(0).cache(CACHE_NAME_LOC).put(KEY_VAL, KEY_VAL + 0);
        grid(1).cache(CACHE_NAME_LOC).put(KEY_VAL, KEY_VAL + 1);

        assert grid(0).cache(CACHE_NAME_LOC).get(KEY_VAL).equals(KEY_VAL + 0);
        assert grid(1).cache(CACHE_NAME_LOC).get(KEY_VAL).equals(KEY_VAL + 1);

        //Local destroy. Cache should be removed from each node.

        grid(0).cache(CACHE_NAME_LOC).destroy();

        try {
            grid(0).cache(CACHE_NAME_LOC).get(KEY_VAL);
            assert false;
        }
        catch (IllegalArgumentException | IllegalStateException ignored0) {
            try {
                grid(1).cache(CACHE_NAME_LOC).get(KEY_VAL);
                assert false;
            }
            catch (IllegalArgumentException | IllegalStateException ignored1) {
                try {
                    grid(2).cache(CACHE_NAME_LOC).get(KEY_VAL);
                    assert false;
                }
                catch (IllegalArgumentException | IllegalStateException ignored2) {
                    // No-op
                }
            }
        }
    }

    /**
     * Test Dht close.
     *
     * @throws Exception If failed.
     */
    public void testDhtClose() throws Exception {
        IgniteCache<String, String> dhtCache0 = grid(0).getOrCreateCache(getDhtConfig());

        assert dhtCache0.get(KEY_VAL) == null;

        dhtCache0.put(KEY_VAL, KEY_VAL);

        assert dhtCache0.get(KEY_VAL).equals(KEY_VAL);

        //DHT Close. No-op.

        IgniteCache<String, String> dhtCache1 = grid(1).cache(CACHE_NAME_DHT);
        IgniteCache<String, String> dhtCache2 = grid(2).cache(CACHE_NAME_DHT);

        dhtCache0.close();

        assert dhtCache0.get(KEY_VAL).equals(KEY_VAL);// Not affected.
        assert dhtCache1.get(KEY_VAL).equals(KEY_VAL);// Not affected.
        assert dhtCache2.get(KEY_VAL).equals(KEY_VAL);// Not affected.

        //DHT Creation after closed.

        dhtCache0 = grid(0).cache(CACHE_NAME_DHT);

        dhtCache0.put(KEY_VAL, KEY_VAL + "recreated");

        assert dhtCache0.get(KEY_VAL).equals(KEY_VAL + "recreated");
        assert dhtCache0.get(KEY_VAL).equals(KEY_VAL + "recreated");
        assert dhtCache0.get(KEY_VAL).equals(KEY_VAL + "recreated");
    }

    /**
     * Test Dht close.
     *
     * @throws Exception If failed.
     */
    public void testDhtCloseWithTry() throws Exception {
        String curVal = null;

        for (int i = 0; i < 3; i++) {
            try (IgniteCache<String, String> cache0 = grid(0).getOrCreateCache(getDhtConfig())) {
                IgniteCache<String, String> cache1 = grid(1).cache(CACHE_NAME_DHT);
                IgniteCache<String, String> cache2 = grid(2).cache(CACHE_NAME_DHT);

                assert cache0.get(KEY_VAL) == null || cache0.get(KEY_VAL).equals(curVal);
                assert cache1.get(KEY_VAL) == null || cache1.get(KEY_VAL).equals(curVal);
                assert cache2.get(KEY_VAL) == null || cache2.get(KEY_VAL).equals(curVal);

                curVal = KEY_VAL + curVal;

                cache0.put(KEY_VAL, curVal);

                assert cache0.get(KEY_VAL).equals(curVal);
                assert cache1.get(KEY_VAL).equals(curVal);
                assert cache2.get(KEY_VAL).equals(curVal);
            }
        }
    }

    /**
     * Test Client close.
     *
     * @throws Exception If failed.
     */
    public void testClientClose() throws Exception {
        IgniteCache<String, String> cache0 = grid(0).getOrCreateCache(getClientConfig());

        assert cache0.get(KEY_VAL) == null;

        cache0.put(KEY_VAL, KEY_VAL);

        assert cache0.get(KEY_VAL).equals(KEY_VAL);

        //DHT Close from client node. Should affect only client node.

        IgniteCache<String, String> cache1 = grid(1).cache(CACHE_NAME_CLIENT);
        IgniteCache<String, String> cache2 = grid(2).cache(CACHE_NAME_CLIENT);

        assert cache2.get(KEY_VAL).equals(KEY_VAL);

        cache2.close();// Client node.

        assert cache0.get(KEY_VAL).equals(KEY_VAL);// Not affected.
        assert cache1.get(KEY_VAL).equals(KEY_VAL);// Not affected.

        try {
            cache2.get(KEY_VAL);// Affected.
            assert false;
        }
        catch (IllegalStateException ignored) {
            // No-op
        }

        //DHT Creation from client node after closed.
        cache2 = grid(2).cache(CACHE_NAME_CLIENT);

        assert cache2.get(KEY_VAL).equals(KEY_VAL);

        cache0.put(KEY_VAL, KEY_VAL + "recreated");

        assert cache0.get(KEY_VAL).equals(KEY_VAL + "recreated");
        assert cache1.get(KEY_VAL).equals(KEY_VAL + "recreated");
        assert cache2.get(KEY_VAL).equals(KEY_VAL + "recreated");
    }

    /**
     * Test Client close.
     *
     * @throws Exception If failed.
     */
    public void testClientCloseWithTry() throws Exception {
        String curVal = null;

        for (int i = 0; i < 3; i++) {
            try (IgniteCache<String, String> cache2 = grid(2).getOrCreateCache(getClientConfig())) {
                IgniteCache<String, String> cache0 = grid(0).cache(CACHE_NAME_CLIENT);
                IgniteCache<String, String> cache1 = grid(1).cache(CACHE_NAME_CLIENT);

                assert cache0.get(KEY_VAL) == null || cache0.get(KEY_VAL).equals(curVal);
                assert cache1.get(KEY_VAL) == null || cache1.get(KEY_VAL).equals(curVal);
                assert cache2.get(KEY_VAL) == null || cache2.get(KEY_VAL).equals(curVal);

                curVal = KEY_VAL + curVal;

                cache2.put(KEY_VAL, curVal);

                assert cache0.get(KEY_VAL).equals(curVal);
                assert cache1.get(KEY_VAL).equals(curVal);
                assert cache2.get(KEY_VAL).equals(curVal);
            }
        }
    }

    /**
     * Test Near close.
     *
     * @throws Exception If failed.
     */
    public void testNearClose() throws Exception {
        IgniteCache<String, String> cache0 = grid(0).getOrCreateCache(getNearConfig());

        //GridDhtTxPrepareRequest requests to Client node will be counted.
        CountingTxRequestsToClientNodeTcpCommunicationSpi.nodeFilter = grid(2).context().localNodeId();

        //Near Close from client node.

        IgniteCache<String, String> cache1 = grid(1).cache(CACHE_NAME_NEAR);
        IgniteCache<String, String> cache2 = grid(2).createNearCache(CACHE_NAME_NEAR, new NearCacheConfiguration());

        assert cache2.get(KEY_VAL) == null;

        //Subscribing to events.
        cache2.put(KEY_VAL, KEY_VAL);

        CountingTxRequestsToClientNodeTcpCommunicationSpi.cnt.set(0);

        cache0.put(KEY_VAL, "near-test");

        U.sleep(1000);

        //Ensure near cache was automatically updated.
        assert CountingTxRequestsToClientNodeTcpCommunicationSpi.cnt.get() != 0;

        assert cache2.localPeek(KEY_VAL).equals("near-test");

        cache2.close();

        CountingTxRequestsToClientNodeTcpCommunicationSpi.cnt.set(0);

        //Should not produce messages to client node.
        cache0.put(KEY_VAL, KEY_VAL + 0);

        U.sleep(1000);

        //Ensure near cache was NOT automatically updated.
        assert CountingTxRequestsToClientNodeTcpCommunicationSpi.cnt.get() == 0;

        assert cache0.get(KEY_VAL).equals(KEY_VAL + 0);// Not affected.
        assert cache1.get(KEY_VAL).equals(KEY_VAL + 0);// Not affected.

        try {
            cache2.get(KEY_VAL);// Affected.
            assert false;
        }
        catch (IllegalArgumentException | IllegalStateException ignored) {
            // No-op
        }

        //Near Creation from client node after closed.

        cache2 = grid(2).createNearCache(CACHE_NAME_NEAR, new NearCacheConfiguration());

        //Subscribing to events.
        cache2.put(KEY_VAL, KEY_VAL);

        assert cache2.localPeek(KEY_VAL).equals(KEY_VAL);

        cache0.put(KEY_VAL, KEY_VAL + "recreated");

        assert cache0.get(KEY_VAL).equals(KEY_VAL + "recreated");
        assert cache1.get(KEY_VAL).equals(KEY_VAL + "recreated");
        assert cache2.localPeek(KEY_VAL).equals(KEY_VAL + "recreated");
    }

    /**
     * Test Near close.
     *
     * @throws Exception If failed.
     */
    public void testNearCloseWithTry() throws Exception {
        String curVal = null;

        grid(0).getOrCreateCache(getNearConfig());

        for (int i = 0; i < 3; i++) {
            try (IgniteCache<String, String> cache2 = grid(2).getOrCreateNearCache(CACHE_NAME_NEAR, new NearCacheConfiguration())) {
                IgniteCache<String, String> cache0 = grid(0).cache(CACHE_NAME_NEAR);
                IgniteCache<String, String> cache1 = grid(1).cache(CACHE_NAME_NEAR);

                assert cache2.localPeek(KEY_VAL) == null;

                assert cache0.get(KEY_VAL) == null || cache0.get(KEY_VAL).equals(curVal);
                assert cache1.get(KEY_VAL) == null || cache1.get(KEY_VAL).equals(curVal);
                assert cache2.get(KEY_VAL) == null || cache2.get(KEY_VAL).equals(curVal);

                curVal = KEY_VAL + curVal;

                cache2.put(KEY_VAL, curVal);

                assert cache2.localPeek(KEY_VAL).equals(curVal);

                assert cache0.get(KEY_VAL).equals(curVal);
                assert cache1.get(KEY_VAL).equals(curVal);
                assert cache2.get(KEY_VAL).equals(curVal);
            }
        }
    }

    /**
     * Test Local close.
     *
     * @throws Exception If failed.
     */
    public void testLocalClose() throws Exception {
        grid(0).getOrCreateCache(getLocalConfig());

        assert grid(0).cache(CACHE_NAME_LOC).get(KEY_VAL) == null;
        assert grid(1).cache(CACHE_NAME_LOC).get(KEY_VAL) == null;

        grid(0).cache(CACHE_NAME_LOC).put(KEY_VAL, KEY_VAL + 0);
        grid(1).cache(CACHE_NAME_LOC).put(KEY_VAL, KEY_VAL + 1);

        assert grid(0).cache(CACHE_NAME_LOC).get(KEY_VAL).equals(KEY_VAL + 0);
        assert grid(1).cache(CACHE_NAME_LOC).get(KEY_VAL).equals(KEY_VAL + 1);

        //Local close. Same as Local destroy.

        grid(1).cache(CACHE_NAME_LOC).close();

        try {
            grid(0).cache(CACHE_NAME_LOC).get(KEY_VAL);
            assert false;
        }
        catch (IllegalArgumentException | IllegalStateException ignored0) {
            try {
                grid(1).cache(CACHE_NAME_LOC).get(KEY_VAL);
                assert false;
            }
            catch (IllegalArgumentException | IllegalStateException ignored1) {
                try {
                    grid(2).cache(CACHE_NAME_LOC).get(KEY_VAL);
                    assert false;
                }
                catch (IllegalArgumentException | IllegalStateException ignored2) {
                    // No-op
                }
            }
        }

        //Local creation after closed.

        grid(0).getOrCreateCache(getLocalConfig());

        grid(0).cache(CACHE_NAME_LOC).put(KEY_VAL, KEY_VAL + "recreated0");
        grid(1).cache(CACHE_NAME_LOC).put(KEY_VAL, KEY_VAL + "recreated1");
        grid(2).cache(CACHE_NAME_LOC).put(KEY_VAL, KEY_VAL + "recreated2");

        assert grid(0).cache(CACHE_NAME_LOC).get(KEY_VAL).equals(KEY_VAL + "recreated0");
        assert grid(1).cache(CACHE_NAME_LOC).get(KEY_VAL).equals(KEY_VAL + "recreated1");
        assert grid(2).cache(CACHE_NAME_LOC).get(KEY_VAL).equals(KEY_VAL + "recreated2");
    }

    /**
     * Test Local close.
     *
     * @throws Exception If failed.
     */
    public void testLocalCloseWithTry() throws Exception {
        String curVal = null;

        for (int i = 0; i < 3; i++) {
            try (IgniteCache<String, String> cache2 = grid(2).getOrCreateCache(getLocalConfig())) {
                IgniteCache<String, String> cache0 = grid(0).cache(CACHE_NAME_LOC);
                IgniteCache<String, String> cache1 = grid(1).cache(CACHE_NAME_LOC);

                assert cache0.get(KEY_VAL) == null;
                assert cache1.get(KEY_VAL) == null;
                assert cache2.get(KEY_VAL) == null;

                curVal = KEY_VAL + curVal;

                cache0.put(KEY_VAL, curVal + 1);
                cache1.put(KEY_VAL, curVal + 2);
                cache2.put(KEY_VAL, curVal + 3);

                assert cache0.get(KEY_VAL).equals(curVal + 1);
                assert cache1.get(KEY_VAL).equals(curVal + 2);
                assert cache2.get(KEY_VAL).equals(curVal + 3);
            }
        }
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
