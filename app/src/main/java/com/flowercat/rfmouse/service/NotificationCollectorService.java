package com.flowercat.rfmouse.service;

import android.app.Notification;
import android.view.View.*;
import android.widget.*;import android.view.*;import android.widget.LinearLayout;
import android.nfc.Tag;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.text.SpannableString;
import android.content.Intent;import android.content.*;
import android.widget.Toast;import java.util.List;import android.util.*;
import android.app.ActivityManager.RunningTaskInfo;
import android.app.ActivityManager.RunningAppProcessInfo;import java.util.ArrayList;import android.app.ActivityManager;
import java.lang.reflect.InvocationTargetException;


public class NotificationCollectorService extends NotificationListenerService {
   
    @Override
    public void onListenerConnected() {
        super.onListenerConnected();
    }
 
    @Override
    public void onListenerDisconnected() {
        super.onListenerDisconnected();
    }
    
 
    
    
    
	@Override
	public void onNotificationPosted(StatusBarNotification sbn) {
   
	}         


   



	@Override
	public void onNotificationRemoved(StatusBarNotification sbn) {
	
	
    }         
   
}
