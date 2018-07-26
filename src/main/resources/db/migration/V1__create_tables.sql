CREATE TABLE target_props (
  id SERIAL PRIMARY KEY,
  target_id INT,
  actor_name TEXT,
  property_name TEXT,
  property_value TEXT
);

CREATE TABLE targets (
  id SERIAL PRIMARY KEY,
  status TEXT,
  device_name TEXT
);
