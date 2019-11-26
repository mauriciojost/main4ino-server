# README

[![Build Status](https://api.travis-ci.org/mauriciojost/main4ino-server.svg)](https://travis-ci.org/mauriciojost/main4ino-server)
[![Coverage Status](https://coveralls.io/repos/github/mauriciojost/main4ino-server/badge.svg?branch=master)](https://coveralls.io/github/mauriciojost/main4ino-server?branch=master)

[![Build Status](https://jenkins.martinenhome.com/buildStatus/icon?job=main4ino-server/master)](https://jenkins.martinenhome.com/job/main4ino-server/job/master/)

Main4ino is a very simple framework to facilitate the synchronization of properties from a common server to multiple embedded systems (devices) that support wifi connectivity (like the ESP8266).

There are two parts: 
- **main4ino-server** (this project): to be launched somewhere accessible by the devices
- [main4ino-arduino](https://bitbucket.org/mauriciojost/main4ino-arduino/): to be used by the soft of the Arduino devices

One example of a project making use of `main4ino-arduino` is [here](https://github.com/mauriciojost/botino-arduino).

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
sbt "runMain org.mauritania.main4ino.cli.Client input.conf add.conf output.conf"
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

