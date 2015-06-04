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
        addresses: [String]
    },
    caches: [{ type: ObjectId, ref: 'Cache' }],
    pubPoolSize: Number,
    sysPoolSize: Number,
    mgmtPoolSize: Number,
    p2pPoolSize: Number
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