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

var TestUtils = require("./test_utils").TestUtils;
var Cache = require(TestUtils.scriptPath() + "cache").Cache;
var Server = require(TestUtils.scriptPath() + "server").Server;

testPutGet = function() {
    var server = new Server('127.0.0.1', 9090);
    var cache = new Cache(server, "mycache");
    cache.put("key", "6", onPut.bind(null, cache));
}

function onPut(cache, error) {
    if (error) {
        TestUtils.testFails("Incorrect error message: " + error);
        return;
    }

    console.log("Put finished");
    cache.get("key", onGet);
}

function onGet(error, value) {
    if (error) {
        console.error("Failed to get " + error);
        TestUtils.testFails("Incorrect error message: " + error);
        return;
    }

    var assert = require("assert");

    assert.equal(value, 6, "Get return incorrect value. + [expected=" + 6 + ", val=" + value + "].");

    TestUtils.testDone();
}