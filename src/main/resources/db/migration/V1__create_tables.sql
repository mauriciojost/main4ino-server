CREATE TABLE target_props (
  id SERIAL PRIMARY KEY,
  target_id INT,
  actor TEXT,
  property TEXT,
  value TEXT
);

CREATE TABLE targets (
  id SERIAL PRIMARY KEY,
  status TEXT,
  device TEXT
);
