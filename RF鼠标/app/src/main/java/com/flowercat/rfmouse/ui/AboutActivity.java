package com.flowercat.rfmouse.ui;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;
import com.flowercat.rfmouse.R;

public class AboutActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                             WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.about_miao);

        // 设置链接点击事件（XML中已通过autoLink实现，这里演示自定义处理）
        TextView projectUrl = findViewById(R.id.tv_project_url);
        projectUrl.setMovementMethod(LinkMovementMethod.getInstance());

		TextView projectUrl1 = findViewById(R.id.tv_project_url_1);
        projectUrl1.setMovementMethod(LinkMovementMethod.getInstance());
		
		
		
        // 自定义链接点击行为
        final String url = projectUrl.getText().toString();
        SpannableString spannableString = new SpannableString(url);
        spannableString.setSpan(new ClickableSpan() {
                @Override
                public void onClick(View widget) {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse(url));
                    startActivity(intent);
                }

                @Override
                public void updateDrawState(TextPaint ds) {
                    super.updateDrawState(ds);
                    ds.setUnderlineText(true);
                    ds.setColor(getResources().getColor(android.R.color.holo_blue_dark));
                }
            }, 0, url.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        projectUrl.setText(spannableString);
		
		
		// 自定义链接点击行为
        final String url1 = projectUrl1.getText().toString();
        SpannableString spannableString1 = new SpannableString(url1);
        spannableString1.setSpan(new ClickableSpan() {
                @Override
                public void onClick(View widget) {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse(url1));
                    startActivity(intent);
                }

                @Override
                public void updateDrawState(TextPaint ds) {
                    super.updateDrawState(ds);
                    ds.setUnderlineText(true);
                    ds.setColor(getResources().getColor(android.R.color.holo_blue_dark));
                }
            }, 0, url1.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        projectUrl1.setText(spannableString1);
	
    }
	
	

    // 复制到剪贴板功能
    private void copyToClipboard(String text) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("project_url", text);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(this, "链接已复制", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
        overridePendingTransition(R.anim.slide_in,R.anim.slide_out);
    }
    
    
    
}

