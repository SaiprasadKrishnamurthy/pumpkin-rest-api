#!/bin/sh
export RELEASE_NAME=$1
export RELEASE_VERSION=$2
>payload.txt
for file in `find /opt/CSCOlumos/lib -name "*.jar" | grep -v "third-party"`
do
props=`unzip -p $file META-INF/maven/*.properties`
echo $props >> payload.txt
done
curl -v -X PUT "http://10.126.219.143:9990/release-dump?name=$RELEASE_NAME&version=$RELEASE_VERSION" --header "Content-Type: text/plain" --data-binary "@`pwd`/payload.txt"

