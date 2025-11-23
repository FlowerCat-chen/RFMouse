package com.flowercat.rfmouse.compati;

// TestThreeFragment.java
import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Random;
import android.app.Activity;
import com.flowercat.rfmouse.R;

public class TestThreeFragment extends Fragment {

    private TextView statusTextView;
    private SeekBar seekBar;
    private View targetMark; // 目标刻度 View
    private RelativeLayout container;
    private int targetProgress;
    private TestFragmentListener listener;

    // ... (省略 onAttach 逻辑)
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            listener = (TestFragmentListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement TestFragmentListener");
        }
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_test_three, container, false);
        statusTextView = (TextView) view.findViewById(R.id.status_text_three);
        seekBar = (SeekBar) view.findViewById(R.id.test_seekbar);
        this.container = (RelativeLayout) view.findViewById(R.id.seekbar_container);
        targetMark = view.findViewById(R.id.target_mark);

        // 设置最大进度
        seekBar.setMax(100);
        // 禁用拖动条的拖动功能，我们只测试点击
        seekBar.setOnTouchListener(new View.OnTouchListener() {
				@Override
				public boolean onTouch(View v, MotionEvent event) {
					// 如果是 Action_Down，我们自己处理点击逻辑，并返回 true 消耗事件
					if (event.getAction() == MotionEvent.ACTION_DOWN) {
						checkClickPosition(event);
					}
					return true;
				}
			});

        // 随机选择一个目标刻度（10到90之间，避免边缘）
        targetProgress = new Random().nextInt(81) + 10;
        updateStatusText();

        // 确保 View 渲染完成后再定位目标标记
        this.container.post(new Runnable() {
				@Override
				public void run() {
					positionTargetMark();
				}
			});

        return view;
    }

    private void updateStatusText() {
        statusTextView.setText("请点击拖动条上方的标记点 (刻度: " + targetProgress + ")。");
    }

    private void positionTargetMark() {
        // 计算目标进度对应的 SeekBar 上的 X 坐标
        int seekbarWidth = seekBar.getWidth() - seekBar.getPaddingLeft() - seekBar.getPaddingRight();
        int markOffset = (int) (seekBar.getPaddingLeft() + (float) seekbarWidth * targetProgress / seekBar.getMax());

        // 设置目标标记的位置
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) targetMark.getLayoutParams();
        // 减去标记自身宽度的一半，使中心对齐
        params.leftMargin = markOffset - (targetMark.getWidth() / 2); 
        targetMark.setLayoutParams(params);
        targetMark.setVisibility(View.VISIBLE);
    }

    private void checkClickPosition(MotionEvent event) {
        // 获取点击事件的 X 坐标 (相对于屏幕/Activity)
        float clickX = event.getRawX();

        // 获取目标标记在屏幕上的坐标
        int[] markLocation = new int[2];
        targetMark.getLocationOnScreen(markLocation);
        int markCenterX = markLocation[0] + targetMark.getWidth() / 2;

        // 定义可接受的误差范围（例如 30 像素，可根据 DPI 调整）
        // 简单的像素值，不考虑 DPI，为了兼容性，也可以用一个 dp 值转换
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        int tolerancePx = (int) (30 * metrics.density); // 30dp 转换为 像素

        if (Math.abs(clickX - markCenterX) <= tolerancePx) {
            Toast.makeText(getActivity(), "点击成功! 误差在 " + tolerancePx + "px 范围内。", Toast.LENGTH_LONG).show();
            // 成功后，用户可以选择完成/退出
            statusTextView.setText("测试3 (拖动条) **通过**! 所有测试已完成。");
            listener.onTestComplete(-1); // 结束所有测试
        } else {
            Toast.makeText(getActivity(), "点击失败! 误差超出范围。请重试。", Toast.LENGTH_SHORT).show();
        }
    }
}
