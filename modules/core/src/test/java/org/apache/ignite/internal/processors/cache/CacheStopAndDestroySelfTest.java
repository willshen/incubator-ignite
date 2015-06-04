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
    protected static String CACHE_NAME_DESTROY_DHT = "cache_d";

    /** cache name 2. */
    protected static String CACHE_NAME_CLOSE_DHT = "cache_c";

    /** cache name 3. */
    protected static String CACHE_NAME_DESTROY_CLIENT = "cache_d_client";

    /** cache name 4. */
    protected static String CACHE_NAME_CLOSE_CLIENT = "cache_c_client";

    /** near cache name 1. */
    protected static String CACHE_NAME_DESTROY_NEAR = "cache_d_near";

    /** near cache name 2. */
    protected static String CACHE_NAME_CLOSE_NEAR = "cache_c_near";

    /** local cache name 1. */
    protected static String CACHE_NAME_DESTROY_LOC = "cache_d_local";

    /** local cache name 2. */
    protected static String CACHE_NAME_CLOSE_LOC = "cache_c_local";

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
     * Test.
     *
     * @throws Exception If failed.
     */
    public void testCacheStopAndDestroy() throws Exception {
        startGridsMultiThreaded(gridCount());

        //GridDhtTxPrepareRequest requests to Client node will be counted.
        CountingTxRequestsToClientNodeTcpCommunicationSpi.nodeFilter = grid(2).context().localNodeId();

        CacheConfiguration cCfgD = defaultCacheConfiguration();
        cCfgD.setName(CACHE_NAME_DESTROY_DHT);
        cCfgD.setCacheMode(CacheMode.PARTITIONED);

        CacheConfiguration cCfgC = defaultCacheConfiguration();
        cCfgC.setName(CACHE_NAME_CLOSE_DHT);
        cCfgC.setCacheMode(CacheMode.PARTITIONED);

        CacheConfiguration cCfgDClient = defaultCacheConfiguration();
        cCfgDClient.setName(CACHE_NAME_DESTROY_CLIENT);
        cCfgDClient.setCacheMode(CacheMode.PARTITIONED);

        CacheConfiguration cCfgCClient = defaultCacheConfiguration();
        cCfgCClient.setName(CACHE_NAME_CLOSE_CLIENT);
        cCfgCClient.setCacheMode(CacheMode.PARTITIONED);

        CacheConfiguration cCfgDNear = defaultCacheConfiguration();
        cCfgDNear.setName(CACHE_NAME_DESTROY_NEAR);
        cCfgDNear.setCacheMode(CacheMode.PARTITIONED);
        cCfgDNear.setNearConfiguration(new NearCacheConfiguration());

        CacheConfiguration cCfgCNear = defaultCacheConfiguration();
        cCfgCNear.setName(CACHE_NAME_CLOSE_NEAR);
        cCfgCNear.setCacheMode(CacheMode.PARTITIONED);
        cCfgCNear.setNearConfiguration(new NearCacheConfiguration());

        CacheConfiguration cCfgDLoc = defaultCacheConfiguration();
        cCfgDLoc.setName(CACHE_NAME_DESTROY_LOC);
        cCfgDLoc.setCacheMode(CacheMode.LOCAL);

        CacheConfiguration cCfgCLoc = defaultCacheConfiguration();
        cCfgCLoc.setName(CACHE_NAME_CLOSE_LOC);
        cCfgCLoc.setCacheMode(CacheMode.LOCAL);

        grid(0).getOrCreateCache(cCfgD);
        grid(0).getOrCreateCache(cCfgC);
        grid(0).getOrCreateCache(cCfgDClient);
        grid(0).getOrCreateCache(cCfgCClient);
        grid(0).getOrCreateCache(cCfgDNear);
        grid(0).getOrCreateCache(cCfgCNear);
        grid(0).getOrCreateCache(cCfgDLoc);
        grid(0).getOrCreateCache(cCfgCLoc);

        grid(0).cache(CACHE_NAME_DESTROY_DHT).put(KEY_VAL, KEY_VAL);
        grid(0).cache(CACHE_NAME_CLOSE_DHT).put(KEY_VAL, KEY_VAL);
        grid(0).cache(CACHE_NAME_DESTROY_CLIENT).put(KEY_VAL, KEY_VAL);
        grid(0).cache(CACHE_NAME_CLOSE_CLIENT).put(KEY_VAL, KEY_VAL);
        grid(0).cache(CACHE_NAME_DESTROY_LOC).put(KEY_VAL, KEY_VAL + 0);
        grid(0).cache(CACHE_NAME_CLOSE_LOC).put(KEY_VAL, KEY_VAL + 0);
        grid(1).cache(CACHE_NAME_DESTROY_LOC).put(KEY_VAL, KEY_VAL + 1);
        grid(1).cache(CACHE_NAME_CLOSE_LOC).put(KEY_VAL, KEY_VAL + 1);

        assert grid(0).cache(CACHE_NAME_DESTROY_DHT).get(KEY_VAL).equals(KEY_VAL);
        assert grid(0).cache(CACHE_NAME_CLOSE_DHT).get(KEY_VAL).equals(KEY_VAL);
        assert grid(0).cache(CACHE_NAME_DESTROY_CLIENT).get(KEY_VAL).equals(KEY_VAL);
        assert grid(0).cache(CACHE_NAME_CLOSE_CLIENT).get(KEY_VAL).equals(KEY_VAL);
        assert grid(0).cache(CACHE_NAME_DESTROY_LOC).get(KEY_VAL).equals(KEY_VAL + 0);
        assert grid(0).cache(CACHE_NAME_CLOSE_LOC).get(KEY_VAL).equals(KEY_VAL + 0);
        assert grid(1).cache(CACHE_NAME_DESTROY_LOC).get(KEY_VAL).equals(KEY_VAL + 1);
        assert grid(1).cache(CACHE_NAME_CLOSE_LOC).get(KEY_VAL).equals(KEY_VAL + 1);

        //Destroy:

        //DHT Destroy. Cache should be removed from each node.

        grid(0).cache(CACHE_NAME_DESTROY_DHT).destroy();

        try {
            grid(0).cache(CACHE_NAME_DESTROY_DHT).get(KEY_VAL);
            assert false;
        }
        catch (IllegalArgumentException | IllegalStateException ignored0) {
            try {
                grid(1).cache(CACHE_NAME_DESTROY_DHT).get(KEY_VAL);
                assert false;
            }
            catch (IllegalArgumentException | IllegalStateException ignored1) {
                try {
                    grid(2).cache(CACHE_NAME_DESTROY_DHT).get(KEY_VAL);
                    assert false;
                }
                catch (IllegalArgumentException | IllegalStateException ignored2) {
                    // No-op
                }
            }
        }

        //DHT Destroy from client node. Cache should be removed from each node.

        grid(2).cache(CACHE_NAME_DESTROY_CLIENT).destroy();// Client node.

        try {
            grid(0).cache(CACHE_NAME_DESTROY_CLIENT).get(KEY_VAL);
            assert false;
        }
        catch (IllegalArgumentException | IllegalStateException ignored0) {
            try {
                grid(1).cache(CACHE_NAME_DESTROY_CLIENT).get(KEY_VAL);
                assert false;
            }
            catch (IllegalArgumentException | IllegalStateException ignored1) {
                try {
                    grid(2).cache(CACHE_NAME_DESTROY_CLIENT).get(KEY_VAL);
                    assert false;
                }
                catch (IllegalArgumentException | IllegalStateException ignored2) {
                    // No-op
                }
            }
        }

        //Local destroy. Cache should be removed from each node.

        grid(0).cache(CACHE_NAME_DESTROY_LOC).destroy();

        try {
            grid(0).cache(CACHE_NAME_DESTROY_LOC).get(KEY_VAL);
            assert false;
        }
        catch (IllegalArgumentException | IllegalStateException ignored0) {
            try {
                grid(1).cache(CACHE_NAME_DESTROY_LOC).get(KEY_VAL);
                assert false;
            }
            catch (IllegalArgumentException | IllegalStateException ignored1) {
                try {
                    grid(2).cache(CACHE_NAME_DESTROY_LOC).get(KEY_VAL);
                    assert false;
                }
                catch (IllegalArgumentException | IllegalStateException ignored2) {
                    // No-op
                }
            }
        }

        //Near destroy. Cache should be removed from each node.

        grid(0).createNearCache(CACHE_NAME_DESTROY_NEAR, new NearCacheConfiguration());
        grid(1).createNearCache(CACHE_NAME_DESTROY_NEAR, new NearCacheConfiguration());
        grid(2).createNearCache(CACHE_NAME_DESTROY_NEAR, new NearCacheConfiguration());

        grid(0).cache(CACHE_NAME_DESTROY_NEAR).put(KEY_VAL, KEY_VAL);

        assert grid(0).cache(CACHE_NAME_DESTROY_NEAR).get(KEY_VAL).equals(KEY_VAL);
        assert grid(1).cache(CACHE_NAME_DESTROY_NEAR).get(KEY_VAL).equals(KEY_VAL);
        assert grid(2).cache(CACHE_NAME_DESTROY_NEAR).get(KEY_VAL).equals(KEY_VAL);

        grid(0).cache(CACHE_NAME_DESTROY_NEAR).destroy();

        try {
            grid(0).cache(CACHE_NAME_DESTROY_NEAR).get(KEY_VAL);
            assert false;
        }
        catch (IllegalArgumentException | IllegalStateException ignored0) {
            try {
                grid(1).cache(CACHE_NAME_DESTROY_NEAR).get(KEY_VAL);
                assert false;
            }
            catch (IllegalArgumentException | IllegalStateException ignored1) {
                try {
                    grid(2).cache(CACHE_NAME_DESTROY_NEAR).get(KEY_VAL);
                    assert false;
                }
                catch (IllegalArgumentException | IllegalStateException ignored2) {
                    // No-op
                }
            }
        }

        //Close:

        //DHT Close. No-op.

        grid(0).cache(CACHE_NAME_CLOSE_DHT).close();

        assert grid(0).cache(CACHE_NAME_CLOSE_DHT).get(KEY_VAL).equals(KEY_VAL);// Not affected.
        assert grid(1).cache(CACHE_NAME_CLOSE_DHT).get(KEY_VAL).equals(KEY_VAL);// Not affected.
        assert grid(2).cache(CACHE_NAME_CLOSE_DHT).get(KEY_VAL).equals(KEY_VAL);// Not affected.

        //DHT Creation after closed.

        grid(0).getOrCreateCache(cCfgC);

        grid(0).cache(CACHE_NAME_CLOSE_DHT).put(KEY_VAL, KEY_VAL + "recreated");

        assert grid(0).cache(CACHE_NAME_CLOSE_DHT).get(KEY_VAL).equals(KEY_VAL + "recreated");
        assert grid(1).cache(CACHE_NAME_CLOSE_DHT).get(KEY_VAL).equals(KEY_VAL + "recreated");
        assert grid(2).cache(CACHE_NAME_CLOSE_DHT).get(KEY_VAL).equals(KEY_VAL + "recreated");

        //DHT Close from client node. Should affect only client node.

        grid(2).cache(CACHE_NAME_CLOSE_CLIENT).close();// Client node.

        assert grid(0).cache(CACHE_NAME_CLOSE_CLIENT).get(KEY_VAL).equals(KEY_VAL);// Not affected.
        assert grid(1).cache(CACHE_NAME_CLOSE_CLIENT).get(KEY_VAL).equals(KEY_VAL);// Not affected.

        try {
            grid(2).cache(CACHE_NAME_CLOSE_CLIENT).get(KEY_VAL);// Affected.
            assert false;
        }
        catch (IllegalArgumentException | IllegalStateException ignored) {
            // No-op
        }

        //DHT Creation from client node after closed.

        grid(2).getOrCreateCache(cCfgCClient);

        grid(0).cache(CACHE_NAME_CLOSE_CLIENT).put(KEY_VAL, KEY_VAL + "recreated");

        assert grid(0).cache(CACHE_NAME_CLOSE_CLIENT).get(KEY_VAL).equals(KEY_VAL + "recreated");
        assert grid(1).cache(CACHE_NAME_CLOSE_CLIENT).get(KEY_VAL).equals(KEY_VAL + "recreated");
        assert grid(2).cache(CACHE_NAME_CLOSE_CLIENT).get(KEY_VAL).equals(KEY_VAL + "recreated");

        //Near Close from client node.

        grid(2).createNearCache(CACHE_NAME_CLOSE_NEAR, new NearCacheConfiguration());

        //Subscribing to events.
        grid(2).cache(CACHE_NAME_CLOSE_NEAR).put(KEY_VAL, KEY_VAL);

        grid(2).cache(CACHE_NAME_CLOSE_NEAR).close();

        CountingTxRequestsToClientNodeTcpCommunicationSpi.cnt.set(0);

        //Should not produce messages to client node.
        grid(0).cache(CACHE_NAME_CLOSE_NEAR).put(KEY_VAL, KEY_VAL + 0);

        U.sleep(1000);

        assert CountingTxRequestsToClientNodeTcpCommunicationSpi.cnt.get() == 0;

        assert grid(0).cache(CACHE_NAME_CLOSE_NEAR).get(KEY_VAL).equals(KEY_VAL + 0);// Not affected.
        assert grid(1).cache(CACHE_NAME_CLOSE_NEAR).get(KEY_VAL).equals(KEY_VAL + 0);// Not affected.

        try {
            grid(2).cache(CACHE_NAME_CLOSE_NEAR).get(KEY_VAL);// Affected.
            assert false;
        }
        catch (IllegalArgumentException | IllegalStateException ignored) {
            // No-op
        }

        //Near Creation from client node after closed.

        grid(2).getOrCreateNearCache(CACHE_NAME_CLOSE_NEAR, new NearCacheConfiguration());

        grid(2).cache(CACHE_NAME_CLOSE_NEAR).put(KEY_VAL, KEY_VAL);
        grid(0).cache(CACHE_NAME_CLOSE_NEAR).put(KEY_VAL, KEY_VAL + "recreated");

        assert grid(0).cache(CACHE_NAME_CLOSE_NEAR).get(KEY_VAL).equals(KEY_VAL + "recreated");
        assert grid(1).cache(CACHE_NAME_CLOSE_NEAR).get(KEY_VAL).equals(KEY_VAL + "recreated");
        assert grid(2).cache(CACHE_NAME_CLOSE_NEAR).localPeek(KEY_VAL).equals(KEY_VAL + "recreated");

        //Local close. Same as Local destroy.

        grid(1).cache(CACHE_NAME_CLOSE_LOC).close();

        try {
            grid(0).cache(CACHE_NAME_CLOSE_LOC).get(KEY_VAL);
            assert false;
        }
        catch (IllegalArgumentException | IllegalStateException ignored0) {
            try {
                grid(1).cache(CACHE_NAME_CLOSE_LOC).get(KEY_VAL);
                assert false;
            }
            catch (IllegalArgumentException | IllegalStateException ignored1) {
                try {
                    grid(2).cache(CACHE_NAME_CLOSE_LOC).get(KEY_VAL);
                    assert false;
                }
                catch (IllegalArgumentException | IllegalStateException ignored2) {
                    // No-op
                }
            }
        }

        //Local creation after closed.

        grid(0).getOrCreateCache(cCfgCLoc);

        grid(0).cache(CACHE_NAME_CLOSE_LOC).put(KEY_VAL, KEY_VAL + "recreated0");
        grid(1).cache(CACHE_NAME_CLOSE_LOC).put(KEY_VAL, KEY_VAL + "recreated1");
        grid(2).cache(CACHE_NAME_CLOSE_LOC).put(KEY_VAL, KEY_VAL + "recreated2");

        assert grid(0).cache(CACHE_NAME_CLOSE_LOC).get(KEY_VAL).equals(KEY_VAL + "recreated0");
        assert grid(1).cache(CACHE_NAME_CLOSE_LOC).get(KEY_VAL).equals(KEY_VAL + "recreated1");
        assert grid(2).cache(CACHE_NAME_CLOSE_LOC).get(KEY_VAL).equals(KEY_VAL + "recreated2");
    }

    /** {@inheritDoc} */
    @Override protected void afterTest() throws Exception {
        super.afterTest();

        stopAllGrids();
    }
}
