package com.flowercat.rfmouse.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.Window;
import android.widget.Button;
import android.widget.LinearLayout;
import com.flowercat.rfmouse.R;
import com.flowercat.rfmouse.ui.HelloScreen;
import com.flowercat.rfmouse.util.AnimUtils;


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
					confirmGuide();
				}
			
		});
        
    }
    
	
	
	
	public void confirmGuide(){
		// 向导完成
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("是否进入初次使用向导？")
			.setMessage("将进行一些基础的设置。")
			.setPositiveButton("确定", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					finish();

					// Code for the back button action
					Intent intentr = new Intent(HelloScreen.this, WizardActivity.class);
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
			})
			.setNegativeButton("不了", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					finish();
				}
			});
			
		// 1. 先创建对话框（不能直接用builder，需先show()获取实例）
		AlertDialog dialog = builder.show();

		// 2. 获取“确认”按钮（DialogInterface.BUTTON_POSITIVE）并设置样式
		Button positiveBtn = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
		if (positiveBtn != null) {
			LinearLayout.LayoutParams positiveParams = (LinearLayout.LayoutParams) positiveBtn.getLayoutParams();
			positiveParams.leftMargin = dp2px(this, 30); // 确认按钮左边距30dp（与取消按钮隔开）
			positiveBtn.setLayoutParams(positiveParams);
			positiveBtn.setBackgroundResource(R.drawable.button_background_selector); // 应用选择器
			//positiveBtn.setPadding(30, 10, 30, 10); // 可选：调整按钮内边距，避免边框紧贴文字

		}

		// 3. 获取“取消”按钮（DialogInterface.BUTTON_NEGATIVE）并设置样式
		Button negativeBtn = dialog.getButton(DialogInterface.BUTTON_NEGATIVE);
		if (negativeBtn != null) {
			LinearLayout.LayoutParams negativeParams = (LinearLayout.LayoutParams) negativeBtn.getLayoutParams();
			negativeParams.rightMargin = dp2px(this, 10); // 取消按钮右边距10dp
			negativeBtn.setLayoutParams(negativeParams);
			negativeBtn.setBackgroundResource(R.drawable.button_background_selector); // 应用选择器
			//negativeBtn.setPadding(30, 10, 30, 10); // 可选：同确认按钮，保持样式一致
		}
	}
	
	
	// 工具方法：dp转px（避免不同分辨率设备间距不一致）
	private int dp2px(Context context, float dpValue) {
		final float scale = context.getResources().getDisplayMetrics().density;
		return (int) (dpValue * scale + 0.5f); // 四舍五入避免精度丢失
	}
}
