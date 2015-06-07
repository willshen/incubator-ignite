/**
 * Create instance of TestUtils
 *
 * @constructor
 */
function TestUtils() {
}

/**
 * @returns {string} Path to script dir
 */
TestUtils.scriptPath = function() {
    return TestUtils.igniteHome() +
       TestUtils.sep() + "modules" +
       TestUtils.sep() + "nodejs" +
       TestUtils.sep() + "src" +
       TestUtils.sep() + "main" +
       TestUtils.sep() + "nodejs" + TestUtils.sep();
}

/**
 * @returns {string} Ignite home path
 */
TestUtils.igniteHome = function() {
    return process.env.IGNITE_HOME;
}

/**
 * @returns {string} Path separator
 */
TestUtils.sep = function() {
    return require('path').sep;
}

/**
 * @param {string} dir Directory with all ignite libs
 * @returns {string} Classpath for ignite node start
 */
TestUtils.classpath = function(dir) {
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

/**
 * @returns Process that starts ignite node
 */
TestUtils.startIgniteNode = function() {
    var libs = classpath(igniteHome() +  TestUtils.sep() + "target" +
        TestUtils.sep() + "bin" +
        TestUtils.sep() + "apache-ignite-fabric-1.1.1-SNAPSHOT-bin" +
        TestUtils.sep() + "libs");

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

/**
 * Print error to console
 *
 * @param {string} error Error
 */
TestUtils.testFails = function(error) {
    console.log("Node JS test failed: " + error);
}

/**
 * Print ok message to console
 */
TestUtils.testDone = function() {
    console.log("Node JS test finished.")
}

exports.TestUtils = TestUtils;