package com.flowercat.rfmouse.service;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import java.util.ArrayList;
import java.util.List;

public  class ServiceUtil
{
	
	//服务是否被开启
	public static boolean isServiceRunning(Context context,String serviceName) {
        ActivityManager myManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        ArrayList<ActivityManager.RunningServiceInfo> runningService = (ArrayList<ActivityManager.RunningServiceInfo>) myManager.getRunningServices(Integer.MAX_VALUE);
        for (int i = 0; i < runningService.size(); i++) {
            if (runningService.get(i).service.getClassName().toString().equals(serviceName)) {
                return true;
            }
        }
        return false;
    }	
	
	
	//将应用提到前台
	public static void setTopApp(Context context) {
        try {
            if (!isRunningForeground(context)) {
                /**获取ActivityManager*/
                ActivityManager activityManager = (ActivityManager) context.getSystemService(context.ACTIVITY_SERVICE);
                /**获得当前运行的task(任务)*/
                List<ActivityManager.RunningTaskInfo> taskInfoList = activityManager.getRunningTasks(100);
                for (ActivityManager.RunningTaskInfo taskInfo : taskInfoList) {
                    /**找到本应用的 task，并将它切换到前台*/
                    if (taskInfo.topActivity.getPackageName().equals(context.getPackageName())) {
                        activityManager.moveTaskToFront(taskInfo.id, ActivityManager.MOVE_TASK_WITH_HOME);
                        break;
                    }
                }
            }
        }catch (Exception e){
			e.printStackTrace();
        }
    }
	
	
	//检测运行在前台的服务
	public static boolean isRunningForeground(Context context) {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> appProcessInfoList = activityManager.getRunningAppProcesses();
        /**枚举进程*/
        for (ActivityManager.RunningAppProcessInfo appProcessInfo : appProcessInfoList) {
            if (appProcessInfo.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                if (appProcessInfo.processName.equals(context.getApplicationInfo().processName)) {
                    return true;
                }
            }
        }
        return false;
    }
	
	
	public static void startKeepAliveServices(Context ctx) {
		//开启了就不重复了
		if(isAliveServiceRunning(ctx)){
			return;
		}
        // 启动保活服务
        Intent serviceIntent = new Intent(ctx, CommonService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ctx.startForegroundService(serviceIntent);
        } else {
            ctx.startService(serviceIntent);
        }
    }

	
	public static void stopKeepAliveServices(Context ctx) {
        // 停止保活服务
        Intent serviceIntent = new Intent(ctx, CommonService.class);  
        ctx.stopService(serviceIntent);
    }
	
	
	public static boolean isAliveServiceRunning(Context ctx){
		return isServiceRunning(ctx,"com.flowercat.rfmouse.service.CommonService");
	}
	
	
}
