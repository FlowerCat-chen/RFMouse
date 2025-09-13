package com.flowercat.rfmouse.ui;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.LruCache;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import android.app.Activity;
import com.flowercat.rfmouse.R;
import android.widget.AdapterView;
import android.view.Window;
import android.view.WindowManager;
import android.app.ProgressDialog;

public class AppListActivity extends Activity {

    private ListView listView;
    private AppAdapter adapter;
    private List<AppInfo> appList = new ArrayList<>();
    private LruCache<String, Bitmap> iconCache;
    private PackageManager pm;
	private ProgressDialog progressDialog;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                             WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.tool_listapp);
        listView = findViewById(R.id.app_list);
        pm = getPackageManager();

		progressDialog = ProgressDialog.show(AppListActivity.this, "请稍候", "加载应用列表中…", true);
		
        // 1. 初始化缓存 (使用应用内存的1/8)
        int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
        int cacheSize = maxMemory / 8;
        iconCache = new LruCache<String, Bitmap>(cacheSize) {
            protected int sizeOf(String key, Bitmap bitmap) {
                return bitmap.getByteCount() / 1024;
            }
        };

        // 2. 立即显示空列表
        adapter = new AppAdapter();
        listView.setAdapter(adapter);

        // 3. 分两步加载：先快速加载基本信息，再异步加载图标
        new FastLoadTask().execute();
		
		
		listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
				@Override
				public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
					AppInfo selectedApp = (AppInfo) parent.getItemAtPosition(position);
					Intent intent = new Intent(AppListActivity.this, ListPermissionsActivity.class);
					intent.putExtra("package_name", selectedApp.packageName);
					startActivity(intent);
				}
			});
		
    }

    // 应用信息类 (不包含图标Drawable)
    private class AppInfo {
        String name;
        String packageName;
        String activityName; // 用于唯一标识图标
		//Drawable appD;
    }

    // 第一步：快速加载任务 (仅获取名称和包名)
    private class FastLoadTask extends AsyncTask<Void, Void, List<AppInfo>> {
        @Override
        protected List<AppInfo> doInBackground(Void... params) {
            List<AppInfo> apps = new ArrayList<>();

            // 使用高效查询 (不获取图标)
            List<ResolveInfo> activities = pm.queryIntentActivities(
				new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER),
				PackageManager.GET_META_DATA); // 最快速查询

            for (ResolveInfo ri : activities) {
                AppInfo app = new AppInfo();
                app.name = ri.loadLabel(pm).toString();
                app.packageName = ri.activityInfo.packageName;
                app.activityName = ri.activityInfo.name; // 唯一标识
                apps.add(app);
            }

            // 按名称排序
            Collections.sort(apps, new Comparator<AppInfo>() {
					public int compare(AppInfo a, AppInfo b) {
						return a.name.compareToIgnoreCase(b.name);
					}
				});
            return apps;
        }

        @Override
        protected void onPostExecute(List<AppInfo> result) {
            // 4. 立即更新列表显示名称
            appList.clear();
            appList.addAll(result);
            adapter.notifyDataSetChanged();

			//取消加载条
			if (progressDialog != null && progressDialog.isShowing()) {
				progressDialog.dismiss();
			}
			
            // 5. 启动图标加载任务
            new IconLoaderTask().execute();
        }
    }

    // 第二步：图标加载任务 (后台加载图标)
    private class IconLoaderTask extends AsyncTask<Void, AppInfo, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            for (AppInfo app : appList) {
                // 检查缓存
                String cacheKey = app.packageName + ":" + app.activityName;
                if (iconCache.get(cacheKey) == null) {
                    // 获取图标
                    try {
                        ActivityInfo ai = pm.getActivityInfo(
							new android.content.ComponentName(app.packageName, app.activityName), 0);

                        // 直接转换为bitmap并压缩
                        Drawable d = ai.loadIcon(pm);
                        Bitmap bitmap = drawableToBitmap(d, 100, 100); // 压缩到100x100
                        iconCache.put(cacheKey, bitmap);

                        // 通知更新单个项
                        publishProgress(app);
                    } catch (Exception e) {
                        // 忽略错误
                    }
                }
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(AppInfo... values) {
            // 6. 逐个更新图标 (不刷新整个列表)
            for (AppInfo app : values) {
                int position = appList.indexOf(app);
                if (position >= 0) {
                    View view = listView.getChildAt(position - listView.getFirstVisiblePosition());
                    if (view != null) {
                        ImageView icon = view.findViewById(R.id.app_icon);
                        String cacheKey = app.packageName + ":" + app.activityName;
                        Bitmap bitmap = iconCache.get(cacheKey);
                        if (bitmap != null) {
                            icon.setImageBitmap(bitmap);
                        }
                    }
                }
            }
        }
    }

    // 适配器优化
    private class AppAdapter extends BaseAdapter {
        public int getCount() {
            return appList.size();
        }

        public Object getItem(int position) {
            return appList.get(position);
        }

        public long getItemId(int position) {
            return position;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                convertView = getLayoutInflater().inflate(R.layout.tool_appitem, parent, false);
                holder = new ViewHolder();
                holder.icon = convertView.findViewById(R.id.app_icon);
                holder.name = convertView.findViewById(R.id.app_name);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            AppInfo app = appList.get(position);
            holder.name.setText(app.name);

            // 7. 设置占位图 (立即显示)
            holder.icon.setImageResource(R.drawable.ic_launcher);

            // 8. 尝试从缓存加载图标 (如果有)
            String cacheKey = app.packageName + ":" + app.activityName;
            Bitmap cached = iconCache.get(cacheKey);
            if (cached != null) {
                holder.icon.setImageBitmap(cached);
            }

            return convertView;
        }

        class ViewHolder {
            ImageView icon;
            TextView name;
        }
    }

    // 工具方法：将Drawable转换为压缩的Bitmap
    private Bitmap drawableToBitmap(Drawable drawable, int width, int height) {
        if (drawable instanceof BitmapDrawable) {
            return Bitmap.createScaledBitmap(((BitmapDrawable) drawable).getBitmap(), width, height, true);
        }

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        android.graphics.Canvas canvas = new android.graphics.Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }
}
