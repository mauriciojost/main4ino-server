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
  id SERIAL PRIMARY KEY,
  device_name TEXT,
  updated LONG,
  version TEXT,
  json TEXT
);

-- CREATE INDEX targets_requests_id_index ON targets_requests (device_name,status,id);
-- CREATE INDEX targets_request_id_index ON targets (request_id);
-- CREATE INDEX reports_requests_id_index ON reports_requests (device_name,status,id);
-- CREATE INDEX reports_request_id_index ON reports (request_id);

