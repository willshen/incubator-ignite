#!/usr/bin/env bash
if [ "${IGNITE_HOME}" = "" ]; then
    export IGNITE_HOME="$(dirname $(readlink -f $0))"/..
fi

nodeunit ${IGNITE_HOME}$/../ggprivate/modules/clients/src/test/nodejs/test.js