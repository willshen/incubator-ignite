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

var assert = require("assert");

testPutGet = function() {
  TestUtils.startIgniteNode(onStart.bind(null, onPut, "mycache"));
}

testIncorrectCacheName = function() {
  TestUtils.startIgniteNode(onStart.bind(null, onIncorrectPut, "mycache1"));
}

testRemove = function() {
  TestUtils.startIgniteNode(onStart.bind(null, onPutRemove, "mycache"));
}

testRemoveNoKey = function() {
  TestUtils.startIgniteNode(onStartRemove.bind(null, onRemove, "mycache"));
}

testRemoveAll = function() {
  TestUtils.startIgniteNode(onStart.bind(null, onPutRemoveAll, "mycache"));
}

testPutAllGetAll = function() {
  TestUtils.startIgniteNode(onStartGetAll.bind(null, "mycache"));
}

function onStartGetAll(cacheName, error, ignite) {
  var cache = ignite.cache(cacheName);

  var map = {"key1": "val1", "key2" : "val2"};

  cache.putAll(map, onPutAll.bind(null, cache, map));
}

function onPutAll(cache, map, error) {
  assert(error == null);

  cache.getAll(Object.keys(map), onGetAll.bind(null, cache, map));
}

function onGetAll(cache, expected, error, values) {
  assert(error == null, error);

  var keys = Object.keys(expected);

  for (var i = 0; i < keys.length; ++i) {
    var key = keys[i];

    assert(!!values[key], "Cannot find key. [key=" + key + "].");

    assert(values[key] === expected[key], "Incorrect value. [key=" + key +
      ", expected=" + expected[key] + ", val= " + values[key] + "].");
  }

  TestUtils.testDone();
}

function onStart(onPut1, cacheName, error, ignite) {
  var cache = ignite.cache(cacheName);

  cache.put("key", "6", onPut1.bind(null, cache));
}

function onStartRemove(onPut1, cacheName, error, ignite) {
  var cache = ignite.cache(cacheName);

  cache.remove("key", onRemove.bind(null, cache));
}

function onPutRemove(cache, error) {
  assert(error == null);

  cache.get("key", onGetRemove.bind(null, cache));
}

function onPutRemoveAll(cache, error) {
  assert(error == null);

  cache.get("key", onGetRemoveAll.bind(null, cache));
}

function onGetRemoveAll(cache, error, value) {
  assert(error == null);

  assert(value == 6);

  cache.removeAll(["key"], onRemove.bind(null, cache));
}

function onGetRemove(cache, error, value) {
  assert(error == null);

  assert(value == 6);

  cache.remove("key", onRemove.bind(null, cache));
}

function onRemove(cache, error) {
  assert(error == null);

  cache.get("key", onGet.bind(null, null));
}

function onPut(cache, error) {
  assert(error == null);

  cache.get("key", onGet.bind(null, 6));
}

function onGet(expected, error, value) {
  console.log("onGet [error=" + error + ", val=" + value + "].");

  assert(error == null);

  assert.equal(value, expected, "Get return incorrect value. [expected=" + expected + ", val=" + value + "].");

  TestUtils.testDone();
}

function onIncorrectPut(cache, error) {
  if (error) {
    console.error("Failed to put " + error);

    assert(error.indexOf("Failed to find cache for given cache name") !== -1);

    TestUtils.testDone();

    return;
  }

  TestUtils.testFails("Exception should be thrown.");
}