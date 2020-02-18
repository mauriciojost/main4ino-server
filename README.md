# README

[![BCH compliance](https://bettercodehub.com/edge/badge/mauriciojost/main4ino-server?branch=master)](https://bettercodehub.com/)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/57c35744da0a475e970c0470db4602a0)](https://app.codacy.com/manual/mauriciojost/main4ino-server?utm_source=github.com&utm_medium=referral&utm_content=mauriciojost/main4ino-server&utm_campaign=Badge_Grade_Dashboard)
[![Travis CI](https://api.travis-ci.org/mauriciojost/main4ino-server.svg)](https://travis-ci.org/mauriciojost/main4ino-server) 
![Github CI](https://github.com/mauriciojost/main4ino-server/workflows/Scala%20CI/badge.svg?branch=master)
[![Coverage Status](https://coveralls.io/repos/github/mauriciojost/main4ino-server/badge.svg?branch=master)](https://coveralls.io/github/mauriciojost/main4ino-server?branch=master) 
[![Scala Steward badge](https://img.shields.io/badge/Scala_Steward-helping-blue.svg?style=flat&logo=data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAA4AAAAQCAMAAAARSr4IAAAAVFBMVEUAAACHjojlOy5NWlrKzcYRKjGFjIbp293YycuLa3pYY2LSqql4f3pCUFTgSjNodYRmcXUsPD/NTTbjRS+2jomhgnzNc223cGvZS0HaSD0XLjbaSjElhIr+AAAAAXRSTlMAQObYZgAAAHlJREFUCNdNyosOwyAIhWHAQS1Vt7a77/3fcxxdmv0xwmckutAR1nkm4ggbyEcg/wWmlGLDAA3oL50xi6fk5ffZ3E2E3QfZDCcCN2YtbEWZt+Drc6u6rlqv7Uk0LdKqqr5rk2UCRXOk0vmQKGfc94nOJyQjouF9H/wCc9gECEYfONoAAAAASUVORK5CYII=)](https://scala-steward.org)

Main4ino is a very simple ArduinoC++(client)/Scala(server) framework to facilitate the development, setup, maintenance and debugging of Arduino based IoT projects. It relies on a common server to support multiple embedded systems (devices) that connect to it via wifi (like the ESP8266, ESP32, etc.).

There are two parts: 
  - **main4ino-server** (**server**, this project): to be launched somewhere accessible by the devices
  - [main4ino-arduino](https://bitbucket.org/mauriciojost/main4ino-arduino/) (**client**): to be used by the soft of the Arduino devices

Examples of projects making use of `main4ino-arduino`:

  - [botino](https://github.com/mauriciojost/botino-arduino)
  - [sleepino](https://github.com/mauriciojost/sleepino)

## Features

Current features include:

  - REST API / JSON for interaction server-devices
  - web UI for interaction server-users
  - authentication on web UI and REST API
  - device properties edition via web UI (to set up devices conveniently)
  - device properties synchronization with server (the server handles the synchronization of targets/reports automaticaly)
  - device time synchronization (devices get their clock automatically synchronized according to the preferred timezone)
  - device logs storage and exploration via web UI (debug and monitor your devices conveniently without the need of physical access to the device)
  - firmware server (let your Arduino upgrade its firmware automatically without the need of physical access to it)
  - CI/CD example using Jenkins (your firmwares get built by Jenkins and deployed automatically, so that auto-upgrade can take place)

## Basics

A `device` is an embedded system. For instance, a device can be an alarm based on Arduino.

A device is made of `actors` (or components). For the alarm example, we could have actors clock, speaker and display.

Each actor has `properties`. They can be readable and/or writeable. For the actor speaker, we could have the property volume.

A device normally informs of its status regularly (the current value of the properties of its actors) by creating `reports` in the server.

Normally a device can load recently created user-requested values for its actor's properties, by reading the `targets` from the server. 

Once the device requested targets from the server successfully, they become consumed, making each target not retrievable more than once.

The corresponding REST API is [here](/src/main/scala/org/mauritania/main4ino/api/v1/Service.scala).

## Run


### Server

Run the server to let it be accessible by your devices:

```
sbt 'runMain org.mauritania.main4ino.Server src/main/resources/defaultconfig/'
```

Default credentials are: `admin` / `password`

Interact with it via the webapp:

```
http://localhost:8080/
```

or via the REST API: 

```
curl -u admin:password -X GET  http://localhost:8080/api/v1/time
curl -u admin:password -X POST http://localhost:8080/api/v1/devices/dev1/targets/actors/clock -d '{"h":5}';
```

### Config generator

It allows to create the configuration files for the server.

```
sbt "runMain org.mauritania.main4ino.security.confgen.Client input.conf add.conf output.conf"
```

## Contribute

Contributions can be done via PRs. Broken PRs are not taken into account.

```
# clean
sbt clean

# compile
sbt compile

# launch tests
sbt test

# check coverage
sbt "set every coverageEnabled := true" test coverageReport
sbt coverageAggregate
```

## Miscellaneous

### Inpiration

  - https://github.com/jaspervz/todo-http4s-doobie
  - https://github.com/pauljamescleary/scala-pet-store

### Guidelines for REST

  - https://blog.octo.com/wp-content/uploads/2014/10/RESTful-API-design-OCTO-Quick-Reference-Card-2.2.pdf
