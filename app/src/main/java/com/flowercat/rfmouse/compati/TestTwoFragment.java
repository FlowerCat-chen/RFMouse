package com.flowercat.rfmouse.compati;

// TestTwoFragment.java
import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import com.flowercat.rfmouse.R;

public class TestTwoFragment extends Fragment {

    private TextView statusTextView;
    private ScrollView verticalScrollView;
    private HorizontalScrollView horizontalScrollView;
    private int scrollStep = 1; // 1:上, 2:下, 3:左, 4:右
    private TestFragmentListener listener;

    // 适配 onAttach
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        // ... (省略与 TestOneFragment 相同的 onAttach 逻辑)
    }

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
        View view = inflater.inflate(R.layout.fragment_test_two, container, false);
        statusTextView = (TextView) view.findViewById(R.id.status_text_two);
        verticalScrollView = (ScrollView) view.findViewById(R.id.vertical_scroll_view);
        horizontalScrollView = (HorizontalScrollView) view.findViewById(R.id.horizontal_scroll_view);

		Button passBtn2 = (Button) view.findViewById(R.id.pass_button2);
        Button failBtn2 = (Button) view.findViewById(R.id.fail_button2);
        Button exitBtn2 = (Button) view.findViewById(R.id.exit_button2);
		
		
		passBtn2.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					// 如果当前阶段完成，则进入下一阶段
					if (scrollStep == 5){
						// 所有滚动完成
						updateStatusText();
						Toast.makeText(getActivity(), "滚动测试完成，进入拖动条测试", Toast.LENGTH_LONG).show();
						listener.onTestComplete(3); // 进入 TestThree
					} else {
						Toast.makeText(getActivity(), "请先完成当前阶段的所有步骤", Toast.LENGTH_SHORT).show();
					}
				}
			});

        failBtn2.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					Toast.makeText(getActivity(), "测试2不通过，开始下一轮", Toast.LENGTH_SHORT).show();
					listener.onTestComplete(3);
				}
			});

        exitBtn2.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					listener.onTestComplete(-1); // 退出
				}
			});
		
		
        // 使用 postDelayed 确保 View 渲染完成后再监听，并设置监听器
        // Android 4.4 及其之前版本没有 onScrollChangeListener
        // 所以我们使用一个简单的 Runnable 来定期检查滚动位置变化
        view.post(new Runnable() {
				@Override
				public void run() {
					startScrollCheck();
				}
			});

        updateStatusText();
        return view;
    }

	/*
    // 简单地每隔一段时间检查滚动位置，这是一种兼容旧API的通用方法
    private void startScrollCheck() {
        final int checkInterval = 200; // 200ms 检查一次
        final View decorView = getActivity().getWindow().getDecorView();

        decorView.post(new Runnable() {
				private int lastScrollY = verticalScrollView.getScrollY();
				private int lastScrollX = horizontalScrollView.getScrollX();

				@Override
				public void run() {
					if (listener == null) return; // Fragment 可能已销毁

					boolean scrollDetected = false;
					int currentY = verticalScrollView.getScrollY();
					int currentX = horizontalScrollView.getScrollX();
					int maxScrollY = verticalScrollView.getChildAt(0).getHeight() - verticalScrollView.getHeight();
					int maxScrollX = horizontalScrollView.getChildAt(0).getWidth() - horizontalScrollView.getWidth();

					if (scrollStep == 1) { // 向上滑动 (Scroll Up) -> ScrollY 减小
						if (currentY < lastScrollY && currentY > 0) {
							scrollDetected = true;
							Toast.makeText(getActivity(), "检测到向上滑动", Toast.LENGTH_SHORT).show();
							scrollStep = 2;
						}
					} else if (scrollStep == 2) { // 向下滑动 (Scroll Down) -> ScrollY 增大
						if (currentY > lastScrollY && currentY < maxScrollY) {
							scrollDetected = true;
							Toast.makeText(getActivity(), "检测到向下滑动", Toast.LENGTH_SHORT).show();
							scrollStep = 3;
						}
					} else if (scrollStep == 3) { // 向左滑动 (Scroll Left) -> ScrollX 减小
						if (currentX < lastScrollX && currentX > 0) {
							scrollDetected = true;
							Toast.makeText(getActivity(), "检测到向左滑动", Toast.LENGTH_SHORT).show();
							scrollStep = 4;
						}
					} else if (scrollStep == 4) { // 向右滑动 (Scroll Right) -> ScrollX 增大
						if (currentX > lastScrollX && currentX < maxScrollX) {
							scrollDetected = true;
							Toast.makeText(getActivity(), "检测到向右滑动", Toast.LENGTH_SHORT).show();
							scrollStep = 5; // 完成
						}
					}

					if (scrollStep == 5) {
						// 所有滚动完成
						updateStatusText();
						Toast.makeText(getActivity(), "滚动测试完成，进入拖动条测试", Toast.LENGTH_LONG).show();
						listener.onTestComplete(3); // 进入 TestThree
						return; // 结束检查
					}

					lastScrollY = currentY;
					lastScrollX = currentX;
					if (scrollDetected) {
						updateStatusText();
					}

					// 继续检查
					decorView.postDelayed(this, checkInterval);
				}
			});
    }
	
	*/

    // 简单地每隔一段时间检查滚动位置，这是一种兼容旧API的通用方法
    private void startScrollCheck() {
        final int checkInterval = 200; // 200ms 检查一次
        final View decorView = getActivity().getWindow().getDecorView();

        decorView.post(new Runnable() {
				private int lastScrollY = verticalScrollView.getScrollY();
				private int lastScrollX = horizontalScrollView.getScrollX();

				@Override
				public void run() {
					if (listener == null) return; // Fragment 可能已销毁

					boolean scrollDetected = false;
					int currentY = verticalScrollView.getScrollY();
					int currentX = horizontalScrollView.getScrollX();
					int maxScrollY = verticalScrollView.getChildAt(0).getHeight() - verticalScrollView.getHeight();
					int maxScrollX = horizontalScrollView.getChildAt(0).getWidth() - horizontalScrollView.getWidth();

					if (scrollStep == 1) { // 步骤 1: 向下滑动 (Scroll Down) -> ScrollY 增大
						if (currentY > lastScrollY && currentY < maxScrollY) {
							scrollDetected = true;
							Toast.makeText(getActivity(), "检测到向上滑动", Toast.LENGTH_SHORT).show();
							scrollStep = 2;
						}
					} else if (scrollStep == 2) { // 步骤 2: 向上滑动 (Scroll Up) -> ScrollY 减小
						if (currentY < lastScrollY && currentY > 0) {
							scrollDetected = true;
							Toast.makeText(getActivity(), "检测到向下滑动", Toast.LENGTH_SHORT).show();
							scrollStep = 3;
						}
					
					} else if (scrollStep == 3) { // 步骤 3: 向右滑动 (Scroll Right) -> ScrollX 增大
						if (currentX > lastScrollX && currentX < maxScrollX) {
							scrollDetected = true;
							Toast.makeText(getActivity(), "检测到向左滑动", Toast.LENGTH_SHORT).show();
							scrollStep = 4; // 完成
						}
					} else if (scrollStep == 4) { // 步骤 4: 向左滑动 (Scroll Left) -> ScrollX 减小
						if (currentX < lastScrollX && currentX > 0) {
							scrollDetected = true;
							Toast.makeText(getActivity(), "检测到向右滑动", Toast.LENGTH_SHORT).show();
							scrollStep = 5;
						}
						
					}

					if (scrollStep == 5) {
						return; // 结束检查
					}

					lastScrollY = currentY;
					lastScrollX = currentX;
					if (scrollDetected) {
						updateStatusText();
					}

					// 继续检查
					decorView.postDelayed(this, checkInterval);
				}
			});
    }

   

	
	

    private void updateStatusText() {
        String action;
        switch (scrollStep) {
            case 1: action = "向上 (Scroll Up)"; break;
            case 2: action = "向下 (Scroll Down)"; break;
            case 3: action = "向左 (Scroll Left)"; break;
            case 4: action = "向右 (Scroll Right)"; break;
            default: action = "完成! 请等待自动跳转..."; break;
        }
        statusTextView.setText("步骤 " + scrollStep + "/4: 请滑动 ScrollView " + action);
    }
}
