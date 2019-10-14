#!/bin/bash

source app.conf

[ ! -d "$DEPLOY_DIR" ] && mkdir "$DEPLOY_DIR"
[ ! -d "$DEPLOY_DIR/bin" ] && mkdir "$DEPLOY_DIR"/bin
[ ! -d "$DEPLOY_DIR/config" ] && mkdir "$DEPLOY_DIR"/config
[ ! -d "$DEPLOY_DIR/inbound" ] && mkdir "$DEPLOY_DIR"/inbound
[ ! -d "$DEPLOY_DIR/lib" ] && mkdir "$DEPLOY_DIR"/lib
[ ! -d "$DEPLOY_DIR/webapps" ] && mkdir "$DEPLOY_DIR"/webapps
[ ! -d "$DEPLOY_DIR/logs" ] && mkdir "$DEPLOY_DIR"/logs
[ ! -d "$DEPLOY_DIR/temp" ] && mkdir "$DEPLOY_DIR"/temp
[ ! -d "$DEPLOY_DIR/work" ] && mkdir "$DEPLOY_DIR"/work

rm -rf "${DEPLOY_DIR:?}"/bin/*
rm -rf "${DEPLOY_DIR:?}"/config/*
#rm -rf "${DEPLOY_DIR:?}"/inbound/*
rm -rf "${DEPLOY_DIR:?}"/lib/*
rm -rf "${DEPLOY_DIR:?}"/webapps/*
cp -pR "$REPO_DIR"/app/bin/* "$DEPLOY_DIR"/bin
cp -pR "$REPO_DIR"/app/config/* "$DEPLOY_DIR"/config
cp -pR "$REPO_DIR"/app/inbound/* "$DEPLOY_DIR"/inbound
cp -pR "$REPO_DIR"/app/lib/* "$DEPLOY_DIR"/lib
cp -pR "$REPO_DIR"/app/webapps/* "$DEPLOY_DIR"/webapps

echo "Done."
