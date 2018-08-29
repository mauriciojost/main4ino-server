# TODO

- Handle properly scenarios where no properties are found in a GET
- Have logs (in a purely functional way if possible)
- Handle non happy paths too
- Do not display None keys in the JSON
- Add pagination/ranges to streamed resources


# DONE

- webapp
- Many times the in sql the ID is obtained to later obtain metadata and then the props, it would be good to get the metadata together with the ids.
- There is too much duplicate code in the Repository due to the fact I don't see how reuse SQL queries patterns with an interpolator
- Authentication per device
- Be able to choose between history of targets or reports
- Allow pre-filled examples in summary
