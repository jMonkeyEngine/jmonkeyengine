#!/bin/bash
#############################################
#
# Usage
#       uploadAllToMaven path/of/dist/maven https://api.bintray.com/maven/riccardo/sandbox-maven/ riccardo $BINTRAY_PASSWORD gitrepo license
#           Note: gitrepo and license are needed only when uploading to bintray if you want to create missing packages automatically
#                   gitrepo must be a valid source repository
#                   license must be a license supported by bintray eg "BSD 3-Clause"
#   or
#       uploadAllToMaven path/of/dist/maven $GITHUB_PACKAGE_REPOSITORY user password
#
#############################################
root="`dirname  ${BASH_SOURCE[0]}`"
source $root/bintray.sh

set -e
function uploadToMaven {
    jar="$1"
    url="$2"
    user="$3"
    password="$4"
    srcrepo="$5"
    license="$6"
    auth=""
    
    tmpPath="/tmp/temp.uploadToMaven.m2-`date +%s`-`tr -dc A-Za-z0-9 < /dev/urandom | head -c 8 | xargs`"
    echo "Create temp path $tmpPath"
    mkdir -p "$tmpPath"
    
    echo "
<settings>
    <servers>
        <server>
            <id>dest.repo</id>
            <username>${user}</username>
            <password>${password}</password>
        </server>
    </servers>
</settings>
    " > "$tmpPath/settings.xml"

    auth="--settings $tmpPath/settings.xml"        

    pom=${jar%.jar} 
    pom=${pom%-javadoc} 
    pom=${pom%-sources} 
    pom="${pom}.pom"
    package="${jar%/*}"    
    package="${package%/*}"
    package="`basename $package`"
    
    if [[ $url == https\:\/\/api.bintray.com\/* ]]; 
    then 
        if [ "$user" = "" -o "$password" = "" ];
        then
            echo "Error! You need username and password to upload to bintray"
             exit 1
        fi
        echo "Detected bintray"

        bintrayRepo="${url/https\:\/\/api.bintray.com\/maven/}"   
        echo "Create package on $bintrayRepo"

        bintray_createPackage $bintrayRepo $package $user $password $srcrepo $license  
        
        url="$url/$package"    
    fi
    
    cmd="mvn deploy:deploy-file -Durl=\"$url\" -Dfile=\"$jar\" -DrepositoryId=dest.repo -DpomFile=\"$pom\" $auth"
    echo "Run $cmd"
    eval "$cmd"    
    echo "Remove temp path $tmpPath"
    rm -Rf "$tmpPath"
}
export -f uploadToMaven

function uploadAllToMaven {
  path="$1"
  
  files="`find \"$path\" -name *.jar -type f -print`"
  IFS="
"
  set -f
  for art in $files; do
    uploadToMaven "$art" ${@:2}
  done
  set +f
  unset IFS
} 
