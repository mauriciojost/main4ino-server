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
        function($scope, $http, $log, $location) {

            $scope.device = $location.search().device;
            $scope.token = $location.search().token;
            $log.log('Device: ' + $scope.device);
            $log.log('Token: ' + $scope.token);

            $scope.search = function() {
                $log.log('Searching device ' + $scope.device + ' with token ' + $scope.token);
                var date = new Date();
                var msAgo = 1000 * 3600 * 24; // 1 day
                var from = date.getTime() - msAgo;

                var req = {
                    method: 'GET',
                    url: 'api/v1/devices/' + $scope.device + '/' + $scope.table + '?from=' + from,
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
        function($scope, $http, $log, $location) {

            $scope.device = $location.search().device;
            $scope.token = $location.search().token;
            $log.log('Device: ' + $scope.device);
            $log.log('Token: ' + $scope.token);

            $scope.propLegends = [];

            // Initialize property legends
            $.get('../conf/proplegends.conf', function(data) {
                var lines = data.split('\n');
                $.each(lines, function(nline, line) {
                    var items = line.split('===');
                    if (items[0].length != 0) {
                        $log.log('Matched line: ' + line);
                        var exs = items[2].split(',').map(function (i) {return i.trim();});
                        var legend = {
                            lines: [line],
                            patterns: items[0].split(','),
                            descriptions: [items[1]],
                            examples: exs
                        }
                        $scope.propLegends.push(legend);
                    };
                });

                $log.log('Legends: ' + JSON.stringify($scope.propLegends));
            });

            $scope.propLegend = function(actor, propName) {
                $log.log('Searching legend for: ' + actor + ' ' + propName);

                var acum = {
                    descriptions: [],
                    examples: []
                };

                for (x of $scope.propLegends) {
                    for (p of x.patterns) {
                        if ((actor + '.' + propName).search(p) != -1) {
                            $log.log('Matched pattern: ' + p + ' with ' + JSON.stringify(x));
                            acum.descriptions = acum.descriptions.concat(x.descriptions);
                            acum.examples = acum.examples.concat(x.examples);
                        };
                    };
                };
                $log.log('Resolved: ' + JSON.stringify(acum));
                return acum;
            }

            $scope.propHelp = function(actor, propName) {
              $log.log('Searching description for: ' + actor + ' ' + propName);
              var l = $scope.propLegend(actor, propName);
              return l.descriptions.join('. ').trim();
            }

            $scope.propExamples = function(actor, propName) {
              $log.log('Searching examples for: ' + actor + ' ' + propName);
              var l = $scope.propLegend(actor, propName);
              return l.examples;
            }

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

            $scope.changeRequest = function(device, actor, propName, propValue) {
                $log.log('Request to change ' + device + ' ' + actor + ' ' + propName + ' ' + propValue);

                BootstrapDialog.show({
                    title: 'Change ' + actor + '.' + propName,
                    message: 'Change property value to: <input type="text" class="form-control" placeholder="new value" value="' + propValue + '">',
                    buttons: [{
                        label: 'Change',
                        action: function(dialog) {
                           var v = dialog.getModalBody().find('input').val();
                           $scope.change(device, actor, propName, v);
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

            $scope.valueFromHelp = function(d) {
                return d.split(' -> ')[0].trim();
            }

            $scope.change = function(device, actor, propName, propValue) {
                $log.log('Changing ' + device + ' ' + actor + ' ' + propName + ' ' + propValue);

                var jsn = {};
                jsn[propName] = propValue;
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

        }
);

