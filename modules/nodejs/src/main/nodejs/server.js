/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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