var TestUtils = require("./test_utils").TestUtils;
var Ignition = require(TestUtils.scriptPath() + "ignition").Ignition;

Ignition.start(9091, ['127.0.0.0', '127.0.0.1'], onConnect);

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