module.exports = {
    'Test put/get' : function(test) {
        test.expect(1);

        var TestUtils = require("./test_utils").TestUtils;
        var Cache = require(TestUtils.scriptPath() + "cache").Cache;
        var Server = require(TestUtils.scriptPath() + "server").Server;

        var assert = require('assert');

        //var node = startIgniteNode();

        setTimeout(initCache, 10000); //If start node from javascrip set timeout 10000);

        function initCache() {
            var server = new Server('127.0.0.1', 9090);
            var cache = new Cache(server, "mycache");
            cache.put("mykey", "6", onPut.bind(null, cache));
        }

        function onPut(cache, error) {
            if (error) {
                console.error("Failed to put " + error);
                finishTest(test/*, node*/);
                return;
            }

            console.log("Put finished");
            cache.get("mykey", onGet);
        }

        function onGet(error, value) {
            if (error) {
                console.error("Failed to get " + error);
                finishTest(test/*, node*/);
                return;
            }

            console.log("Get finished");
            test.ok(value === "6", "This shouldn't fail " + value + "<>6");
            finishTest(test/*, node*/);
        }
    },
    'Test connection' : function(test) {
        test.expect(0);

        //var node = startIgniteNode();
        var TestUtils = require("./test_utils").TestUtils;
        var Server = require(TestUtils.scriptPath() + "server").Server;

        setTimeout(initServer, 10000);

        function initServer() {
            var server = new Server('127.0.0.1', 9090);

            console.log("Try to check connection");

            server.checkConnection(onConnect);
        }

        function onConnect(error) {
            if (error) {
                finishWithError(test/*, node*/, error);
                return;
            }
            console.log("Successfully connected");
            finishTest(test/*, node*/);
        }
    },
    'Test ignition' : function(test) {
        test.expect(1);

        //var node = startIgniteNode('127.0.0.1', 9090);
        var TestUtils = require("./test_utils").TestUtils;
        var Ignition = require(TestUtils.scriptPath() + "ignition").Ignition;

        setTimeout(Ignition.start.bind(null, 9090, ['127.0.0.0', '127.0.0.1'], onConnect), 5000);

        function onConnect(error, server) {
            if (error) {
                finishWithError(test/*, node*/, error);
                return;
            }
            test.ok(server.host() === '127.0.0.1')
            finishTest(test/*, node*/);
        }
    }
 };

function finishWithError(test/*, node*/, error) {
    console.log("Error: " + error);
    test.ok(false);
    finishTest(test/*, node*/);
}

function finishTest(test/*, node*/) {
    //node.kill();
    test.done();
}