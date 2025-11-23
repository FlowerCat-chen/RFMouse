package com.flowercat.rfmouse.ui;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import com.flowercat.rfmouse.R;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import com.flowercat.rfmouse.util.SPUtil;
import com.flowercat.rfmouse.service.FlowerMouseService;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.view.View.OnClickListener;
import android.widget.LinearLayout;
import android.content.Context;

public class MouseOverSettingActivity extends Activity {

    private ListView appListView;
    private List<AppInfo> appList = new ArrayList<>();
    private AppListAdapter adapter;
    
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                             WindowManager.LayoutParams.FLAG_FULLSCREEN);
		
        setContentView(R.layout.mouse_over_setting);
		
        // 设置添加按钮点击事件
        Button addAppButton = findViewById(R.id.add_app_button);
        addAppButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					// 跳转到应用选择Activity
					Intent intent = new Intent(MouseOverSettingActivity.this, AppListActivity.class);
					intent.putExtra("choose_mode", "yes");
					startActivityForResult(intent, 1);
				}
			});
			
		// 设置添加按钮点击事件.清空
        Button clear_all = findViewById(R.id.clear_app_button);
        clear_all.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					showClearConfirmationDialog();
				}
			});
			

        // 初始化列表视图
        appListView = findViewById(R.id.added_app_view);
        adapter = new AppListAdapter(appList);
        appListView.setAdapter(adapter);
		//加载列表
        SPUtil.loadAppList(this,appList);
		adapter.notifyDataSetChanged();
		
		appListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
				@Override
				public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
					//view.performClick();
					showPressTypeSelectionDialog(position,(RadioGroup)(view.findViewById(R.id.mode_group)));
				}
			});
		
			
		appListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
				@Override
				public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
					//view.performClick();
					showPressTypeSelectionDialog(position,(RadioGroup)(view.findViewById(R.id.mode_group)));
					return true;
				}
			});
    }

    /**
     * 删除指定的应用，并更新列表和保存数据
     * @param appInfo 要删除的应用信息
     */
    private void deleteApp(AppInfo appInfo) {
        if (appList.remove(appInfo)) {
			
			if(FlowerMouseService.getInstance() != null){
				FlowerMouseService.getInstance().updateAppList(appList);
			}
			
            adapter.notifyDataSetChanged();
            SPUtil.saveAppList(appList);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == RESULT_OK && data != null) {
            String packageName = data.getStringExtra("selected_app");
            if (packageName != null) {
                addAppToList(packageName);
            }
        }
    }

    /**
     * 根据包名向列表中添加新应用
     * @param packageName 要添加的应用的包名
     */
    private void addAppToList(String packageName) {
        // 完善的错误处理：检查包名是否为空
        if (packageName == null || packageName.trim().isEmpty()) {
            Toast.makeText(this, "包名不能为空", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // 检查应用是否已在列表中，防止重复添加
            if (isAppSaved(packageName)) {
                Toast.makeText(this, "该应用已在列表中", Toast.LENGTH_SHORT).show();
                return;
            }

            // 获取应用信息
            ApplicationInfo appInfo = getPackageManager().getApplicationInfo(packageName, 0);
            Drawable icon = getPackageManager().getApplicationIcon(appInfo);
            String name = getPackageManager().getApplicationLabel(appInfo).toString();

            // 添加到列表
            AppInfo newApp = new AppInfo(name, packageName, icon, 0); // 默认选择鼠标模式
            appList.add(newApp);
			
			if(FlowerMouseService.getInstance() != null){
				FlowerMouseService.getInstance().updateAppList(appList);
			}
			
            adapter.notifyDataSetChanged();
            SPUtil.saveAppList(appList);
			
			
        } catch (PackageManager.NameNotFoundException e) {
            // 异常处理：如果找不到应用
            Toast.makeText(this, "应用未找到：" + packageName, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            // 异常处理：处理其他未知错误
            Toast.makeText(this, "添加应用时出错", Toast.LENGTH_SHORT).show();
        }
    }

	
    /**
     * 判断某个包名是否已在列表中
     * @param packageName 要检查的包名
     * @return 如果存在返回true，否则返回false
     */
    public boolean isAppSaved(String packageName) {
        for (AppInfo app : appList) {
            if (app.getPackageName().equals(packageName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 获取指定包名对应的模式
     * @param packageName 要查询的包名
     * @return 对应的模式（0, 1, 2），如果未找到则返回-1
     */
    public int getAppMode(String packageName) {
        for (AppInfo app : appList) {
            if (app.getPackageName().equals(packageName)) {
                return app.getMode();
            }
        }
        return -1; // 返回-1表示未找到
    }

    // 应用信息类
    public static class AppInfo {
        private String name;
        private String packageName;
        private Drawable icon;
        private int mode; // 0:鼠标模式, 1:活动模式, 2:按键模式

        public AppInfo(String name, String packageName, Drawable icon, int mode) {
            this.name = name;
            this.packageName = packageName;
            this.icon = icon;
            this.mode = mode;
        }

        public String getName() { return name; }
        public String getPackageName() { return packageName; }
        public Drawable getIcon() { return icon; }
        public int getMode() { return mode; }
        public void setMode(int mode) { this.mode = mode; }
    }
	

    // 自定义适配器
	private class AppListAdapter extends ArrayAdapter<AppInfo> {
		private List<AppInfo> apps;

		public AppListAdapter(List<AppInfo> apps) {
			super(MouseOverSettingActivity.this, R.layout.mouse_over_appitem, apps);
			this.apps = apps;
		}

		@Override
		public View getView(final int position, View convertView, ViewGroup parent) {
			if (convertView == null) {
				convertView = getLayoutInflater().inflate(R.layout.mouse_over_appitem, parent, false);
			}

			final AppInfo appInfo = apps.get(position);

			// 设置应用图标和名称
			ImageView appIcon = convertView.findViewById(R.id.app_icon_over);
			appIcon.setImageDrawable(appInfo.getIcon());

			TextView appName = convertView.findViewById(R.id.app_name_over);
			appName.setText(appInfo.getName());

			// 设置模式选择
			final RadioGroup modeGroup = convertView.findViewById(R.id.mode_group);

			// **关键步骤：先移除旧的监听器**
			modeGroup.setOnCheckedChangeListener(null);

			// 根据保存的模式设置选中状态
			switch (appInfo.getMode()) {
				case 0:
					modeGroup.check(R.id.mode_mouse);
					break;
				case 1:
					modeGroup.check(R.id.mode_activity);
					break;
				case 2:
					modeGroup.check(R.id.mode_key);
					break;
			}

			// **关键步骤：再设置新的监听器**
			modeGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
					@Override
					public void onCheckedChanged(RadioGroup group, int checkedId) {
						int mode = 0;
						if (checkedId == R.id.mode_mouse) {
							mode = 0;
						} else if (checkedId == R.id.mode_activity) {
							mode = 1;
						} else if (checkedId == R.id.mode_key) {
							mode = 2;
						}

						// 使用当前的 `position` 来更新数据
						// 确保对列表的操作是安全的
						if (position < apps.size()) {
							apps.get(position).setMode(mode);
							
							if(FlowerMouseService.getInstance() != null){
								FlowerMouseService.getInstance().updateAppList(apps);
							}
							
							SPUtil.saveAppList(apps); // 保存更改
							adapter.notifyDataSetChanged();
						}
					}
				});
				
			/*
			// 在 convertView 上设置长按监听器
			convertView.setOnLongClickListener(new View.OnLongClickListener() {
					@Override
					public boolean onLongClick(View v) {
						showPressTypeSelectionDialog(position,modeGroup);
						return true;
					}
				});
				
			convertView.setOnClickListener(new OnClickListener(){

					@Override
					public void onClick(View v) {
						showPressTypeSelectionDialog(position,modeGroup);
					}
			});
			
			
			*/
			return convertView;
		}
		
	}
	
	
	// 长按对话框
    private void showPressTypeSelectionDialog(final int position,final RadioGroup modeGroup) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("选择功能");
		
        final String[] pressTypes = new String[]{"删除", "鼠标模式","滚动模式","键盘模式"};
        
        builder.setItems(pressTypes, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					switch(which) { //删除
						case 0:
							showDeleteConfirmationDialog(position);
							break;
						case 1:
							modeGroup.check(R.id.mode_mouse);
							break;
						case 2:
							modeGroup.check(R.id.mode_activity);
							break;
						case 3:
							modeGroup.check(R.id.mode_key);
							break;
							
					}
	
				}
			});
        builder.show();
    }


	// 显示删除确认对话框
    private void showDeleteConfirmationDialog(final int position) {

		AlertDialog.Builder builder = new AlertDialog.Builder(this);

		builder
			.setTitle("删除按键")
			.setMessage("确定要删除此按键吗？")
			.setPositiveButton("确定", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					AppInfo selectedApp = adapter.getItem(position);
					if (selectedApp != null) {
						deleteApp(selectedApp);
						Toast.makeText(MouseOverSettingActivity.this, "已删除 " + selectedApp.getName(), Toast.LENGTH_SHORT).show();
					}
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
	
	// 显示删除确认对话框
    private void showClearConfirmationDialog() {

		AlertDialog.Builder builder = new AlertDialog.Builder(this);

		builder
			.setTitle("清空列表")
			.setMessage("确定要清空添加的应用吗？")
			.setPositiveButton("确定", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					SPUtil.clearAppList();
					appList.clear();
					adapter.notifyDataSetChanged();
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
	
	
	
	
	// 工具方法：dp转px（避免不同分辨率设备间距不一致）
	private int dp2px(Context context, float dpValue) {
		final float scale = context.getResources().getDisplayMetrics().density;
		return (int) (dpValue * scale + 0.5f); // 四舍五入避免精度丢失
	}
	
	

	@Override
	public void onBackPressed() {
		super.onBackPressed();
		finish();
		overridePendingTransition(R.anim.slide_in,R.anim.slide_out);
	}
	
	
	
}
