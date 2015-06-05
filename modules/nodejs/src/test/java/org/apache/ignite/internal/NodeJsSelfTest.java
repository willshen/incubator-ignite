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

package org.apache.ignite.internal;

import org.apache.ignite.internal.util.*;
import org.apache.ignite.internal.util.typedef.*;

import java.util.*;
import java.util.concurrent.*;

import static java.util.concurrent.TimeUnit.*;

/**
 * Test node js client.
 */
public class NodeJsSelfTest extends NodeJsAbstractTest {
    /**
     * @throws Exception If failed.
     */
    public void testPutGetJs() throws Exception {
        startGrid(0);

        final CountDownLatch readyLatch = new CountDownLatch(1);

        GridJavaProcess proc = null;

        final List<String> errors = new ArrayList<>();

        List<String> cmd = new ArrayList<>();

        cmd.add("C:\\Program Files\\nodejs\\node_modules\\.bin\\nodeunit.cmd");

        cmd.add(getNodeJsTestDir() + "test.js");

        Map<String, String> env = new HashMap<>();

        env.put("IGNITE_HOME", IgniteUtils.getIgniteHome());
        try {
            proc = GridJavaProcess.exec(
                cmd,
                env,
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
}
