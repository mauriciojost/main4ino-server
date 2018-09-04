# TODO

- Have logs (in a purely functional way if possible)

# DONE

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
