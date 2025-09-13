package com.flowercat.rfmouse.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.Window;
import android.widget.LinearLayout;
import com.flowercat.rfmouse.R;
import com.flowercat.rfmouse.ui.HelloScreen;
import com.flowercat.rfmouse.util.AnimUtils;
import android.app.PendingIntent;


public class HelloScreen extends Activity {
	
	private LinearLayout animContainer;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

		requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.hello_miao);
		animContainer = findViewById(R.id.animContainer);
		
		//解决开发者模式中设置动画缩放为0导致的动画无效
		if(!AnimUtils.areAnimationsEnabled(this)){
			AnimUtils.forceEnableAllAnimators();
		}
		
        AnimUtils.animateView(animContainer,new AnimatorListenerAdapter(){
			    //动画结束
				public void onAnimationEnd(Animator animation) {
					SystemClock.sleep(1000);//等1秒
					finish();
					
					// Code for the back button action
					Intent intentr = new Intent(HelloScreen.this, PermissionRequestActivity.class);
					intentr.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);

					PendingIntent pendingIntent =
						PendingIntent.getActivity(HelloScreen.this, 0, intentr, 0);
					try {
						pendingIntent.send();
					} catch (PendingIntent.CanceledException e) {
						startActivity(intentr);
						e.printStackTrace();
					}	
					
				}
			
		});
        
    }
    
}
