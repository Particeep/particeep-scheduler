# TODO

* fix next_run_at si frequency < 1 day ET job non démarré. Ex: start_at : 00:01:00, frequency : 1 minutes
* add pagination in view


------------------------------

# HOW TO DEPLOY

## First add remote

* git remote add clever_test git+ssh://git@push-par-clevercloud-customers.services.clever-cloud.com/???
* git remote add clever_prod git+ssh://git@push-par-clevercloud-customers.services.clever-cloud.com/???

## Deploy cmd

* git push clever_test develop:master
* git push clever_prod master

------------------------------

# HOW TO DEBUG

### Locally

In local run sbt with

>sbt -jvm-debug 9999 "~run"

in intellij (or eclipse) add a remote debugger on the port 9999

------------------------------

## Test

### launch only one test

In a sub module
>sbt "domain/test"

A specific test case
>sbt "testOnly kpi.KpiControllerTest -- -z update"

sbt "testOnly filter.FilterTest -- -z update"

launch test of class kpi.KpiControllerTest with update in their name

------------------------------

## Sample data

```sql
insert into jobs values(
  '1',
  '2021-01-11',
  'Test job 1',
  'now',
  '1 days',
  null,
  'https://www.particeep.com',
  'GET',
  '{}'
);
insert into jobs values(
  '2',
  '2021-01-11',
  'Test job 1',
  '00:01:00',
  '1 minutes',
  null,
  'https://www.particeep.com',
  'GET',
  '{}'
);
```
