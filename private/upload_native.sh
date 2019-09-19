#!/bin/bash
set -euo pipefail

# This shell script runs only during continuous integration at TravisCI.
# Its purpose is to commit native libraries to the project's GitHub repo.

function native_changes_common() {
    echo " - Configuring GIT user"
    git config --global user.email "travis-ci"
    git config --global user.name "travis-ci"
    echo " - Decrypting private key"
    openssl aes-256-cbc -K $encrypted_f0a0b284e2e8_key -iv $encrypted_f0a0b284e2e8_iv -in private/key.enc -out "$HOME/.ssh/id_rsa" -d
    chmod 600 "$HOME/.ssh/id_rsa"
    # ls jme3-bullet-native/build/libs/bulletjme/shared/
    # md5 -r jme3-bullet-native/libs/native/osx/x86/libbulletjme.dylib jme3-bullet-native/build/libs/bulletjme/shared/mac32/libbulletjme.dylib
    echo " - Pushing natives onto branch $TRAVIS_BRANCH"
    git checkout "$TRAVIS_BRANCH"
}

echo " - Determine which native libraries changed, if any"
NATIVE_CHANGES_BULLET="$(git diff --name-only -- jme3-bullet-native/libs/native/)"
NATIVE_CHANGES_ANDROID_BULLET="$(git diff --name-only -- jme3-bullet-native-android/libs/)"
NATIVE_CHANGES_ANDROID_NATIVES="$(git diff --name-only -- jme3-android-native/libs/)"

if [ "$NATIVE_CHANGES_BULLET" != "" ]; then
    echo " - Found changes in jme3-bullet-native"
    native_changes_common
    git add -v -- jme3-bullet-native/libs/native/
    git status
    echo " - Commit changes in jme3-bullet-native"
    git commit -v -m "[ci skip] bullet: update $TRAVIS_OS_NAME natives"
    git pull -q --rebase
    echo " - Push changes in jme3-bullet-native"
    git push git@github.com:jMonkeyEngine/jmonkeyengine.git
else
    echo " - No changes in jme3-bullet-native"
fi
if [ "$NATIVE_CHANGES_ANDROID_BULLET" != "" ]; then
    echo " - Found changes in jme3-bullet-native-android"
    native_changes_common
    git add -v -- jme3-bullet-native-android/libs/
    git status
    echo " - Commit changes in jme3-bullet-native-android"
    git commit -v -m "[ci skip] android bullet: update natives"
    git pull -q --rebase
    echo " - Push changes in jme3-bullet-native-android"
    git push git@github.com:jMonkeyEngine/jmonkeyengine.git
else
    echo " - No changes in jme3-bullet-native-android"
fi
if [ "$NATIVE_CHANGES_ANDROID_NATIVES" != "" ]; then
    echo " - Found changes in jme3-android-native"
    native_changes_common
    git add -v -- jme3-android-native/libs/
    git status
    echo " - Commmit changes in jme3-android-native"
    git commit -v -m "[ci skip] android: update natives"
    git pull -q --rebase
    echo " - Push changes in jme3-android-native"
    git push git@github.com:jMonkeyEngine/jmonkeyengine.git
else
    echo " - No changes in jme3-android-native"
fi
