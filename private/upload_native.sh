#!/bin/bash
set -euo pipefail
NATIVE_CHANGES="$(git diff-tree --name-only "$TRAVIS_COMMIT" -- jme3-bullet-native/)"
if [ "$NATIVE_CHANGES" != "" ]; then
    git config --global user.email "travis-ci"
    git config --global user.name "travis-ci"
    openssl aes-256-cbc -K $encrypted_f0a0b284e2e8_key -iv $encrypted_f0a0b284e2e8_iv -in private/key.enc -out "$HOME/.ssh/id_rsa" -d
    chmod 600 "$HOME/.ssh/id_rsa"
    git checkout "$TRAVIS_BRANCH"
    git add -v -- jme3-bullet-native/libs/native/
    git commit -v -m "[ci skip] bullet: update $TRAVIS_OS_NAME natives"
    git pull -q --rebase
    git push git@github.com:jMonkeyEngine/jmonkeyengine.git
fi
