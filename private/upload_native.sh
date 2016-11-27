#!/bin/sh
set -e
# NATIVE_CHANGES="$(git diff-tree --name-only "$TRAVIS_COMMIT" -- jme3-bullet-native/)"
# if [ "$NATIVE_CHANGES" != "" ]; then
    git config --global user.email "travis-ci"
    git config --global user.name "travis-ci"
    # ./gradlew --no-daemon -PbuildNativeProjects=true :jme3-bullet-native:assemble
    openssl aes-256-cbc -K $encrypted_f0a0b284e2e8_key -iv $encrypted_f0a0b284e2e8_iv -in private/key.enc -out "$HOME/.ssh/id_ecdsa" -d
    chmod 600 "$HOME/.ssh/id_ecdsa"
    ls -l "$HOME/.ssh/id_ecdsa"
    ssh -v -T git@github.com
    # git checkout -q "$TRAVIS_BRANCH"
    # git add -- jme3-bullet-native/libs/native/
    # git commit -m "[ci skip] bullet: update $TRAVIS_OS_NAME natives"
    # git pull -q --rebase
    # git push git@github.com:jMonkeyEngine/jmonkeyengine.git
# fi
