package com.flowercat.rfmouse.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.Toast;
import com.flowercat.rfmouse.R;
import com.flowercat.rfmouse.service.ServiceUtil;
import com.flowercat.rfmouse.util.SPUtil;
import android.os.Handler;

public class KeepAliveConfigActivity extends Activity {
    private RelativeLayout rl_bind_setting,rl_bind_noti,rl_alive_service;
	private Switch bind_noti,alive_service;
	private Handler mHandler;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                             WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.keep_alive);
		mHandler = new Handler();
		rl_bind_setting = findViewById(R.id.rl_bind_setting);
		rl_bind_noti = findViewById(R.id.rl_bind_noti);
		rl_alive_service = findViewById(R.id.rl_alive_service);
		
		bind_noti = findViewById(R.id.bind_noti);
		alive_service = findViewById(R.id.alive_service);
		
		rl_bind_setting.setOnClickListener(new OnClickListener(){

				@Override
				public void onClick(View v) {
					// 仅在安卓7.0及以上版本支持
					if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
						Toast.makeText(KeepAliveConfigActivity.this,"啊哦…不支持安卓7以下呢",Toast.LENGTH_SHORT).show();
						return ;
					}
					showGuideDialog();
				}

		});
		
		rl_bind_noti.setOnClickListener(new OnClickListener(){
				@Override
				public void onClick(View v) {
					try {
						Intent intent = new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS");
						// 添加标志，避免设置页面影响任务栈
						intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS | 
										Intent.FLAG_ACTIVITY_NO_HISTORY);
						startActivity(intent);

						// 延时启动提示 Activity
						mHandler.postDelayed(new Runnable() {
								@Override
								public void run() {
									
									/*
									Intent overlayIntent = new Intent(KeepAliveConfigActivity.this, OverlayActivity.class);
									// 确保透明 Activity 在新的任务栈中
									overlayIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | 
														   Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
									overlayIntent.putExtra("adv_text", "请找到 RF鼠标 并勾选");
									startActivity(overlayIntent);
									*/
									
									Toast.makeText(KeepAliveConfigActivity.this,"请找到 RF鼠标 并勾选",Toast.LENGTH_SHORT).show();
								}
							}, 500);
					} catch (ActivityNotFoundException e) {
						// 处理异常
					}
				}
			});
			
		rl_alive_service.setOnClickListener(new OnClickListener(){

				@Override
				public void onClick(View v) {
					if(alive_service.isChecked()){
						alive_service.setChecked(false);
					} else {
						alive_service.setChecked(true);
					}
		
				}
			});
			
		if(SPUtil.getString(SPUtil.KEY_ALIVE_SERVICE,"disable").equals("enable")){
			alive_service.setChecked(true);
		} else {
			alive_service.setChecked(false);
		}
			
		//保活服务
        alive_service.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					if(isChecked){
						SPUtil.putString(SPUtil.KEY_ALIVE_SERVICE,"enable");
						ServiceUtil.startKeepAliveServices(KeepAliveConfigActivity.this);
					} else {
						SPUtil.putString(SPUtil.KEY_ALIVE_SERVICE,"disable");
						ServiceUtil.stopKeepAliveServices(KeepAliveConfigActivity.this);
					}
				}
			});
		
			
		// 设置Switch为不可点击，只能通过RelativeLayout点击
		bind_noti.setClickable(false);
		bind_noti.setFocusable(false);
		
 
    }
	
	
	// 提示用户开启通知栏快捷方式
    private void showGuideDialog() {

		AlertDialog.Builder builder = new AlertDialog.Builder(this);

		builder
			.setTitle("添加快捷方式")
			.setMessage("请按照以下步骤将应用添加到快速设置：\n\n" +
						"1. 完全展开通知栏\n" +
						"2. 点击编辑按钮（一般为铅笔图标）\n" +
						"3. 找到RF鼠标的应用图标\n" +
						"4. 拖动到快速设置面板中，要下拉通知栏后可见。")
			.setPositiveButton("直接打开设置", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					openQuickSettings();
				}
			})
			.setNegativeButton("知道了", null);

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
	
	private void openQuickSettings() {
		try {
			// 尝试直接打开快速设置编辑界面
			Intent intent = new Intent();
			intent.setClassName("com.android.systemui", 
									"com.android.systemui.qs.QSEditActivity");
			startActivity(intent);
		} catch (Exception e) {
			// 备用方案：打开系统设置
				Toast.makeText(KeepAliveConfigActivity.this,"抱歉，请手动下滑通知栏",Toast.LENGTH_SHORT).show();
		}
	}
	
	
	private boolean notificationListenerEnable() {
		boolean enable = false;
		String packageName = getPackageName();
		String flat= Settings.Secure.getString(getContentResolver(),"enabled_notification_listeners");
		if (flat != null) {
			enable= flat.contains(packageName);
		}
		return enable;
	}
	
	
	private boolean gotoNotificationAccessSetting(Context context) {
		try {
			Intent intent = new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS");
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			context.startActivity(intent);
			return true;
		} catch(ActivityNotFoundException e) {
			try {
				Intent intent = new Intent();
				intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				ComponentName cn = new ComponentName("com.android.settings","com.android.settings.Settings$NotificationAccessSettingsActivity");
				intent.setComponent(cn);
				intent.putExtra(":settings:show_fragment", "NotificationAccessSettings");
				context.startActivity(intent);
				return true;
			} catch(Exception ex) {
				ex.printStackTrace();
			}
			return false;
		}
	}
	
	


	@Override
	protected void onResume() {
		super.onResume();
		
		if(notificationListenerEnable()){
			bind_noti.setChecked(true);
		} else {
			bind_noti.setChecked(false);
		}
		
	}
	
	
	@Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
        overridePendingTransition(R.anim.slide_in,R.anim.slide_out);
    }
	
    
}
