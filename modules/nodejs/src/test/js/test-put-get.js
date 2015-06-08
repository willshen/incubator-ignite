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

var TestUtils = require("./test-utils").TestUtils;

var Apache = require(TestUtils.scriptPath());
var Cache = Apache.Cache;
var Server = Apache.Server;

testPutGet = function() {
  TestUtils.startIgniteNode(onStart.bind(null, onPut, "mycache"));
}

testIncorrectCacheName = function() {
  TestUtils.startIgniteNode(onStart.bind(null, onIncorrectPut, "mycache1"));
}

function onStart(onPut1, cacheName, error, ignite) {
  var cache = ignite.cache(cacheName);

  cache.put("key", "6", onPut1.bind(null, cache));
}

function onPut(cache, error) {
  if (error) {
    TestUtils.testFails("Incorrect error message: " + error);

    return;
  }

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

function onIncorrectPut(cache, error) {
  if (error) {
    console.error("Failed to get " + error);

    var assert = require("assert");

    assert(error.indexOf("Failed to find cache for given cache name") !== -1);

    TestUtils.testDone();

    return;
  }

  TestUtils.testFails("Exception should be thrown.");
}