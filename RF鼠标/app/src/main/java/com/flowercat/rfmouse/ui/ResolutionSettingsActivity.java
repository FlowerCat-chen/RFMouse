package com.flowercat.rfmouse.ui;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import com.flowercat.rfmouse.R;
import com.flowercat.rfmouse.util.RootShellManager;
import java.util.ArrayList;
import java.util.List;

public class ResolutionSettingsActivity extends Activity {

    private Spinner resolutionSpinner;
    private Spinner dpiSpinner;
    private EditText widthEditText;
    private EditText heightEditText;
    private EditText dpiEditText;
    private Button applyButton;
    private Button resetButton;
    private Button applyDpiButton;
    private Button resetDpiButton;
    private TextView statusTextView;

    private RootShellManager rootShellManager;
    private List<Resolution> resolutionList;
    private List<Dpi> dpiList;
    private String currentResolution;
    private String currentDpi;
	private Button exitButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                             WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.tool_resolution_setting);

        // 初始化视图
        resolutionSpinner = findViewById(R.id.resolution_spinner);
        dpiSpinner = findViewById(R.id.dpi_spinner);
        widthEditText = findViewById(R.id.width_edittext);
        heightEditText = findViewById(R.id.height_edittext);
        dpiEditText = findViewById(R.id.dpi_edittext);
        applyButton = findViewById(R.id.apply_button);
        resetButton = findViewById(R.id.reset_button);
        applyDpiButton = findViewById(R.id.apply_dpi_button);
        resetDpiButton = findViewById(R.id.reset_dpi_button);
        statusTextView = findViewById(R.id.status_textview);

		exitButton = findViewById(R.id.bt_exit_resolution);
        // 初始化RootShellManager
        rootShellManager = RootShellManager.getInstance();

        // 初始化分辨率列表
        initResolutionList();

        // 初始化DPI列表
        initDpiList();

        // 设置分辨率Spinner适配器
        ArrayAdapter<String> resolutionAdapter = new ArrayAdapter<>(
            this, android.R.layout.simple_spinner_item, getResolutionNames());
        resolutionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        resolutionSpinner.setAdapter(resolutionAdapter);

        // 设置DPI Spinner适配器
        ArrayAdapter<String> dpiAdapter = new ArrayAdapter<>(
            this, android.R.layout.simple_spinner_item, getDpiNames());
        dpiAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        dpiSpinner.setAdapter(dpiAdapter);

        // 设置分辨率Spinner选择监听
        resolutionSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
				@Override
				public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
					if (position > 0) { // 跳过"请选择"项
						Resolution selected = resolutionList.get(position - 1);
						widthEditText.setText(String.valueOf(selected.width));
						heightEditText.setText(String.valueOf(selected.height));
					}
				}

				@Override
				public void onNothingSelected(AdapterView<?> parent) {
					// 什么都不做
				}
			});

        // 设置DPI Spinner选择监听
        dpiSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
				@Override
				public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
					if (position > 0) { // 跳过"请选择"项
						Dpi selected = dpiList.get(position - 1);
						dpiEditText.setText(String.valueOf(selected.value));
					}
				}

				@Override
				public void onNothingSelected(AdapterView<?> parent) {
					// 什么都不做
				}
			});

        // 设置应用分辨率按钮点击事件
        applyButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					applyResolution();
				}
			});

        // 设置重置分辨率按钮点击事件
        resetButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					resetResolution();
				}
			});

        // 设置应用DPI按钮点击事件
        applyDpiButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					applyDpi();
				}
			});

        // 设置重置DPI按钮点击事件
        resetDpiButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					resetDpi();
				}
			});
			
		// 设置应用分辨率按钮点击事件
        exitButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					finish();
				}
			});
			

        // 初始化Root会话并获取当前分辨率和DPI
        new InitRootTask().execute();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 关闭Root会话
        if (rootShellManager != null) {
            rootShellManager.closeSuSession();
        }
    }

    private void initResolutionList() {
        resolutionList = new ArrayList<>();
        resolutionList.add(new Resolution(320, 240, "SMALL"));
        resolutionList.add(new Resolution(240, 320, "SMALL"));
        resolutionList.add(new Resolution(240, 240, "SMALL"));
        resolutionList.add(new Resolution(320, 320, "SMALL"));
        resolutionList.add(new Resolution(480, 640, "MID"));
        resolutionList.add(new Resolution(640, 480, "MID"));
        resolutionList.add(new Resolution(480, 480, "MID"));
        resolutionList.add(new Resolution(640, 640, "MID"));
        resolutionList.add(new Resolution(800, 600, "SVGA"));
        resolutionList.add(new Resolution(1024, 768, "XGA"));
        resolutionList.add(new Resolution(1280, 720, "HD"));
        resolutionList.add(new Resolution(1366, 768, "WXGA"));
        resolutionList.add(new Resolution(1920, 1080, "Full HD"));
        resolutionList.add(new Resolution(2560, 1440, "QHD"));
        resolutionList.add(new Resolution(3840, 2160, "4K UHD"));
    }

    private void initDpiList() {
        dpiList = new ArrayList<>();
        dpiList.add(new Dpi(120, "极低DPI"));
        dpiList.add(new Dpi(160, "低DPI"));
        dpiList.add(new Dpi(240, "中DPI"));
        dpiList.add(new Dpi(320, "高DPI"));
        dpiList.add(new Dpi(480, "极高DPI"));
    }

    private List<String> getResolutionNames() {
        List<String> names = new ArrayList<>();
        names.add("请选择预设分辨率▼");
        for (Resolution res : resolutionList) {
            names.add(res.toString());
        }
        return names;
    }

    private List<String> getDpiNames() {
        List<String> names = new ArrayList<>();
        names.add("请选择预设DPI▼");
        for (Dpi dpi : dpiList) {
            names.add(dpi.toString());
        }
        return names;
    }

    private void applyResolution() {
        String widthStr = widthEditText.getText().toString();
        String heightStr = heightEditText.getText().toString();

        if (widthStr.isEmpty() || heightStr.isEmpty()) {
            Toast.makeText(this, "请输入宽度和高度", Toast.LENGTH_SHORT).show();
            return;
        }

        int width = Integer.parseInt(widthStr);
        int height = Integer.parseInt(heightStr);

        if (width < 100 || height < 100) {
            Toast.makeText(this, "分辨率不能小于100x100", Toast.LENGTH_SHORT).show();
            return;
        }

        new ApplyResolutionTask().execute(width + "x" + height);
    }

    private void resetResolution() {
        new ResetResolutionTask().execute();
    }

    private void applyDpi() {
        String dpiStr = dpiEditText.getText().toString();

        if (dpiStr.isEmpty()) {
            Toast.makeText(this, "请输入DPI值", Toast.LENGTH_SHORT).show();
            return;
        }

        int dpi = Integer.parseInt(dpiStr);

        if (dpi < 80 || dpi > 600) {
            Toast.makeText(this, "DPI值应在80-600之间", Toast.LENGTH_SHORT).show();
            return;
        }

        new ApplyDpiTask().execute(String.valueOf(dpi));
    }

    private void resetDpi() {
        new ResetDpiTask().execute();
    }

    // 内部类：分辨率对象
    private class Resolution {
        int width;
        int height;
        String name;

        Resolution(int width, int height, String name) {
            this.width = width;
            this.height = height;
            this.name = name;
        }

        @Override
        public String toString() {
            return name + " (" + width + "x" + height + ")";
        }
    }

    // 内部类：DPI对象
    private class Dpi {
        int value;
        String name;

        Dpi(int value, String name) {
            this.value = value;
            this.name = name;
        }

        @Override
        public String toString() {
            return name + " (" + value + ")";
        }
    }

    // 异步任务：初始化Root会话
    private class InitRootTask extends AsyncTask<Void, Void, Boolean> {
        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                rootShellManager.initializeSuSession();
                return true;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (success) {
                // 获取当前分辨率
                rootShellManager.executeCommandWithResult("wm size", new RootShellManager.CommandCallback() {
						@Override
						public void onSuccess(final String result) {
							runOnUiThread(new Runnable() {
									@Override
									public void run() {
										currentResolution = result.trim();
										statusTextView.setText("当前状态: " + currentResolution + " | ");
									}
								});

							// 获取当前DPI
							rootShellManager.executeCommandWithResult("wm density", new RootShellManager.CommandCallback() {
									@Override
									public void onSuccess(final String result) {
										runOnUiThread(new Runnable() {
												@Override
												public void run() {
													currentDpi = result.trim();
													statusTextView.setText(statusTextView.getText().toString() + currentDpi);
												}
											});
									}

									@Override
									public void onFailure(String error) {
										runOnUiThread(new Runnable() {
												@Override
												public void run() {
													statusTextView.setText(statusTextView.getText().toString() + "无法获取当前DPI");
												}
											});
									}
								});
						}

						@Override
						public void onFailure(String error) {
							runOnUiThread(new Runnable() {
									@Override
									public void run() {
										statusTextView.setText("当前状态: 无法获取当前分辨率 | ");
									}
								});
						}
					});
            } else {
                Toast.makeText(ResolutionSettingsActivity.this, 
                               "无法获取Root权限", Toast.LENGTH_SHORT).show();
                statusTextView.setText("当前状态: 无Root权限");
                applyButton.setEnabled(false);
                resetButton.setEnabled(false);
                applyDpiButton.setEnabled(false);
                resetDpiButton.setEnabled(false);
            }
        }
    }

    // 异步任务：应用分辨率
    private class ApplyResolutionTask extends AsyncTask<String, Void, Boolean> {
        private String resolution;

        @Override
        protected Boolean doInBackground(String... params) {
            resolution = params[0];
            try {
                rootShellManager.executeCommand("wm size " + resolution);
                // 等待一下让系统处理
                Thread.sleep(1000);
                return true;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (success) {
                statusTextView.setText("已应用分辨率: " + resolution);
                Toast.makeText(ResolutionSettingsActivity.this, 
                               "分辨率已修改", Toast.LENGTH_SHORT).show();
            } else {
                statusTextView.setText("应用分辨率失败");
                Toast.makeText(ResolutionSettingsActivity.this, 
                               "分辨率修改失败", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // 异步任务：重置分辨率
    private class ResetResolutionTask extends AsyncTask<Void, Void, Boolean> {
        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                rootShellManager.executeCommand("wm size reset");
                // 等待一下让系统处理
                Thread.sleep(1000);
                return true;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (success) {
                statusTextView.setText("已恢复默认分辨率");
                Toast.makeText(ResolutionSettingsActivity.this, 
                               "已恢复默认分辨率", Toast.LENGTH_SHORT).show();
            } else {
                statusTextView.setText("恢复分辨率失败");
                Toast.makeText(ResolutionSettingsActivity.this, 
                               "恢复默认分辨率失败", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // 异步任务：应用DPI
    private class ApplyDpiTask extends AsyncTask<String, Void, Boolean> {
        private String dpi;

        @Override
        protected Boolean doInBackground(String... params) {
            dpi = params[0];
            try {
                rootShellManager.executeCommand("wm density " + dpi);
                // 等待一下让系统处理
                Thread.sleep(1000);
                return true;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (success) {
                statusTextView.setText("已应用DPI: " + dpi);
                Toast.makeText(ResolutionSettingsActivity.this, 
                               "DPI已修改", Toast.LENGTH_SHORT).show();
            } else {
                statusTextView.setText("应用DPI失败");
                Toast.makeText(ResolutionSettingsActivity.this, 
                               "DPI修改失败", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // 异步任务：重置DPI
    private class ResetDpiTask extends AsyncTask<Void, Void, Boolean> {
        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                rootShellManager.executeCommand("wm density reset");
                // 等待一下让系统处理
                Thread.sleep(1000);
                return true;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (success) {
                statusTextView.setText("已恢复默认DPI");
                Toast.makeText(ResolutionSettingsActivity.this, 
                               "已恢复默认DPI", Toast.LENGTH_SHORT).show();
            } else {
                statusTextView.setText("恢复DPI失败");
                Toast.makeText(ResolutionSettingsActivity.this, 
                               "恢复默认DPI失败", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
