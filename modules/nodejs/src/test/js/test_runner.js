/**
 * Create instance of TestUtils
 *
 * @constructor
 */
function TestRunner() {
}

/**
 * Test routine.
 */
TestRunner.runTest = function() {
    var fileName = process.argv[2].toString().trim();

    console.log("FileName " + fileName);

    var test = require("./" + fileName);

    var functionName = process.argv[3].toString().trim();

    if (!test[functionName]) {
        console.log("node js test failed: function with name " + functionName + " not found");
        return;
    }
    test[functionName]();
}


TestRunner.runTest();