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

var Server = require("./server").Server;

/**
 * Creates an instance of Cache
 *
 * @constructor
 * @this {Cache}
 * @param {Server} server Server class
 * @param {string} cacheName Cache name
 */
function Cache(server, cacheName) {
  this._server = server;
  this._cacheName = cacheName;
  this._cacheNameParam = Server.pair("cacheName", this._cacheName);
}

/**
 * Callback for cache get
 * @callback Cache~onGet
 * @param {string} error Error
 * @param {string} result Result value
 */

/**
 * Get cache value
 *
 * @this {Cache}
 * @param {string} key Key
 * @param {Cache~onGet} callback Called on finish
 */
Cache.prototype.get = function(key, callback) {
  this._server.runCommand("get", [this._cacheNameParam, Server.pair("key", key)], callback);
};

/**
 * Callback for cache put
 * @callback Cache~noValue
 * @param {string} error Error
 */

/**
 * Put cache value
 *
 * @this {Cache}
 * @param {string} key Key
 * @param {string} value Value
 * @param {Cache~noValue} callback Called on finish
 */
Cache.prototype.put = function(key, value, callback) {
  this._server.runCommand("put", [this._cacheNameParam, Server.pair("key", key), Server.pair("val", value)],
    callback);
}

/**
 * Remove cache key
 *
 * @this {Cache}
 * @param {string} key Key
 * @param {Cache~noValue} callback Called on finish
 */
Cache.prototype.remove = function(key, callback) {
  this._server.runCommand("rmv", [this._cacheNameParam, Server.pair("key", key)], callback);
}

/**
 * Remove cache keys
 *
 * @this {Cache}
 * @param {string[]} keys Keys to remove
 * @param {Cache~noValue} callback Called on finish
 */
Cache.prototype.removeAll = function(keys, callback) {
  var params = [this._cacheNameParam];

  params = params.concat(Cache.concatParams("k", keys));

  this._server.runCommand("rmvall", params, callback);
}

/**
 * Put keys to cache
 *
 * @this {Cache}
 * @param {string[]} keys Keys
 * @param {string[]} values Values
 * @param {Cache~noValue} callback Called on finish
 */
Cache.prototype.putAll = function(keys, values, callback) {
  if (keys.length !== values.length) {
    callback.call(null, "Number of keys is not equal to number of values." +
      "keys size=" + keys.length + ", values size=" + values.length + "].");

    return;
  }

  var params = Cache.concatParams("k", keys);
  params = params.concat(Cache.concatParams("v", values));

  params.push(this._cacheNameParam);

  this._server.runCommand("putall", params, callback);
}

/**
 * Callback for cache get
 * @callback Cache~onGetAll
 * @param {string} error Error
 * @param {string[]} results Result values
 */

/**
 * Get keys from the cache
 *
 * @this {Cache}
 * @param {string[]} keys Keys
 * @param {Cache~onGetAll} callback Called on finish
 */
Cache.prototype.getAll = function(keys, callback) {
  var params = Cache.concatParams("k", keys);

  params.push(this._cacheNameParam);


  console.log("PARAM GET ALL "+ params);

  this._server.runCommand("getall", params, callback);
}

/**
 * Concatenate all parameters
 *
 * @param {string} pref Prefix
 * @param {string[]} keys Keys
 * @returns List of parameters.
 */
Cache.concatParams = function(pref, keys) {
  var temp = []

  for (var i = 0; i < keys.length; ++i) {
    temp.push(Server.pair(pref + i, keys[i]));
  }

  return temp;
}
exports.Cache = Cache