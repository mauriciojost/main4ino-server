var webPortalApp = angular.module('webPortalApp', ['ui.router']);

webPortalApp.config(function($stateProvider, $urlRouterProvider) {
    
    $urlRouterProvider.otherwise('/home');
    
    // Examples from https://scotch.io/tutorials/angular-routing-using-ui-router

    $stateProvider
        
        .state('home', {
            url: '/home?device&token',
            templateUrl: 'partial-home.html'
        })
        
        .state('history', {
            url: '/history?device&token',
            templateUrl: 'partial-history.html'
        })

        .state('summary', {
            url: '/summary?device&token',
            templateUrl: 'partial-summary.html'
        })

});

webPortalApp.controller(
    'HomeController',
        function($scope, $http, $log, $location, $state) {

            $scope.device = $location.search().device;
            $scope.token = $location.search().token;

            $log.log('Home Device: ' + $scope.device);
            $log.log('Home Token: ' + $scope.token);

            $scope.goHome = function() {
              $log.log('Going home');
              $state.go('home', {device: $scope.device, token: $scope.token})
            }

            $scope.goHistory = function() {
              $log.log('Going to history');
              $state.go('history', {device: $scope.device, token: $scope.token})
            }

            $scope.goSummary = function() {
              $log.log('Going to summary');
              $state.go('summary', {device: $scope.device, token: $scope.token})
            }

        }
);

webPortalApp.controller(
    'HistoryController',
        function($scope, $http, $log, $location) {

            $scope.device = $location.search().device;
            $scope.token = $location.search().token;
            $scope.tabl = 'reports'; // table to get records from
            $scope.from = 24; // in hours, lower-bound to filter history records

            $log.log('Device: ' + $scope.device);
            $log.log('Token: ' + $scope.token);

            $scope.search = function() {
                $log.log('Searching device ' + $scope.device + ' in ' + $scope.tabl + ' with token ' + $scope.token);
                var date = new Date();
                var msAgo = 1000 * 3600 * $scope.from;
                var from = date.getTime() - msAgo;

                var req = {
                    method: 'GET',
                    url: 'api/v1/devices/' + $scope.device + '/' + $scope.tabl + '?from=' + from,
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
    'SummaryController',
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
                        $scope.reportsSummary = {};
                        BootstrapDialog.show({
                            title: 'Error',
                            message: 'Failed to retrieve reports needed to perform a query: ' + data
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
                        $log.log('Failed to retrieve targets: ' + data);
                        $scope.targetsSummary = {};
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

