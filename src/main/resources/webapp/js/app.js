var DebugPropPrefix = "~";
var StatusPropPrefix = ".";
var SensitivePropPrefix = "_";
var AdvancedPropPrefix = "+";
var UnknownAlias = '';

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

function intoLogsJson(linesStr) {
  var lines = linesStr.split("\n");
  var data = [];
  for (const line of lines) {
      if (line) {
        var d = {};
        d["t"] = line.substr(0,line.indexOf(' '));
        d["content"] = line.substr(line.indexOf(' ')+1);
        data.push(d);
      }
  }
  return data;
}
function asDate(ms) {
  $log.log("Date useddd: " + ms);
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
            $rootScope.aliases = {};

            $scope.setAlias = function(dev) {
                $log.log("Alias for: " + dev);
                $rootScope.aliases[dev] = UnknownAlias;
                var req = {
                    method: "GET",
                    url: "api/v1/devices/" + dev + "/reports/summary",
                    headers: {"Content-Type": "application/json", "Session": $scope.session}
                };

                $log.log("Executing request...");

                $http(req).then(
                    function(r) {
                        try {
                          $rootScope.aliases[dev] = r.data.device.alias || UnknownAlias;
                        } catch (error) {
                          $log.log("Failed to load alias for " + dev + ": " + error + ": " + JSON.stringify(r.data));
                          $rootScope.aliases[dev] = UnknownAlias;
                        }
                        $log.log("So far: " + JSON.stringify($rootScope.aliases));
                    },
                    function(r) {
                        $log.log("Problem requesting alias for: " + dev);
                        $rootScope.aliases[dev] = UnknownAlias;
                    }
                );
            }

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
                        $rootScope.logged = r.data;
                    },
                    function(r) {
                        $log.log("Not logged in: " + r.data);
                    }
                );
                var requsDevs = {
                    method: "GET",
                    url: "api/v1/devices",
                    headers: {"Session": $scope.session}
                };
                $http(requsDevs).then(
                    function(r) {
                        $log.log("Devices: " + r.data);
                        $rootScope.devices = r.data;
                        $rootScope.aliases = {};
                        for (const d of r.data) {
                            $scope.setAlias(d);
                        }
                        $log.log("Aliases: " + JSON.stringify($rootScope.aliases));
                    },
                    function(r) {
                        $log.log("Failed to retrieve devices");
                        $rootScope.devices = [];
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
            }

            $scope.rememberCredentials = function() {
              $log.log("Remember credentials");
              $log.log("User: " + $scope.username);
              $log.log("Session: " + $scope.session);
                var req = {
                    method: "POST",
                    url: "api/v1/session",
                    headers: {"Authorization": "Basic " + btoa($scope.username + ":" + $scope.password)}
                };
                $http(req).then(
                    function(r) {
                        $log.log("Logged in correctly: " + r.data);
                        setCookie("session", r.data, 100);
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
            $scope.fromHours = -1; // in hours, lower-bound to filter history records
            $scope.toHours = 0.0; // in hours, upper-bound to filter history records

            $scope.readRange = function() {
                $scope.fromHours = Number(getCookie("fromHours") || -1);
                $scope.toHours = Number(getCookie("toHours") || 0);
            }

            $scope.updateRange = function() {
                setCookie("fromHours", $scope.fromHours, 100);
                setCookie("toHours", $scope.toHours, 100);

                var date = new Date();
                var msTo = 1000 * 3600 * $scope.toHours;
                var toMs = date.getTime() + msTo;

                var msFrom = 1000 * 3600 * $scope.fromHours;
                var fromMs = date.getTime() + msFrom;
                $scope.toSec = (toMs / 1000) | 0; // take to seconds and cast to int
                $scope.fromSec = (fromMs / 1000) | 0; // take to seconds and cast to int
                $scope.toDate = new Date(toMs).toString();
                $scope.fromDate = new Date(fromMs).toString();
            }

            $scope.readRange();
            $scope.updateRange();

            $scope.search = function() {

                $scope.queriedDevice = $stateParams.device + " (in progress)";

                $log.log("Searching device " + $stateParams.device + " in " + $scope.tabl);

                var req = {
                    method: "GET",
                    url: "api/v1/devices/" + $stateParams.device + "/" + $scope.tabl + "?from=" + $scope.fromSec + "&to=" + $scope.toSec,
                    headers: {"Content-Type": "application/json", "Session": $scope.session},
                    data: $scope.request
                };

                $log.log("Executing request...");

                $http(req).then(
                    function(r) {
                        $log.log("Found: " + JSON.stringify(r.data));
                        $scope.result = r.data;
                        $scope.queriedDevice = $stateParams.device;
                    },
                    function(r) {
                        $log.log("Problem requesting: " + JSON.stringify(r.data));
                        $scope.result = "[]"
                        $scope.queriedDevice = "Failed query for " + $stateParams.device;
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

            $scope.initLegends = function(project, platform, version) {
                $log.log("Loading descriptions: " + project + " / " + platform + " / " + version);
                $scope.propLegends = [];

                var reqDescs = {
                    method: "GET",
                    url: "api/v1/devices/" + $stateParams.device  + "/firmware/firmwares/" + project + "/" + platform + "/content?version=" + version + "&mode=description",
                    headers: {"Content-Type": "application/json", "Session": $scope.session}
                };

                $http(reqDescs).then(
                    function(r) {

                        $log.log("Data raw json: " + JSON.stringify(r.data));
                        var dt = r.data["json"];
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

                $log.log("Executing requests...");
                $scope.queriedDevice = $stateParams.device + " (in progress)";

                $scope.reportsSummary = {};
                $scope.targetsSummaryUserInput = {};
                $scope.targetsSummary = {};

                $http(reqReports).then(
                    function(r) {
                        $log.log("Success reports: " + JSON.stringify(r.data));
                        $scope.reportsSummary = r.data;
                        $scope.targetsSummaryUserInput = r.data;
                        if (r.data) {
                            if (r.data.device) {
                                var project = r.data.device.project;
                                var platform = r.data.device.platform;
                                var version = r.data.device['~version'];
                                $scope.initLegends(project, platform, version);
                            }
                        }

                        var reqTargets = {
                            method: "GET",
                            url: "api/v1/devices/" + $stateParams.device + "/targets/summary?status=C",
                            headers: {"Content-Type": "application/json", "Session": $scope.session},
                            data: $scope.request
                        };

                        $http(reqTargets).then(
                            function(r) {
                                $log.log("Success targets: " + JSON.stringify(r.data));
                                $scope.targetsSummary = r.data;
                                $scope.queriedDevice = $stateParams.device;
                            },
                            function(r) {
                                $log.log("Failed to retrieve targets: " + r.data);
                                $scope.reportsSummary = {};
                                $scope.targetsSummaryUserInput = {};
                                $scope.queriedDevice = $stateParams.device + " (failed: " + r.data + ")";
                            }
                        );


                    },
                    function(r) {
                        $log.log("Failed reports: " + r.data);
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
                var jsn2 = {};
                jsn2[propName] = propValue;
                jsn[actor] = jsn2;
                var req = {
                    method: "POST",
                    url: "api/v1/devices/" + device + "/targets",
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
                $scope.search();
            }

        }
);


webPortalApp.controller(
    "LogController",
    function($scope, $http, $log, $stateParams, $rootScope) {

        $scope.session = getCookie("session");

        $scope.fromHours = -1; // in hours, lower-bound to filter history records
        $scope.toHours = 0; // in hours, upper-bound to filter history records
        
        $scope.readRange = function() {
            $scope.fromHours = Number(getCookie("fromHours") || -1);
            $scope.toHours = Number(getCookie("toHours") || 0);
        }

        $scope.updateRange = function() {
            setCookie("fromHours", $scope.fromHours, 100);
            setCookie("toHours", $scope.toHours, 100);

            var date = new Date();
            var msTo = 1000 * 3600 * $scope.toHours;
            var toMs = date.getTime() + msTo;

            var msFrom = 1000 * 3600 * $scope.fromHours;
            var fromMs = date.getTime() + msFrom;
            $scope.toSec = (toMs / 1000) | 0; // take to seconds and cast to int
            $scope.fromSec = (fromMs / 1000) | 0; // take to seconds and cast to int
            $scope.toDate = new Date(toMs).toString();
            $scope.fromDate = new Date(fromMs).toString();
        }

        $scope.readLogsMode = function() {
            $scope.logsMode = getCookie("logsMode") || 'normal';
        }

        $scope.setLogsMode = function() {
            $scope.logs = [];
            setCookie("logsMode", $scope.logsMode, 100);
            if ($scope.socket) {
                $scope.socket.close();
            }
        }

        $scope.asDateScoped = function(d) {
            $log.log("Date input: " + d);
            return new Date(d * 1000);
        };

        $scope.readRange();
        $scope.updateRange();
        $scope.readLogsMode();

        $scope.queriedDevice = "...";

        $scope.getLogs = function() {
            $log.log("Getting logs for device " + $stateParams.device);
            const wsurl = window.location.protocol.replace('http', 'ws') + '//' + window.location.host + window.location.pathname + 'api/v1/session/' + $scope.session + '/devices/' + $stateParams.device + '/logstail';

            if ($scope.logsMode == 'live') {
                $log.log("Websocket url: " + wsurl);
                $scope.socket = new WebSocket(wsurl);
                $scope.logs = intoLogsJson('0 Listening...');
                $scope.socket.addEventListener('message', function (event) {
                    if (event.data) {
                        var d = {};
                        d["t"] = event.data.substr(0, event.data.indexOf(' '));
                        d["content"] = event.data.substr(event.data.indexOf(' ')+1);
                        console.log('Device ' + $stateParams.device + ': ', event.data);
                        $scope.logs.push(d);
                        $scope.$apply();
                    }
                });
            } else {
                var req = {
                    method: "GET",
                    url: "api/v1/devices/" + $stateParams.device + "/logs?from=" + $scope.fromSec + "&to=" + $scope.toSec,
                    headers: {"Session": $scope.session}
                };
                $log.log("Executing request...");
                $scope.queriedDevice = $stateParams.device + " (in progress)";

                $http(req).then(
                    function(r) {
                        $log.log("Logs obtained.");
                        $scope.logs = intoLogsJson(r.data);
                        $scope.queriedDevice = $stateParams.device;
                    },
                    function(r) {
                        $log.log("Could not retrieve logs.");
                        $scope.logs = [];
                        $scope.queriedDevice = $stateParams.device + " (failed)";
                        BootstrapDialog.show({
                            title: "Error",
                            message: "Failed to retrieve logs."
                        });
                    }
                );

                $log.log("Executed request.");
            }

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

