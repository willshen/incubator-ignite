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

var configuratorModule =  angular.module('ignite-web-configurator', ['smart-table', 'mgcrea.ngStrap', 'ngSanitize']);

configuratorModule.config(function($tooltipProvider) {
    angular.extend($tooltipProvider.defaults, {
        placement: 'right',
        html: 'true',
        trigger: 'click hover'
    });
});

configuratorModule.config(function($selectProvider) {
    angular.extend($selectProvider.defaults, {
        allText: 'Select All',
        noneText: 'Clear All'
    });
});

// Decode name using map(value, label).
configuratorModule.filter('displayValue', function () {
    return function (v, m, dflt) {
        var i = _.findIndex(m, function(item) {
            return item.value == v;
        });

        if (i >= 0)
            return m[i].label;

        if (dflt)
            return dflt;

        return 'Unknown value';
    }
});

configuratorModule.controller('activeLink', ['$scope', function($scope) {
    $scope.isActive = function(path) {
        return window.location.pathname.substr(0, path.length) == path;
    };
}]);

configuratorModule.controller('auth', ['$scope', '$modal', '$http', '$window', function($scope, $modal, $http, $window) {
    $scope.action = 'login';

    $scope.errorMessage = '';

    $scope.valid = false;

    // Pre-fetch an external template populated with a custom scope
    var authModal = $modal({scope: $scope, template: '/login', show: false});

    $scope.login = function() {
        // Show when some event occurs (use $promise property to ensure the template has been loaded)
        authModal.$promise.then(authModal.show);
    };

    $scope.auth = function(action, user_info) {
        $http.post('/rest/auth/' + action, user_info)
            .success(function(data) {
                authModal.hide();

                $window.location = '/clusters';
            })
            .error(function (data) {
                $scope.errorMessage = data;
            });
    };
}]);