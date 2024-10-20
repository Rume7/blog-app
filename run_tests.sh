#!/bin/bash


echo "Starting PostgreSQL Docker container..."
docker run --name postgres-test -e POSTGRES_USER=test -e POSTGRES_PASSWORD=test -e POSTGRES_DB=testdb -p 55432:55432 -d postgres

# Wait for a few seconds to ensure PostgreSQL is ready
echo "Waiting for PostgreSQL to initialize..."
sleep 10

# Run Maven tests
echo "Running tests..."
mvn test

# Capture the exit code of the test run
TEST_RESULT=$?

# Stop and remove the PostgreSQL container
echo "Stopping PostgreSQL Docker container..."
docker stop postgres-test
docker rm postgres-test

# Exit with the test result code
exit $TEST_RESULT
