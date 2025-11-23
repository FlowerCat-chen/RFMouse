package com.flowercat.rfmouse.util;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.app.Activity;
import android.widget.Toast;
import com.flowercat.rfmouse.ui.PermissionRequestActivity;
import com.flowercat.rfmouse.receiver.MyDeviceAdminReceiver;
import android.app.PendingIntent;

public class DeviceAdminUtil {
	
	public static final int REQUEST_CODE_DEVICE_ADMIN = 1001;
    
    public static DevicePolicyManager mDpm;
    public static ComponentName mDeviceAdminReceiver;
	public static DeviceAdminUtil deviceUtil;
	public static Context mCtx;

	//设备管理器管理初始化
    public static void init(Context ctx){
		mCtx = ctx;
		//权限申请
		mDpm = (DevicePolicyManager) ctx.getSystemService(Context.DEVICE_POLICY_SERVICE);
        mDeviceAdminReceiver = new ComponentName(ctx, MyDeviceAdminReceiver.class);
	}
    
	// --- 权限检查方法 ---
	//检查设备管理器是否激活
    public static boolean isDeviceAdminActive() {
        return mDpm.isAdminActive(mDeviceAdminReceiver);
    }
    
	// --- 权限申请方法 ---
    public static void requestDeviceAdminPermission(Activity host) {
        //setAdviseText("正在请求设备管理员权限...");
        Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
        intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, mDeviceAdminReceiver);
        intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "需要管理员权限锁屏的说…");
        host.startActivityForResult(intent, REQUEST_CODE_DEVICE_ADMIN);
    }
	
	//锁屏
	public static void lockScreen() {
		
		if(!isDeviceAdminActive()){
			Toast.makeText(mCtx, "请先授权设备管理员权限", Toast.LENGTH_LONG).show();
			// Code for the back button action
			Intent intentr = new Intent(mCtx, PermissionRequestActivity.class);
			intentr.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);

			PendingIntent pendingIntent =
				PendingIntent.getActivity(mCtx, 0, intentr, 0);
			try {
				pendingIntent.send();
			} catch (PendingIntent.CanceledException e) {
				mCtx.startActivity(intentr);
				e.printStackTrace();
			}	
			
			return;
		}
		
		mDpm.lockNow();
	}
	
	
	public static void cancelDeviceAdmin(){
		if(!isDeviceAdminActive()){
			return;
		}

		try {
			mDpm.removeActiveAdmin(mDeviceAdminReceiver);
		} catch (Exception e){
			e.printStackTrace();
		}
	}
	
	
	
}
