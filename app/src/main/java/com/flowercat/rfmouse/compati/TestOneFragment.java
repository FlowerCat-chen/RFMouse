package com.flowercat.rfmouse.compati;

// TestOneFragment.java

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.flowercat.rfmouse.R;
import android.os.Build;
import com.flowercat.rfmouse.util.SPUtil;

public class TestOneFragment extends Fragment implements View.OnClickListener, View.OnLongClickListener {

    private TextView statusTextView;
    private Button btn1, btn2, btn3;
    private int currentPhase = 0; // 0: 短按, 1: 长按
    private int clickStep = 1; // 1, 2, 3
    private TestFragmentListener listener;
	private String mode = "";
	private boolean isModeSwitched = false;
	private boolean canModeSwith = false;
	
	private Button exitBtn;
	private Button passBtn;
	private Button failBtn;
	

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            listener = (TestFragmentListener) context;
			initModeCheck();
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement TestFragmentListener");
        }
    }

    // 兼容旧API
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            listener = (TestFragmentListener) activity;
			initModeCheck();
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement TestFragmentListener");
        }
    }
	
	
	public void initModeCheck(){
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
			canModeSwith = true;
			mode = SPUtil.getString(SPUtil.KEY_CLICK_MODE,"gesture");
		} else {
			canModeSwith = false;
			mode = SPUtil.getString(SPUtil.KEY_CLICK_MODE,"node");
		}

	}
	
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_test_one, container, false);
        statusTextView = (TextView) view.findViewById(R.id.status_text);
        btn1 = (Button) view.findViewById(R.id.button1);
        btn2 = (Button) view.findViewById(R.id.button2);
        btn3 = (Button) view.findViewById(R.id.button3);
        passBtn = (Button) view.findViewById(R.id.pass_button);
        failBtn = (Button) view.findViewById(R.id.fail_button);
        exitBtn = (Button) view.findViewById(R.id.exit_button);

        btn1.setOnClickListener(this);
        btn2.setOnClickListener(this);
        btn3.setOnClickListener(this);
        btn1.setOnLongClickListener(this);
        btn2.setOnLongClickListener(this);
        btn3.setOnLongClickListener(this);

        passBtn.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					// 如果当前阶段完成，则进入下一阶段
					if (currentPhase == 1 && clickStep == 4) {
						Toast.makeText(getActivity(), "测试1通过，进入滚动测试", Toast.LENGTH_SHORT).show();
						listener.onTestComplete(2); // 进入 TestTwo
					} else {
						Toast.makeText(getActivity(), "请先完成当前阶段的所有步骤", Toast.LENGTH_SHORT).show();
					}
				}
			});

        failBtn.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					
					if(canModeSwith && !isModeSwitched){
						Toast.makeText(getActivity(), "测试1不通过", Toast.LENGTH_SHORT).show();
						showModeSwitchDialog();
					} else {
						Toast.makeText(getActivity(), "测试1不通过，进入下一轮测试", Toast.LENGTH_SHORT).show();
						listener.onTestComplete(2);
					}
					//listener.onTestComplete(114514);
				}
			});

        exitBtn.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					listener.onTestComplete(-1); // 退出
				}
			});

        updateStatusText();
        return view;
    }

    private void resetTest() {
        currentPhase = 0;
        clickStep = 1;
        updateStatusText();
        btn1.setBackgroundResource(android.R.color.darker_gray);
        btn2.setBackgroundResource(android.R.color.darker_gray);
        btn3.setBackgroundResource(android.R.color.darker_gray);
    }

    private void updateStatusText() {
        String phase = currentPhase == 0 ? "短按" : "长按";
        if (clickStep <= 3) {
            statusTextView.setText("阶段: " + phase + " (步骤 " + clickStep + "/3). 请点击按钮 " + clickStep + "。");
        } else {
            statusTextView.setText("阶段: " + phase + " 完成. 请点击'通过'或'不通过'。");
            if (currentPhase == 0) {
                // 短按完成后，进入长按阶段
                currentPhase = 1;
                clickStep = 1;
                Toast.makeText(getActivity(), "短按测试完成，现在开始长按测试", Toast.LENGTH_LONG).show();
                updateStatusText();
                btn1.setBackgroundResource(android.R.color.darker_gray);
                btn2.setBackgroundResource(android.R.color.darker_gray);
                btn3.setBackgroundResource(android.R.color.darker_gray);
            }
        }
    }

    // 处理短按事件
    @Override
    public void onClick(View v) {
        if (currentPhase == 0 && clickStep <= 3) {
            int buttonId = 0;
            if (v.getId() == R.id.button1) buttonId = 1;
            else if (v.getId() == R.id.button2) buttonId = 2;
            else if (v.getId() == R.id.button3) buttonId = 3;

            if (buttonId == clickStep) {
                v.setBackgroundResource(android.R.color.holo_green_light);
                clickStep++;
                updateStatusText();
            } else {
                Toast.makeText(getActivity(), "短按顺序错误! 请按 " + clickStep + "。", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // 处理长按事件
    @Override
    public boolean onLongClick(View v) {
        if (currentPhase == 1 && clickStep <= 3) {
            int buttonId = 0;
            if (v.getId() == R.id.button1) buttonId = 1;
            else if (v.getId() == R.id.button2) buttonId = 2;
            else if (v.getId() == R.id.button3) buttonId = 3;

            if (buttonId == clickStep) {
                v.setBackgroundResource(android.R.color.holo_blue_light);
                clickStep++;
                updateStatusText();
                return true; // 消耗事件
            } else {
                Toast.makeText(getActivity(), "长按顺序错误! 请长按 " + clickStep + "。", Toast.LENGTH_SHORT).show();
            }
        }
        return false; // 不消耗事件
    }
	
	
	
	

	// 点击模式切换
    private void showModeSwitchDialog() {

		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

		builder
			.setTitle("测试不通过，是否尝试切换操作模式？")
			.setMessage("当前模式为：" + (mode.equals("gesture")? "手势分发":"节点点击"))
			.setPositiveButton("确定", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					isModeSwitched = true;//标记一下，模式已经切换过了。
					Toast.makeText(getActivity(), "已切换为…模式，请重新测试", Toast.LENGTH_SHORT).show();
					resetTest();
				}
			})
			.setNegativeButton("取消", null);

		// 1. 先创建对话框（不能直接用builder，需先show()获取实例）
		AlertDialog dialog = builder.show();

		// 2. 获取“确认”按钮（DialogInterface.BUTTON_POSITIVE）并设置样式
		Button positiveBtn = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
		if (positiveBtn != null) {
			LinearLayout.LayoutParams positiveParams = (LinearLayout.LayoutParams) positiveBtn.getLayoutParams();
			positiveParams.leftMargin = dp2px(getActivity(), 30); // 确认按钮左边距30dp（与取消按钮隔开）
			positiveBtn.setLayoutParams(positiveParams);
			positiveBtn.setBackgroundResource(R.drawable.button_background_selector); // 应用选择器
			//positiveBtn.setPadding(30, 10, 30, 10); // 可选：调整按钮内边距，避免边框紧贴文字

		}

		// 3. 获取“取消”按钮（DialogInterface.BUTTON_NEGATIVE）并设置样式
		Button negativeBtn = dialog.getButton(DialogInterface.BUTTON_NEGATIVE);
		if (negativeBtn != null) {
			LinearLayout.LayoutParams negativeParams = (LinearLayout.LayoutParams) negativeBtn.getLayoutParams();
			negativeParams.rightMargin = dp2px(getActivity(), 10); // 取消按钮右边距10dp
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
	
	
	
	
}
