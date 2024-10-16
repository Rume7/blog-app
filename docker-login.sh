#!/bin/bash

# Load environment variables from .env file
if [ ! -f ".env" ]; then
    echo ".env file not found!"
    exit 1
fi

# Export variables from .env file
export $(grep -v '^#' .env | xargs)

# Check if the necessary variables are set
if [ -z "$DOCKER_USERNAME" ] || [ -z "$DOCKER_TOKEN" ]; then
    echo "Docker username or token not set in .env file!"
    exit 1
fi

# Log in to Docker
echo "$DOCKER_TOKEN" | docker login -u "$DOCKER_USERNAME" --password-stdin

# Check if the login was successful
if [ $? -eq 0 ]; then
    echo "Docker login successful!"
else
    echo "Docker login failed!"
fi
