package com.flowercat.rfmouse.ui;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;
import com.flowercat.rfmouse.MouseMainActivity;
import com.flowercat.rfmouse.R;
import com.flowercat.rfmouse.service.FlowerMouseService;
import com.flowercat.rfmouse.util.DeviceAdminUtil;
import com.flowercat.rfmouse.util.RootPermissionGranter;
import com.flowercat.rfmouse.util.RootShellManager;
import com.flowercat.rfmouse.util.SPUtil;
import com.flowercat.rfmouse.util.ScreenCaptureHelper;
import java.io.IOException;
import java.util.List;


public class PermissionRequestActivity extends BaseActivity {

    public static final int REQUEST_CODE_SCREEN_CAPTURE = 1002;
    private static final int REQUEST_CODE_WRITE_STORAGE = 1003;
    private static final int REQUEST_CODE_OVERLAY_PERMISSION = 1004;

    private ScreenCaptureHelper mScreenCapture;
	// Root 操作管理器实例
    private RootShellManager rootShellManager;
    private Handler mHandler;
    private boolean inAutoMode = false;
	private ProgressDialog progressDialog;
	private Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 注意：这里不再需要 setContentView，因为 BaseActivity 已经设置了
        // 如果你的子类需要额外的视图除了 ListView，你可以在 onCreate 后添加
        // 但对于只需要 ListView 的情况，这样写很简洁
        setAdviseText("权限申请界面");
        hideInfo();
        mHandler = new Handler();
        // 初始化截屏助手
        mScreenCapture = ScreenCaptureHelper.getInstance(this);
		// 获取 RootShellManager 的单例实例
        rootShellManager = RootShellManager.getInstance();
		// 初始化Handler用于UI更新
		
        handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {

				//取消加载条
				if (progressDialog != null && progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }

                switch (msg.what) {
					case 1:
						showPremSuccessDialog();
						break;
					case 2: 
						String error2 =(String) msg.getData().getString("error");
                        Toast.makeText(PermissionRequestActivity.this, error2, Toast.LENGTH_SHORT).show();
                        break;
					case 3: 
						String error =(String) msg.getData().getString("error");
                        showPremErrorDialog(error);
                        break;
						
                }
            }
        };
		
		
    }

	
	
    @Override
    protected void initializeSections() {
		
		addSection("一键申请(root)", new FunctionExecutor() {
				@Override
				public void execute() {
					if(rootShellManager.isRootAvailable()){
						startRootAutoPerm();
					} else {
						showToastOnMainThread("root不可用，请重新点击");
					}
				}
			}, Color.BLACK, Color.parseColor("#FFCFE682"));
		
		
        // 使用 addSection 辅助方法来添加列表项和对应的动作
        addSection("一键申请", new FunctionExecutor() {
				@Override
				public void execute() {
					inAutoMode = true;
					checkAndRequestPermissions();
				}
			});

        addSection("读写权限申请", new FunctionExecutor() {
				@Override
				public void execute() {
					inAutoMode = false;
					if (checkReadWritePermission()) {
						Toast.makeText(PermissionRequestActivity.this, "读写权限已获取", Toast.LENGTH_LONG).show();
					} else {
						requestReadWritePermission();
					}
				}
			}, Color.BLACK, Color.parseColor("#FFFFE082"));
			
		

        addSection("悬浮窗权限申请", new FunctionExecutor() {
				@Override
				public void execute() {
					inAutoMode = false;
					if (checkOverlayPermission()) {
						Toast.makeText(PermissionRequestActivity.this, "悬浮窗权限已获取", Toast.LENGTH_LONG).show();
					} else {
						requestOverlayPermission();
					}
				}
			}, Color.BLACK, Color.parseColor("#FFF48FB1"));

        addSection("设备管理员申请", new FunctionExecutor() {
				@Override
				public void execute() {
					inAutoMode = false;
					if (!DeviceAdminUtil.isDeviceAdminActive()) {
						requestDeviceAdminPermission();
					} else {
						Toast.makeText(PermissionRequestActivity.this, "设备管理器权限已获取", Toast.LENGTH_LONG).show();
					}
				}

			}, Color.BLACK, Color.parseColor("#FF80DEEA"));

        addSection("辅助服务申请", new FunctionExecutor() {
				@Override
				public void execute() {
					inAutoMode = false;

					if (!isAccessibilityServiceEnabled()) {
						requestAccessibilityPermission();
					} else {
						Toast.makeText(PermissionRequestActivity.this, "辅助服务权限已获取", Toast.LENGTH_LONG).show();
					}
				}
			}, Color.BLACK, Color.parseColor("#FF88C6F3"));

        addSection("截屏录屏申请", new FunctionExecutor() {
				@Override
				public void execute() {
					if (Build.VERSION.SDK_INT <21){
						Toast.makeText(PermissionRequestActivity.this,"该功能在安卓五以下无法使用", Toast.LENGTH_LONG).show();
						return;//安卓5及以下没有截屏。
					}
					
					inAutoMode = false;
					if (!mScreenCapture.isInitialized()) {
						requestScreenCapturePermission();
					} else {
						Toast.makeText(PermissionRequestActivity.this, "录屏权限已获取", Toast.LENGTH_LONG).show();
					}
				}
			}, Color.BLACK, Color.parseColor("#FFD1C4E9")); // 跳转到 DeviceInfoActivity

		addSection("授权完毕，返回主页", new FunctionExecutor() {
				@Override
				public void execute() {
					Intent main = new Intent(PermissionRequestActivity.this,MouseMainActivity.class);
					// 添加标志确保主页在新任务中打开
					main.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
					startActivity(main);
					finish();
					overridePendingTransition(R.anim.slide_in,R.anim.slide_out);
				}
			}, Color.BLACK, Color.parseColor("#cc6e62"));
		
    }




    //检查并申请权限
    private void checkAndRequestPermissions() {
        if (!checkReadWritePermission()) {
            requestReadWritePermission();
        } else if (!checkOverlayPermission()) {
            requestOverlayPermission();
        } else if (!DeviceAdminUtil.isDeviceAdminActive()) {
            requestDeviceAdminPermission();
        } else if (!isAccessibilityServiceEnabled()) {
            requestAccessibilityPermission();
        } else {
			if (Build.VERSION.SDK_INT <21){
				Toast.makeText(PermissionRequestActivity.this, "所有权限均已获取。截屏录屏无法使用。", Toast.LENGTH_LONG).show();
				finish();
				return;//安卓5及以下没有截屏。
			}
            // 辅助服务已开启，直接申请录屏权限
            if (!mScreenCapture.isInitialized()) {
                requestScreenCapturePermission();
            } else {
                Toast.makeText(PermissionRequestActivity.this, "所有权限已获取，可以开始使用了！", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }


    //检查辅助服务是否打开？
    private boolean isAccessibilityServiceEnabled() {
        int accessibilityEnabled = 0;
        try {
            accessibilityEnabled = Settings.Secure.getInt(getContentResolver(), Settings.Secure.ACCESSIBILITY_ENABLED);
        } catch (Settings.SettingNotFoundException e) {
            // 忽略
        }

        if (accessibilityEnabled == 1) {
            String service = getPackageName() + "/" + FlowerMouseService.class.getName();
            String settingValue = Settings.Secure.getString(getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            if (settingValue != null && settingValue.contains(service)) {
                return true;
            }
        }
        return false;
    }

    //跳转设置请求设备管理器权限
    private void requestDeviceAdminPermission() {
        setAdviseText("正在请求设备管理员权限...");
        DeviceAdminUtil.requestDeviceAdminPermission(this);
    }

    //跳转设置申请辅助服务权限。
    private void requestAccessibilityPermission() {
        if (inAutoMode) {
            setAdviseText("正在请求辅助服务权限...请再次点击一键申请按钮判断");
        } else {
            setAdviseText("正在请求辅助服务权限...");
        }

        Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
        startActivity(intent);

        // 延时启动提示 Activity
        mHandler.postDelayed(new Runnable() {
				@Override
				public void run() {
					Intent overlayIntent = new Intent(PermissionRequestActivity.this, OverlayActivity.class);
					startActivity(overlayIntent);
				}
			}, 500); // 延时 500ms
    }

    //申请录屏权限
    private void requestScreenCapturePermission() {
        //Toast.makeText(this, "请允许截屏,并勾选不再提醒。", Toast.LENGTH_LONG).show();
        setAdviseText("正在请求录屏权限...");
        MediaProjectionManager projectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);

        if (projectionManager == null) {
            return;
        }
        try {
            Intent captureIntent = projectionManager.createScreenCaptureIntent();
            startActivityForResult(captureIntent, REQUEST_CODE_SCREEN_CAPTURE);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 检查读写权限
    private boolean checkReadWritePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
        return true; // 低于 Android M 的系统默认有权限
    }
	
	private boolean checkSecurePermission() {
    	return checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_GRANTED;
    }
	

    // 申请读写权限
    private void requestReadWritePermission() {
        setAdviseText("正在请求读写权限...");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
											  REQUEST_CODE_WRITE_STORAGE);
        } else {
            // Android M 以下系统，权限已默认授予，直接继续
            onPermissionSuccess("读写");
            if (inAutoMode) {
                checkAndRequestPermissions();
            }
        }
    }

    // 检查悬浮窗权限
    private boolean checkOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return Settings.canDrawOverlays(this);
        }
        return true; // 低于 Android M 的系统默认有权限
    }

    // 申请悬浮窗权限
    private void requestOverlayPermission() {
        setAdviseText("正在请求悬浮窗权限...");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
            try {
                startActivityForResult(intent, REQUEST_CODE_OVERLAY_PERMISSION);
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "无法跳转到悬浮窗设置页面", Toast.LENGTH_SHORT).show();
            }
        } else {
            // Android M 以下系统，权限已默认授予，直接继续
            onPermissionSuccess("悬浮窗");
            if (inAutoMode) {
                checkAndRequestPermissions();
            }
        }
    }



    // --- 回调处理 ---

    @Override
    protected void onResume() {
        super.onResume();
        // 当从其他 Activity 返回时，检查当前权限状态，继续下一个权限请求
        if (inAutoMode) {
            mHandler.postDelayed(new Runnable() {
					@Override
					public void run() {
						//checkAndRequestPermissions();
					}
				}, 500); // 延时 500ms，以确保界面状态稳定
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        //自动模式下我们接着处理
        if (requestCode == DeviceAdminUtil.REQUEST_CODE_DEVICE_ADMIN && inAutoMode) {
            if (resultCode == Activity.RESULT_OK) {
                onPermissionSuccess("设备管理员");
                // 成功后，继续请求下一个权限
                checkAndRequestPermissions();
            } else {
                onPermissionFailure("设备管理员");
            }
        } else if (requestCode == REQUEST_CODE_SCREEN_CAPTURE) {
            if (resultCode == Activity.RESULT_OK) {
                try {
                    mScreenCapture.initialize(resultCode, data);
                } catch (ScreenCaptureHelper.ScreenCaptureException e) {
                    e.printStackTrace();
                }
                onPermissionSuccess("录屏");
                if (inAutoMode) {
                    // 所有权限都已成功获取
                    Toast.makeText(this, "所有权限已获取，可以开始使用了！", Toast.LENGTH_LONG).show();
                    finish();
                }
            } else {
                onPermissionFailure("录屏");
            }
        } else if (requestCode == REQUEST_CODE_OVERLAY_PERMISSION) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(this)) {
                onPermissionSuccess("悬浮窗");
                if (inAutoMode) {
                    checkAndRequestPermissions();
                }
            } else {
                onPermissionFailure("悬浮窗");
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_WRITE_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                onPermissionSuccess("读写");
                if (inAutoMode) {
                    checkAndRequestPermissions();
                }
            } else {
                onPermissionFailure("读写");
            }
        }
    }

    // 成功回调
    private void onPermissionSuccess(String permissionName) {
        Toast.makeText(this, permissionName + " 权限已授予", Toast.LENGTH_SHORT).show();
        setAdviseText(permissionName + " 权限已授予。");
    }

    // 失败回调
    private void onPermissionFailure(String permissionName) {
        Toast.makeText(this, permissionName + " 权限被拒绝，请重试", Toast.LENGTH_SHORT).show();
        setAdviseText(permissionName + " 权限被拒绝。");
        // 可以在这里提供一个重试按钮或者直接退出
    }
	
	
	@Override
	public void onBackPressed() {
		super.onBackPressed();
		finish();
		overridePendingTransition(R.anim.slide_in,R.anim.slide_out);
	}
	
	//重新请求超级用户
	public void requireSU(){
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					rootShellManager.initializeSuSession(); // 初始化 Root 会话

				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}).start(); // 启动新线程	
	}
	
	

	// 在 Activity 类内部调用此方法
	private void showToastOnMainThread(final String toastContent) {
		// 切换到主线程
		runOnUiThread(new Runnable() {
				@Override
				public void run() {
					// 主线程中弹出 Toast
					Toast.makeText(PermissionRequestActivity.this, toastContent, Toast.LENGTH_SHORT).show();
				}
			});
	}
	
	
	
	public void startRootAutoPerm(){
		
		progressDialog = ProgressDialog.show(PermissionRequestActivity.this, "请稍候", "权限申请中…", true);

		// 创建权限授予器实例
		RootPermissionGranter permissionGranter = new RootPermissionGranter("com.flowercat.rfmouse", rootShellManager);

		// 添加需要授予的权限
		if(!checkReadWritePermission()){
			permissionGranter.addPermission(RootPermissionGranter.PERMISSION_READ_EXTERNAL_STORAGE);
			permissionGranter.addPermission(RootPermissionGranter.PERMISSION_WRITE_EXTERNAL_STORAGE);
		}
		
		if(!checkOverlayPermission()){
			permissionGranter.addPermission(RootPermissionGranter.PERMISSION_SYSTEM_ALERT_WINDOW);
		}
		
		//if(!checkSecurePermission()){
			permissionGranter.addPermission(RootPermissionGranter.PERMISSION_WRITE_SECURE_SETTINGS);
		//}
		
		if(!DeviceAdminUtil.isDeviceAdminActive()){
			//添加设备管理员权限（需要传入设备管理员接收器的完整类名）
			permissionGranter.addDeviceAdminPermission(".receiver.MyDeviceAdminReceiver");
		}
		
		if(!isAccessibilityServiceEnabled()){
			// 添加辅助服务权限（需要传入辅助服务的完整类名）
			permissionGranter.addAccessibilityServicePermission(".service.FlowerMouseService");
		}
		// 或者使用快速方法授予常用权限
		// permissionGranter.grantCommonPermissions(callback);

		// 开始授予权限
		permissionGranter.grantPermissions(new RootPermissionGranter.GrantCallback() {
				@Override
				public void onAllGranted() {
					// 所有权限都已成功授予
					Message msg = handler.obtainMessage(1);
					handler.sendMessage(msg);
				}

				@Override
				public void onPartialGranted(List<String> failedPermissions) {
					// 部分权限授予失败
					Message msg = handler.obtainMessage(3);
					Bundle data = new Bundle();
					data.putString("error", "部分权限授予错误:" + failedPermissions.toString());
					msg.setData(data);
					handler.sendMessage(msg);
				}

				@Override
				public void onGrantFailed(String permission, String error) {
					// 单个权限授予失败
					Message msg = handler.obtainMessage(2);
					Bundle data = new Bundle();
					data.putString("error", permission+"权限授予错误:" + error);
					msg.setData(data);
					handler.sendMessage(msg);
				}

				@Override
				public void onRetryOption(String permission, final RootPermissionGranter.RetryHandler retryHandler) {
					// 显示重试选项给用户
					showToastOnMainThread("权限 " + permission + " 授予失败，重试中");
					// 在实际应用中，这里应该显示一个对话框或按钮让用户选择
					// 示例中我们简单模拟用户选择重试
					boolean userChooseRetry = true; // 假设用户选择重试

					if (userChooseRetry) {
						retryHandler.retry();
					} else {
						retryHandler.skip();
					}
				}
			});
	}
	
	
	// 显示部分错误
    private void showPremErrorDialog(String error) {

		AlertDialog.Builder builder = new AlertDialog.Builder(this);

		builder
			.setTitle("部分权限授权失败，请手动授权")
			.setMessage(error)
			.setPositiveButton("确定", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
				
				}
			})
			.setNegativeButton("取消", null);

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
	
	
	
	// 显示部分错误
    private void showPremSuccessDialog() {

		AlertDialog.Builder builder = new AlertDialog.Builder(this);

		builder
			.setTitle("授权成功！")
			.setMessage("除了截屏录屏，请手动申请哦~")
			.setPositiveButton("确定", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {

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

    }
	
	// 工具方法：dp转px（避免不同分辨率设备间距不一致）
	private int dp2px(Context context, float dpValue) {
		final float scale = context.getResources().getDisplayMetrics().density;
		return (int) (dpValue * scale + 0.5f); // 四舍五入避免精度丢失
	}
	
	
}
