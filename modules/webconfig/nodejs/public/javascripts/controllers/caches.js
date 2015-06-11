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

configuratorModule.controller('cachesController', ['$scope', '$modal', '$http', function ($scope, $modal, $http) {
        $scope.templates = [
            {value: {mode: 'PARTITIONED', atomicityMode: 'ATOMIC'}, label: 'Partitioned'},
            {value: {mode: 'REPLICATED', atomicityMode: 'ATOMIC'}, label: 'Replicated'},
            {value: {mode: 'LOCAL', atomicityMode: 'ATOMIC'}, label: 'Local'}
        ];

        $scope.atomicities = [
            {value: 'ATOMIC', label: 'Atomic'},
            {value: 'TRANSACTIONAL', label: 'Transactional'}
        ];

        $scope.modes = [
            {value: 'PARTITIONED', label: 'Partitioned'},
            {value: 'REPLICATED', label: 'Replicated'},
            {value: 'LOCAL', label: 'Local'}
        ];

        $scope.atomicWriteOrderModes = [
            {value: 'CLOCK', label: 'Clock'},
            {value: 'PRIMARY', label: 'Primary'}
        ];

        $scope.memoryModes = [
            {value: 'ONHEAP_TIERED', label: 'ONHEAP_TIERED'},
            {value: 'OFFHEAP_TIERED', label: 'OFFHEAP_TIERED'},
            {value: 'OFFHEAP_VALUES', label: 'OFFHEAP_VALUES'}
        ];

        $scope.evictionPolicies = [
            {value: 'LRU', label: 'Least Recently Used'},
            {value: 'RND', label: 'Random'},
            {value: 'FIFO', label: 'FIFO'},
            {value: 'SORTED', label: 'Sorted'}
        ];

        $scope.rebalanceModes = [
            {value: 'SYNC', label: 'Synchronous'},
            {value: 'ASYNC', label: 'Asynchronous'},
            {value: 'NONE', label: 'None'}
        ];

        $scope.general = [];
        $scope.advanced = [];

        $http.get('/form-models/caches.json')
            .success(function (data) {
                $scope.general = data.general;
                $scope.advanced = data.advanced;
            });

        $scope.editIndexedTypes = function (idx) {
            $scope.indexedTypeIdx = idx;

            if (idx < 0) {
                $scope.currKeyCls = '';
                $scope.currValCls = '';
            }
            else {
                var idxType = $scope.backupItem.indexedTypes[idx];

                $scope.currKeyCls = idxType.keyClass;
                $scope.currValCls = idxType.valueClass;
            }

            $scope.indexedTypesModal = $modal({scope: $scope, template: '/indexedTypes', show: true});
        };

        $scope.saveIndexedType = function (k, v) {
            var idxTypes = $scope.backupItem.indexedTypes;

            var idx = $scope.indexedTypeIdx;

            if (idx < 0) {
                var newItem = {keyClass: k, valueClass: v};

                if (undefined == idxTypes)
                    $scope.backupItem.indexedTypes = [newItem];
                else
                    idxTypes.push(newItem);
            }
            else {
                var idxType = idxTypes[idx];

                idxType.keyClass = k;
                idxType.valueClass = v;
            }

            $scope.indexedTypesModal.hide();
        };

        $scope.removeIndexedType = function (idx) {
            $scope.backupItem.indexedTypes.splice(idx, 1);
        };

        $scope.caches = [];

        // When landing on the page, get caches and show them.
        $http.get('/rest/caches')
            .success(function (data) {
                $scope.spaces = data.spaces;
                $scope.caches = data.caches;
            });

        $scope.selectItem = function (item) {
            $scope.selectedItem = item;

            $scope.backupItem = angular.copy(item);
        };

        // Add new cache.
        $scope.createItem = function () {
            var item = angular.copy($scope.create.template);

            item.name = 'Cache ' + ($scope.caches.length + 1);
            item.space = $scope.spaces[0]._id;

            $http.post('/rest/caches/save', item)
                .success(function (_id) {
                    item._id = _id;

                    $scope.caches.push(item);

                    $scope.selectItem(item);
                })
                .error(function (errorMessage) {
                    console.log('Error: ' + errorMessage);
                });
        };

        $scope.removeItem = function () {
            var _id = $scope.selectedItem._id;

            $http.post('/rest/caches/remove', {_id: _id})
                .success(function () {
                    var i = _.findIndex($scope.caches, function (cache) {
                        return cache._id == _id;
                    });

                    if (i >= 0) {
                        $scope.caches.splice(i, 1);

                        $scope.selectedItem = undefined;
                        $scope.backupItem = undefined;
                    }
                })
                .error(function (errorMessage) {
                    console.log('Error: ' + errorMessage);
                });
        };

        // Save cache in db.
        $scope.saveItem = function () {
            var item = $scope.backupItem;

            $http.post('/rest/caches/save', item)
                .success(function () {
                    var i = _.findIndex($scope.caches, function (cache) {
                        return cache._id == item._id;
                    });

                    if (i >= 0)
                        angular.extend($scope.caches[i], item);

                    $scope.selectItem(item);
                })
                .error(function (errorMessage) {
                    console.log('Error: ' + errorMessage);
                });
        };

        $scope.createSimpleItem = function(desc, rows) {
            $scope.simplePopup = {
                rows: rows,
                desc: desc
            };

            $scope.pupup = $modal({scope: $scope, template: '/simplePopup', show: true});
        };

        $scope.saveSimpleItem = function(row) {
            var popup = $scope.simplePopup;
            var rows = popup.rows;

            if (popup.index)
                angular.extend(rows[popup.index], row);
            else if (undefined == rows)
                popup.rows = [row];
            else
                popup.rows.push(row);

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
    }]
);