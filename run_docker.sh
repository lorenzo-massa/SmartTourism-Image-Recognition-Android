#!/bin/bash

echo "$0"

# Check if the correct number of arguments are provided
if [ "$#" -lt 1 ] || [ "$#" -gt 3 ]; then
    echo "Usage: $0 CITYNAME [copy] [container_name]"
    echo "CITYNAME: name of the city to build the guide for"
    echo "copy: optional argument to copy also guide/categories files"
    exit 1
fi

# Assign arguments to variables with default values
CITYNAME=${1}
COPY=${2:-false}
CONTAINER_NAME=${3:-tfimage}
CONTAINER_PATH_IMAGE_DB=/app/models/src/main/assets/databases
LOCAL_PATH_CONTAINER_PATH_IMAGE_DB=./models/src/main/assets/
CONTAINER_PATH_CATEGORIES=/app/models/src/main/assets/categories/${CITYNAME}/
LOCAL_PATH_CATEGORIES=./models/src/main/assets/currentCategories/
CONTAINER_PATH_GUIDES=/app/models/src/main/assets/guides/${CITYNAME}/
LOCAL_PATH_GUIDES=./models/src/main/assets/currentGuide/

echo "Creating guide for: $CITYNAME"

if test -d "models/src/main/assets/categories/${CITYNAME}"; then
    echo "Categories directory: " models/src/main/assets/categories/${CITYNAME} " exists."
else
    echo "[ERROR] Categories directory " models/src/main/assets/categories/${CITYNAME} " does not exist."
    echo "[ERROR] Exiting..."
    exit 1
fi

if test -d "models/src/main/assets/guides/${CITYNAME}"; then
    echo "Guides directory: " models/src/main/assets/guides/${CITYNAME} " exists."
else
    echo "[ERROR] Categories directory: " models/src/main/assets/guides/${CITYNAME} " does not exist."
    echo "[ERROR] Exiting..."
    exit 1
fi

if [ "$(echo "$COPY" | tr '[:upper:]' '[:lower:]')" = "copy" ]; then  # Convert to lowercase and compare
    echo "[INFO] Copying also guide/categories files..."
    ./copy_guides.sh "$CITYNAME"
else
    echo "[INFO] Not copying guide/categories files... Use ./copy_guides.sh to copy them manually or add 'copy' as second argument to this script."
fi

# Build Docker image
echo "[INFO] Building Docker image: $CONTAINER_NAME"
docker build -t "$CONTAINER_NAME" .

# Check if the build was successful
if [ $? -ne 0 ]; then
    echo "Failed to build Docker image."
    exit 1
fi

# Run Docker container in the background
echo "[INFO] Starting Docker container: $CONTAINER_NAME"
CONTAINER_ID=$(docker run -itd "$CONTAINER_NAME" "$CITYNAME")
echo "Docker container started with ID: $CONTAINER_ID"
docker logs -f "$CONTAINER_ID"

# Check if the log message is found using grep
if docker logs "$CONTAINER_ID" | grep -q 'Build DB script terminated correctly'; then
    echo "[INFO]: Docker terminated correctly."

    # Use docker cp to copy files
    docker cp "$CONTAINER_ID":"$CONTAINER_PATH_IMAGE_DB" "$LOCAL_PATH_CONTAINER_PATH_IMAGE_DB"
    # Check if the copy operation was successful
    if [ $? -eq 0 ]; then
        echo "[INFO] Image database files copied successfully from $CONTAINER_NAME:$CONTAINER_PATH_IMAGE_DB to $LOCAL_PATH_CONTAINER_PATH_IMAGE_DB"
    else
        echo "[ERROR] Failed to copy image database files from $CONTAINER_NAME:$CONTAINER_PATH_IMAGE_DB to $LOCAL_PATH_CONTAINER_PATH_IMAGE_DB"
        echo "[ERROR] Exiting..."
        exit 1
    fi

else
    echo "[ERROR]: Docker container did not run correctly. Check logs."
    exit 1
fi

echo "[INFO] Process terminated correctly."
echo "[INFO] Now build the project with Android Studio."
