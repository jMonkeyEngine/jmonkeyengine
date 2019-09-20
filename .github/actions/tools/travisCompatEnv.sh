#!/bin/bash
#############################################
#   Helps Travis -> GithubActions migration.
#############################################

echo "GITHUB_REF $GITHUB_REF"
export GITHUB_TAG=`if [[ $GITHUB_REF == refs\/tags* ]]; then echo ${GITHUB_REF//refs\/tags\//}; fi`
export GITHUB_BRANCH=`if [[ $GITHUB_REF == refs\/heads* ]]; then echo ${GITHUB_REF//refs\/heads\//}; fi`

if [ "$GITHUB_TAG" != "" ];
then
    export TRAVIS_TAG="$GITHUB_TAG"
    export TRAVIS_BRANCH="$GITHUB_TAG"
else
    export TRAVIS_TAG=""
    export TRAVIS_BRANCH="$GITHUB_BRANCH"
fi

if [ "$GITHUB_EVENT_NAME" == "pull_request" ];
then
    export TRAVIS_PULL_REQUEST="$GITHUB_SHA"
fi

echo "GIHUB_TAG $GITHUB_TAG"
echo "GITHUB_BRANCH $GITHUB_BRANCH"
echo "TRAVIS_TAG $TRAVIS_TAG"
echo "TRAVIS_BRANCH $TRAVIS_BRANCH"
echo "GITHUB_EVENT_NAME $GITHUB_EVENT_NAME"
echo "TRAVIS_PULL_REQUEST $TRAVIS_PULL_REQUEST"
