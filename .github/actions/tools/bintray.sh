#!/bin/bash

# minio_uploadFile <LOCAL_FILEPATH> <REMOTE_FILEPATH> <MINIO_URL> <MINIO_ACCESS_KEY> <MINIO_SECRET_KEY>
#
# Upload the specified file to the specified MinIO instance.
function minio_uploadFile {
    file="$1"
    dest="$2"
    url="$3"
    access="$4"
    secret="$5"

    echo "Install MinIO client"
    wget --quiet https://dl.min.io/client/mc/release/linux-amd64/mc
    chmod +x ./mc

    echo "Add an alias for the MinIO instance to the MinIO configuration file"
    ./mc alias set objects "$url" "$access" "$secret"

    echo "Upload $file to $url/$dest"
    ./mc cp "$file" "objects/$dest"
}
