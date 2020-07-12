CREATE TABLE targets_requests (
  id SERIAL PRIMARY KEY,
  creation LONG,
  device_name VARCHAR(20),
  status VARCHAR(1) CHECK (status IN ('O', 'C', 'X', '?'))
);

CREATE TABLE targets (
  id SERIAL PRIMARY KEY,
  request_id INT,
  actor_name VARCHAR(20),
  property_name VARCHAR(20),
  property_value TEXT,
  creation LONG
);

CREATE TABLE reports_requests (
  id SERIAL PRIMARY KEY,
  creation LONG,
  device_name VARCHAR(20),
  status VARCHAR(1) CHECK (status IN ('O', 'C', 'X', '?'))
);

CREATE TABLE reports (
  id SERIAL PRIMARY KEY,
  request_id INT,
  actor_name VARCHAR(20),
  property_name VARCHAR(20),
  property_value TEXT,
  creation LONG
);

CREATE TABLE descriptions (
  device_name VARCHAR(20) PRIMARY KEY,
  updated LONG,
  version TEXT,
  json TEXT
);

CREATE INDEX targets_requests_id_index ON targets_requests (device_name,status,id);
CREATE INDEX targets_request_id_index ON targets (request_id);
CREATE INDEX reports_requests_id_index ON reports_requests (device_name,status,id);
CREATE INDEX reports_request_id_index ON reports (request_id);

