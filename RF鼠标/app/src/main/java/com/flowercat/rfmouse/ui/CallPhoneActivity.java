package com.flowercat.rfmouse.ui;


import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.app.Activity;
import android.view.Window;
import android.view.WindowManager;
import com.flowercat.rfmouse.R;
import com.flowercat.rfmouse.service.FlowerMouseService;
import android.os.Build;


public class CallPhoneActivity extends Activity {

    private static final int PERMISSION_REQUEST_CALL_PHONE = 1;
    
    private EditText phoneNumberEditText;
    private Button dialButton;
    private Button backspaceButton;


	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                             WindowManager.LayoutParams.FLAG_FULLSCREEN);
		
        setContentView(R.layout.flower_call);
		
		if(FlowerMouseService.getInstance() != null){
			FlowerMouseService.getInstance().spaceMenu = true;
		}

        phoneNumberEditText = (EditText) findViewById(R.id.phoneNumberEditText);
        dialButton = (Button) findViewById(R.id.dialButton);
        backspaceButton = (Button) findViewById(R.id.backspaceButton);

        // 确保EditText可以获取焦点以接收按键事件
        phoneNumberEditText.requestFocus();
        phoneNumberEditText.setCursorVisible(true);

        // 设置触摸事件监听器，避免软键盘弹出
        phoneNumberEditText.setOnTouchListener(new View.OnTouchListener() {
				@Override
				public boolean onTouch(View v, MotionEvent event) {
					// 阻止软键盘弹出
					v.onTouchEvent(event);
					return true;
				}
			});

        // 拨打按钮点击事件
        dialButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					dialPhoneNumber();
				}
			});

        // 退格按钮点击事件
        backspaceButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					handleBackspace();
				}
			});
			
			
		phoneNumberEditText.setOnKeyListener(new View.OnKeyListener() {
				@Override
				public boolean onKey(View v, int keyCode, KeyEvent event) {
					// 只处理按键"按下"动作（避免重复触发）
					if (event.getAction() == KeyEvent.ACTION_DOWN) {
						// 判断按下的按键类型
						switch (keyCode) {
							case KeyEvent.KEYCODE_MENU:
								dialPhoneNumber();
							return true; // 消耗事件，不再传递
						}
					}
					// 未处理的事件交给系统默认处理
					return false;
				}
			});
				
     }
	 
	 
	 
	 

    // 处理退格操作
    private void handleBackspace() {
        int selectionStart = phoneNumberEditText.getSelectionStart();
        if (selectionStart > 0) {
            phoneNumberEditText.getText().delete(selectionStart - 1, selectionStart);
            phoneNumberEditText.setSelection(selectionStart - 1); // 移动光标
        }
    }

 

    // 拨打电话
    private void dialPhoneNumber() {
        String phoneNumber = phoneNumberEditText.getText().toString().trim();
        if (phoneNumber.isEmpty()) {
            //Toast.makeText(this, "请输入电话号码", Toast.LENGTH_SHORT).show();
            return;
        }

		//大于等于安卓6.0
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
		
        // 检查CALL_PHONE权限
        if (checkSelfPermission(Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            // 如果没有权限，请求权限
            requestPermissions( new String[]{Manifest.permission.CALL_PHONE}, PERMISSION_REQUEST_CALL_PHONE);
        } else {
            // 已经有权限，直接拨打
            makeCall(phoneNumber);
        }
		
		} else {
			// 已经有权限，直接拨打
            makeCall(phoneNumber);
		}
    }

    // 执行拨打电话的Intent
    private void makeCall(String phoneNumber) {
        try {
            Intent callIntent = new Intent(Intent.ACTION_CALL);
            callIntent.setData(Uri.parse("tel:" + phoneNumber));
            startActivity(callIntent);
        } catch (SecurityException e) {
            Toast.makeText(this, "拨号失败：权限不足或设备不支持", Toast.LENGTH_LONG).show();
            e.printStackTrace();
        } catch (Exception e) {
            Toast.makeText(this, "拨号失败：" + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    // 处理权限请求结果
    @Override
    public void onRequestPermissionsResult(int requestCode,  String[] permissions,  int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CALL_PHONE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 权限已授予，重新尝试拨打
                dialPhoneNumber();
            } else {
                Toast.makeText(this, "拨打电话权限被拒绝，无法拨打电话", Toast.LENGTH_SHORT).show();
            }
        }
    }

	@Override
	protected void onNewIntent(Intent intent) {
		if(phoneNumberEditText != null){
			phoneNumberEditText.setText("");
		}
		if(FlowerMouseService.getInstance() != null){
			FlowerMouseService.getInstance().spaceMenu = true;
		}
		super.onNewIntent(intent);
	}

	@Override
	protected void onDestroy()
	{
	super.onDestroy();
		if(FlowerMouseService.getInstance() != null){
			FlowerMouseService.getInstance().spaceMenu = false;
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		if(FlowerMouseService.getInstance() != null){
			FlowerMouseService.getInstance().spaceMenu = false;
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		if(FlowerMouseService.getInstance() != null){
			FlowerMouseService.getInstance().spaceMenu = true;
		}
	}

	

	
	
	
}
