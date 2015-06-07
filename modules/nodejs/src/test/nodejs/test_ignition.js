exports = module.exports = {};

function TestIgnition () {
}

var TestUtils = require("./test_utils").TestUtils;
var Ignition = require(TestUtils.scriptPath() + "ignition").Ignition;

TestIgnition.test_ignition_fail = function ()  {
    Ignition.start(['127.0.0.3:9091', '127.0.0.1:9092'], onConnect);

    function onConnect(error, server) {
        if (error) {
            if (error.indexOf("Cannot connect to servers.") == -1)
                TestUtils.testFails("Incorrect error message: " + error);
            else
                TestUtils.testDone();

            return;
        }

        TestUtils.testFails("Test should fail.");
    }
}

TestIgnition.ignition_start_success = function() {
    Ignition.start(['127.0.0.1:9090'], onConnect);

    function onConnect(error, server) {
        if (error) {
            TestUtils.testFails(error);

            return;
        }
        TestUtils.testDone();
    }
}


exports.TestIgnition = TestIgnition;