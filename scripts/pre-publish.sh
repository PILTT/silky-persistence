#!/bin/sh

openssl des3 -d -salt -in ./scripts/sonatype.asc.enc -out ./scripts/sonatype.asc -k "$SECRET"

createuser silky-app
#createdb --owner=silky-app --encoding=UTF8 silky-db

#psql -c "CREATE USER silky-app WITH CREATEDB;" -U postgres
psql -c "CREATE DATABASE silky-db;" -U silky-app
#psql -c "ALTER DATABASE silky-db OWNER TO silky-app;" -U postgres
