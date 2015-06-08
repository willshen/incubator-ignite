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
  this._cacheNameParam = _pair("cacheName", this._cacheName);
}

/**
 * Get cache value
 *
 * @this {Cache}
 * @param {string} key Key
 * @param {Cache~onGet} callback Called on finish
 */
Cache.prototype.get = function(key, callback) {
  this._server.runCommand("get", [this._cacheNameParam, _pair("key", key)], callback);
};

/**
 * Callback for cache get
 * @callback Cache~onGet
 * @param {string} error Error
 * @param {string} result Result value
 */

/**
 * Put cache value
 *
 * @this {Cache}
 * @param {string} key Key
 * @param {string} value Value
 * @param {Cache~onPut} callback Called on finish
 */
Cache.prototype.put = function(key, value, callback) {
  this._server.runCommand("put", [this._cacheNameParam, _pair("key", key), _pair("val", value)],
    callback);
}

/**
 * Callback for cache put
 * @callback Cache~onPut
 * @param {string} error Error
 */

function _pair(key, value) {
  return {key: key, value: value}
}

exports.Cache = Cache