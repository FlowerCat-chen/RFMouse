// BaseActivity.java

// BaseActivity.java
package com.flowercat.rfmouse.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import android.graphics.Color;
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
    public List<SectionItem> sectionItems = new ArrayList<SectionItem>();
    public Map<String, Object> sectionActions = new HashMap<String, Object>();
    public SectionAdapter adapter;
    public TextView text_adv;
    public LinearLayout info_linear;

    // 用于闪烁效果的Handler
    private Handler blinkHandler = new Handler();
    private boolean isBlinking = false;
    // 修改：支持多个位置同时闪烁
    private List<Integer> blinkingPositions = new ArrayList<>();
    private Map<Integer, Integer> originalTextColors = new HashMap<>();
    private Map<Integer, Integer> originalBackgroundColors = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                             WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.base_list);

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
                    // 如果正在闪烁，停止闪烁
                    if (isBlinking) {
                        stopBlink();
                    }

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

                                // **已移除: overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);** // 保持原有的 Activity 切换动画，或者没有动画。

                            } catch (ClassCastException e) {
                                e.printStackTrace();
                            } catch (Exception e) {
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 停止闪烁，防止内存泄漏
        stopBlink();
    }

    // 隐藏底下的作者信息
    public void hideInfo(){
        info_linear.setVisibility(View.GONE);
    }

    // 设置提示内容
    public void setAdviseText(String content){
        text_adv.setText(content);
    }

    /**
     * 让指定位置的选项闪烁
     * @param position 位置索引
     * @param duration 闪烁持续时间（毫秒），默认3000毫秒
     */
    public void blinkItem(int position, final int duration) {
        if (position < 0 || position >= sectionItems.size()) {
            return;
        }

        // 如果该位置已经在闪烁，则不再添加
        if (blinkingPositions.contains(position)) {
            return;
        }

        // 保存原始颜色
        SectionItem item = sectionItems.get(position);
        originalTextColors.put(position, item.getTextColor());
        originalBackgroundColors.put(position, item.getBackgroundColor());

        // 添加到闪烁列表
        blinkingPositions.add(position);
        isBlinking = true;

        final int blinkTextColor = Color.parseColor("#eaa1ff");
        final int blinkBackgroundColor = Color.parseColor("#FFEA00"); // 黄色

        final Runnable blinkRunnable = new Runnable() {
            boolean showBlinkColor = false;
            int blinkCount = 0;
            final int totalBlinks = (duration / 500); // 每500毫秒闪烁一次

            @Override
            public void run() {
                if (!isBlinking || blinkCount >= totalBlinks) {
                    // 闪烁结束，恢复原始颜色
                    if (isBlinking) {
                        for (int pos : blinkingPositions) {
                            if (pos < sectionItems.size()) {
                                SectionItem currentItem = sectionItems.get(pos);
                                Integer originalText = originalTextColors.get(pos);
                                Integer originalBg = originalBackgroundColors.get(pos);
                                if (originalText != null && originalBg != null) {
                                    currentItem.setTextColor(originalText);
                                    currentItem.setBackgroundColor(originalBg);
                                }
                            }
                        }
                        adapter.notifyDataSetChanged();
                        isBlinking = false;
                        blinkingPositions.clear();
                        originalTextColors.clear();
                        originalBackgroundColors.clear();
                    }
                    return;
                }

                // 更新所有闪烁项的颜色
                for (int pos : blinkingPositions) {
                    if (pos < sectionItems.size()) {
                        SectionItem currentItem = sectionItems.get(pos);
                        if (showBlinkColor) {
                            // 显示闪烁颜色
                            currentItem.setTextColor(blinkTextColor);
                            currentItem.setBackgroundColor(blinkBackgroundColor);
                        } else {
                            // 显示原始颜色
                            Integer originalText = originalTextColors.get(pos);
                            Integer originalBg = originalBackgroundColors.get(pos);
                            if (originalText != null && originalBg != null) {
                                currentItem.setTextColor(originalText);
                                currentItem.setBackgroundColor(originalBg);
                            }
                        }
                    }
                }

                adapter.notifyDataSetChanged();
                showBlinkColor = !showBlinkColor;
                blinkCount++;

                blinkHandler.postDelayed(this, 500); // 每500毫秒切换一次
            }
        };

        // 开始闪烁
        blinkHandler.post(blinkRunnable);

        // 设置持续时间后自动停止
        blinkHandler.postDelayed(new Runnable() {
				@Override
				public void run() {
					stopBlink();
				}
			}, duration);
    }

    /**
     * 让多个指定位置的选项同时闪烁
     * @param positions 位置索引数组
     * @param duration 闪烁持续时间（毫秒），默认3000毫秒
     */
    public void blinkItems(int[] positions, final int duration) {
        if (positions == null || positions.length == 0) {
            return;
        }

        // 停止之前的闪烁
        stopBlink();

        // 添加所有位置到闪烁列表
        for (int position : positions) {
            if (position >= 0 && position < sectionItems.size() && 
                !blinkingPositions.contains(position)) {
                blinkingPositions.add(position);

                // 保存原始颜色
                SectionItem item = sectionItems.get(position);
                originalTextColors.put(position, item.getTextColor());
                originalBackgroundColors.put(position, item.getBackgroundColor());
            }
        }

        if (blinkingPositions.isEmpty()) {
            return;
        }

        isBlinking = true;

        final int blinkTextColor = Color.parseColor("#eaa1ff");
        final int blinkBackgroundColor = Color.parseColor("#FFEA00"); // 黄色

        final Runnable blinkRunnable = new Runnable() {
            boolean showBlinkColor = false;
            int blinkCount = 0;
            final int totalBlinks = (duration / 500); // 每500毫秒闪烁一次

            @Override
            public void run() {
                if (!isBlinking || blinkCount >= totalBlinks) {
                    // 闪烁结束，恢复原始颜色
                    if (isBlinking) {
                        for (int pos : blinkingPositions) {
                            if (pos < sectionItems.size()) {
                                SectionItem currentItem = sectionItems.get(pos);
                                Integer originalText = originalTextColors.get(pos);
                                Integer originalBg = originalBackgroundColors.get(pos);
                                if (originalText != null && originalBg != null) {
                                    currentItem.setTextColor(originalText);
                                    currentItem.setBackgroundColor(originalBg);
                                }
                            }
                        }
                        adapter.notifyDataSetChanged();
                        isBlinking = false;
                        blinkingPositions.clear();
                        originalTextColors.clear();
                        originalBackgroundColors.clear();
                    }
                    return;
                }

                // 更新所有闪烁项的颜色
                for (int pos : blinkingPositions) {
                    if (pos < sectionItems.size()) {
                        SectionItem currentItem = sectionItems.get(pos);
                        if (showBlinkColor) {
                            // 显示闪烁颜色
                            currentItem.setTextColor(blinkTextColor);
                            currentItem.setBackgroundColor(blinkBackgroundColor);
                        } else {
                            // 显示原始颜色
                            Integer originalText = originalTextColors.get(pos);
                            Integer originalBg = originalBackgroundColors.get(pos);
                            if (originalText != null && originalBg != null) {
                                currentItem.setTextColor(originalText);
                                currentItem.setBackgroundColor(originalBg);
                            }
                        }
                    }
                }

                adapter.notifyDataSetChanged();
                showBlinkColor = !showBlinkColor;
                blinkCount++;

                blinkHandler.postDelayed(this, 500); // 每500毫秒切换一次
            }
        };

        // 开始闪烁
        blinkHandler.post(blinkRunnable);

        // 设置持续时间后自动停止
        blinkHandler.postDelayed(new Runnable() {
				@Override
				public void run() {
					stopBlink();
				}
			}, duration);
    }

    /**
     * 让指定位置的选项闪烁（使用默认持续时间3000毫秒）
     * @param position 位置索引
     */
    public void blinkItem(int position) {
        blinkItem(position, 3000);
    }

    /**
     * 让多个指定位置的选项同时闪烁（使用默认持续时间3000毫秒）
     * @param positions 位置索引数组
     */
    public void blinkItems(int[] positions) {
        blinkItems(positions, 3000);
    }

    /**
     * 停止闪烁
     */
    public void stopBlink() {
        isBlinking = false;
        blinkHandler.removeCallbacksAndMessages(null);

        // 恢复所有闪烁项的颜色
        for (int position : blinkingPositions) {
            if (position >= 0 && position < sectionItems.size()) {
                SectionItem item = sectionItems.get(position);
                Integer originalText = originalTextColors.get(position);
                Integer originalBg = originalBackgroundColors.get(position);
                if (originalText != null && originalBg != null) {
                    item.setTextColor(originalText);
                    item.setBackgroundColor(originalBg);
                }
            }
        }

        adapter.notifyDataSetChanged();
        blinkingPositions.clear();
        originalTextColors.clear();
        originalBackgroundColors.clear();
    }

    /**
     * 滚动到指定位置并闪烁（用于引导用户）
     * @param position 位置索引
     * @param duration 闪烁持续时间（毫秒）
     */
    public void guideToItem(final int position, final int duration) {
        // 滚动到指定位置
        listView.smoothScrollToPosition(position);

        // 延迟一段时间后开始闪烁，确保滚动完成
        blinkHandler.postDelayed(new Runnable() {
				@Override
				public void run() {
					blinkItem(position, duration);
				}
			}, 300);
    }

    /**
     * 滚动到指定位置并闪烁（使用默认持续时间）
     * @param position 位置索引
     */
    public void guideToItem(int position) {
        guideToItem(position, 3000);
    }

    /**
     * 初始化导航板块数据。
     */
    protected abstract void initializeSections();

    // 辅助方法，方便子类添加 Activity 跳转项（使用新的默认颜色）
    protected void addSection(String title, Class<? extends Activity> targetActivity) {
        // 新的默认颜色：白色背景，深灰色文字 (#FF212121)
        addSection(title, targetActivity, Color.parseColor("#FFEFF8F7"), Color.parseColor("#FF89A1B1"));
    }

    // 辅助方法，方便子类添加方法执行项（使用新的默认颜色）
    protected void addSection(String title, FunctionExecutor executor) {
        // 新的默认颜色：白色背景，深灰色文字 (#FF212121)
        addSection(title, executor, Color.parseColor("#FFEFF8F7"), Color.parseColor("#FF89A1B1"));
    }

    // 新增辅助方法，方便子类添加 Activity 跳转项并自定义颜色
    protected void addSection(String title, Class<? extends Activity> targetActivity, int textColor, int backgroundColor) {
        try {
            // 检查是否已存在同名项，如果存在则先移除
            removeSection(title);
            sectionItems.add(new SectionItem(title, textColor, backgroundColor));
            sectionActions.put(title, targetActivity);
            refreshAdapterIfNeeded();
        } catch (Exception e) {
            e.printStackTrace();
            // 异常处理：记录日志或提示用户
        }
    }

    // 新增辅助方法，方便子类添加方法执行项并自定义颜色
    protected void addSection(String title, FunctionExecutor executor, int textColor, int backgroundColor) {
        try {
            // 检查是否已存在同名项，如果存在则先移除
            removeSection(title);
            sectionItems.add(new SectionItem(title, textColor, backgroundColor));
            sectionActions.put(title, executor);
            refreshAdapterIfNeeded();
        } catch (Exception e) {
            e.printStackTrace();
            // 异常处理：记录日志或提示用户
        }
    }

    /**
     * 动态添加选项到列表顶部
     * @param title 选项标题
     * @param targetActivity 目标Activity
     */
    public void addSectionToTop(String title, Class<? extends Activity> targetActivity) {
        addSectionToTop(title, targetActivity, Color.parseColor("#FFEFF8F7"), Color.parseColor("#FF89A1B1"));
    }

    /**
     * 动态添加选项到列表顶部
     * @param title 选项标题
     * @param executor 功能执行器
     */
    public void addSectionToTop(String title, FunctionExecutor executor) {
        addSectionToTop(title, executor, Color.parseColor("#FFEFF8F7"), Color.parseColor("#FF89A1B1"));
    }

    /**
     * 动态添加选项到列表顶部（自定义颜色）
     * @param title 选项标题
     * @param targetActivity 目标Activity
     * @param textColor 文字颜色
     * @param backgroundColor 背景颜色
     */
    public void addSectionToTop(String title, Class<? extends Activity> targetActivity, int textColor, int backgroundColor) {
        try {
            // 检查是否已存在同名项，如果存在则先移除
            removeSection(title);
            sectionItems.add(0, new SectionItem(title, textColor, backgroundColor));
            sectionActions.put(title, targetActivity);
            refreshAdapterIfNeeded();
        } catch (Exception e) {
            e.printStackTrace();
            // 异常处理：记录日志或提示用户
        }
    }

    /**
     * 动态添加选项到列表顶部（自定义颜色）
     * @param title 选项标题
     * @param executor 功能执行器
     * @param textColor 文字颜色
     * @param backgroundColor 背景颜色
     */
    public void addSectionToTop(String title, FunctionExecutor executor, int textColor, int backgroundColor) {
        try {
            // 检查是否已存在同名项，如果存在则先移除
            removeSection(title);
            sectionItems.add(0, new SectionItem(title, textColor, backgroundColor));
            sectionActions.put(title, executor);
            refreshAdapterIfNeeded();
        } catch (Exception e) {
            e.printStackTrace();
            // 异常处理：记录日志或提示用户
        }
    }

    /**
     * 动态添加选项到指定位置
     * @param position 插入位置
     * @param title 选项标题
     * @param targetActivity 目标Activity
     */
    public void addSectionAtPosition(int position, String title, Class<? extends Activity> targetActivity) {
        addSectionAtPosition(position, title, targetActivity, Color.parseColor("#FFEFF8F7"), Color.parseColor("#FF89A1B1"));
    }

    /**
     * 动态添加选项到指定位置
     * @param position 插入位置
     * @param title 选项标题
     * @param executor 功能执行器
     */
    public void addSectionAtPosition(int position, String title, FunctionExecutor executor) {
        addSectionAtPosition(position, title, executor, Color.parseColor("#FFEFF8F7"), Color.parseColor("#FF89A1B1"));
    }

    /**
     * 动态添加选项到指定位置（自定义颜色）
     * @param position 插入位置
     * @param title 选项标题
     * @param targetActivity 目标Activity
     * @param textColor 文字颜色
     * @param backgroundColor 背景颜色
     */
    public void addSectionAtPosition(int position, String title, Class<? extends Activity> targetActivity, int textColor, int backgroundColor) {
        try {
            if (position < 0) position = 0;
            if (position > sectionItems.size()) position = sectionItems.size();

            // 检查是否已存在同名项，如果存在则先移除
            removeSection(title);
            sectionItems.add(position, new SectionItem(title, textColor, backgroundColor));
            sectionActions.put(title, targetActivity);
            refreshAdapterIfNeeded();
        } catch (Exception e) {
            e.printStackTrace();
            // 异常处理：记录日志或提示用户
        }
    }

    /**
     * 动态添加选项到指定位置（自定义颜色）
     * @param position 插入位置
     * @param title 选项标题
     * @param executor 功能执行器
     * @param textColor 文字颜色
     * @param backgroundColor 背景颜色
     */
    public void addSectionAtPosition(int position, String title, FunctionExecutor executor, int textColor, int backgroundColor) {
        try {
            if (position < 0) position = 0;
            if (position > sectionItems.size()) position = sectionItems.size();

            // 检查是否已存在同名项，如果存在则先移除
            removeSection(title);
            sectionItems.add(position, new SectionItem(title, textColor, backgroundColor));
            sectionActions.put(title, executor);
            refreshAdapterIfNeeded();
        } catch (Exception e) {
            e.printStackTrace();
            // 异常处理：记录日志或提示用户
        }
    }

    /**
     * 替换指定位置的选项
     * @param position 要替换的位置
     * @param title 新选项标题
     * @param targetActivity 新目标Activity
     * @param textColor 新文字颜色
     * @param backgroundColor 新背景颜色
     */
    public void replaceSection(int position, String title, Class<? extends Activity> targetActivity, int textColor, int backgroundColor) {
        try {
            if (position < 0 || position >= sectionItems.size()) {
                throw new IllegalArgumentException("位置索引越界: " + position);
            }

            // 移除旧项
            SectionItem oldItem = sectionItems.remove(position);
            if (oldItem != null) {
                sectionActions.remove(oldItem.getTitle());
            }

            // 添加新项到相同位置
            sectionItems.add(position, new SectionItem(title, textColor, backgroundColor));
            sectionActions.put(title, targetActivity);
            refreshAdapterIfNeeded();
        } catch (Exception e) {
            e.printStackTrace();
            // 异常处理：记录日志或提示用户
        }
    }

    /**
     * 替换指定位置的选项
     * @param position 要替换的位置
     * @param title 新选项标题
     * @param executor 新功能执行器
     * @param textColor 新文字颜色
     * @param backgroundColor 新背景颜色
     */
    public void replaceSection(int position, String title, FunctionExecutor executor, int textColor, int backgroundColor) {
        try {
            if (position < 0 || position >= sectionItems.size()) {
                throw new IllegalArgumentException("位置索引越界: " + position);
            }

            // 移除旧项
            SectionItem oldItem = sectionItems.remove(position);
            if (oldItem != null) {
                sectionActions.remove(oldItem.getTitle());
            }

            // 添加新项到相同位置
            sectionItems.add(position, new SectionItem(title, textColor, backgroundColor));
            sectionActions.put(title, executor);
            refreshAdapterIfNeeded();
        } catch (Exception e) {
            e.printStackTrace();
            // 异常处理：记录日志或提示用户
        }
    }

    /**
     * 移除指定位置的选项
     * @param position 位置索引
     */
    public void removeSection(int position) {
        try {
            if (position >= 0 && position < sectionItems.size()) {
                SectionItem item = sectionItems.remove(position);
                if (item != null) {
                    sectionActions.remove(item.getTitle());
                }
                refreshAdapterIfNeeded();

                // 如果移除的项正在闪烁，从闪烁列表中移除
                if (blinkingPositions.contains(position)) {
                    blinkingPositions.remove((Integer) position);
                    originalTextColors.remove(position);
                    originalBackgroundColors.remove(position);
                }
            } else {
                throw new IllegalArgumentException("位置索引越界: " + position);
            }
        } catch (Exception e) {
            e.printStackTrace();
            // 异常处理：记录日志或提示用户
        }
    }

    /**
     * 移除指定标题的选项
     * @param title 选项标题
     */
    public void removeSection(String title) {
        try {
            for (int i = 0; i < sectionItems.size(); i++) {
                if (sectionItems.get(i).getTitle().equals(title)) {
                    sectionItems.remove(i);
                    sectionActions.remove(title);
                    refreshAdapterIfNeeded();

                    // 如果移除的项正在闪烁，从闪烁列表中移除
                    if (blinkingPositions.contains(i)) {
                        blinkingPositions.remove((Integer) i);
                        originalTextColors.remove(i);
                        originalBackgroundColors.remove(i);
                    }
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            // 异常处理：记录日志或提示用户
        }
    }

    /**
     * 清除所有选项
     */
    public void clearAllSections() {
        try {
            sectionItems.clear();
            sectionActions.clear();
            // 停止所有闪烁
            stopBlink();
            refreshAdapterIfNeeded();
        } catch (Exception e) {
            e.printStackTrace();
            // 异常处理：记录日志或提示用户
        }
    }

    /**
     * 刷新适配器（如果适配器已初始化）
     */
    private void refreshAdapterIfNeeded() {
        try {
            if (adapter != null) {
                adapter.notifyDataSetChanged();
            }
        } catch (Exception e) {
            e.printStackTrace();
            // 异常处理：记录日志
        }
    }
}
