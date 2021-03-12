#!/bin/bash

# bintray_createPackage [REPO] [PACKAGE] [USER] [PASSWORD] [GIT REPO] [LICENSE]
function bintray_createPackage {
    repo="$1"
    package="$2"
    user="$3"
    password="$4"
    srcrepo="$5"
    license="$6"

    repoUrl="https://api.bintray.com/packages/$repo"
    if [ "`curl -u$user:$password -H Content-Type:application/json -H Accept:application/json \
    --write-out %{http_code} --silent --output /dev/null -X GET \"$repoUrl/$package\"`" != "200" ];
    then

        if [ "$srcrepo" != "" -a "$license" != "" ];
        then 
            echo "Package does not exist... create."
            data="{
                \"name\": \"${package}\",
                \"labels\": [],
                \"licenses\": [\"${license}\"],
                \"vcs_url\": \"${srcrepo}\"
            }"
     

            curl -u$user:$password -H "Content-Type:application/json" -H "Accept:application/json" -X POST \
                -d "${data}" "$repoUrl"
        else
            echo "Package does not exist... you need to specify a repo and license for it to be created."
        fi
    else    
        echo "The package already exists. Skip."
    fi
}

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
    wget https://dl.min.io/client/mc/release/linux-amd64/mc
    chmod +x ./mc

    echo "Add an alias for the MinIO instance to the MinIO configuration file"
    ./mc alias set objects "$url" "$access" "$secret"

    echo "Upload $file to $url/$dest"
    ./mc cp "$file" "objects/$dest"
}
