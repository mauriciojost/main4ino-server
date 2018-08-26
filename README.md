# README

[![Build Status](https://api.travis-ci.org/mauriciojost/main4ino-server.svg)](https://travis-ci.org/mauriciojost/main4ino-server)
[![Coverage Status](https://coveralls.io/repos/github/mauriciojost/main4ino-server/badge.svg?branch=master)](https://coveralls.io/github/mauriciojost/main4ino-server?branch=master)

[![Build Status](https://jenkins.martinenhome.com/buildStatus/icon?job=main4ino-server/master)](https://jenkins.martinenhome.com/job/main4ino-server/job/master/)

Main4ino is a very simple framework for the delivery of properties to embedded systems (devices) that support wifi connectivity (like the ESP8266).

There are two parts: 
- main4ino-server (this project): to be launched somewhere accessible by the devices
- [main4ino-arduino](https://bitbucket.org/mauriciojost/main4ino-arduino/): to be used by the soft of the devices

One example of a project making use of `main4ino-arduino` is [here](https://github.com/mauriciojost/botino-arduino/src).

## Basics

A `device` is an embedded system (device "alarm"). 

It is made of `actors`/components: a clock, a speaker, a display.

Each actor has `properties` readable and/or writeable: for the speaker could be the volume.

A device can inform of its status (the current value of its properties) by creating `reports`. 

Normally a device can load user-requested values for its properties by reading the `targets`.

## Run

Run the server to let it be accessible by your devices:

```
sbt run
```

Interact with it via the webapp:

```
http://localhost:8080/index.html#/device-history
```

or via the REST API: 

```
curl -H "Authorization: token 012345678901234567890123456789" -X POST http://localhost:8080/api/v1/devices/dev1/actors/clock/targets -d '{"h":5}'
```

## Contribute


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

### Guidelines for REST

- https://blog.octo.com/wp-content/uploads/2014/10/RESTful-API-design-OCTO-Quick-Reference-Card-2.2.pdf

