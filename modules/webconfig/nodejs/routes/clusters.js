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

var router = require('express').Router();
var db = require('../db');

/**
 * Send spaces and clusters accessed for user account.
 *
 * @param req Request.
 * @param res Response.
 */
function selectAll(req, res) {
    var user_id = req.user._id;

    // Get owned space and all accessed space.
    db.Space.find({$or: [{owner: user_id}, {usedBy: {$elemMatch: {account: user_id}}}]}, function (err, spaces) {
        if (err)
            return res.status(500).send(err);

        var space_ids = spaces.map(function(value) {
            return value._id;
        });

        db.Cache.find({space: {$in: space_ids}}, '_id name', function (err, caches) {
            if (err)
                return res.status(500).send(err);

            // Get all clusters for spaces.
            db.Cluster.find({space: {$in: space_ids}}, function (err, clusters) {
                if (err)
                    return res.status(500).send(err);

                var cachesJson = caches.map(function(cache) {
                    return {value: cache._id, label: cache.name};
                });

                res.json({spaces: spaces, caches: cachesJson, clusters: clusters});
            });
        });
    });
}

/**
 * Get spaces and clusters accessed for user account.
 */
router.get('/', function(req, res) {
    selectAll(req, res);
});

/**
 * Save cluster.
 */
router.post('/save', function(req, res) {
    if (req.body._id)
        db.Cluster.update({_id: req.body._id}, req.body, {upsert: true}, function(err) {
            if (err)
                return res.send(err);

            res.sendStatus(200);
        });
    else {
        var cluster = new db.Cluster(req.body);

        cluster.save(function(err, cluster) {
            if (err)
                return res.send(err.message);

            res.send(cluster._id);
        });
    }
});

/**
 * Remove cluster by ._id.
 */
router.post('/remove', function(req, res) {
    db.Cluster.remove(req.body, function (err) {
        if (err)
            return res.send(err);

        res.sendStatus(200);
    })
});

module.exports = router;