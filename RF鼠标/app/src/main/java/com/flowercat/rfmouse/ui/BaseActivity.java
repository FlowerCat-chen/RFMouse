// BaseActivity.java
package com.flowercat.rfmouse.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import android.graphics.Color; // 导入 Color 类
import android.view.KeyEvent;

import android.view.Window;
import android.view.WindowManager;

import android.widget.TextView;
import android.widget.LinearLayout;
import com.flowercat.rfmouse.adapter.SectionItem;
import com.flowercat.rfmouse.adapter.SectionAdapter;
import com.flowercat.rfmouse.R;


public abstract class BaseActivity extends Activity {

    public ListView listView;
    // 存储 SectionItem 对象列表，每个对象包含标题、文本颜色和背景颜色
    public List<SectionItem> sectionItems = new ArrayList<SectionItem>();
    // 存储板块标题和对应动作的映射（Activity 类或 FunctionExecutor 实例）
    public Map<String, Object> sectionActions = new HashMap<String, Object>();
    public SectionAdapter adapter;
	public TextView text_adv;
	public LinearLayout info_linear;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                             WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.base_list); // 设置基础布局

        listView = (ListView) findViewById(R.id.main_list_view);

		text_adv = findViewById(R.id.text_adv);
		info_linear = findViewById(R.id.info_linear);
		
        // 初始化板块数据和对应的Activity或方法
        initializeSections();

        // 使用自定义的 SectionAdapter
        adapter = new SectionAdapter(
            this,
            R.layout.section_item_layout, // 使用我们自定义的列表项布局
            sectionItems // 将 SectionItem 列表传递给适配器
        );

        listView.setAdapter(adapter);

        // 设置ListView的点击事件监听器
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
				@Override
				public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
					// 从 SectionItem 中获取被点击板块的标题
					String clickedSectionTitle = sectionItems.get(position).getTitle();
					// 根据标题获取对应的动作
					Object action = sectionActions.get(clickedSectionTitle);

					if (action != null) {
						if (action instanceof Class) {
							// 如果是 Activity 类，则跳转
							try {
								Class<? extends Activity> targetActivity = (Class<? extends Activity>) action;
								Intent intent = new Intent(getApplicationContext(), targetActivity);
								startActivity(intent);
							} catch (ClassCastException e) {
								// 实际开发中可以添加日志或错误处理
								e.printStackTrace();
							}
						} else if (action instanceof FunctionExecutor) {
							// 如果是 FunctionExecutor 接口实例，则调用 execute 方法
							((FunctionExecutor) action).execute();
						}
					}
				}
			});
    }

	//隐藏底下的作者信息
	public void hideInfo(){
		info_linear.setVisibility(View.GONE);
	}
	//设置提示内容
	public void setAdviseText(String content){
		text_adv.setText(content);
	}
	
    /**
     * 初始化导航板块数据。
     * 你可以方便地在这里添加、修改或删除板块。
     * 现在支持传入 Activity 类或实现 FunctionExecutor 接口的匿名类。
     */
    // 抽象方法，子类必须实现来初始化其特有的列表项数据和动作
    protected abstract void initializeSections();

    // 辅助方法，方便子类添加 Activity 跳转项（使用默认颜色）
    protected void addSection(String title, Class<? extends Activity> targetActivity) {
        // 默认文本颜色为黑色，背景颜色为白色
        addSection(title, targetActivity, Color.parseColor("#FFEFF8F7"), Color.parseColor("#FF89A1B1"));
    }

    // 辅助方法，方便子类添加方法执行项（使用默认颜色）
    protected void addSection(String title, FunctionExecutor executor) {
        // 默认文本颜色为黑色，背景颜色为白色
        addSection(title, executor, Color.parseColor("#FFEFF8F7"), Color.parseColor("#FF89A1B1"));
    }

    // 新增辅助方法，方便子类添加 Activity 跳转项并自定义颜色
    protected void addSection(String title, Class<? extends Activity> targetActivity, int textColor, int backgroundColor) {
        sectionItems.add(new SectionItem(title, textColor, backgroundColor));
        sectionActions.put(title, targetActivity);
    }

    // 新增辅助方法，方便子类添加方法执行项并自定义颜色
    protected void addSection(String title, FunctionExecutor executor, int textColor, int backgroundColor) {
        sectionItems.add(new SectionItem(title, textColor, backgroundColor));
        sectionActions.put(title, executor);
    }
}
