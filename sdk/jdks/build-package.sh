#!/bin/bash
set -e
#(c) jMonkeyEngine.com
#This script creates SFX binaries of the JDK for the specified platform
#Author Normen Hansen

#gather options
os="$1"
source="$2"
if [ -z "$1" ]; then
    echo "No platform supplied"
    echo "Specify a platform like macosx, windows-x86, linux-x64 and a source like /path/to/jdk/home"
    echo "If no source is specified, local/jdk-(platform) will be used"
    exit 1
fi
if [ -z "$2" ]; then
	source="local/jdk-$os"
fi
if [ ! -d "$source" ]; then
    echo "Source JDK directory $source was not found, specify another source folder as second parameter or copy the needed JDK to $source"
    exit 1
fi
unzipsfxname="unzipsfx/unzipsfx-$os"
if [ ! -f "$unzipsfxname" ]; then
	echo "No unzipsfx for platform $os found at $unzipsfxname, cannot continue"
    exit 1
fi
suffix="bin"
if [[ "$os" == *"windows"* ]]; then
	suffix="exe"
fi
name="jdk-$os.$suffix"

echo "Creating SFX JDK package $name for $os with source $source."

#code logic
rm -rf $name
cp -r $source ./jdk_tmp
cd jdk_tmp/jre
pack200 -J-Xmx1024m lib/rt.jar.pack.gz lib/rt.jar
rm -rf lib/rt.jar
cd ..
zip -9 -r -y ../jdk_tmp_sfx.zip .
cd ..
cat $unzipsfxname jdk_tmp_sfx.zip > $name
chmod +x $name
rm -rf jdk_tmp
rm -rf jdk_tmp_sfx.zip
