# TODO

- Remove unneeded API methods
- Allow to create a target/report for a given device and fill it in actor per actor
- Be able to change the target/report status
- Render target/report and properties statuses type-safe
- Perform cleanup of repository, service v1 and its doc, and url class
- Consume done at transaction/request level, one transaction at a time, no merging of different transactions

# DONE

- Document which parts of the API have which usecase.
- Have logs (in a purely functional way if possible)
- Do not display None keys in the JSON -> by displaying more appropriate types according to the queries
- Handle non happy paths too
- Handle properly scenarios where no properties are found in a GET
- Add pagination/ranges to streamed resources
- webapp
- Many times the in sql the ID is obtained to later obtain metadata and then the props, it would be good to get the metadata together with the ids.
- There is too much duplicate code in the Repository due to the fact I don't see how reuse SQL queries patterns with an interpolator
- Authentication per device
- Be able to choose between history of targets or reports
- Allow pre-filled examples in summary
