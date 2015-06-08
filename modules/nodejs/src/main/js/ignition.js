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
 * Creates an instance of Ignition
 *
 * @constructor
 */
function Ignition() {
}

/**
 * Callback for Ignition start
 *
 * @callback Ignition~onStart
 * @param {string} error Error
 * @param {Server} server Connected server
 */

/**
 * Open connection with server node
 *
 * @param {string[]} address List of nodes hosts with ports
 * @param {Ignition~onStart} callback Called on finish
 */
Ignition.start = function(address, callback) {
    var Server = require("./server").Server;
    var numConn = address.length;
    for (var addr of address) {
        var params = addr.split(":");
        var server = new Server(params[0], params[1]);
        server.checkConnection(onConnect.bind(null, server));
    }

    function onConnect(server, error) {
        if (!callback)
            return;

        numConn--;
        if (!error) {
            callback.call(null, null, server);
            callback = null;
            return;
        }

        console.log("onConnect:" + error);

        if (!numConn)
            callback.call(null, "Cannot connect to servers.", null);
    }
}

exports.Ignition = Ignition;