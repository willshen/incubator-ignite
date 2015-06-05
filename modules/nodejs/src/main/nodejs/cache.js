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