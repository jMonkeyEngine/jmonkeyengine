#!/bin/bash
set -euo pipefail

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

echo " - Checking if natives changed in commit $TRAVIS_COMMIT.."
NATIVE_CHANGES_BULLET="$(git diff-tree --name-only "$TRAVIS_COMMIT" -- jme3-bullet-native/)"
NATIVE_CHANGES_ANDROID_BULLET="$(git diff-tree --name-only "$TRAVIS_COMMIT" -- jme3-bullet-native-android/)"
NATIVE_CHANGES_ANDROID_NATIVES="$(git diff-tree --name-only "$TRAVIS_COMMIT" -- jme3-android-native/)"

if [ "$NATIVE_CHANGES_BULLET" != "" ]; then
    native_changes_common
    git add -v -- jme3-bullet-native/libs/native/
    git commit -v -m "[ci skip] bullet: update $TRAVIS_OS_NAME natives"
    git pull -q --rebase
    git push git@github.com:jMonkeyEngine/jmonkeyengine.git
fi
if [ "$NATIVE_CHANGES_ANDROID_BULLET" != "" ]; then
    native_changes_common
    git add -v -- jme3-bullet-native-android/libs/
    git commit -v -m "[ci skip] android bullet: update natives"
    git pull -q --rebase
    git push git@github.com:jMonkeyEngine/jmonkeyengine.git
fi
if [ "$NATIVE_CHANGES_ANDROID_NATIVES" != "" ]; then
    native_changes_common
    git add -v -- jme3-android-native/libs/
    git commit -v -m "[ci skip] android: update natives"
    git pull -q --rebase
    git push git@github.com:jMonkeyEngine/jmonkeyengine.git
fi
