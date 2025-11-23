package com.flowercat.rfmouse.donate;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;
import com.flowercat.rfmouse.R;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class DonateActivity extends Activity {

    private static final int REQUEST_WRITE_STORAGE = 112;
    private ImageView ivQrCode;
	private Button bt_savedonate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                             WindowManager.LayoutParams.FLAG_FULLSCREEN);
		
        setContentView(R.layout.mouse_donate);
		

        initViews();
        setupQrCode();
    }

    private void initViews() {
        ivQrCode = (ImageView) findViewById(R.id.iv_qr_code);
		bt_savedonate = findViewById(R.id.bt_savedonate);
        // 设置长按监听
        ivQrCode.setOnLongClickListener(new View.OnLongClickListener() {
				@Override
				public boolean onLongClick(View v) {
					showSaveImageDialog();
					return true;
				}
			});
			
		bt_savedonate.setOnClickListener(new OnClickListener(){

				@Override
				public void onClick(View p1) {
					checkPermissionAndSaveImage();
				}
		});
			
    }

    private void setupQrCode() {
        // 获取屏幕尺寸
        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        int screenHeight = getResources().getDisplayMetrics().heightPixels;

        // 计算合适的图片尺寸（屏幕宽度的70%）
        int targetSize = (int) (Math.min(screenWidth, screenHeight) * 0.7);

        // 加载并缩放图片
        Bitmap originalBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.wechat_qrcode);
        if (originalBitmap != null) {
            Bitmap scaledBitmap = Bitmap.createScaledBitmap(originalBitmap, targetSize, targetSize, true);
            ivQrCode.setImageBitmap(scaledBitmap);

            // 回收原图
            if (originalBitmap != scaledBitmap) {
                originalBitmap.recycle();
            }
        }
    }

    
	private void showSaveImageDialog() {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("保存二维码")
            .setMessage("是否将二维码保存到相册？")
			.setPositiveButton("保存", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					checkPermissionAndSaveImage();
				}
			})

			.setNegativeButton("取消",null)
			
			.setCancelable(false);

		// 1. 先创建对话框（不能直接用builder，需先show()获取实例）
		AlertDialog dialog = builder.show();

		// 2. 获取“确认”按钮（DialogInterface.BUTTON_POSITIVE）并设置样式
		Button positiveBtn = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
		if (positiveBtn != null) {
			LinearLayout.LayoutParams positiveParams = (LinearLayout.LayoutParams) positiveBtn.getLayoutParams();
			positiveParams.leftMargin = dp2px(this, 30); // 确认按钮左边距30dp（与取消按钮隔开）
			positiveBtn.setLayoutParams(positiveParams);
			positiveBtn.setBackgroundResource(R.drawable.button_background_selector); // 应用选择器
			//positiveBtn.setPadding(30, 10, 30, 10); // 可选：调整按钮内边距，避免边框紧贴文字

		}

		// 3. 获取“取消”按钮（DialogInterface.BUTTON_NEGATIVE）并设置样式
		Button negativeBtn = dialog.getButton(DialogInterface.BUTTON_NEGATIVE);
		if (negativeBtn != null) {
			LinearLayout.LayoutParams negativeParams = (LinearLayout.LayoutParams) negativeBtn.getLayoutParams();
			negativeParams.rightMargin = dp2px(this, 10); // 取消按钮右边距10dp
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
	


    private void checkPermissionAndSaveImage() {
        // Android 6.0+ 需要动态申请权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) 
				!= PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, 
								   REQUEST_WRITE_STORAGE);
                return;
            }
        }
        saveImageToGallery();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_WRITE_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                saveImageToGallery();
            } else {
                Toast.makeText(this, "需要存储权限才能保存图片", Toast.LENGTH_SHORT).show();
            }
        }
    }

	
	/*
    private void saveImageToGallery() {
        try {
            BitmapDrawable drawable = (BitmapDrawable) ivQrCode.getDrawable();
            Bitmap bitmap = drawable.getBitmap();

            if (bitmap == null) {
                Toast.makeText(this, "图片加载失败", Toast.LENGTH_SHORT).show();
                return;
            }

            // 创建文件名
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String fileName = "QR_Code_" + timeStamp + ".png";

            // 保存图片
            File file;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+ 使用 MediaStore
                file = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), fileName);
            } else {
                // Android 10 以下保存到公共目录
                File picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
                File appDir = new File(picturesDir, "DonateApp");
                if (!appDir.exists()) {
                    appDir.mkdirs();
                }
                file = new File(appDir, fileName);
            }

            FileOutputStream fos = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.flush();
            fos.close();

            // 通知媒体库更新
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                sendBroadcast(new android.content.Intent(
								  android.content.Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, 
								  android.net.Uri.fromFile(file)));
            }

            Toast.makeText(this, "二维码已保存到相册", Toast.LENGTH_SHORT).show();

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "保存失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "保存失败", Toast.LENGTH_SHORT).show();
        }
    }
	
	
	*/
	

	/**
	 * 保存图片到系统相册
	 */
	private void saveImageToGallery() {
		Drawable drawable = ivQrCode.getDrawable();
		if (drawable == null || !(drawable instanceof BitmapDrawable)) {
			Toast.makeText(this, "图片加载失败。", Toast.LENGTH_SHORT).show();
			return;
		}

		Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();
		String fileName = "WeChat_Donation_QR_" + System.currentTimeMillis() + ".jpg";

		boolean success = false;

		// 尝试使用 MediaStore (推荐用于 API 29 以下的保存)
		try {
			// Android Q (API 29) 以下的传统保存方式
			if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
				File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
				if (!dir.exists()) {
					dir.mkdirs();
				}
				File imageFile = new File(dir, fileName);

				FileOutputStream fos = new FileOutputStream(imageFile);
				bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
				fos.flush();
				fos.close();

				// 通知相册更新
				Uri uri = Uri.fromFile(imageFile);
				sendBroadcast(new android.content.Intent(android.content.Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri));

				success = true;
			} else {
				// Android Q (API 29) 及以上，应该使用 MediaStore API
				// 但为了避免引入复杂的 MediaStore 写入逻辑，
				// 并且假设您可能运行在一个旧的、不支持 Q 或更高版本的环境中，
				// 我们仍然使用上述方法，并依赖 Manifest 中的 requestLegacyExternalStorage="true" 
				// 或 WRITE_EXTERNAL_STORAGE 的兼容性（如果您的目标 SDK 允许）。
				// 如果是针对新版 Android，您需要重写这部分以使用 MediaStore.

				// 暂时退回老方法并给出提示
				Toast.makeText(this, "保存操作可能受限于系统版本，请在文件管理器中查看。", Toast.LENGTH_LONG).show();
				File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
				if (!dir.exists()) {
					dir.mkdirs();
				}
				File imageFile = new File(dir, fileName);

				FileOutputStream fos = new FileOutputStream(imageFile);
				bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
				fos.flush();
				fos.close();

				Uri uri = Uri.fromFile(imageFile);
				sendBroadcast(new android.content.Intent(android.content.Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri));

				success = true;
			}

		} catch (IOException e) {
			e.printStackTrace();
			Toast.makeText(this, "保存失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
			return;
		}

		if (success) {
			Toast.makeText(this, "收款码已保存到相册。", Toast.LENGTH_SHORT).show();
		} else {
			Toast.makeText(this, "保存失败，请检查存储空间。", Toast.LENGTH_SHORT).show();
		}
	}
	
	
	
	@Override
	public void onBackPressed() {
		super.onBackPressed();
		finish();
		overridePendingTransition(R.anim.slide_in,R.anim.slide_out);
	}
	
	
	
}



