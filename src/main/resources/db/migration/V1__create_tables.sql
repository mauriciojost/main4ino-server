CREATE TABLE targets (
  id SERIAL PRIMARY KEY,
  status TEXT,
  device_name TEXT,
  creation LONG
);

CREATE TABLE target_props (
  target_id INT,
  actor_name TEXT,
  property_name TEXT,
  property_value TEXT
);

CREATE TABLE reports (
  id SERIAL PRIMARY KEY,
  status TEXT,
  device_name TEXT,
  creation LONG
);

CREATE TABLE report_props (
  target_id INT,
  actor_name TEXT,
  property_name TEXT,
  property_value TEXT
);
