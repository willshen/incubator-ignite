var TestUtils = require("./test_utils").TestUtils;
var Ignition = require(TestUtils.scriptPath() + "ignition").Ignition;

Ignition.start(9090, ['127.0.0.0', '127.0.0.1'], onConnect);

function onConnect(error, server) {
    if (error) {
        TestUtils.testFails(error);

        return;
    }

    TestUtils.testDone();
}