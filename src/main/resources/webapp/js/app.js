var DebugPropPrefix = "~"
var StatusPropPrefix = "."
var SensitivePropPrefix = "_"
var AdvancedPropPrefix = "+"

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
            url: '/home?device',
            templateUrl: 'partial-home.html'
        })
        
        .state('history', {
            url: '/history?device',
            templateUrl: 'partial-history.html'
        })

        .state('summary', {
            url: '/summary?device',
            templateUrl: 'partial-summary.html'
        })

});

webPortalApp.controller(
    'HomeController',
        function($scope, $http, $log, $location, $state) {

            $scope.device = $location.search().device || getCookie("device");
            $scope.logged = null;

            $scope.loginUsingSession = function() {
              $log.log('Login with session');
                $scope.session = getCookie("session");
                var requs = {
                    method: 'GET',
                    url: 'api/v1/user',
                    headers: {'Session': $scope.session}
                };
                $http(requs).success(
                    function(usname) {
                        $log.log('Found: ' + usname);
                        $scope.logged = usname;
                    }
                ).error(
                    function(data) {
                        $log.log('Not logged in: ' + data);
                    }
                );
            }

            $scope.rememberCredentials = function() {
              $log.log('Remember credentials');
              $log.log('User: ' + $scope.username);
              $log.log('Session: ' + $scope.session);
              $log.log('Device: ' + $scope.device);
                var req = {
                    method: 'POST',
                    url: 'api/v1/session',
                    headers: {'Authorization': 'Basic ' + btoa($scope.username + ":" + $scope.password)}
                };
                $http(req).success(
                    function(sessn) {
                        $log.log('Logged in correctly: ' + sessn);
                        setCookie("device", $scope.device, 100);
                        setCookie("session", sessn, 100);
                        $scope.loginUsingSession();
                    }
                ).error(
                    function(data) {
                        $log.log('Invalid session: ' + data);
                        BootstrapDialog.show({
                            title: 'Error',
                            message: 'Failed to log in: ' + data
                        });
                    }
                );
            }

            $scope.removeCredentials = function() {
              $log.log('Removed credentials');
              eraseCookie("session");
              $scope.logged = null;
            }

            $scope.goHome = function() {
              $log.log('Going home');
              $state.go('home', {device: $scope.device, session: $scope.session})
            }

            $scope.goHistory = function() {
              $log.log('Going to history');
              $state.go('history', {device: $scope.device, session: $scope.session})
            }

            $scope.goSummary = function() {
              $log.log('Going to summary');
              $state.go('summary', {device: $scope.device, session: $scope.session})
            }

            $log.log('Device: ' + $scope.device);

            $scope.loginUsingSession();

        }
);

webPortalApp.controller(
    'HistoryController',
        function($scope, $http, $log, $location) {

            $scope.session = getCookie("session");
            $scope.device = $location.search().device || getCookie("device");

            $scope.tabl = 'reports'; // table to get records from
            $scope.from = -0.5; // in days, lower-bound to filter history records
            $scope.until = 0.0; // in days, upper-bound to filter history records

            $scope.deleteDev = function() {
                $log.log('Deleting device ' + $scope.device + ' in ' + $scope.tabl);
                var req = {
                    method: 'DELETE',
                    url: 'api/v1/administrator/devices/' + $scope.device + '/' + $scope.tabl,
                    headers: {'Content-Type': 'application/json', 'Session': $scope.session},
                    data: $scope.request
                };

                $log.log('Executing request...');

                $http(req).success(
                    function(data) {
                        $log.log('Deleted: ' + JSON.stringify(data));
                        $scope.result = '[]';
                    }
                ).error(
                    function(data) {
                        $log.log('Problem requesting delete: ' + JSON.stringify(data));
                        BootstrapDialog.show({
                            title: 'Error',
                            message: 'Failed to delete: ' + data
                        });
                    }
                );

                $log.log('Executed request.');

            };


            $scope.search = function() {
                $log.log('Searching device ' + $scope.device + ' in ' + $scope.tabl);
                var date = new Date();

                var msUntil = 1000 * 3600 * 24 * $scope.until;
                var untilMs = date.getTime() + msUntil;
                var until = (untilMs / 1000) | 0; // take to seconds and cast to int

                var msFrom = 1000 * 3600 * 24 * $scope.from;
                var fromMs = date.getTime() + msFrom;
                var from = (fromMs / 1000) | 0; // take to seconds and cast to int

                var req = {
                    method: 'GET',
                    url: 'api/v1/devices/' + $scope.device + '/' + $scope.tabl + '?from=' + from + '&to=' + until,
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
        function($scope, $http, $log, $location, $window) {

            $scope.session = getCookie("session");
            $scope.device = $location.search().device || getCookie("device");

            $scope.queriedDevice = '';

            $log.log('Device: ' + $scope.device);

            $scope.propLegends = [];

            $log.log('Initialize property legends');
            $.get(window.location.pathname + 'conf/proplegends.json', function(data) {
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

            $scope.isPropNameEligible = function(name, incStatus, incDebug, incSensitive, incAdvanced) {
                var isStatus = name.startsWith(StatusPropPrefix);
                var isDebug = name.startsWith(DebugPropPrefix);
                var isSensitive = name.startsWith(SensitivePropPrefix);
                var isAdvanced = name.startsWith(AdvancedPropPrefix);
                if (isStatus) {
                    return incStatus;
                } else if (isDebug) {
                    return incDebug;
                } else if (isSensitive) {
                    return incSensitive;
                } else if (isAdvanced) {
                    return incAdvanced;
                } else {
                    return true;
                }
            }

            $scope.propExamples = function(actor, propName) {
              var l = $scope.propLegend(actor, propName);
              return l.examples;
            }

            $scope.search = function() {
                $log.log('Searching device ' + $scope.device);

                var reqReports = {
                    method: 'GET',
                    url: 'api/v1/devices/' + $scope.device + '/reports/summary?status=C',
                    headers: {'Content-Type': 'application/json', 'Session': $scope.session},
                    data: $scope.request
                };

                var reqTargets = {
                    method: 'GET',
                    url: 'api/v1/devices/' + $scope.device + '/targets/summary?status=C',
                    headers: {'Content-Type': 'application/json', 'Session': $scope.session},
                    data: $scope.request
                };

                $log.log('Executing requests...');
                $scope.queriedDevice = $scope.device + " (in progress)";

                $http(reqReports).success(
                    function(data) {
                        $log.log('Success reports: ' + JSON.stringify(data));
                        $scope.reportsSummary = data;
                        $scope.targetsSummaryUserInput = data;
                        $scope.queriedDevice = $scope.device;
                    }
                ).error(
                    function(data) {
                        $log.log('Failed reports: ' + data);
                        $scope.reportsSummary = {};
                        $scope.queriedDevice = $scope.device + " (failed: " + data + ")";
                    }
                );

                $http(reqTargets).success(
                    function(data) {
                        $log.log('Success targets: ' + JSON.stringify(data));
                        $scope.targetsSummary = data;
                        $scope.queriedDevice = $scope.device;
                    }
                ).error(
                    function(data) {
                        $log.log('Failed to retrieve targets: ' + data);
                        $scope.targetsSummary = {};
                        $scope.queriedDevice = $scope.device + " (failed: " + data + ")";
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

            if ($scope.device) { // proceed if device is provided
                $scope.search();
            }

        }
);

