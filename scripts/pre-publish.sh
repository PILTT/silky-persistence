#!/bin/sh

openssl des3 -d -salt -in ./scripts/sonatype.asc.enc -out ./scripts/sonatype.asc -k "$SECRET"

psql -c "CREATE USER silky-app WITH CREATEDB;" -U postgres
psql -c "CREATE DATABASE silky-db;" -U silky-app
