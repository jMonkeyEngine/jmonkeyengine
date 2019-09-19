#!/bin/bash
set -euo pipefail

# This shell script runs only during continuous integration at TravisCI.
# Its purpose is to commit native libraries to the project's GitHub repo.

echo " - Determine which native libraries changed, if any"
NATIVE_CHANGES_ANDROID="$(git diff --name-only -- jme3-android-native/libs jme3-bullet-native-android/libs)"
NATIVE_CHANGES_DESKTOP="$(git diff --name-only -- jme3-bullet-native/libs)"

if [ "$NATIVE_CHANGES_ANDROID$NATIVE_CHANGES_DESKTOP" != "" ]; then
    echo " - Configure git user"
    git config --global user.email "travis-ci"
    git config --global user.name "travis-ci"
    echo " - Decrypt private key"
    openssl aes-256-cbc -K $encrypted_f0a0b284e2e8_key -iv $encrypted_f0a0b284e2e8_iv -in private/key.enc -out "$HOME/.ssh/id_rsa" -d
    chmod 600 "$HOME/.ssh/id_rsa"
    # md5 -r jme3-bullet-native/libs/native/osx/x86/libbulletjme.dylib jme3-bullet-native/build/libs/bulletjme/shared/mac32/libbulletjme.dylib
    echo " - Checkout branch $TRAVIS_BRANCH"
    git checkout "$TRAVIS_BRANCH"
    echo " - Stage native libraries"
    git add -v -- jme3-android-native/libs jme3-bullet-native/libs jme3-bullet-native-android/libs
    git status
    echo " - Commit native libraries"
    if [ "$NATIVE_CHANGES_ANDROID" != "" ]; then
        git commit -v -m "[ci skip] update android natives"
    else
        git commit -v -m "[ci skip] bullet: update $TRAVIS_OS_NAME natives"
    fi
    echo " - Rebase changes"
    git pull -q --rebase
    echo " - Push changes"
    git push git@github.com:jMonkeyEngine/jmonkeyengine.git
else
    echo " - No changes to native libraries"
fi