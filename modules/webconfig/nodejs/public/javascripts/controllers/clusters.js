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

configuratorModule.controller('clustersController', ['$scope', '$modal', '$http', function($scope, $modal, $http) {
        $scope.templates = [
            {value: {}, label: 'None'},
            {value: {discovery: {kind: 'Vm', Vm: {addresses: ['127.0.0.1:47500..47510']}}}, label: 'Local'},
            {value: {discovery: {kind: 'Multicast', Multicast: {}}}, label: 'Basic'}
        ];

        $scope.discoveries = [
            {value: 'Vm', label: 'Static IPs'},
            {value: 'Multicast', label: 'Multicast'},
            {value: 'S3', label: 'AWS S3'},
            {value: 'Cloud', label: 'Apache jclouds'},
            {value: 'GoogleStorage', label: 'Google Cloud Storage'},
            {value: 'Jdbc', label: 'JDBC'},
            {value: 'SharedFs', label: 'Shared Filesystem'}
        ];

        $scope.events = [
            {value: 'EVTS_CHECKPOINT', label: 'Checkpoint'},
            {value: 'EVTS_DEPLOYMENT', label: 'Deployment'},
            {value: 'EVTS_ERROR', label: 'Error'},
            {value: 'EVTS_DISCOVERY', label: 'Discovery'},
            {value: 'EVTS_JOB_EXECUTION', label: 'Job execution'},
            {value: 'EVTS_TASK_EXECUTION', label: 'Task execution'},
            {value: 'EVTS_CACHE', label: 'Cache'},
            {value: 'EVTS_CACHE_REBALANCE', label: 'Cache rebalance'},
            {value: 'EVTS_CACHE_LIFECYCLE', label: 'Cache lifecycle'},
            {value: 'EVTS_CACHE_QUERY', label: 'Cache query'},
            {value: 'EVTS_SWAPSPACE', label: 'Swap space'},
            {value: 'EVTS_IGFS', label: 'Igfs'}
        ];

        $scope.cacheModes = [
            {value: 'LOCAL', label: 'LOCAL'},
            {value: 'REPLICATED', label: 'REPLICATED'},
            {value: 'PARTITIONED', label: 'PARTITIONED'}
        ];

        $scope.deploymentModes = [
            {value: 'PRIVATE', label: 'PRIVATE'},
            {value: 'ISOLATED', label: 'ISOLATED'},
            {value: 'SHARED', label: 'SHARED'},
            {value: 'CONTINUOUS', label: 'CONTINUOUS'}
        ];

        $scope.transactionConcurrency = [
            {value: 'OPTIMISTIC', label: 'OPTIMISTIC'},
            {value: 'PESSIMISTIC', label: 'PESSIMISTIC'}
        ];

        $scope.transactionIsolation = [
            {value: 'READ_COMMITTED', label: 'READ_COMMITTED'},
            {value: 'REPEATABLE_READ', label: 'REPEATABLE_READ'},
            {value: 'SERIALIZABLE', label: 'SERIALIZABLE'}
        ];

        $scope.segmentationPolicy = [
            {value: 'RESTART_JVM', label: 'RESTART_JVM'},
            {value: 'STOP', label: 'STOP'},
            {value: 'NOOP', label: 'NOOP'}
        ];

        $scope.clusters = [];

        $http.get('/form-models/clusters.json')
            .success(function(data) {
                $scope.general = data.general;
                $scope.advanced = data.advanced;
            });

        $scope.createSimpleItem = function(desc, rows) {
            $scope.simplePopup = {
                rows: rows,
                desc: desc
            };

            $scope.pupup = $modal({scope: $scope, template: '/simplePopup', show: true});
        };

        $scope.saveSimpleItem = function(row) {
            if ($scope.simplePopup.index)
                angular.extend($scope.simplePopup.rows[$scope.simplePopup.index], row);
            else
                $scope.simplePopup.rows.push(row);

            $scope.pupup.hide();
        };

        $scope.editSimpleItem = function(desc, rows, idx) {
            $scope.simplePopup = {
                desc: desc,
                rows: rows,
                index: index,
                row: angular.copy(rows[idx])
            };

            $modal({scope: $scope, template: '/simplePopup', show: true});
        };

        // When landing on the page, get clusters and show them.
        $http.get('/rest/clusters')
            .success(function(data) {
                $scope.caches = data.caches;
                $scope.spaces = data.spaces;
                $scope.clusters = data.clusters;
            });

        $scope.selectItem = function(item) {
            $scope.selectedItem = item;

            $scope.backupItem = angular.copy(item);
        };

        // Add new cluster.
        $scope.createItem = function() {
            var item = angular.copy($scope.create.template);

            item.name = 'Cluster ' + ($scope.clusters.length + 1);
            item.space = $scope.spaces[0]._id;

            $http.post('/rest/clusters/save', item)
                .success(function(_id) {
                    item._id = _id;

                    $scope.clusters.push(item);

                    $scope.selectItem(item);
                })
                .error(function(errorMessage) {
                    console.log('Error: ' + errorMessage);
                });
        };

        $scope.removeItem = function() {
            var _id = $scope.selectedItem._id;

            $http.post('/rest/clusters/remove', {_id: _id})
                .success(function() {
                    var i = _.findIndex($scope.clusters, function(cluster) {
                        return cluster._id == _id;
                    });

                    if (i >= 0) {
                        $scope.clusters.splice(i, 1);

                        $scope.selectedItem = undefined;
                        $scope.backupItem = undefined;
                    }
                })
                .error(function(errorMessage) {
                    console.log('Error: ' + errorMessage);
                });
        };

        // Save cluster in db.
        $scope.saveItem = function() {
            var item = $scope.backupItem;

            $http.post('/rest/clusters/save', item)
                .success(function() {
                    var i = _.findIndex($scope.clusters, function(cluster) {
                        return cluster._id == item._id;
                    });

                    if (i >= 0)
                        angular.extend($scope.clusters[i], item);

                    $scope.selectItem(item);
                })
                .error(function(errorMessage) {
                    console.log('Error: ' + errorMessage);
                });
        };
    }]
);