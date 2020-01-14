#!/bin/bash

source app.conf

"$DEPLOY_DIR/bin/jsvc_daemon.sh" --proc-name "$PROC_NAME" --pid-file "$PID_FILE" --user "$DAEMON_USER" $1
