/**
 * Creates an instance of Server
 *
 * @constructor
 * @this {Server}
 * @param {string} host Host address
 * @param {number} port Port
 */
function Server(host, port) {
    this._host = host;
    this._port = port;
}

/**
 * Host value
 *
 * @this {Server}
 * @returns {string} Host value
 */
Server.prototype.host = function()
{
    return this._host;
}

/**
 * Callback for Server runCommand
 *
 * @callback Server~onRunCommand
 * @param {string} error Error
 * @param {string} result Result value
 */

/**
 * Run http request
 *
 * @this {Server}
 * @param {string} cmdName command name.
 * @param params Parameters for command.
 * @param {Server~onRunCommand} Called on finish
 */
Server.prototype.runCommand = function(cmdName, params, callback) {
    var paramsString = "";

    for (var p of params)
        //TODO: escape value
        paramsString += "&" + p.key + "=" + p.value;

    var requestQry = "cmd=" + cmdName + paramsString;

    var http = require('http');

    var options = {
        host: this._host,
        port: this._port,
        path: "/ignite?" + requestQry
    };

    function streamCallback(response) {
        var fullResponseString = '';

        response.on('data', function (chunk) {
            fullResponseString += chunk;
        });

        response.on('end', function () {
            try {
                var response = JSON.parse(fullResponseString);
                if (response.successStatus)
                    callback.call(null, response.error, null)
                else
                    callback.call(null, null, response.response);
            } catch (e) {
                console.log("fail on json parse: " + fullResponseString)
                callback.call(null, e, null);
            }
        });
    }

    var request = http.request(options, streamCallback);

    request.setTimeout(5000, callback.bind(null, "Request timeout: >5 sec"));

    request.on('error', callback);
    request.end();
}

/**
 * Check the connection with server node.
 *
 * @this {Server}
 * @param {Server~onRunCommand} callback Called on finish
 */
Server.prototype.checkConnection = function(callback) {
    this.runCommand("version", [], callback);
}

exports.Server = Server;