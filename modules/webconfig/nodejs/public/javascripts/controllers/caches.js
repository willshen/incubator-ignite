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

configuratorModule.controller('cachesController', ['$scope', '$http', function($scope, $http) {
        $scope.templates = [
            {value: {}, label: 'None'},
            {value: {mode: 'PART', atomicity: 'ATOMIC'}, label: 'Partitioned'},
            {value: {mode: 'REPL', atomicity: 'ATOMIC'}, label: 'Replicated'},
            {value: {mode: 'LOCAL', atomicity: 'ATOMIC'}, label: 'Local'}
        ];

        $scope.atomicities = [
            {value: 'ATOMIC', label: 'Atomic'},
            {value: 'TRANSACTIONAL', label: 'Transactional'}
        ];

        $scope.modes = [
            {value: 'PART', label: 'Partitioned'},
            {value: 'REPL', label: 'Replicated'},
            {value: 'LOCAL', label: 'Local'}
        ];

        $scope.atomicWriteOrderModes = [
            {value: 'CLOCK', label: 'Clock'},
            {value: 'PRIMARY', label: 'Primary'}
        ];

        $scope.rebalanceModes = [
            {value: 'SYNC', label: 'Synchronous'},
            {value: 'ASYNC', label: 'Asynchronous'},
            {value: 'NONE', label: 'None'}
        ];

        $scope.memoryModes = [
            {value: 'ONHT', label: 'Onheap tiered'},
            {value: 'OFHT', label: 'Offheap tiered'},
            {value: 'OFHV', label: 'Offheap values'}
            ];

        //DefaultLockTimeout dfltLockTimeout
        //invalidate
        //TransactionManagerLookupClassName tmLookupClsName
        // swapEnabled
        // maxConcurrentAsyncOps
        // writeBehindEnabled
        // writeBehindFlushSize
        // writeBehindFlushFreq
        // writeBehindFlushThreadCnt
        // writeBehindBatchSize
        // offHeapMaxMem

        $scope.caches = [];

        // When landing on the page, get caches and show them.
        $http.get('/rest/caches')
            .success(function(data) {
                $scope.spaces = data.spaces;
                $scope.caches = data.caches;
            });

        $scope.selectItem = function(item) {
            $scope.selectedItem = item;

            $scope.backupItem = angular.copy(item);
        };

        // Add new cache.
        $scope.createItem = function() {
            var item = angular.copy($scope.create.template);

            item.name = 'Cache ' + ($scope.caches.length + 1);
            item.space = $scope.spaces[0]._id;


            $http.post('/rest/caches/save', item)
                .success(function(_id) {
                    item._id = _id;

                    $scope.caches.push(item);

                    $scope.selectItem(item);
                })
                .error(function(errorMessage) {
                    console.log('Error: ' + errorMessage);
                });
        };

        $scope.removeItem = function(item) {
            $http.post('/rest/caches/remove', {_id: item._id})
                .success(function() {
                    var index = $scope.caches.indexOf(item);

                    if (index !== -1) {
                        $scope.caches.splice(index, 1);

                        if ($scope.selectedItem == item) {
                            $scope.selectedItem = undefined;

                            $scope.backupItem = undefined;
                        }
                    }
                })
                .error(function(errorMessage) {
                    console.log('Error: ' + errorMessage);
                });
        };

        // Save cache in db.
        $scope.saveItem = function(item) {
            $http.post('/rest/caches/save', item)
                .success(function() {
                    var cache = $scope.caches.find(function(cache) {
                        return cache._id == item._id;
                    });

                    if (cache)
                        angular.extend(cache, item);
                })
                .error(function(errorMessage) {
                    console.log('Error: ' + errorMessage);
                });
        };
    }]
);