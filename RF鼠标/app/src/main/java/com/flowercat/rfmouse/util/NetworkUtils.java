package com.flowercat.rfmouse.util;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;

public class NetworkUtils {

    private static final String TAG = "NetworkUtils";
	
	private static Context context;


	/**
	 * 单例模式的初始化方法，必须在Application中调用
	 * @param context Application的Context
	 */
	public static void init(Context mCtx) {
		context = mCtx;
	}

	

    /**
     * 打开或关闭 Wi-Fi，优先使用非 Root 方法，失败则尝试 Root
     * @param context Context
     * @param enabled true为开启，false为关闭
     */
    public static void setWifiEnabled(boolean enabled) {
        WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifiManager == null) {
            return;
        }

        if (wifiManager.setWifiEnabled(enabled)) {
            Log.d(TAG, "非Root方法设置Wi-Fi成功");
            return;
        }
        Log.e(TAG, "非Root方法设置Wi-Fi失败，尝试使用Root权限");
        executeShellCommand("svc wifi " + (enabled ? "enable" : "disable"));
    }

    /**
     * 打开或关闭移动数据，优先使用非 Root 方法（通常会失败），失败则尝试 Root
     * @param context Context
     * @param enabled true为开启，false为关闭
     */
    public static void setMobileDataEnabled(boolean enabled) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager == null) {
            return;
        }

        try {
            Method setMobileDataEnabledMethod = connectivityManager.getClass().getDeclaredMethod("setMobileDataEnabled", boolean.class);
            setMobileDataEnabledMethod.setAccessible(true);
            setMobileDataEnabledMethod.invoke(connectivityManager, enabled);
            Log.d(TAG, "非Root方法设置移动数据成功");
        } catch (Exception e) {
            Log.e(TAG, "非Root方法设置移动数据失败，尝试使用Root权限", e);
            executeShellCommand("svc data " + (enabled ? "enable" : "disable"));
        }
    }

    /**
     * 打开或关闭热点，优先使用非 Root 方法，失败则尝试 Root
     * @param context Context
     * @param enabled true为开启，false为关闭
     */
    public static void setHotspotEnabled(boolean enabled) {
        WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifiManager == null) {
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Android O 及以上版本需要跳转到系统设置
            Log.e(TAG, "Android 8.0及以上版本无法直接设置热点，请引导用户跳转到设置");
            Intent intent = new Intent(Settings.ACTION_WIRELESS_SETTINGS);
            context.startActivity(intent);
        } else {
            // Android 7.0 及以下版本尝试反射
            try {
                Method setWifiApEnabledMethod = wifiManager.getClass().getMethod("setWifiApEnabled", WifiManager.class, boolean.class);
                setWifiApEnabledMethod.setAccessible(true);
                if ((boolean) setWifiApEnabledMethod.invoke(wifiManager, null, enabled)) {
                    Log.d(TAG, "非Root方法设置热点成功");
                    return;
                }
            } catch (Exception e) {
                Log.e(TAG, "非Root方法设置热点失败，尝试使用Root权限", e);
            }
            executeShellCommand("svc wifi tether " + (enabled ? "enable" : "disable"));
        }
    }

    /**
     * 判断 Wi-Fi 是否开启
     */
    public static boolean isWifiEnabled() {
        WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        return wifiManager != null && wifiManager.isWifiEnabled();
    }

    /**
     * 判断移动数据是否开启
     */
    public static boolean isMobileDataEnabled() {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager == null) {
            return false;
        }
        try {
            Method getMobileDataEnabledMethod = connectivityManager.getClass().getDeclaredMethod("getMobileDataEnabled");
            getMobileDataEnabledMethod.setAccessible(true);
            return (boolean) getMobileDataEnabledMethod.invoke(connectivityManager);
        } catch (Exception e) {
            Log.e(TAG, "获取移动数据状态失败", e);
            return false;
        }
    }

    /**
     * 判断热点是否开启
     */
    public static boolean isHotspotEnabled() {
        WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifiManager == null) {
            return false;
        }
        try {
            int state = (Integer) wifiManager.getClass().getMethod("getWifiApState").invoke(wifiManager);
            return state == 13; // 13 代表热点已开启 (WIFI_AP_STATE_ENABLED)
        } catch (Exception e) {
            Log.e(TAG, "获取热点状态失败", e);
            return false;
        }
    }

    /**
     * 使用 Root 权限执行 Shell 命令
     * @param command Shell命令
     */
    private static void executeShellCommand(String command) {
        try {
			RootShellManager.getInstance().executeCommand(command);
		} catch (SecurityException e) {} catch (IOException e) {}
    }
	
	
	/**
	 * 打开或关闭飞行模式，优先尝试非Root方法，失败则尝试Root权限
	 * @param enabled true为开启，false为关闭
	 */
	public static void setAirplaneModeEnabled(boolean enabled) {
		// 尝试非Root方法 (在某些旧版系统可能有效，但Android 4.2+通常无效)
		try {
			Settings.Global.putInt(context.getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, enabled ? 1 : 0);
			Intent intent = new Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED);
			intent.putExtra("state", enabled);
			context.sendBroadcast(intent);
			Log.d(TAG, "非Root方法设置飞行模式成功");
			return;
		} catch (Exception e) {
			Log.e(TAG, "非Root方法设置飞行模式失败，尝试使用Root权限", e);
		}

		// 非Root方法失败，尝试Root权限
		executeShellCommand("settings put global airplane_mode_on " + (enabled ? "1" : "0"));
		executeShellCommand("am broadcast -a android.intent.action.AIRPLANE_MODE --ez state " + enabled);
	}

	/**
	 * 判断飞行模式是否开启
	 * @return true为开启，false为关闭
	 */
	public static boolean isAirplaneModeEnabled() {
		try {
			return Settings.Global.getInt(context.getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, 0) != 0;
		} catch (Exception e) {
			Log.e(TAG, "获取飞行模式状态失败", e);
			return false;
		}
	}
	
	
	//切换WiFi状态
	public static void switchWiFi(){
		if(isWifiEnabled()){
			setWifiEnabled(false);
		} else {
			setWifiEnabled(true);
		}
	}
	
	//切换移动网络状态
	public static void switchMobileData(){
		if(isMobileDataEnabled()){
			setMobileDataEnabled(false);
		} else {
			setMobileDataEnabled(true);
		}
	}
	
	//切换热点状态
	public static void switchHotSpot(){
		if(isHotspotEnabled()){
			setHotspotEnabled(false);
		} else {
			setHotspotEnabled(true);
		}
	}
	
	
}
