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

        console.log(error);
        if (!numConn)
            callback.call(null, "Cannot connect to servers.", null);
    }
}

exports.Ignition = Ignition;