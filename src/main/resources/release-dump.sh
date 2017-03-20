#!/bin/sh
export RELEASE_ARTIFACT_LOCATION=http://10.104.119.201/prime/
RELEASE_ARTIFACT=`wget --spider --level=1 -nd -r -np $RELEASE_ARTIFACT_LOCATION 2>&1 | sed 's/.*PI/PI/' | grep 'ova$' | tail -1`
RELEASE_NAME='Prime Infrastructure'
RELEASE_VERSION=`echo $RELEASE_ARTIFACT | sed 's/[A-Za-z]*//g' | sed 's/[\-]*//g' | sed 's/[\,]*//g'  | rev | cut -c 2- | rev`

>payload.txt
for file in `find /opt/CSCOlumos/lib -name "*.jar" | grep -v "third-party"`
do
props=`unzip -p $file META-INF/maven/*.properties`
echo $props >> payload.txt
done
curl -v -X PUT "http://10.126.219.143:9990/release-dump?name=$RELEASE_NAME&version=$RELEASE_VERSION" --header "Content-Type: text/plain" --data-binary "@`pwd`/payload.txt"

