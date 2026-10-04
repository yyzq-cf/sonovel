#!/bin/sh
# Docker entrypoint: 首次启动时把默认 config.ini 拷到挂载目录，然后启动应用
set -e

# 如果用户挂载了 /data 目录，把配置和默认规则放进去
if [ -d /data ]; then
  # 首次启动：拷贝默认 config.ini
  if [ ! -f /data/config.ini ]; then
    cp /sonovel/config.ini /data/config.ini
    echo "[entrypoint] 初始化配置文件 → /data/config.ini"
  fi
  # 拷贝规则文件
  if [ ! -d /data/rules ]; then
    cp -r /sonovel/rules /data/rules
    echo "[entrypoint] 初始化规则文件 → /data/rules"
  fi
  # 创建下载目录
  mkdir -p /data/downloads
  CONFIG_FILE=/data/config.ini
else
  CONFIG_FILE=/sonovel/config.ini
fi

exec java $JAVA_OPTS \
  -XX:+UseZGC \
  -XX:+ZGenerational \
  -Dfile.encoding=UTF-8 \
  -Duser.timezone=GMT+08 \
  -Dconfig.file="$CONFIG_FILE" \
  ${AUTH_USERNAME:+-Dauth.username="$AUTH_USERNAME"} \
  ${AUTH_PASSWORD:+-Dauth.password="$AUTH_PASSWORD"} \
  -jar /sonovel/app.jar
