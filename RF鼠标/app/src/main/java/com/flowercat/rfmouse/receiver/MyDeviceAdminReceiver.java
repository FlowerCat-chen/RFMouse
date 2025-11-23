package com.flowercat.rfmouse.receiver;


import android.app.admin.DeviceAdminReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;
import android.util.Log;

public class MyDeviceAdminReceiver extends DeviceAdminReceiver {

    private static final String TAG = "MyDeviceAdminReceiver";

    @Override
    public void onEnabled(Context context, Intent intent) {
        super.onEnabled(context, intent);
        Toast.makeText(context, "设备管理员权限已激活", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "设备管理员权限已激活");
    }

    @Override
    public CharSequence onDisableRequested(Context context, Intent intent) {
        // 当用户尝试禁用设备管理员时会调用此方法
        // 你可以在这里返回一个提示信息，告诉用户禁用后会失去哪些功能
        return "禁用设备管理员将导致无法使用锁屏功能。确定禁用吗？";
    }

    @Override
    public void onDisabled(Context context, Intent intent) {
        super.onDisabled(context, intent);
        Toast.makeText(context, "设备管理员权限已禁用", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "设备管理员权限已禁用");
    }

    @Override
    public void onLockTaskModeEntering(Context context, Intent intent, String pkg) {
        super.onLockTaskModeEntering(context, intent, pkg);
        Log.d(TAG, "进入锁定任务模式");
    }

    @Override
    public void onLockTaskModeExiting(Context context, Intent intent) {
        super.onLockTaskModeExiting(context, intent);
        Log.d(TAG, "退出锁定任务模式");
    }

    // 你可以根据需要覆盖更多的方法
}
