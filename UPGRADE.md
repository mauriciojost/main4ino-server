# FIRMWARE UPGRADE

Here an example of the headers obtained from ESP8266 when the firmware
to be downloaded is requested: 

```
GET .../firmware/content
```

```
Host: localhost:8080,
Connection: close,
User-Agent: ESP8266-http-Update,
x-ESP8266-STA-MAC: CC:50:E3:55:F6:07,
x-ESP8266-AP-MAC: CE:50:E3:55:F6:07,
x-ESP8266-free-space: 2736128,
x-ESP8266-sketch-size: 408640,
x-ESP8266-sketch-md5: 7ba6a95cece0abbed66e0784c111d668,
x-ESP8266-chip-size: 4194304,
x-ESP8266-sdk-version: 2.2.1(cfd48f3),
x-ESP8266-mode: sketch,
x-ESP8266-version: 3.9.12-SNAPSHOT-692c8fc
```
