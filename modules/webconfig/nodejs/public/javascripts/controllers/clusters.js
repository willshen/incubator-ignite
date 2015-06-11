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
            {value: {}, label: 'none'},
            {value: {discovery: {kind: 'Vm', Vm: {addresses: ['127.0.0.1:47500..47510']}}}, label: 'local'},
            {value: {discovery: {kind: 'Multicast', Multicast: {}}}, label: 'basic'}
        ];

        $scope.discoveries = [
            {value: 'Vm', label: 'static IPs'},
            {value: 'Multicast', label: 'multicast'},
            {value: 'S3', label: 'AWS S3'},
            {value: 'Cloud', label: 'apache jclouds'},
            {value: 'GoogleStorage', label: 'google cloud storage'},
            {value: 'Jdbc', label: 'JDBC'},
            {value: 'SharedFs', label: 'shared filesystem'}
        ];

        $scope.events = [
            {value: 'EVTS_CHECKPOINT', label: 'evts_checkpoint'},
            {value: 'EVTS_DEPLOYMENT', label: 'evts_deployment'},
            {value: 'EVTS_ERROR', label: 'evts_error'},
            {value: 'EVTS_DISCOVERY', label: 'evts_discovery'},
            {value: 'EVTS_JOB_EXECUTION', label: 'evts_job_execution'},
            {value: 'EVTS_TASK_EXECUTION', label: 'evts_task_execution'},
            {value: 'EVTS_CACHE', label: 'evts_cache'},
            {value: 'EVTS_CACHE_REBALANCE', label: 'evts_cache_rebalance'},
            {value: 'EVTS_CACHE_LIFECYCLE', label: 'evts_cache_lifecycle'},
            {value: 'EVTS_CACHE_QUERY', label: 'evts_cache_query'},
            {value: 'EVTS_SWAPSPACE', label: 'evts_swapspace'},
            {value: 'EVTS_IGFS', label: 'evts_igfs'}
        ];

        $scope.cacheModes = [
            {value: 'LOCAL', label: 'local'},
            {value: 'REPLICATED', label: 'replicated'},
            {value: 'PARTITIONED', label: 'partitioned'}
        ];

        $scope.deploymentModes = [
            {value: 'PRIVATE', label: 'private'},
            {value: 'ISOLATED', label: 'isolated'},
            {value: 'SHARED', label: 'shared'},
            {value: 'CONTINUOUS', label: 'continuous'}
        ];

        $scope.transactionConcurrency = [
            {value: 'OPTIMISTIC', label: 'optimistic'},
            {value: 'PESSIMISTIC', label: 'pessimistic'}
        ];

        $scope.transactionIsolation = [
            {value: 'READ_COMMITTED', label: 'read_committed'},
            {value: 'REPEATABLE_READ', label: 'repeatable_read'},
            {value: 'SERIALIZABLE', label: 'serializable'}
        ];

        $scope.segmentationPolicy = [
            {value: 'RESTART_JVM', label: 'restart_jvm'},
            {value: 'STOP', label: 'stop'},
            {value: 'NOOP', label: 'noop'}
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
                index: idx,
                row: angular.copy(rows[idx])
            };

            $modal({scope: $scope, template: '/simplePopup', show: true});
        };

        $scope.removeSimpleItem = function(rows, idx) {
            rows.splice(idx, 1);
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
            $scope.backupItem = angular.copy($scope.create.template);

            $scope.backupItem.space = $scope.spaces[0]._id;
        };

        // Save cluster in db.
        $scope.saveItem = function() {
            var item = $scope.backupItem;

            $http.post('/rest/clusters/save', item)
                .success(function(_id) {
                    var i = _.findIndex($scope.clusters, function(cluster) {
                        return cluster._id == _id;
                    });

                    if (i >= 0)
                        angular.extend($scope.clusters[i], item);
                    else {
                        item._id = _id;

                        $scope.clusters.push(item);
                    }

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
    }]
);