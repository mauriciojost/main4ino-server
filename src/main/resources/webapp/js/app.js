var webPortalApp = angular.module('webPortalApp', ['ui.router']);

webPortalApp.config(function($stateProvider, $urlRouterProvider) {
    
    $urlRouterProvider.otherwise('/home');
    
    // Examples from https://scotch.io/tutorials/angular-routing-using-ui-router

    $stateProvider
        
        .state('home', {
            url: '/home',
            templateUrl: 'partial-home.html'
        })
        
        .state('insert', {
            url: '/insert',
            templateUrl: 'partial-insert.html'
        })
        
        .state('search', {
            url: '/search',
            templateUrl: 'partial-search.html'
        })

        .state('device-history', {
            url: '/device-history',
            templateUrl: 'partial-device-history.html'
        })

        .state('device-summary', {
            url: '/device-summary',
            templateUrl: 'partial-device-summary.html'
        })

        .state('about', {
            url: '/about',
            views: {
                    '': { templateUrl: 'partial-about.html' },
                    'columnProperties@about': { 
                                templateUrl: 'partial-about-table.html',
                                controller: 'AboutController'
                            }
                    }
            
        });
        
});

webPortalApp.controller(
    'AboutController', 
    function($scope, $log, $http) {
    

        $log.log('Creating version request...'); 

        var req = {
            method: 'GET',
            url: '/api/v1/app/properties/'
        };

        $log.log('Executing version request...');

        $http(req).success(
            function(data) {
                $scope.properties = data;
            }
        ).error(
            function(data) {
                $scope.properties = 'unknown';
            }
        );

        $log.log('Executed version request.');
    
    }
);

webPortalApp.controller(
    'DeviceHistoryController',
        function($scope, $http, $log) {

            $scope.search = function() {
                $log.log('Searching device ' + $scope.device + ' with token ' + $scope.token);

                var req = {
                    method: 'GET',
                    url: 'api/v1/devices/' + $scope.device + '/targets',
                    headers: {'Content-Type': 'application/json', 'Authorization': 'token ' + $scope.token},
                    data: $scope.request
                };

                $log.log('Executing request...');

                $http(req).success(
                    function(data) {
                        $log.log('Found: ' + JSON.stringify(data));
                        $scope.result = data;
                    }
                ).error(
                    function(data) {
                        $log.log('Problem requesting: ' + JSON.stringify(data));
                        $scope.result = '[]'
                    }
                );

                $log.log('Executed request.');

            };

        }
);

webPortalApp.controller(
    'DeviceSummaryController',
        function($scope, $http, $log) {

            $scope.search = function() {
                $log.log('Searching device ' + $scope.device + ' with token ' + $scope.token);

                var reqReports = {
                    method: 'GET',
                    url: 'api/v1/devices/' + $scope.device + '/reports/summary?consume=false',
                    headers: {'Content-Type': 'application/json', 'Authorization': 'token ' + $scope.token},
                    data: $scope.request
                };

                var reqTargets = {
                    method: 'GET',
                    url: 'api/v1/devices/' + $scope.device + '/targets/summary?status=C&consume=false',
                    headers: {'Content-Type': 'application/json', 'Authorization': 'token ' + $scope.token},
                    data: $scope.request
                };

                $log.log('Executing requests...');

                $http(reqReports).success(
                    function(data) {
                        $log.log('Success reports: ' + JSON.stringify(data));
                        $scope.reportsSummary = data;
                    }
                ).error(
                    function(data) {
                        $log.log('Failed reports: ' + data);
                        $scope.reportsSummary = '{}';
                        BootstrapDialog.show({
                            title: 'Error',
                            message: 'Failed to retrieve reports: ' + data
                        });
                    }
                );

                $http(reqTargets).success(
                    function(data) {
                        $log.log('Success targets: ' + JSON.stringify(data));
                        $scope.targetsSummary = data;
                    }
                ).error(
                    function(data) {
                        $log.log('Failed targets: ' + data);
                        $scope.targetsSummary = '{}';
                        BootstrapDialog.show({
                            title: 'Error',
                            message: 'Failed to retrieve targets: ' + data
                        });
                    }
                );

                $log.log('Executed requests.');

            };

            $scope.changeRequest = function(device, actor, pname, pvalue) {
                $log.log('Request to change ' + device + ' ' + actor + ' ' + pname + ' ' + pvalue);

                BootstrapDialog.show({
                    title: 'Change ' + actor + '.' + pname,
                    message: 'Change property value to: <input type="text" class="form-control" placeholder="new value" value="' + pvalue + '">',
                    buttons: [{
                        label: 'Change',
                        action: function(dialog) {
                           var v = dialog.getModalBody().find('input').val();
                           $scope.change(device, actor, pname, v);
                           $log.log('Changed to: ' + v);
                           dialog.close();
                        }
                    }, {
                        label: 'Cancel',
                        action: function(dialog) {
                           $log.log('Cancelled');
                           dialog.close();
                        }
                    }]
                });

            }

            $scope.change = function(device, actor, pname, pvalue) {
                $log.log('Changing ' + device + ' ' + actor + ' ' + pname + ' ' + pvalue);

                var jsn = {};
                jsn[pname] = pvalue;
                var req = {
                    method: 'POST',
                    url: '/api/v1/devices/' + device + '/actors/' + actor + '/targets',
                    headers: {'Content-Type': 'application/json', 'Authorization': 'token ' + $scope.token},
                    data: JSON.stringify(jsn)
                };

                $http(req).success(
                    function(data) {
                        $log.log('Success change');
                        $scope.search();
                    }
                ).error(
                    function(data) {
                        $log.log('Failed change');
                    }
                );

            }

            $scope.helpRequest = function(actor, pname) {
                $log.log('Request for help ' + actor + ' ' + pname);
                $.get('../help.conf', function(data) {
                    var lines = data.split('\n');
                    $.each(lines, function(lineNo, line) {
                        var items = line.split('===');
                        if ((items[0].length != 0) && ((actor + '.' + pname).search(items[0]) != -1)) {
                            $log.log('Matched line: ' + line);
                            BootstrapDialog.show({
                                title: 'Help for actor "' + actor + '" property "' + pname + '"',
                                message: items[1]
                            });
                        };
                    });
                });
            }
        }
);

