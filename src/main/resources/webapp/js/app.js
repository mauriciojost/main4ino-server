var webPortalApp = angular.module('webPortalApp', ['ui.router', 'ngSanitize']);

function setCookie(name,value,days) {
    var expires = "";
    if (days) {
        var date = new Date();
        date.setTime(date.getTime() + (days*24*60*60*1000));
        expires = "; expires=" + date.toUTCString();
    }
    document.cookie = name + "=" + (value || "")  + expires + "; path=/";
}

function getCookie(name) {
    var nameEQ = name + "=";
    var ca = document.cookie.split(';');
    for(var i=0;i < ca.length;i++) {
        var c = ca[i];
        while (c.charAt(0)==' ') c = c.substring(1,c.length);
        if (c.indexOf(nameEQ) == 0) return c.substring(nameEQ.length,c.length);
    }
    return null;
}

function eraseCookie(name) {
    document.cookie = name+'=; Max-Age=-99999999;';
}

webPortalApp.config(function($stateProvider, $urlRouterProvider) {
    
    $urlRouterProvider.otherwise('/home');
    
    // Examples from https://scotch.io/tutorials/angular-routing-using-ui-router

    $stateProvider
        
        .state('home', {
            url: '/home?device&token&session',
            templateUrl: 'partial-home.html'
        })
        
        .state('history', {
            url: '/history?device&token&session',
            templateUrl: 'partial-history.html'
        })

        .state('summary', {
            url: '/summary?device&token&session',
            templateUrl: 'partial-summary.html'
        })

});

webPortalApp.controller(
    'HomeController',
        function($scope, $http, $log, $location, $state) {

            $scope.device = getCookie("device") || $location.search().device;
            $scope.token = getCookie("authcookie") || $location.search().token;
            $scope.session = getCookie("session") || $location.search().session;

            $log.log('Device: ' + $scope.device);
            $log.log('Token: ' + $scope.token);
            $log.log('Session: ' + $scope.session);

            var requs = {
                method: 'GET',
                url: 'api/v1/user',
                headers: {'Session': $scope.session}
            };

            $http(requs).success(
                function(usname) {
                    $log.log('Found: ' + usname);
                    BootstrapDialog.show({
                        title: 'Settings',
                        message: 'You are logged in ' + usname + '!'
                    });
                }
            ).error(
                function(data) {
                    BootstrapDialog.show({
                        title: 'Settings',
                        message: 'You are NOT logged in!'
                    });
                    $log.log('Not logged in: ' + data);
                }
            );

            $scope.rememberCredentials = function() {
              $log.log('Device: ' + $scope.device);
              $log.log('Token: ' + $scope.token);
              $log.log('Session: ' + $scope.session);
                var req = {
                    method: 'POST',
                    url: 'api/v1/session',
                    headers: {'Authorization': 'token ' + $scope.token},
                    data: ''
                };

                $log.log('Executing request...');

                $http(req).success(
                    function(sessn) {
                        $log.log('Found: ' + sessn);

                        setCookie("device", $scope.device, 100);
                        //setCookie("authcookie", $scope.token, 100);
                        setCookie("session", sessn, 100);
                        $scope.session = sessn;

                        BootstrapDialog.show({
                            title: 'Settings',
                            message: 'You are logged in!'
                        });

                    }
                ).error(
                    function(data) {
                        $log.log('Problem requesting session: ' + data);
                        BootstrapDialog.show({
                            title: 'Error',
                            message: 'Failed to log in: ' + data
                        });
                    }
                );

                $log.log('Executed request.');

            }

            $scope.removeCredentials = function() {
                eraseCookie("session");
                BootstrapDialog.show({
                    title: 'Log off',
                    message: 'You have been logged off'
                });
                $log.log('Executed request.');
            }

            $scope.goHome = function() {
              $log.log('Going home');
              $state.go('home', {device: $scope.device, token: $scope.token, session: $scope.session})
            }

            $scope.goHistory = function() {
              $log.log('Going to history');
              $state.go('history', {device: $scope.device, token: $scope.token, session: $scope.session})
            }

            $scope.goSummary = function() {
              $log.log('Going to summary');
              $state.go('summary', {device: $scope.device, token: $scope.token, session: $scope.session})
            }

        }
);

webPortalApp.controller(
    'HistoryController',
        function($scope, $http, $log, $location) {

            $scope.device = getCookie("device") || $location.search().device;
            //$scope.token = getCookie("authcookie") || $location.search().token;
            $scope.session = getCookie("session") || $location.search().session;

            $log.log('Device: ' + $scope.device);
            $log.log('Token: ' + $scope.token);
            $log.log('Session: ' + $scope.session);

            $scope.tabl = 'reports'; // table to get records from
            $scope.from = 2; // in days, lower-bound to filter history records

            $scope.search = function() {
                $log.log('Searching device ' + $scope.device + ' in ' + $scope.tabl + ' with token ' + $scope.token);
                var date = new Date();
                var msAgo = 1000 * 3600 * 24 * $scope.from;
                var from = date.getTime() - msAgo;

                var req = {
                    method: 'GET',
                    url: 'api/v1/devices/' + $scope.device + '/' + $scope.tabl + '?from=' + from,
                    //headers: {'Content-Type': 'application/json', 'Authorization': 'token ' + $scope.token},
                    headers: {'Content-Type': 'application/json', 'Session': $scope.session},
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
                        BootstrapDialog.show({
                            title: 'Error',
                            message: 'Failed to retrieve history: ' + data
                        });
                    }
                );

                $log.log('Executed request.');

            };

        }
);

webPortalApp.controller(
    'SummaryController',
        function($scope, $http, $log, $location) {

            $scope.device = getCookie("device") || $location.search().device;
            //$scope.token = getCookie("authcookie") || $location.search().token;
            $scope.session = getCookie("session") || $location.search().session;

            $scope.queriedDevice = '';

            $log.log('Device: ' + $scope.device);
            $log.log('Token: ' + $scope.token);
            $log.log('Session: ' + $scope.session);

            $scope.propLegends = [];

            $log.log('Initialize property legends');
            $.get('/conf/proplegends.json', function(data) {
                for(var i in data) {
                    var o = data[i];
                    var legend = {
                        patterns: o["patterns"],
                        descriptions: o["descriptions"],
                        examples: o["examples"]
                    };
                    $log.log('Element proplegend parsed: ' + JSON.stringify(legend));
                    $scope.propLegends.push(legend);
                };
            }, "json");

            $scope.propLegend = function(actor, propName) {
                var acum = {
                    descriptions: [],
                    examples: []
                };

                for (x of $scope.propLegends) {
                    for (p of x.patterns) {
                        if ((actor + '.' + propName).search(p) != -1) {
                            acum.descriptions = acum.descriptions.concat(x.descriptions);
                            acum.examples = acum.examples.concat(x.examples);
                        };
                    };
                };
                return acum;
            }

            $scope.propDescriptions = function(actor, propName) {
              var l = $scope.propLegend(actor, propName);
              return l.descriptions.join('. ').trim();
            }

            $scope.propExamples = function(actor, propName) {
              var l = $scope.propLegend(actor, propName);
              return l.examples;
            }

            $scope.search = function() {
                $log.log('Searching device ' + $scope.device + ' with token ' + $scope.token);

                var reqReports = {
                    method: 'GET',
                    url: 'api/v1/devices/' + $scope.device + '/reports/summary?consume=false',
                    //headers: {'Content-Type': 'application/json', 'Authorization': 'token ' + $scope.token},
                    headers: {'Content-Type': 'application/json', 'Session': $scope.session},
                    data: $scope.request
                };

                var reqTargets = {
                    method: 'GET',
                    url: 'api/v1/devices/' + $scope.device + '/targets/summary?status=C&consume=false',
                    //headers: {'Content-Type': 'application/json', 'Authorization': 'token ' + $scope.token},
                    headers: {'Content-Type': 'application/json', 'Session': $scope.session},
                    data: $scope.request
                };

                $log.log('Executing requests...');
                $scope.queriedDevice = $scope.device;
                $scope.reportsSummary = {};
                $scope.summarySummary = {};

                $http(reqReports).success(
                    function(data) {
                        $log.log('Success reports: ' + JSON.stringify(data));
                        $scope.reportsSummary = data;
                        $scope.targetsSummaryUserInput = data;
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
                    //headers: {'Content-Type': 'application/json', 'Authorization': 'token ' + $scope.token},
                    headers: {'Content-Type': 'application/json', 'Session': $scope.session},
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

