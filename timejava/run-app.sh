#!/bin/bash

# Check if libraries are downloaded
if [ ! -d "lib" ] || [ -z "$(ls -A lib)" ]; then
    echo "Libraries not found. Downloading required libraries..."
    ./download-libs.sh
fi

# Run the main application using make
make run 