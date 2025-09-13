package com.flowercat.rfmouse.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.view.KeyEvent;
import android.widget.Toast;
import com.flowercat.rfmouse.util.NetworkUtils;
import com.flowercat.rfmouse.util.RootShellManager;
import com.flowercat.rfmouse.util.SPUtil;
import java.io.IOException;
import com.flowercat.rfmouse.MouseMainActivity;

public class BootReceiver extends BroadcastReceiver{
       
    @Override
    public void onReceive(Context context, Intent intent) {
		
		//开启WiFi
		if(SPUtil.getBoolean(SPUtil.KEY_WIFI_ON_BOOT, false)){
			try {
				NetworkUtils.setWifiEnabled(true);
			} catch (SecurityException e) {} 
			Toast.makeText(context, "wifi开启", Toast.LENGTH_LONG).show();
		}
		
		//关闭键盘灯
		if(SPUtil.getBoolean(SPUtil.KEY_KEYBOARD_LIGHT, true)){
			try {
				RootShellManager.getInstance().setBacklight(false);
			} catch (SecurityException e) {} catch (IOException e) {} catch (InterruptedException e) {}
			Toast.makeText(context, "屏蔽键盘灯", Toast.LENGTH_LONG).show();
		}
		
        //开机启动
		Intent intent1 = new Intent(context, MouseMainActivity.class);
		intent1.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
		context.startActivity(intent1);
        
    }
    
    
}
