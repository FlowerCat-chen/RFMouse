#!/system/bin/sh

SOURCE_PATH="/sdcard/Android/data/com.flowercat.rfmouse/files/Server.apk"
TARGET_DIR="/data/local/tmp/RFMouse"
TARGET_APK="/data/local/tmp/RFMouse/Server.apk"
DALVIK_CACHE_DIR="/data/local/tmp/RFMouse/dalvik-cache"

echo "info: rfmouse_start.sh begin"

recreate_tmp() {
  echo "info: /data/local/tmp is possible broken, recreating..."
  rm -rf /data/local/tmp
  mkdir -p /data/local/tmp
  if [ $? -ne 0 ]; then
    broken_tmp
  fi
}

broken_tmp() {
  echo "fatal: /data/local/tmp is broken, please try reboot the device or manually recreate it..."
  exit 1
}

ensure_directory() {
  if [ ! -d "$1" ]; then
    echo "info: creating directory $1"
    mkdir -p "$1"
    if [ $? -ne 0 ]; then
      echo "error: failed to create directory $1"
      return 1
    fi
  fi
  return 0
}

# 检查并创建 RFMouse 目录
echo "info: checking RFMouse directory"
if [ ! -d "$TARGET_DIR" ]; then
  echo "info: RFMouse directory does not exist, creating..."
  ensure_directory "$TARGET_DIR"
  if [ $? -ne 0 ]; then
    # 如果创建失败，可能是 tmp 目录损坏
    recreate_tmp
    ensure_directory "$TARGET_DIR"
    if [ $? -ne 0 ]; then
      broken_tmp
    fi
  fi
fi

# 创建 dalvik-cache 目录
echo "info: creating dalvik-cache directory"
ensure_directory "$DALVIK_CACHE_DIR"
if [ $? -ne 0 ]; then
  echo "error: failed to create dalvik-cache directory"
  exit 1
fi

# 复制 APK 文件
if [ -f "$SOURCE_PATH" ]; then
    # 检查目标文件是否已存在且无需更新
    if [ -f "$TARGET_APK" ]; then
        echo "info: Server.apk already exists at $TARGET_APK, skipping copy"
    else
        echo "info: attempt to copy Server.apk from $SOURCE_PATH to $TARGET_APK"
        rm -f "$TARGET_APK"

        cp "$SOURCE_PATH" "$TARGET_APK"
        res=$?
        if [ $res -ne 0 ]; then
          echo "warning: first copy failed, recreating tmp and retrying..."
          recreate_tmp
          ensure_directory "$TARGET_DIR"
          ensure_directory "$DALVIK_CACHE_DIR"
          cp "$SOURCE_PATH" "$TARGET_APK"
          
          res=$?
          if [ $res -ne 0 ]; then
            broken_tmp
          fi
        fi

        chmod 644 "$TARGET_APK"
        echo "info: Server.apk copied successfully"
    fi
else
    echo "error: Source Server.apk not found at $SOURCE_PATH"
    exit 1
fi


# 执行应用进程
if [ -f "$TARGET_APK" ]; then
  echo "info: starting server with app_process"
  
  # 设置环境变量并执行
  export ANDROID_DATA="/data/local/tmp/RFMouse"
  
  # 执行命令，使用 & 符号将进程放入后台
  app_process -Djava.class.path="$TARGET_APK" /system/bin --nice-name=flowermouse shellService.Main &
  PID=$!
  
  # 设置陷阱忽略 HUP 信号
  #trap "" HUP
  
  # 等待进程启动并检查状态
  sleep 2
  if [ -n "$PID" ]; then
    echo "info: RFMouseService started successfully with PID $PID"
  else
    echo "error: RFMouseService failed to start"
    exit 1
  fi
else
    echo "error: Target Server.apk not found at $TARGET_APK"
    exit 1
fi


exit 0

