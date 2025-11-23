package com.flowercat.rfmouse.service;

import android.app.Notification;
import android.app.Notification.Builder;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.os.Build;
import android.os.IBinder;
import com.flowercat.rfmouse.R;
import java.util.Objects;

public class CommonService extends Service {
	

	
    public CommonService() {
   
    }
 
    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
	
    @Override
    public void onCreate() {
        super.onCreate();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            showProcessNotification(this);
    	}
    }
 
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
             return START_STICKY;
    }
	
        @Override
    public void onDestroy() {
        super.onDestroy();
    }
	
   
	public final void showProcessNotification(Service service) {
        Builder builder;
        Object systemService = service.getSystemService("notification");
        Objects.requireNonNull(systemService, "null cannot be cast to non-null type android.app.NotificationManager");
        NotificationManager notificationManager = (NotificationManager) systemService;
        ApplicationInfo applicationInfo = service.getApplicationInfo();
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel notificationChannel = new NotificationChannel("鼠标保活", "debug…", 2);
            notificationChannel.setLockscreenVisibility(0);
            notificationManager.createNotificationChannel(notificationChannel);
            notificationChannel.setSound(null, null);
            notificationChannel.setVibrationPattern(null);
            notificationChannel.setShowBadge(false);
            notificationChannel.enableVibration(false);
            notificationChannel.enableLights(false);
            builder = new Builder(service.getApplicationContext(), "鼠标保活");
        } else {
            builder = new Builder(service.getApplicationContext());
        }
		//  Intent intent = new Intent();
		//  intent.setAction("com.kidguard360.intent.action.Settings");
		// intent.setPackage(service.getPackageName());
		//  intent.setComponent(new ComponentName(service.getPackageName(), "com.kidguard360.supertool.plugin.activity.SystemSettingsPluginActivity_AopProxy0"));
		// intent.setFlags(272637952);
        builder.setWhen(System.currentTimeMillis())
			.setOnlyAlertOnce(true)
			.setSound(null)
			.setAutoCancel(true)
			//.setContentIntent(PendingIntent.getActivity(service, 0, intent, 0))
			.setContentTitle("Alive")
			.setContentText("鼠标运行中…")
			.setSmallIcon(R.drawable.ic_launcher)
			.setVibrate(null)
			.setVisibility(0)
			.setDefaults(0)
			.setVisibility(-1)
			.setOngoing(true)
			.build();
        Notification build = builder.build();
        service.startForeground(1000202, build);
        notificationManager.notify(1000202, build);
    }
}
    
    


    
      
    
        
        
        
        
    
   
