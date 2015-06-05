/*
 *  Copyright (C) GridGain Systems. All Rights Reserved.
 *  _________        _____ __________________        _____
 *  __  ____/___________(_)______  /__  ____/______ ____(_)_______
 *  _  / __  __  ___/__  / _  __  / _  / __  _  __ `/__  / __  __ \
 *  / /_/ /  _  /    _  /  / /_/ /  / /_/ /  / /_/ / _  /  _  / / /
 *  \____/   /_/     /_/   \_,__/   \____/   \__,_/  /_/   /_/ /_/
 */

package org.apache.ignite.internal;

import org.apache.ignite.cache.*;
import org.apache.ignite.configuration.*;
import org.apache.ignite.internal.util.*;
import org.apache.ignite.internal.util.typedef.*;
import org.apache.ignite.internal.util.typedef.internal.*;
import org.apache.ignite.testframework.junits.common.*;

import java.util.*;
import java.util.concurrent.*;

import static java.util.concurrent.TimeUnit.*;

/**
 * Test node js client.
 */
public class NodeJsSelfTest extends GridCommonAbstractTest {
    /** Cache name. */
    private static final String CACHE_NAME = "mycache";

    /** {@inheritDoc} */
    @Override protected IgniteConfiguration getConfiguration(String gridName) throws Exception {
        IgniteConfiguration cfg = super.getConfiguration(gridName);
        cfg.setCacheConfiguration(cacheConfiguration());

        ConnectorConfiguration conCfg = new ConnectorConfiguration();

        conCfg.setJettyPath(getNodeJsTestDir() + "rest-jetty.xml");

        cfg.setConnectorConfiguration(conCfg);

        return cfg;
    }

    /**
     * @return Cache configuration.
     */
    private CacheConfiguration cacheConfiguration() {
        CacheConfiguration ccfg = new CacheConfiguration();

        ccfg.setName(CACHE_NAME);
        ccfg.setAtomicityMode(CacheAtomicityMode.ATOMIC);

        return ccfg;
    }

    /**
     * @throws Exception If failed.
     */
    public void testPutGetJs() throws Exception {
        startGrid(0);

        final CountDownLatch readyLatch = new CountDownLatch(1);

        GridJavaProcess proc = null;

        final List<String> errors = new ArrayList<>();

        try {
            proc = GridJavaProcess.exec(
                getNodeJsTestDir() + "runtest.bat",
                log,
                new CI1<String>() {
                    @Override
                    public void apply(String s) {
                        info("Node js: " + s);

                        if (s.contains("OK: "))
                            readyLatch.countDown();

                        if (s.contains("Error") || s.contains("FAILURES")) {
                            errors.add("Script failed: " + s);

                            readyLatch.countDown();
                        }
                    }
                },
                null
            );

            assertTrue(readyLatch.await(60, SECONDS));

            assertEquals(errors.toString(), 0, errors.size());

            proc.getProcess().waitFor();
        }
        finally {
            stopAllGrids();

            if (proc != null)
                proc.killProcess();
        }
    }

    /**
     * @return Node js test dir.
     */
    private String getNodeJsTestDir() {
        String sep = System.getProperty("file.separator");

        return U.getIgniteHome() +
            sep + "modules" +
            sep + "nodejs" +
            sep + "src" +
            sep + "test" +
            sep + "nodejs" + sep;
    }
}
