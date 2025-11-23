package com.flowercat.rfmouse.util;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import com.flowercat.rfmouse.R;
import android.content.DialogInterface;

public class SystemOverlayHelper {

    private static final int TOAST_DURATION_SHORT = 2000;
    private static final int TOAST_DURATION_LONG = 3500;
    private static Handler mHandler = new Handler(Looper.getMainLooper());

    private Context mContext;
    private WindowManager mWindowManager;
    private Dialog mCurrentDialog;
    private View mCurrentToastView;
	
	
	private final Runnable mDismissRunnable = new Runnable() {
        @Override
        public void run() {
            dismissCurrentToast();
        }
    };
	

    private static SystemOverlayHelper instance;

    public static SystemOverlayHelper getInstance(Context context) {
        if (instance == null) {
            instance = new SystemOverlayHelper(context.getApplicationContext());
        }
        return instance;
    }

    private SystemOverlayHelper(Context context) {
        this.mContext = context;
        this.mWindowManager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
    }

    public interface OnListItemClickListener {
        void onListItemClick(int position, String text);
		void onDismiss();
    }

    /**
     * 显示一个列表形式的悬浮对话框
     */
    public void showListDialog(boolean isSystem, String[] items, final OnListItemClickListener listener) {
        if (items == null || items.length == 0) {
            return;
        }

        dismissCurrentDialog();

        // 使用一个不带主题的Dialog
        mCurrentDialog = new Dialog(mContext);
        mCurrentDialog.setCancelable(true);
        mCurrentDialog.setCanceledOnTouchOutside(true);

        int overlayType;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            overlayType = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY |
				WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY;
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            overlayType = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
        } else {
            overlayType = WindowManager.LayoutParams.TYPE_PHONE;
        }
		//如果是系统级
		if(isSystem){
        	mCurrentDialog.getWindow().setType(overlayType);
		}
		
        mCurrentDialog.getWindow().setGravity(Gravity.CENTER);
        mCurrentDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        mCurrentDialog.getWindow().setDimAmount(0.5f); // 设置背景变暗

        LayoutInflater inflater = LayoutInflater.from(mContext);
        View dialogView = inflater.inflate(R.layout.overlay_list_dialog, null);
        mCurrentDialog.setContentView(dialogView);

        // 显式设置ListView中文字的颜色
        ListView listView = (ListView) dialogView.findViewById(R.id.list_view);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(mContext, android.R.layout.simple_list_item_1, android.R.id.text1, items) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView textView = (TextView) view.findViewById(android.R.id.text1);
                textView.setTextColor(Color.BLACK); // 强制设置为黑色
                return view;
            }
        };
        listView.setAdapter(adapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
				@Override
				public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
					if (listener != null) {
						listener.onListItemClick(position, (String) parent.getItemAtPosition(position));
					}
					mCurrentDialog.dismiss();
				}
			});

        try {
            mCurrentDialog.show();
			
			mCurrentDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
					@Override
					public void onDismiss(DialogInterface dialog) {
						// Dialog消失后的逻辑处理
						if (listener != null) {
							listener.onDismiss();
						}
					}
				});
			
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(mContext, "无法显示对话框，请检查悬浮窗权限。", Toast.LENGTH_SHORT).show();
            dismissCurrentDialog();
        }
    }

    /**
     * 移除当前显示的悬浮对话框
     */
    public void dismissCurrentDialog() {
        if (mCurrentDialog != null && mCurrentDialog.isShowing()) {
            try {
                mCurrentDialog.dismiss();
                mCurrentDialog = null;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 显示一个自定义的悬浮Toast
     */
    public void showCustomToast(String text, int duration, Drawable icon) {
		
		if(mHandler !=null){
			// 在添加新 Toast 之前，先移除所有旧的 Handler 回调
        	mHandler.removeCallbacks(mDismissRunnable);
        }
		
        dismissCurrentToast();

        LayoutInflater inflater = LayoutInflater.from(mContext);
        mCurrentToastView = inflater.inflate(R.layout.overlay_toast, null);

        TextView textView = (TextView) mCurrentToastView.findViewById(R.id.toast_text);
        ImageView imageView = (ImageView) mCurrentToastView.findViewById(R.id.toast_icon);

        textView.setText(text);
        if (icon != null) {
            imageView.setImageDrawable(icon);
            imageView.setVisibility(View.VISIBLE);
        } else {
            imageView.setVisibility(View.GONE);
        }

        int toastDuration = (duration == Toast.LENGTH_LONG) ? TOAST_DURATION_LONG : TOAST_DURATION_SHORT;
        WindowManager.LayoutParams params = createToastLayoutParams();

        try {
            mWindowManager.addView(mCurrentToastView, params);

            Animation animation = AnimationUtils.loadAnimation(mContext, R.anim.slide_in_left);
            mCurrentToastView.startAnimation(animation);

            // 重新设置新的延时任务
            mHandler.postDelayed(mDismissRunnable, toastDuration);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(mContext, "无法显示Toast，请检查悬浮窗权限。", Toast.LENGTH_SHORT).show();
            dismissCurrentToast();
        }
    }

    /**
     * 移除当前显示的悬浮Toast
     */
    public void dismissCurrentToast() {
        if (mCurrentToastView != null) {
            try {
                mWindowManager.removeView(mCurrentToastView);
                mCurrentToastView = null;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private WindowManager.LayoutParams createToastLayoutParams() {
        final WindowManager.LayoutParams params = new WindowManager.LayoutParams();
        params.width = ViewGroup.LayoutParams.WRAP_CONTENT;
        params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        params.format = PixelFormat.TRANSLUCENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            params.type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY |
				WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY;
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            params.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
        } else {
            params.type = WindowManager.LayoutParams.TYPE_PHONE;
        }
        params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
			| WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
			| WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
        params.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        params.y = (int) (mContext.getResources().getDisplayMetrics().density * 100);
        params.windowAnimations = R.style.ToastAnimation;
        return params;
    }
}
