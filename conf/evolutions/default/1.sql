# Scheduler schema
# --- !Ups

CREATE TABLE jobs (
  id              varchar(36) PRIMARY KEY,
  created_at      timestamptz NOT NULL,
  name            text        NOT NULL,
  start_time      text        NOT NULL,
  frequency       text        NOT NULL,
  credentials     jsonb       DEFAULT NULL,
  url             text        NOT NULL,
  method          text        NOT NULL,
  tag             text[]      NOT NULL
);


CREATE TABLE executions (
  id              varchar(36) PRIMARY KEY,
  job_id          varchar(36) REFERENCES jobs(id),
  executed_at     timestamptz NOT NULL,
  status          int,
  response        text
);


# --- !Downs
DROP TABLE executions;
DROP TABLE jobs;
