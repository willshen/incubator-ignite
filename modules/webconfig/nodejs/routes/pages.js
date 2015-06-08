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

// GET dropdown-menu template.
router.get('/select', function(req, res) {
    res.render('templates/select', { });
});

/* GET login page. */
router.get('/login', function(req, res) {
    res.render('login');
});

/* GET page for discovery advanced settings. */
router.get('/discovery', function(req, res) {
    res.render('discovery');
});

/* GET page for indexedTypes popup. */
router.get('/indexedTypes', function(req, res) {
    res.render('indexedTypes');
});

/* GET register page. */
router.get('/register', function(req, res) {
    res.render('register');
});

/* GET home page. */
router.get('/', function(req, res) {
    if (req.isAuthenticated())
        res.redirect('/clusters');
    else
        res.render('index', { user: req.user });
});

/* GET clusters page. */
router.get('/clusters', function(req, res) {
    res.render('clusters', { user: req.user });
});

router.get('/discovery', function(req, res) {
    res.render('discovery');
});

/* GET advanced options for TcpDiscoveryVmIpFinder page. */
router.get('/tcpDiscoveryVmIpFinder', function(req, res) {
    res.render('tcpDiscoveryVmIpFinder');
});

/* GET advanced options for TcpDiscoveryMulticastIpFinder page. */
router.get('/tcpDiscoveryMulticastIpFinder', function(req, res) {
    res.render('tcpDiscoveryMulticastIpFinder');
});

/* GET caches page. */
router.get('/caches', function(req, res) {
    res.render('caches', { user: req.user });
});

/* GET persistence page. */
router.get('/persistence', function(req, res) {
    res.render('persistence', { user: req.user });
});

/* GET sql page. */
router.get('/sql', function(req, res) {
    res.render('sql', { user: req.user });
});

/* GET clients page. */
router.get('/clients', function(req, res) {
    res.render('clients', { user: req.user });
});

module.exports = router;
