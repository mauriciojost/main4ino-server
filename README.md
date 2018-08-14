# README


[![Build Status](https://jenkins.martinenhome.com/buildStatus/icon?job=botinobe/master)](https://jenkins.martinenhome.com/job/botinobe/job/master/)

Botino Back-end application. 

The embedded part of the project is [here](https://bitbucket.org/mauriciojost/botino/src).

## Run

Run the server:

```
sbt run
```

Interact with it:
```
http://localhost:8080/index.html#/device-history
curl -H "Authorization: token 11" -X POST http://localhost:8080/api/v1/devices/dev1/actors/clock/targets -d '{"h":5}'
```

## Contribute

```
sbt clean "set every coverageEnabled := true" test coverageReport && sbt coverageAggregate
```

## Miscellaneous

- Inspiration: https://github.com/jaspervz/todo-http4s-doobie

- Guidelines for REST: https://blog.octo.com/wp-content/uploads/2014/10/RESTful-API-design-OCTO-Quick-Reference-Card-2.2.pdf

