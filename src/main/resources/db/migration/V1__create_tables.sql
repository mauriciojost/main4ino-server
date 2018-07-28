CREATE TABLE targets (
  id SERIAL PRIMARY KEY,
  t TIMESTAMP,
  status TEXT,
  device_name TEXT
);

CREATE TABLE target_props (
  target_id INT,
  actor_name TEXT,
  property_name TEXT,
  property_value TEXT
);

