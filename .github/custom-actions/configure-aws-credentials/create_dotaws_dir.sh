#!/usr/bin/env sh

mkdir ~/.aws

cat << HERE > ~/.aws/config
[default]
region = $AWS_REGION
HERE

cat << HERE > ~/.aws/credentials
[default]
aws_access_key_id = $AWS_ACCESS_KEY_ID
aws_secret_access_key = $AWS_SECRET_ACCESS_KEY
aws_session_token = $AWS_SESSION_TOKEN
HERE
