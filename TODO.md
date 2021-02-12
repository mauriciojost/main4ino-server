# TODO

- [ ] Document the return type of each method of the REST API
- [ ] Make the naming for deviceName/dev/device homogeneous: dev
- [ ] Make the naming for actor/act/a homogeneous: act
- [ ] Make time selection in UI nicer (phones cannot put negative)
- [ ] Export metrics on Prometheus using [this](https://github.com/prometheus/client_java)
- [ ] Improve web UI (tried many times, I suck at it, need someone's help)
- [ ] Provide shortcut resources to reduce amount of queries on low power projects: PUSH (current firmware version;offset of targets; reports) and GET (time, firmware updates, targets)
- [ ] Modularize description jsons (one for main4ino, one for mod4ino, one for ...)

# DONE

- [x] Fix issues with descriptions, take them from published firmware
- [x] Speed up 'last' request from device as it takes way too long (>10secs !!!)
- [x] Perform cleanup of repository, service v1 and its doc, and url class
- [x] Make the DevLogger tests write to a temporary directory that is afterwards cleaned up
- [x] Make a check on status transitions: forbid invalid transitions and document them
- [x] Migrate HELP as documentation in Service
- [x] Do a check to ensure that documentation matches service v1
- [x] Resolve code TODOs
- [x] Make tuples/actortuples/propsmap/map/etc.etc.etc. simpler
- [x] Render target/report and properties statuses type-safe
- [x] Consume done at transaction/request level, one transaction at a time, no merging of different transactions
- [x] Remove unneeded API methods
- [x] Be able to change the target/report status
- [x] Go at Device level in Translator, avoid going to ActorTup level
- [x] Add target/report status and tuples creation timestamp
- [x] Allow to create a target/report for a given device and fill it in actor per actor
- [x] Auto-format code
- [x] Document which parts of the API have which usecase.
- [x] Have logs (in a purely functional way if possible)
- [x] Do not display None keys in the JSON -> by displaying more appropriate types according to the queries
- [x] Handle non happy paths too
- [x] Handle properly scenarios where no properties are found in a GET
- [x] Add pagination/ranges to streamed resources
- [x] webapp
- [x] Many times the in sql the ID is obtained to later obtain metadata and then the props, it would be good to get the metadata together with the ids.
- [x] There is too much duplicate code in the Repository due to the fact I don't see how reuse SQL queries patterns with an interpolator
- [x] Authentication per device
- [x] Be able to choose between history of targets or reports
- [x] Allow pre-filled examples in summary
