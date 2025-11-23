package com.flowercat.rfmouse.util;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import com.flowercat.rfmouse.R;

public class AppLauncherDialog {

    public static void showAppLauncher(final Context context) {
        // 定义应用列表
        final AppItem[] appItems = new AppItem[]{
            new AppItem("GitHub", "https://github.com/FlowerCat-chen/RFMouse", "com.github.android", "github.com"),
            new AppItem("QQ邮箱", "https://mail.qq.com", "com.tencent.androidqqmail", "QQ邮箱")
        };

        // 创建对话框
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("方式选择-kemonomimi666@qq.com");

        // 创建ListView
        ListView listView = new ListView(context);
        listView.setDivider(null); // 移除默认分割线

        // 创建适配器
        ArrayAdapter<AppItem> adapter = new ArrayAdapter<AppItem>(context, android.R.layout.simple_list_item_1, appItems) {
            @Override
            public View getView(int position, View convertView, android.view.ViewGroup parent) {
                if (convertView == null) {
                    convertView = LayoutInflater.from(context).inflate(android.R.layout.simple_list_item_1, parent, false);
                }

                TextView textView = (TextView) convertView.findViewById(android.R.id.text1);
                textView.setText(appItems[position].displayName);
                textView.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_menu_more, 0, 0, 0);
                textView.setCompoundDrawablePadding(16);

                return convertView;
            }
        };

        listView.setAdapter(adapter);

        // 设置点击事件
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
				@Override
				public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
					launchApp(context, appItems[position]);
				}
			});

        builder.setView(listView);
        builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
				}
			});

        AlertDialog dialog = builder.show();
    
		// 1. 先创建对话框（不能直接用builder，需先show()获取实例）
		

		// 2. 获取“确认”按钮（DialogInterface.BUTTON_POSITIVE）并设置样式
		Button positiveBtn = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
		if (positiveBtn != null) {
			LinearLayout.LayoutParams positiveParams = (LinearLayout.LayoutParams) positiveBtn.getLayoutParams();
			positiveParams.leftMargin = dp2px(context, 30); // 确认按钮左边距30dp（与取消按钮隔开）
			positiveBtn.setLayoutParams(positiveParams);
			positiveBtn.setBackgroundResource(R.drawable.button_background_selector); // 应用选择器
			//positiveBtn.setPadding(30, 10, 30, 10); // 可选：调整按钮内边距，避免边框紧贴文字

		}

		// 3. 获取“取消”按钮（DialogInterface.BUTTON_NEGATIVE）并设置样式
		Button negativeBtn = dialog.getButton(DialogInterface.BUTTON_NEGATIVE);
		if (negativeBtn != null) {
			LinearLayout.LayoutParams negativeParams = (LinearLayout.LayoutParams) negativeBtn.getLayoutParams();
			negativeParams.rightMargin = dp2px(context, 10); // 取消按钮右边距10dp
			negativeBtn.setLayoutParams(negativeParams);
			negativeBtn.setBackgroundResource(R.drawable.button_background_selector); // 应用选择器
			//negativeBtn.setPadding(30, 10, 30, 10); // 可选：同确认按钮，保持样式一致
		}
    }
	
	
	// 工具方法：dp转px（避免不同分辨率设备间距不一致）
	private static int dp2px(Context context, float dpValue) {
		final float scale = context.getResources().getDisplayMetrics().density;
		return (int) (dpValue * scale + 0.5f); // 四舍五入避免精度丢失
	}

    private static void launchApp(Context context, AppItem appItem) {
        try {
            // 先尝试打开本地应用
            Intent appIntent = context.getPackageManager().getLaunchIntentForPackage(appItem.packageName);
            if (appIntent != null) {
                context.startActivity(appIntent);
                return;
            }
        } catch (Exception e) {
            // 忽略应用启动异常
        }

        try {
            // 如果应用不存在，尝试打开网站
            Intent webIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(appItem.url));
            webIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            // 检查是否有浏览器可以处理该Intent
            if (webIntent.resolveActivity(context.getPackageManager()) != null) {
                context.startActivity(webIntent);
            } else {
                showError(context, "无法打开链接: " + appItem.url);
            }
        } catch (Exception e) {
            showError(context, "打开失败: " + appItem.displayName);
        }
    }

    private static void showError(Context context, String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }

    // 应用项数据类
    static class AppItem {
        String displayName;
        String url;
        String packageName;
        String fallbackDescription;

        AppItem(String displayName, String url, String packageName, String fallbackDescription) {
            this.displayName = displayName;
            this.url = url;
            this.packageName = packageName;
            this.fallbackDescription = fallbackDescription;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }
}
