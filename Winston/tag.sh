#!/bin/sh

TAG="Winston_1.2.4"
BASE="http://avosouth.wr.usgs.gov/vhpsvn"
tag() {
        svn copy ${BASE}/${1}/trunk ${BASE}/${1}/tags/$TAG -m "Tagging $TAG"
}

tag Earthworm
tag Math
tag Net
tag Plot
tag Swarm
tag USGS
tag Util
tag VDX
tag Winston
