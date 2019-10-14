#!/bin/bash

source app.conf

"$DEPLOY_DIR"/bin/jsvc_daemon.sh $1
