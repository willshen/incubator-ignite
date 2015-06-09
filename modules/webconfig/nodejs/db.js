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

var config = require('./configuration.js');

// Mongoose for mongodb.
var mongoose = require('mongoose'),
    Schema = mongoose.Schema,
    ObjectId = mongoose.Schema.Types.ObjectId,
    passportLocalMongoose = require('passport-local-mongoose');

// Connect to mongoDB database.
mongoose.connect(config.get('mongoDB:url'), {server: {poolSize: 4}});

// Define account model.
var AccountSchema = new Schema({
    username: String
});

AccountSchema.plugin(passportLocalMongoose, { usernameField: 'email' });

exports.Account = mongoose.model('Account', AccountSchema);

// Define space model.
exports.Space =  mongoose.model('Space', new Schema({
    name: String,
    owner: { type: ObjectId, ref: 'Account' },
    usedBy: [{
        permission: { type: String, enum: ['VIEW', 'FULL']},
        account: { type: ObjectId, ref: 'Account' }
    }]
}));

var DiscoveryObj = {
    className: String, enum: ['TcpDiscoveryVmIpFinder', 'TcpDiscoveryMulticastIpFinder', 'TcpDiscoveryS3IpFinder',
        'TcpDiscoveryCloudIpFinder', 'TcpDiscoveryGoogleStorageIpFinder', 'TcpDiscoveryJdbcIpFinder',
        'TcpDiscoverySharedFsIpFinder'],
    addresses: [String]
};

// Define cache model.
var CacheSchema = new Schema({
    space: { type: ObjectId, ref: 'Space' },
    name: String,
    mode: { type: String, enum: ['PARTITIONED', 'REPLICATED', 'LOCAL'] },
    backups: Number,
    atomicity: { type: String, enum: ['ATOMIC', 'TRANSACTIONAL'] }
});

exports.Cache =  mongoose.model('Cache', CacheSchema);

// Define discovery model.
exports.Discovery =  mongoose.model('Discovery', new Schema(DiscoveryObj));

var ClusterSchema = new Schema({
    space: { type: ObjectId, ref: 'Space' },
    name: String,
    discovery: {
        kind: { type: String, enum: ['Vm', 'Multicast', 'S3', 'Cloud', 'GoogleStorage', 'Jdbc', 'SharedFs'] },
        Vm: {
            addresses: [String]
        },
        Multicast: {
            multicastGroup: String,
            multicastPort: Number,
            responseWaitTime: Number,
            addressRequestAttempts: Number,
            localAddress: String
        },
        S3: {
            bucketName: String
        },
        Cloud: {
            credential: String,
            credentialPath: String,
            identity: String,
            provider: String
        },
        GoogleStorage: {
            projectName: String,
            bucketName: String,
            serviceAccountP12FilePath: String,
            addrReqAttempts:String
        },
        Jdbc: {
            initSchema: Boolean
        },
        SharedFs: {
            path: String
        }
    },
    atomicConfiguration: {
        backups: Number,
        cacheMode: { type: String, enum: ['LOCAL', 'REPLICATED', 'PARTITIONED'] },
        atomicSequenceReserveSize: Number
    },
    caches: [{ type: ObjectId, ref: 'Cache' }],
    cacheSanityCheckEnabled: Boolean,
    clockSyncSamples: Number,
    clockSyncFrequency: Number,
    deploymentMode: { type: String, enum: ['PRIVATE', 'ISOLATED', 'SHARED', 'CONTINUOUS'] },
    discoveryStartupDelay: Number,
    includeEventTypes: [{ type: String, enum: ['EVTS_CHECKPOINT', 'EVTS_DEPLOYMENT', 'EVTS_ERROR', 'EVTS_DISCOVERY',
        'EVTS_JOB_EXECUTION', 'EVTS_TASK_EXECUTION', 'EVTS_CACHE', 'EVTS_CACHE_REBALANCE', 'EVTS_CACHE_LIFECYCLE',
        'EVTS_CACHE_QUERY', 'EVTS_SWAPSPACE', 'EVTS_IGFS'] }],
    marshalLocalJobs: Boolean,
    marshCacheKeepAliveTime: Number,
    marshCachePoolSize: Number,
    metricsExpireTime: Number,
    metricsHistorySize: Number,
    metricsLogFrequency: Number,
    metricsUpdateFrequency: Number,
    localHost: String,
    networkTimeout: Number,
    networkSendRetryDelay: Number,
    networkSendRetryCount: Number,
    segmentCheckFrequency: Number,
    waitForSegmentOnStart: Boolean,
    peerClassLoadingEnabled: Boolean,
    peerClassLoadingLocalClassPathExclude: [String],
    peerClassLoadingMissedResourcesCacheSize: Number,
    peerClassLoadingThreadPoolSize: Number,
    timeServerPortBase: Number,
    timeServerPortRange: Number,
    publicThreadPoolSize: Number,
    systemThreadPoolSize: Number,
    managementThreadPoolSize: Number,
    igfsThreadPoolSize: Number,
    transactionConfiguration: {
        defaultTxConcurrency: { type: String, enum: ['OPTIMISTIC', 'PESSIMISTIC'] },
        transactionIsolation: { type: String, enum: ['READ_COMMITTED', 'REPEATABLE_READ', 'SERIALIZABLE'] },
        defaultTxTimeout: Number,
        pessimisticTxLogLinger: Number,
        pessimisticTxLogSize: Number,
        txSerializableEnabled: Boolean
    },
    segmentationPolicy: { type: String, enum: ['RESTART_JVM', 'STOP', 'NOOP'] },
    allSegmentationResolversPassRequired: Boolean,
    segmentationResolveAttempts: Number,
    utilityCacheKeepAliveTime: Number,
    utilityCachePoolSize: Number
});

// Define cluster model.
exports.Cluster =  mongoose.model('Cluster', ClusterSchema);

exports.upsert = function(model, data, cb){
    if (data._id) {
        var id = data._id;

        delete data._id;

        model.findOneAndUpdate({_id: id}, data, cb);
    }
    else
        new model(data).save(cb);
};

exports.mongoose = mongoose;