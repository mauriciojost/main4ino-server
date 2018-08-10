# README

Botino Back-end application. 

Inspiration:
- https://github.com/jaspervz/todo-http4s-doobie/tree/master/src/main/scala

## Run

```
sbt run

curl -H "Authorization: token 11" -X POST http://localhost:8080/api/v1/devices/dev1/actors/clock/targets -d '{"h":5}'

http://localhost:8080/index.html#/device-history
```

Guidelines for REST:
- https://blog.octo.com/wp-content/uploads/2014/10/RESTful-API-design-OCTO-Quick-Reference-Card-2.2.pdf

