var DebugPropPrefix = "~";
var StatusPropPrefix = ".";
var SensitivePropPrefix = "_";
var AdvancedPropPrefix = "+";

var webPortalApp = angular.module("webPortalApp", ["ui.router", "ngRoute", "ngSanitize"]);

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
    var ca = document.cookie.split(";");
    for(var i=0;i < ca.length;i++) {
        var c = ca[i];
        while (c.charAt(0) == " ") c = c.substring(1,c.length);
        if (c.indexOf(nameEQ) === 0) return c.substring(nameEQ.length,c.length);
    }
    return null;
}

function eraseCookie(name) {
    document.cookie = name+"=; Max-Age=-99999999;";
}
webPortalApp.config(function($stateProvider, $urlRouterProvider) {
    
    $urlRouterProvider.otherwise("/login");
    
    // Examples from https://scotch.io/tutorials/angular-routing-using-ui-router

    $stateProvider
        
        .state("login", {
            url: "/login?device",
            templateUrl: "partial-login.html",
            params: {
                device: {
                    dynamic: true
                }
            }
        })
        
        .state("history", {
            url: "/history?device",
            templateUrl: "partial-history.html",
            params: {
                device: {
                    dynamic: true
                }
            }
        })

        .state("control", {
            url: "/control?device",
            templateUrl: "partial-control.html",
            params: {
                device: {
                    dynamic: true
                }
            }
        })

        .state("log", {
            url: "/log?device",
            templateUrl: "partial-log.html",
            params: {
                device: {
                    dynamic: true
                }
            }
        })

        .state("administrate", {
            url: "/administrate?device",
            templateUrl: "partial-administrate.html",
            params: {
                device: {
                    //value: 'dev1',
                    dynamic: true
                }
            }
        })


});

webPortalApp.controller(
    "LoginController",
        function($scope, $http, $log, $stateParams, $state, $rootScope) {

            $rootScope.logged = null;
            $rootScope.devices = [];
            $scope.device = $stateParams.device;

            $scope.loginUsingSession = function() {
              $log.log("Login with session");
                $scope.session = getCookie("session");
                var requs = {
                    method: "GET",
                    url: "api/v1/user",
                    headers: {"Session": $scope.session}
                };
                $http(requs).then(
                    function(r) {
                        $log.log("Logged in: " + r.data);
                        var devicesCookie = getCookie("devices");
                        if (devicesCookie) {
                            $rootScope.devices = devicesCookie.split(",");
                        }
                        $log.log("Devices: " + $rootScope.devices + " (total: " + $rootScope.devices.length + ")");
                        $rootScope.logged = r.data;
                    },
                    function(r) {
                        $log.log("Not logged in: " + r.data);
                    }
                );
                var requsVer = {
                    method: "GET",
                    url: "api/v1/version",
                    headers: {"Session": $scope.session}
                };
                $http(requsVer).then(
                    function(r) {
                        $log.log("Version: " + JSON.stringify(r.data));
                    },
                    function(r) {
                        $log.log("Could not figure out version: " + r.data);
                    }
                );
                $state.go("login", {session: $scope.session, device: $scope.device})
            }

            $scope.rememberCredentials = function() {
              $log.log("Remember credentials");
              $log.log("User: " + $scope.username);
              $log.log("Session: " + $scope.session);
              $log.log("Devices: " + $scope.devicesStr);
                var req = {
                    method: "POST",
                    url: "api/v1/session",
                    headers: {"Authorization": "Basic " + btoa($scope.username + ":" + $scope.password)}
                };
                $http(req).then(
                    function(r) {
                        $log.log("Logged in correctly: " + r.data);
                        setCookie("session", r.data, 100);
                        setCookie("devices", $scope.devicesStr, 100);
                        $scope.loginUsingSession();
                    },
                    function(r) {
                        $log.log("Invalid session: " + r.data);
                        BootstrapDialog.show({
                            title: "Error",
                            message: "Failed to log in: " + r.data
                        });
                    }
                );
            }

            $scope.removeCredentials = function() {
              $log.log("Removed credentials");
              eraseCookie("session");
              eraseCookie("devices");
              $rootScope.logged = null;
            }

            $scope.goLogin = function() {
              $log.log("Going to login");
              $state.go("login", {session: $scope.session, device: $stateParams.device});
            }

            $scope.goHistory = function() {
              $log.log("Going to history");
              $state.go("history", {session: $scope.session, device: $stateParams.device});
            }

            $scope.goControl = function() {
              $log.log("Going to control");
              $state.go("control", {session: $scope.session, device: $stateParams.device});
            }

            $scope.goLog = function() {
                $log.log("Going to log");
                $state.go("log", {session: $scope.session, device: $stateParams.device});
            }

            $scope.goAdministrate = function() {
                $log.log("Going to administrate");
                $state.go("administrate", {session: $scope.session, device: $stateParams.device});
            }

            $scope.switchTo = function(dev) {
                $log.log("Switching to: " + dev + " " + $state.$current);
                $state.go($state.$current, {session: $scope.session, device: dev }, {reload:true});
            }

            $scope.loginUsingSession();

        }
);

webPortalApp.controller(
    "HistoryController",
        function($scope, $http, $log, $stateParams, $rootScope) {

            $scope.session = getCookie("session");

            $scope.queriedDevice = "...";

            $scope.tabl = "reports"; // table to get records from
            $scope.from = -0.1; // in days, lower-bound to filter history records
            $scope.to = 0.0; // in days, upper-bound to filter history records

            $scope.search = function() {

                $scope.queriedDevice = $stateParams.device + " (in progress)";

                $log.log("Searching device " + $stateParams.device + " in " + $scope.tabl);
                var date = new Date();

                var msTo = 1000 * 3600 * 24 * $scope.to;
                var toMs = date.getTime() + msTo;
                var toSec = (toMs / 1000) | 0; // take to seconds and cast to int

                var msFrom = 1000 * 3600 * 24 * $scope.from;
                var fromMs = date.getTime() + msFrom;
                var fromSec = (fromMs / 1000) | 0; // take to seconds and cast to int

                $log.log('Query from ' + new Date(fromMs).toString() + ' to ' + new Date(toMs).toString());

                var req = {
                    method: "GET",
                    url: "api/v1/devices/" + $stateParams.device + "/" + $scope.tabl + "?from=" + fromSec + "&to=" + toSec,
                    headers: {"Content-Type": "application/json", "Session": $scope.session},
                    data: $scope.request
                };

                $log.log("Executing request...");

                $http(req).then(
                    function(r) {
                        $log.log("Found: " + JSON.stringify(r.data));
                        $scope.queriedDevice = $stateParams.device;
                        $scope.result = r.data;
                    },
                    function(r) {
                        $log.log("Problem requesting: " + JSON.stringify(r.data));
                        $scope.queriedDevice = "Failed query for " + $stateParams.device;
                        $scope.result = "[]"
                        BootstrapDialog.show({
                            title: "Error",
                            message: "Failed to retrieve history: " + r.data
                        });
                    }
                );

                $log.log("Executed request.");

            };

            if ($stateParams.device) { // proceed if device is provided
                $scope.search();
            }

        }
);

webPortalApp.controller(
    "ControlController",
        function($scope, $http, $log, $stateParams, $rootScope) {

            $scope.session = getCookie("session");

            $scope.queriedDevice = "...";

            $log.log("Device: " + $stateParams.device);

            $scope.readIncludes = function() {
                $scope.includeStatus = (getCookie("includeStatus") == 'true');
                $scope.includeDebug =  (getCookie("includeDebug") == 'true');
                $scope.includeSensitive =  (getCookie("includeSensitive") == 'true');
                $scope.includeAdvanced =  (getCookie("includeAdvanced") == 'true');
            }

            $scope.setIncludes = function() {
                setCookie("includeStatus", $scope.includeStatus?'true':'false', 100);
                setCookie("includeDebug", $scope.includeDebug?'true':'false', 100);
                setCookie("includeSensitive", $scope.includeSensitive?'true':'false', 100);
                setCookie("includeAdvanced", $scope.includeAdvanced?'true':'false', 100);
            }


            $scope.readIncludes();

            $scope.isPropNameEligible = function(name, incStatus, incDebug, incSensitive, incAdvanced) {
                if (name.startsWith(StatusPropPrefix)) {
                    return incStatus;
                } else if (name.startsWith(DebugPropPrefix)) {
                    return incDebug;
                } else if (name.startsWith(SensitivePropPrefix)) {
                    return incSensitive;
                } else if (name.startsWith(AdvancedPropPrefix)) {
                    return incAdvanced;
                } else {
                    return true;
                }
            }

            $scope.initLegends = function() {
                $scope.propLegends = [];

                var reqDescs = {
                    method: "GET",
                    url: "api/v1/devices/" + $stateParams.device + "/descriptions",
                    headers: {"Content-Type": "application/json", "Session": $scope.session}
                };

                $http(reqDescs).then(
                    function(r) {

                        $log.log("Data raw json: " + JSON.stringify(r.data));
                        var dt = r.data["versionJson"]["json"];
                        $log.log("Json parsed: " + JSON.stringify(dt));

                        $log.log("Initialize property legends");
                        for(var i in dt) {
                            var o = dt[i];
                            var legend = {
                                patterns: o["patterns"],
                                descriptions: o["descriptions"],
                                examples: o["examples"]
                            };
                            $log.log("Element proplegend parsed: " + JSON.stringify(legend));
                            $scope.propLegends.push(legend);
                        };

                        $scope.propLegend = function(actor, propName) {
                            var acum = {
                                descriptions: [],
                                examples: []
                            };

                            for (x of $scope.propLegends) {
                                for (p of x.patterns) {
                                    if ((actor + "." + propName).search(p) != -1) {
                                        acum.descriptions = acum.descriptions.concat(x.descriptions);
                                        acum.examples = acum.examples.concat(x.examples);
                                    };
                                };
                            };
                            return acum;
                        }

                        $scope.propDescriptions = function(actor, propName) {
                          var l = $scope.propLegend(actor, propName);
                          return l.descriptions.join(". ").trim();
                        }

                        $scope.propExamples = function(actor, propName) {
                          var l = $scope.propLegend(actor, propName);
                          return l.examples;
                        }


                    },
                    function(r) {
                        $log.log("Failed descritpion: " + r.data);
                        $scope.propDescriptions = function(actor, propName) {
                          return "?";
                        }

                        $scope.propExamples = function(actor, propName) {
                          return "?";
                        }

                    }
                );

            }

            $scope.search = function() {

                $log.log("Searching device " + $stateParams.device);

                var reqReports = {
                    method: "GET",
                    url: "api/v1/devices/" + $stateParams.device + "/reports/summary?status=C",
                    headers: {"Content-Type": "application/json", "Session": $scope.session},
                    data: $scope.request
                };

                var reqTargets = {
                    method: "GET",
                    url: "api/v1/devices/" + $stateParams.device + "/targets/summary?status=C",
                    headers: {"Content-Type": "application/json", "Session": $scope.session},
                    data: $scope.request
                };

                $log.log("Executing requests...");
                $scope.queriedDevice = $stateParams.device + " (in progress)";

                $http(reqReports).then(
                    function(r) {
                        $log.log("Success reports: " + JSON.stringify(r.data));
                        $scope.reportsSummary = r.data;
                        $scope.targetsSummaryUserInput = r.data;
                        $scope.queriedDevice = $stateParams.device;
                    },
                    function(r) {
                        $log.log("Failed reports: " + r.data);
                        $scope.reportsSummary = {};
                        $scope.queriedDevice = $stateParams.device + " (failed: " + r.data + ")";
                    }
                );

                $http(reqTargets).then(
                    function(r) {
                        $log.log("Success targets: " + JSON.stringify(r.data));
                        $scope.targetsSummary = r.data;
                        $scope.queriedDevice = $stateParams.device;
                    },
                    function(r) {
                        $log.log("Failed to retrieve targets: " + r.data);
                        $scope.targetsSummary = {};
                        $scope.queriedDevice = $stateParams.device + " (failed: " + r.data + ")";
                    }
                );

                $log.log("Executed requests.");

            };

            $scope.changeRequest = function(device, actor, propName, propValue) {
                $log.log("Request to change " + device + " " + actor + " " + propName + " " + propValue);

                BootstrapDialog.show({
                    cssClass: "dialog-vertical-center",
                    title: "Change " + actor + "." + propName,
                    message: "Change property value to: <p><div><textarea style=\"display:block;width:100%;\">" + propValue + "</textarea></div><p>",
                    buttons: [{
                        label: "Change",
                        hotkey: 13, // enter
                        action: function(dialog) {
                           var v = dialog.getModalBody().find("textarea").val();
                           $scope.change(device, actor, propName, v);
                           $log.log("Changed to: " + v);
                           dialog.close();
                        }
                    }, {
                        label: "Cancel",
                        action: function(dialog) {
                           $log.log("Cancelled");
                           dialog.close();
                        }
                    }]
                });
            }

            $scope.valueFromHelp = function(d) {
                return d.split(" -> ")[0].trim();
            }

            $scope.change = function(device, actor, propName, propValue) {
                $log.log("Changing " + device + " " + actor + " " + propName + " " + propValue);

                var jsn = {};
                jsn[propName] = propValue;
                var req = {
                    method: "POST",
                    url: "api/v1/devices/" + device + "/targets/actors/" + actor,
                    headers: {"Content-Type": "application/json", "Session": $scope.session},
                    data: JSON.stringify(jsn)
                };

                $http(req).then(
                    function(r) {
                        $log.log("Success change");
                        $scope.search();
                    },
                    function(r) {
                        $log.log("Failed change");
                    }
                );

            }

            if ($stateParams.device) { // proceed if device is provided
                $scope.initLegends();
                $scope.search();
            }

        }
);


webPortalApp.controller(
    "LogController",
    function($scope, $http, $log, $stateParams, $rootScope) {

        $scope.session = getCookie("session");

        $scope.from = -0.1; // in days, lower-bound to filter history records
        $scope.to = 0.0; // in days, upper-bound to filter history records

        $scope.queriedDevice = "...";

        $scope.getLogs = function() {
            $log.log("Getting logs for device " + $stateParams.device);

            var date = new Date();

            var msTo = 1000 * 3600 * 24 * $scope.to;
            var toMs = date.getTime() + msTo;
            var toSec = (toMs / 1000) | 0; // take to seconds and cast to int

            var msFrom = 1000 * 3600 * 24 * $scope.from;
            var fromMs = date.getTime() + msFrom;
            var fromSec = (fromMs / 1000) | 0; // take to seconds and cast to int

            $log.log('Query from ' + new Date(fromMs).toString() + ' to ' + new Date(toMs).toString());

            var req = {
                method: "GET",
                url: "api/v1/devices/" + $stateParams.device + "/logs?from=" + fromSec + "&to=" + toSec,
                headers: {"Session": $scope.session}
            };

            $log.log("Executing request...");
            $scope.queriedDevice = $stateParams.device + " (in progress)";

            $http(req).then(
                function(r) {
                    $log.log("Logs obtained.");
                    $scope.logs = r.data;
                    $scope.queriedDevice = $stateParams.device;
                },
                function(r) {
                    $log.log("Could not retrieve logs.");
                    $scope.logs = "[]";
                    $scope.queriedDevice = $stateParams.device + " (failed)";
                    BootstrapDialog.show({
                        title: "Error",
                        message: "Failed to retrieve logs."
                    });
                }
            );

            $log.log("Executed request.");

        };

        if ($stateParams.device) { // proceed if device is provided
            $scope.getLogs();
        }
    }
);


webPortalApp.controller(
    "AdministrateController",
    function($scope, $http, $log, $stateParams, $rootScope) {

        $scope.session = getCookie("session");

        $scope.tabl = "reports"; // table to get records from

        $scope.deleteDev = function() {
            $log.log("Deleting device " + $stateParams.device + " in " + $scope.tabl);
            var req = {
                method: "DELETE",
                url: "api/v1/administrator/devices/" + $stateParams.device + "/" + $scope.tabl,
                headers: {"Content-Type": "application/json", "Session": $scope.session},
                data: $scope.request
            };

            $log.log("Executing request...");

            $http(req).then(
                function(r) {
                    $log.log("Deleted: " + JSON.stringify(r.data));
                    $scope.result = "[]";
                },
                function(r) {
                    $log.log("Problem requesting delete: " + JSON.stringify(r.data));
                    BootstrapDialog.show({
                        title: "Error",
                        message: "Failed to delete: " + r.data
                    });
                }
            );

            $log.log("Executed request.");

        };

    }
);

