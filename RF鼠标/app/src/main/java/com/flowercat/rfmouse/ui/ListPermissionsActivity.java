package com.flowercat.rfmouse.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PermissionInfo;
import android.content.pm.ServiceInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import com.flowercat.rfmouse.R;
import com.flowercat.rfmouse.adapter.PermissionsInfoAdapter;
import com.flowercat.rfmouse.util.RootPermissionGranter;
import com.flowercat.rfmouse.util.RootShellManager;
import java.util.ArrayList;
import java.util.List;
import android.view.View.OnClickListener;
import android.view.View;
import android.widget.AdapterView;

public class ListPermissionsActivity extends Activity {

    private static final String TAG = "PermissionsActivity";
    private ListView permissionsListView;
    private TextView packageNameTextView;
    private TextView specialPermissionsTextView;
	private ProgressDialog progressDialog;
	public Button bt_onclick_prem;
	public RootShellManager rootShellManager;
	private Handler handler;
	
	public String packageName = "";
	public List<String> deviceAdmin = new ArrayList<String>();
	public List<String> accessibilty = new ArrayList<String>();
	public List<String> permissionList = new ArrayList<>();
	
	
	
	
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		
		requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                             WindowManager.LayoutParams.FLAG_FULLSCREEN);
							 
        setContentView(R.layout.tool_activity_prem);
		rootShellManager = RootShellManager.getInstance();

        permissionsListView = findViewById(R.id.permissions_list_view);
        packageNameTextView = findViewById(R.id.package_name_text_view);
        specialPermissionsTextView = findViewById(R.id.special_permissions_text_view);
		bt_onclick_prem = findViewById(R.id.bt_onclick_prem);
        packageName = getIntent().getStringExtra("package_name");
        packageNameTextView.setText("Package: " + packageName);

        getPermissionsAndSpecialComp(packageName);
		
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
                        Toast.makeText(ListPermissionsActivity.this, error2, Toast.LENGTH_SHORT).show();
                        break;
					case 3: 
						String error =(String) msg.getData().getString("error");
                        showPremErrorDialog(error);
                        break;

                }
            }
        };
		
		
		
		bt_onclick_prem.setOnClickListener(new OnClickListener(){
				@Override
				public void onClick(View p1) {
					if(rootShellManager.isRootAvailable()){
						startRootAutoPerm(1,null);
					} else {
						showToastOnMainThread("Root不可用");
					}
					
				}
		});
		
		
		permissionsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
				@Override
				public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
					String perm = permissionList.get(position);
					String[] parts = perm.split("@@");
					String name = parts[0];
					showPremComfirmDialog(name);
				}
			});
		
    }

    private void getPermissionsAndSpecialComp(String packageName) {
        
        StringBuilder specialServicesInfo = new StringBuilder();

        try {
            PackageManager pm = getPackageManager();
            PackageInfo packageInfo = pm.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS | PackageManager.GET_SERVICES | PackageManager.GET_RECEIVERS);

            // 获取并列出权限
            if (packageInfo.requestedPermissions != null) {
                for (String permissionName : packageInfo.requestedPermissions) {
                    try {
                        PermissionInfo pInfo = pm.getPermissionInfo(permissionName, 0);
                        String permissionDesc = pInfo.loadDescription(pm) != null ? pInfo.loadDescription(pm).toString() : "没有描述";
                        permissionList.add(permissionName + "@@" + permissionDesc); // 使用 @@ 作为分隔符
                    } catch (PackageManager.NameNotFoundException e) {
                        permissionList.add(permissionName + "@@" + "权限没有描述");
                    }
                }
            }

            // 检查特殊服务（辅助服务和设备管理员）
            if (packageInfo.services != null) {
                for (ServiceInfo serviceInfo : packageInfo.services) {
                    if ("android.permission.BIND_ACCESSIBILITY_SERVICE".equals(serviceInfo.permission)) {
                        specialServicesInfo.append("辅助服务：").append(serviceInfo.name.replace(serviceInfo.packageName,"")).append("■");
						accessibilty.add(serviceInfo.name.replace(serviceInfo.packageName,""));
                    }
				}
				
			}
			
			
            // 检查设备管理员
            if (packageInfo.receivers != null) {
                for (ActivityInfo receiverInfo : packageInfo.receivers) {
                    if ("android.permission.BIND_DEVICE_ADMIN".equals(receiverInfo.permission)) {
                        specialServicesInfo.append("设备管理接收器：").append(receiverInfo.name.replace(receiverInfo.packageName,""));
						deviceAdmin.add(receiverInfo.name.replace(receiverInfo.packageName,""));
                    }
                }
            }
			
				

        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Package not found: " + packageName, e);
        }

        PermissionsInfoAdapter adapter = new PermissionsInfoAdapter(this, permissionList);
        permissionsListView.setAdapter(adapter);

        if (specialServicesInfo.length() > 0) {
            specialPermissionsTextView.setText(specialServicesInfo.toString());
        } else {
            specialPermissionsTextView.setText("未发现设备管理员或辅助服务接收器。");
        }
    }

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                             WindowManager.LayoutParams.FLAG_FULLSCREEN);
	}
	
	
	
	public void startRootAutoPerm(int mode,String singlePrem){

		progressDialog = ProgressDialog.show(ListPermissionsActivity.this, "请稍候", "权限申请中…", true);

		// 创建权限授予器实例
		RootPermissionGranter permissionGranter = new RootPermissionGranter(packageName, rootShellManager);

		if(mode == 1){
		
		// 添加需要授予的权限
		if(permissionList != null && permissionList.size() > 0){
			for(String prem:permissionList){
				if(prem.contains("SYSTEM_ALERT_WINDOW")){
					permissionGranter.addPermission(RootPermissionGranter.PERMISSION_SYSTEM_ALERT_WINDOW);
				}
			}
		}


		if(deviceAdmin !=null && deviceAdmin.size() > 0){
			for(String device:deviceAdmin){
				//添加设备管理员权限（需要传入设备管理员接收器的完整类名）
				permissionGranter.addDeviceAdminPermission(device);
			}
		}
		
		if(accessibilty !=null && accessibilty.size() > 0){
			for(String acce:accessibilty){
				permissionGranter.addAccessibilityServicePermission(acce);
			}
		}
		
		}
		
		
		if(mode == 2){
			permissionGranter.addPermission(singlePrem);
		}
		
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
	
	// 在 Activity 类内部调用此方法
	private void showToastOnMainThread(final String toastContent) {
		// 切换到主线程
		runOnUiThread(new Runnable() {
				@Override
				public void run() {
					// 主线程中弹出 Toast
					Toast.makeText(ListPermissionsActivity.this, toastContent, Toast.LENGTH_SHORT).show();
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
			.setMessage("zzz~")
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
	
	// 显示部分错误
    private void showPremComfirmDialog(final String prem) {

		AlertDialog.Builder builder = new AlertDialog.Builder(this);

		builder
			.setTitle("尝试授权")
			.setMessage("确定要给予应用" + prem+"权限吗？")
			.setPositiveButton("确定", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					startRootAutoPerm(2,prem);
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
	
	
	
	
	
	
}
