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
 * @param {number} port Port
 * @param {string[]} hosts List of nodes hosts
 * @param {Ignition~onStart} callback Called on finish
 */
Ignition.start = function(port, hosts, callback) {
    var Server = require("./server").Server;
    var numConn = hosts.length;
    for (var host of hosts) {
        var server = new Server(host, port);
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

        console.log(error);
        if (!numConn)
            callback.call(null, "Cannot connect to servers.", null);
    }
}

exports.Ignition = Ignition;