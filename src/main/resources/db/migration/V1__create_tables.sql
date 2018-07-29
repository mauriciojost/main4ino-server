CREATE TABLE target_requests (
  id SERIAL PRIMARY KEY,
  creation LONG,
  device_name TEXT
);

CREATE TABLE targets (
  target_id INT,
  device_name TEXT,
  actor_name TEXT,
  property_name TEXT,
  property_value TEXT,
  property_status TEXT
);

CREATE TABLE report_requests (
  id SERIAL PRIMARY KEY,
  creation LONG,
  device_name TEXT
);

CREATE TABLE reports (
  target_id INT,
  device_name TEXT,
  actor_name TEXT,
  property_name TEXT,
  property_value TEXT,
  property_status TEXT
);
