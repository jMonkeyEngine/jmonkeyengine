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

# uploadFile file destination [REPO] "content" [PACKAGE] [USER] [PASSWORD] [SRCREPO] [LICENSE]
function bintray_uploadFile {
    file="$1"
    dest="$2"
    
    echo "Upload $file to $dest"

    repo="$3"
    type="$4"
    package="$5"

    user="$6"
    password="$7"
   
    srcrepo="$8"
    license="$9"
    publish="${10}"

    bintray_createPackage $repo $package $user $password $srcrepo $license

    url="https://api.bintray.com/$type/$repo/$package/$dest"
    if [ "$publish" = "true" ]; then url="$url;publish=1"; fi

    curl -T "$file" -u$user:$password "$url"
     
}

function bintray_uploadAll {
    path="$1"
    destpath="$2"
    repo="$3"
    type="$4"
    package="$5"

    user="$6"
    password="$7"
   
    srcrepo="$8"
    license="$9"
    publish="${10}"

    cdir="$PWD"
    cd "$path"

    files="`find . -type f -print`"
    IFS="
"
    set -f
    for f in $files; do
        destfile="$destpath/${f:2}"
        bintray_uploadFile $f $destfile $repo $type $package $user $password $srcrepo $license $publish
    done
    set +f
    unset IFS
    cd "$cdir"
}
