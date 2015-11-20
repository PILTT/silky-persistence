#!/bin/sh

openssl des3 -d -salt -in ./scripts/sonatype.asc.enc -out ./scripts/sonatype.asc -k "$SECRET"

psql -c "CREATE USER silky_app WITH CREATEDB;"
psql -c "CREATE DATABASE silky_db WITH ENCODING 'UTF8' OWNER silky_app;"
