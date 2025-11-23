package com.flowercat.rfmouse.ui;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;
import com.flowercat.rfmouse.R;
import com.flowercat.rfmouse.util.RootShellManager;
import android.content.Context;

public class HideSystemUIActivity extends Activity {

    private Button btnDisableNav;
    private Button btnEnableNav;
	private Button bt_exit_hideui;
    private RootShellManager rootShellManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                             WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.tool_hideui);

        // 初始化UI组件
        btnDisableNav = findViewById(R.id.btn_disable_nav);
        btnEnableNav = findViewById(R.id.btn_enable_nav);
		bt_exit_hideui = findViewById(R.id.bt_exit_hideui);

        // 初始化RootShellManager
        rootShellManager = RootShellManager.getInstance();

        // 设置按钮点击监听器
        btnDisableNav.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					disableNavigationBar();
				}
			});

        btnEnableNav.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					enableNavigationBar();
				}
			});
			
		bt_exit_hideui.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					finish();
				}
			});
			

        // 初始化Root会话
        initializeRootSession();
    }

    /**
     * 初始化Root会话
     */
    private void initializeRootSession() {
        new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						rootShellManager.initializeSuSession();
						runOnUiThread(new Runnable() {
								@Override
								public void run() {
									Toast.makeText(HideSystemUIActivity.this, 
												   "Root会话已初始化", Toast.LENGTH_SHORT).show();
								}
							});
					} catch (Exception e) {
						runOnUiThread(new Runnable() {
								@Override
								public void run() {
									Toast.makeText(HideSystemUIActivity.this, 
												   "无法获取Root权限", Toast.LENGTH_SHORT).show();
									btnDisableNav.setEnabled(false);
									btnEnableNav.setEnabled(false);
								}
							});
					}
				}
			}).start();
    }

    /**
     * 禁用导航栏
     */
    private void disableNavigationBar() {
        new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						final boolean result = hideNavigation();
						runOnUiThread(new Runnable() {
								@Override
								public void run() {
									if (result) {
										Toast.makeText(HideSystemUIActivity.this, 
													   "导航栏已禁用", Toast.LENGTH_SHORT).show();
									} else {
										Toast.makeText(HideSystemUIActivity.this, 
													   "禁用导航栏失败", Toast.LENGTH_SHORT).show();
									}
								}
							});
					} catch (final Exception e) {
						e.printStackTrace();
						runOnUiThread(new Runnable() {
								@Override
								public void run() {
									Toast.makeText(HideSystemUIActivity.this, 
												   "执行命令时出错: " + e.getMessage(), Toast.LENGTH_SHORT).show();
								}
							});
					}
				}
			}).start();
    }

    /**
     * 启用导航栏
     */
    private void enableNavigationBar() {
        new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						final boolean result = showNavigation();
						runOnUiThread(new Runnable() {
								@Override
								public void run() {
									if (result) {
										Toast.makeText(HideSystemUIActivity.this, 
													   "导航栏已启用", Toast.LENGTH_SHORT).show();
									} else {
										Toast.makeText(HideSystemUIActivity.this, 
													   "启用导航栏失败", Toast.LENGTH_SHORT).show();
									}
								}
							});
					} catch (final Exception e) {
						e.printStackTrace();
						runOnUiThread(new Runnable() {
								@Override
								public void run() {
									Toast.makeText(HideSystemUIActivity.this, 
												   "执行命令时出错: " + e.getMessage(), Toast.LENGTH_SHORT).show();
								}
							});
					}
				}
			}).start();
    }

    /**
     * 隐藏SystemUI
     */
    private boolean hideNavigation() {
        boolean isHide;
        try {
            String command = "wm overscan 0," +"-" + getStatusBarHeight(HideSystemUIActivity.this) + ",0," + "-" + getNavigationBarHeight(HideSystemUIActivity.this) ;
            rootShellManager.executeCommand(command);
            isHide = true;
        } catch (Exception ex) {
            isHide = false;
            ex.printStackTrace();
        }
        return isHide;
    }

    /**
     * 显示SystemUI
     */
    private boolean showNavigation() {
        boolean isShow;
        try {
            String command = "wm overscan 0,0,0,0";
            rootShellManager.executeCommand(command);
            isShow = true;
        } catch (Exception e) {
            isShow = false;
            e.printStackTrace();
        }
        return isShow;
    }
	
	//导航栏高度
	public static int getNavigationBarHeight(Context context) {
		
		int result = 0;
		int resourceId = context.getResources().getIdentifier("navigation_bar_height", "dimen", "android");
		if (resourceId > 0) {
			result = context.getResources().getDimensionPixelSize(resourceId);
		}
		return result;
	}
	
	
	//状态栏高度
	public static int getStatusBarHeight(Context context) {
		int result = 0;
		int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
		if (resourceId > 0) {
			result = context.getResources().getDimensionPixelSize(resourceId);
		}
		return result;
	}
	
	
	
	
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 关闭Root会话
        if (rootShellManager != null) {
            rootShellManager.closeSuSession();
        }
    }
}
