package com.flowercat.rfmouse.adb;

import android.app.Activity;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import com.flowercat.rfmouse.R;

public class AppProcessTestActivity extends Activity {

    private EditText mCmdInputEt;
    private Button mRunShellBtn;
    private TextView mOutputTv,gethigh;

    private void initView(){
        mCmdInputEt = findViewById(R.id.et_cmd);
        mRunShellBtn = findViewById(R.id.btn_runshell);
        mOutputTv = findViewById(R.id.tv_output);
		gethigh = findViewById(R.id.tv_gethigh);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.appprocess_test);
        initView();
		initFile();
        mRunShellBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String cmd = mCmdInputEt.getText().toString();
                if (TextUtils.isEmpty(cmd)) {
                    Toast.makeText(AppProcessTestActivity.this, "输入内容为空", Toast.LENGTH_SHORT).show();
                    return;
                }
                runShell(cmd);
            }
        });
		
		
		gethigh.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					executeShellCommand();
				}
			});
    }

	
	
	
	private void initFile() {
		//步骤1: 检查文件是否存在
        File targetDex = new File(getFilesDir(), "server.dex");
        if (!targetDex.exists()) {
            try {
                // 步骤2: 复制assets/classes.dex到目标位置
                copyAssetToFilesDir("classes.dex", "server.dex");
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
        }

        // 步骤3: 执行app_process命令
        //executeShellCommand();
	}

	private void copyAssetToFilesDir(String assetName, String outputName) throws Exception {
        AssetManager am = getAssets();
        try (InputStream is = am.open(assetName);
		OutputStream os = new FileOutputStream(new File(/*getFilesDir()*/ "/data/local/tmp", outputName))) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = is.read(buffer)) > 0) {
                os.write(buffer, 0, length);
            }
        }
    }
	
	/*我想要说的 前人们都说过了，我想要做的 有钱人都做过了。我想要的公平 都是不公们虚构的。*/
	
	private void executeShellCommand() {
        try {
            // 构建命令参数
            String[] cmd = {
                "/system/bin/app_process",
                "-Djava.class.path=" + "/data/local/tmp/server.dex"/*new File(getFilesDir(), "server.dex").getAbsolutePath()*/,
                "/system/bin",
                "shellService.Main" // 确保类名正确（需完整包名，如com.example.Main）
				
            };

            // 执行命令
            Runtime.getRuntime().exec(cmd);

			// 示例：尝试修改文件权限为 777（通常失败）
			//Runtime.getRuntime().exec("chmod 500 " + new File(getFilesDir(), "server.dex").getAbsolutePath());
			
			
			
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
	
	
	
    private void runShell(final String cmd){
        if (TextUtils.isEmpty(cmd)) return;
        new Thread(new Runnable() {
            @Override
            public void run() {
              new SocketClient(cmd, new SocketClient.onServiceSend() {
                  @Override
                  public void getSend(String result) {
                      showTextOnTextView(result);
                  }
              });
            }
        }).start();
    }

    private void showTextOnTextView(final String text){
      AppProcessTestActivity. this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (TextUtils.isEmpty(mOutputTv.getText())) {
                    mOutputTv.setText(text);
                } else {
                    mOutputTv.setText(mOutputTv.getText() + "\n" + text);
                }
            }
        });
    }
}
