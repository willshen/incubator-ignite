module.exports = {
    'Test put/get' : function(test) {
        test.expect(1);

        var Cache = require(scriptPath() + "cache").Cache;
        var Server = require(scriptPath() + "server").Server;

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

        var Server = require(scriptPath() + "server").Server;

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

        var Ignition = require(scriptPath() + "ignition").Ignition;

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

function scriptPath() {
    return igniteHome() +
       sep() + "modules" +
       sep() + "nodejs" +
       sep() + "src" +
       sep() + "main" +
       sep() + "nodejs" + sep();
}

function startIgniteNode() {
    var libs = classpath(igniteHome() +  sep() + "target" +
        sep() + "bin" +
        sep() + "apache-ignite-fabric-1.1.1-SNAPSHOT-bin" +
        sep() + "libs");

    var cp = libs.join(require('path').delimiter);

    var spawn = require('child_process').spawn;

    var child = spawn('java',['-classpath', cp, 'org.apache.ignite.startup.cmdline.CommandLineStartup',
        "test-node.xml"]);

    child.stdout.on('data', function (data) {
        console.log("" + data);
    });

    child.stderr.on('data', function (data) {
        console.log("" + data);
    });

    return child;
}

function finishWithError(test/*, node*/, error) {
    console.log("Error: " + error);
    test.ok(false);
    finishTest(test/*, node*/);
}

function finishTest(test/*, node*/) {
    //node.kill();
    test.done();
}

function igniteHome() {
    return process.env.IGNITE_HOME;
}

function sep() {
    return require('path').sep;
}

function classpath(dir) {
    var fs = require('fs');
    var path = require('path');
    function walk(dir, done) {
        var results = [];
        var list = fs.readdirSync(dir)
        for (var i = 0; i < list.length; ++i) {
            file = path.resolve(dir, list[i]);
            var stat = fs.statSync(file);
            if (stat && stat.isDirectory()) {
                if (list[i] != "optional" && file.indexOf("optional") !== -1 && file.indexOf("rest") == -1 )
                    continue;

                var sublist = walk(file);
                results = results.concat(sublist);
            } else {
                if (file.indexOf(".jar") !== -1) {
                    results.push(file);
                }
            }
        }
        return results;
    };

    return walk(dir);
};