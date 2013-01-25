#!/bin/sh
#(c) jMonkeyEngine.com
#Author Normen Hansen
set -e
rm -rf jdk-macosx.zip
cp -r local/jdk7u11-macosx ./jdk
zip -9 -r -y ./jdk-macosx.zip ./jdk
rm -rf jdk
