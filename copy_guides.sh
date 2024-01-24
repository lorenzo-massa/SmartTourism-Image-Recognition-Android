#!/bin/bash

echo "$0"

# Check if the correct number of arguments are provided
if [ "$#" -ne 1 ]; then
    echo "Usage: $0 CITYNAME"
    exit 1
fi

# Assign arguments to variables with default values
CITYNAME=${1}

echo "Copying guide/categories for: $CITYNAME"

python ./copy_guides.py -g "$CITYNAME"
if [ $? -eq 0 ]; then
    echo "[INFO] Guide/categories files copied successfully."
    echo "[INFO] Now build the project with Android Studio."
else
    echo "[ERROR] Failed to copy guide. Check that guides and categories exist for '$CITYNAME'."
    echo "[ERROR] Exiting... check the logs."
    exit 1
fi
