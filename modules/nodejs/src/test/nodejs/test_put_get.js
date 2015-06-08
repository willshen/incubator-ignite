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