CREATE TABLE targets_requests (
  id SERIAL PRIMARY KEY,
  creation LONG,
  device_name TEXT,
  status TEXT
);

CREATE TABLE targets (
  id SERIAL PRIMARY KEY,
  request_id INT,
  actor_name TEXT,
  property_name TEXT,
  property_value TEXT,
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
  actor_name TEXT,
  property_name TEXT,
  property_value TEXT,
  creation LONG
);

CREATE TABLE descriptions (
  device_name TEXT,
  updated LONG,
  version TEXT,
  json TEXT,
);
