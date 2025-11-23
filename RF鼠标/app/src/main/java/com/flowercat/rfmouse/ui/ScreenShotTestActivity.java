package com.flowercat.rfmouse.ui;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;
import com.flowercat.rfmouse.util.ScreenCaptureHelper;
import com.flowercat.rfmouse.R;
import android.os.Build;



public class ScreenShotTestActivity extends Activity {
	
    private static final int REQUEST_SCREEN_CAPTURE = 100;
    private static final String TAG = "ScreenCapture";

    private ScreenCaptureHelper mScreenCapture;
    private ImageView mPreview;
    private Button mCaptureButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		
		requestWindowFeature(Window.FEATURE_NO_TITLE);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                             WindowManager.LayoutParams.FLAG_FULLSCREEN);
		
        setContentView(R.layout.screen_shot);

        mPreview = findViewById(R.id.preview);
        mCaptureButton = findViewById(R.id.capture_button);

        // 初始化截屏助手
        mScreenCapture = ScreenCaptureHelper.getInstance(this);

        mCaptureButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if (Build.VERSION.SDK_INT <21){
						showError("安卓5及以下没有截屏类");
						return;//安卓5及以下没有截屏。
					}
					
					if (!mScreenCapture.isInitialized()) {
						startScreenCapture();
					} else {
						captureScreenshot();
					}
				}
			});
    }

    private void startScreenCapture() {
        MediaProjectionManager projectionManager = 
            (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);

        if (projectionManager == null) {
            showError("无法获取屏幕捕捉服务");
            return;
        }

        try {
            Intent captureIntent = projectionManager.createScreenCaptureIntent();
            startActivityForResult(captureIntent, REQUEST_SCREEN_CAPTURE);
        } catch (Exception e) {
            Log.e(TAG, "启动屏幕捕捉失败: " + e.getMessage());
            showError("启动屏幕捕捉失败: " + e.getLocalizedMessage());
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_SCREEN_CAPTURE) return;

        if (resultCode != Activity.RESULT_OK || data == null) {
            showError("用户拒绝屏幕捕捉权限");
            return;
        }

        try {
            mScreenCapture.initialize(resultCode, data);
            captureScreenshot();
        } catch (ScreenCaptureHelper.ScreenCaptureException e) {
            Log.e(TAG, "初始化失败: " + e.getMessage());
            showError("初始化失败: " + e.getLocalizedMessage());
        }
    }

    private void captureScreenshot() {
        mScreenCapture.captureScreenshot(new ScreenCaptureHelper.CaptureCallback() {
				@Override
				public void onCaptureSuccess(Bitmap bitmap) {
					mPreview.setImageBitmap(bitmap);
				}

				@Override
				public void onCaptureError(String error) {
					showError(error);
				}
			});
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        if (mScreenCapture != null) {
            //mScreenCapture.release();
        }
        super.onDestroy();
    }
	
	@Override
	public void onBackPressed() {
		super.onBackPressed();
		finish();
		overridePendingTransition(R.anim.slide_in,R.anim.slide_out);
	}
	
}

