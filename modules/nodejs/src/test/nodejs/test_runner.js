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

    var Test = require("./" + fileName);

    console.log("Test : " + Object.keys(Test))

    Test = Test[Object.keys(Test)[0]];

    console.log("Result test: " + Test);

    var functionName = process.argv[3].toString().trim();

    if (!Test[functionName]) {
        console.log("node js test failed: function with name " + functionName + " not found");
        return;
    }
    Test[functionName]();
}


TestRunner.runTest();