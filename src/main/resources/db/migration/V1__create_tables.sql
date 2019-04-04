CREATE TABLE targets_requests (
  id SERIAL PRIMARY KEY,
  creation LONG,
  device_name TEXT,
  status TEXT
);

CREATE TABLE targets (
  id SERIAL PRIMARY KEY,
  request_id INT,
  device_name TEXT,
  actor_name TEXT,
  property_name TEXT,
  property_value TEXT,
  property_status TEXT,
  creation LONG
);

CREATE TABLE reports_requests (
  id SERIAL PRIMARY KEY,
  creation LONG,
  device_name TEXT,
  status TEXT
);

CREATE TABLE reports (
  id SERIAL PRIMARY KEY,
  request_id INT,
  device_name TEXT,
  actor_name TEXT,
  property_name TEXT,
  property_value TEXT,
  property_status TEXT,
  creation LONG
);
