CREATE TABLE IF NOT EXISTS USERPROFILE (
  id uuid DEFAULT random_uuid(),
  name varchar(64) NOT NULL,
  registered_at timestamp NOT NULL,
  password varchar(64) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE (name)
);