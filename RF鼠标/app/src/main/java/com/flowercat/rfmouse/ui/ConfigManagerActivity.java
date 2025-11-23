package com.flowercat.rfmouse.ui;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import com.flowercat.rfmouse.R;
import com.flowercat.rfmouse.adapter.ConfigItem;
import com.flowercat.rfmouse.adapter.ConfigListAdapter;
import com.flowercat.rfmouse.mouse.ConfigManager;
import com.flowercat.rfmouse.service.FlowerMouseService;
import com.flowercat.rfmouse.util.BitmapManager;
import com.flowercat.rfmouse.util.SPHelper;
import com.flowercat.rfmouse.util.SPUtil;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import android.widget.AdapterView;
import android.content.Context;
import android.widget.LinearLayout;

public class ConfigManagerActivity extends Activity implements ConfigListAdapter.OnConfigActionListener {

    private static final String TAG = "ConfigManagerActivity";
    // SPHelper 中用于保存当前选中配置名称的 Key
    private static final String KEY_CURRENT_CONFIG_NAME = "current_config_name"; 

    private ListView listView;
    private Button btnImport, btnSaveCurrent;
    private TextView tvEmpty;
    private List<ConfigItem> configList;
    private ConfigListAdapter adapter;

    // 在 ConfigManagerActivity.java 中添加以下常量
    private static final int REQUEST_READ_EXTERNAL_STORAGE = 1000;
    private static final int REQUEST_WRITE_EXTERNAL_STORAGE = 1001;
    private static final int REQUEST_PICK_FILE = 1002;
    private int currentExportPosition = -1;

    // 用于暂存导入时需要处理的 URI 和文件名
    private Uri pendingImportUri = null;
    private String pendingImportName = null;
	private boolean inGuideMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                             WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.config_manager);

		if(getIntent() != null){
			inGuideMode = getIntent().hasExtra("guide_mode");
		}
		
        initViews();
        loadConfigList();
		
		if(inGuideMode){
			btnImport.performClick();
		}
		
    }

    private void initViews() {
        listView = (ListView) findViewById(R.id.list_view);
        btnImport = (Button) findViewById(R.id.btn_import);
        btnSaveCurrent = (Button) findViewById(R.id.btn_save_current);
        tvEmpty = (TextView) findViewById(R.id.tv_empty);

        configList = new ArrayList<ConfigItem>();
        adapter = new ConfigListAdapter(this, configList, this);
        listView.setAdapter(adapter);

        btnImport.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					showImportDialog();
				}
			});

        btnSaveCurrent.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					showSaveCurrentDialog();
				}
			});
			
		Button exit = findViewById(R.id.exitButton_config);
		exit.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					setResult(RESULT_OK);
					finish();
					overridePendingTransition(R.anim.slide_in,R.anim.slide_out);
				}
			});
			
		// 处理列表项点击事件
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
				@Override
				public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
					adapter.touchItem(position);
				}
			});
			
    }

    /**
     * 加载配置列表，并标记当前选中的配置。
     */
    private void loadConfigList() {
        configList.clear();
        File[] configFiles = ConfigManager.getConfigFiles(this);
        // 获取当前选中的配置名称
        String currentConfigName = SPUtil.getString(KEY_CURRENT_CONFIG_NAME, "");

        boolean currentConfigFound = false;

        for (File file : configFiles) {
            String fileName = file.getName();
            // 移除文件扩展名
            String configName = fileName.substring(0, fileName.lastIndexOf('.'));

            ConfigItem item = new ConfigItem(
                configName, 
                file.getAbsolutePath(), 
                file.length(), 
                file.lastModified()
            );

            // 标记当前选中的配置
            if (configName.equals(currentConfigName)) {
                item.setSelected(true);
                currentConfigFound = true;
            } else {
                item.setSelected(false);
            }

            configList.add(item);
        }

        // 异常处理：如果 SP 中记录的配置不存在于文件列表中，清除 SP 记录
        if (!currentConfigFound && !currentConfigName.isEmpty()) {
            SPUtil.remove(KEY_CURRENT_CONFIG_NAME);
            // 此时不需要刷新列表，因为 loadConfigList 已经更新了 configList
            Log.w(TAG, "上次选中的配置 \"" + currentConfigName + "\" 丢失，已清除记录。");
        }

        adapter.updateList(configList);
        updateEmptyView();
    }

    private void updateEmptyView() {
        if (configList.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
            listView.setVisibility(View.GONE);
        } else {
            tvEmpty.setVisibility(View.GONE);
            listView.setVisibility(View.VISIBLE);
        }
	
    }

    // 实现配置操作监听器
    /**
     * 用户点击选中配置项时调用，现在增加确认弹窗。
     */
    @Override
    public void onConfigSelected(final int position) {
        showConfigSelectedDialog(position);
    }

	
	
	
	
	public void showConfigSelectedDialog(final int position){
		
		final ConfigItem item = configList.get(position);

        // 如果用户点击的是当前已选中的配置，则不重复弹窗
        if (item.isSelected()) {
            // 确保 RadioButton 仍被选中，防止某些情况下点击后状态被清除
            adapter.notifyDataSetChanged(); // 刷新列表，保证选中状态正确
            Toast.makeText(this, item.getConfigName() + " 已是当前配置。", Toast.LENGTH_SHORT).show();
            return;
        }

		
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		
        // 弹出确认对话框
        
            builder.setTitle("应用配置");
            builder.setMessage("确定要应用配置 \"" + item.getConfigName() + "\" 吗？\n(这将立即替换当前设置)");
            builder.setPositiveButton("应用", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // 用户点击“应用”，执行操作并刷新状态
                    // 1. 应用配置
                    applyConfig(position);

                    // 2. 更新列表的选中状态和持久化记录
                    for (int i = 0; i < configList.size(); i++) {
                        configList.get(i).setSelected(i == position);
                    }
                    adapter.notifyDataSetChanged();

                    // 3. 保存选中状态
                    SPUtil.putString( KEY_CURRENT_CONFIG_NAME, item.getConfigName());
                }
            });
            builder.setNegativeButton("取消", new DialogInterface.OnClickListener() { // 👈 关键修改在这里
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // 用户点击“取消”，**立即刷新列表**
                    // 这将强制列表项根据 configList 中的数据重新渲染
                    // 此时 configList 中没有项被选中 (item.isSelected() 仍为 false)，
                    // 从而清除 RadioButton 上可能残留的选中状态。
                    adapter.notifyDataSetChanged(); 
                }
            });
			
			
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

	

    @Override
    public void onConfigTouched(int position) {
		//这里懒得改了，直接用删除的。
        showFunctionSelectionDialog(position);
    }

    /**
     * 实际应用配置的逻辑，不包含 UI 状态的更新和持久化。
     */
    private void applyConfig(int position) {
        ConfigItem item = configList.get(position);

        ConfigManager.ImportResult result = ConfigManager.importConfig(this, item.getFilePath());
        if (result.success) {
            // 导入配置数据
            if (result.jsonConfig != null) {
                SPHelper.importFromJson(this, "rfmouse", result.jsonConfig);
            }

            // 导入鼠标图片
            if (result.mouseBitmap != null) {
                BitmapManager.putBitmap(this, "mouse", result.mouseBitmap);
            } else {
                BitmapManager.deleteBitmap(this, "mouse");
            }

            // 导入滚动图片
            if (result.scrollBitmap != null) {
                BitmapManager.putBitmap(this, "scroll", result.scrollBitmap);
            } else {
                BitmapManager.deleteBitmap(this, "scroll");
            }
			
			//更新辅助服务的所有操作
			if(FlowerMouseService.getInstance() != null){
				FlowerMouseService.getInstance().updateServiceConfig();
			}

            Toast.makeText(this, "配置应用成功", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "配置应用失败: " + result.errorMessage, Toast.LENGTH_LONG).show();
        }
    }


    private void showSaveCurrentDialog() {
        final EditText input = new EditText(this);
        String defaultName = SPHelper.getPhoneModel() + "_" + new SimpleDateFormat("HHmmss").format(new Date());
        input.setText(defaultName);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("保存当前配置");
            builder.setMessage("请输入配置名称:");
            builder.setView(input);
            builder.setPositiveButton("保存", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    String configName = input.getText().toString().trim();
                    if (!configName.isEmpty()) {
                        saveCurrentConfig(configName);
                    } else {
                        Toast.makeText(ConfigManagerActivity.this, "配置名称不能为空", Toast.LENGTH_SHORT).show();
                    }
                }
            });
            builder.setNegativeButton("取消", null);
			
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

    private void saveCurrentConfig(String configName) {
        boolean success = ConfigManager.saveCurrentConfig(this, configName);
        if (success) {
            Toast.makeText(this, "配置保存成功", Toast.LENGTH_SHORT).show();
            loadConfigList(); // 刷新列表
        } else {
            Toast.makeText(this, "配置保存失败", Toast.LENGTH_SHORT).show();
        }
    }


    private void showDeleteDialog(final int position) {
        ConfigItem item = configList.get(position);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("删除配置");
            builder.setMessage("确定要删除配置 \"" + item.getConfigName() + "\" 吗？");
            builder.setPositiveButton("删除", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    deleteConfig(position);
                }
            });
            builder.setNegativeButton("取消", null);
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

    /**
     * 删除配置，并处理删除当前选中配置的异常情况。
     */
    private void deleteConfig(int position) {
        ConfigItem item = configList.get(position);
        String configName = item.getConfigName();

        boolean success = ConfigManager.deleteConfig(this, configName);
        if (success) {

            // 异常处理：如果删除的是当前正在使用的配置，则清除 SP 记录
            String currentConfigName = SPUtil.getString(KEY_CURRENT_CONFIG_NAME, "");
            if (configName.equals(currentConfigName)) {
                SPUtil.remove(KEY_CURRENT_CONFIG_NAME);
                Toast.makeText(this, "当前配置已被删除，已清除选中标记。", Toast.LENGTH_LONG).show();
            }

            Toast.makeText(this, "配置删除成功", Toast.LENGTH_SHORT).show();
            loadConfigList(); // 刷新列表
        } else {
            Toast.makeText(this, "配置删除失败", Toast.LENGTH_SHORT).show();
        }
    }


    // 修改 onConfigExported 方法
    @Override
    public void onConfigExported(int position) {
        currentExportPosition = position;
        ConfigItem item = configList.get(position);
        String defaultExportName = ConfigManager.getDefaultExportName(item.getConfigName());
        showExportDialog(item.getConfigName(), defaultExportName);
    }

    // 更新 showExportDialog 方法
    private void showExportDialog(final String sourceName, final String defaultExportName) {
        final EditText input = new EditText(this);
        input.setText(defaultExportName);
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("导出配置");
            builder.setMessage("请输入导出名称:");
            builder.setView(input);
            builder.setPositiveButton("导出", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    String exportName = input.getText().toString().trim();
                    if (!exportName.isEmpty()) {
                        performExport(sourceName, exportName);
                    } else {
                        Toast.makeText(ConfigManagerActivity.this, "导出名称不能为空", Toast.LENGTH_SHORT).show();
                    }
                }
            });
            builder.setNegativeButton("取消", null);
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

    // 执行导出操作
    private void performExport(String sourceName, String exportName) {
		// Android Q+ 无需 WRITE_EXTERNAL_STORAGE，低版本兼容处理（含安卓4等超旧版本）
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q && !ConfigManager.hasExternalStoragePermission(this)) {
			// 仅 API 23+（安卓6.0+）才调用 requestPermissions，避免低版本报错
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
				requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
								   REQUEST_WRITE_EXTERNAL_STORAGE);
			}
			return;
		}
		
        // 执行导出
        boolean success = ConfigManager.exportConfigToExternal(this, sourceName, exportName);
        if (success) {
            Toast.makeText(this, "配置导出成功，文件保存在Download/MouseConfigs目录", Toast.LENGTH_LONG).show();

            // 显示文件路径
            File exportDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "MouseConfigs");
            File exportFile = new File(exportDir, exportName + ".mcfg");
            //showExportSuccessDialog(exportFile.getAbsolutePath());
        } else {
            Toast.makeText(this, "配置导出失败", Toast.LENGTH_SHORT).show();
        }
    }

    // 显示导出成功对话框
    private void showExportSuccessDialog(final String filePath) {
        new AlertDialog.Builder(this)
            .setTitle("导出成功")
            .setMessage("配置文件已导出到:\n" + filePath)
            .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // TODO: 实现分享逻辑
                }
            })
            .show();
    }


    // 处理权限请求结果
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_WRITE_EXTERNAL_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 权限已授予，重新执行导出
                if (currentExportPosition != -1) {
                    ConfigItem item = configList.get(currentExportPosition);
                    String defaultExportName = ConfigManager.getDefaultExportName(item.getConfigName());
                    showExportDialog(item.getConfigName(), defaultExportName);
                }
            } else {
                Toast.makeText(this, "存储权限被拒绝，无法导出配置", Toast.LENGTH_LONG).show();
            }
        } else if (requestCode == REQUEST_READ_EXTERNAL_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 权限已授予，重新打开文件选择器
                openFilePicker();
            } else {
                Toast.makeText(this, "读取权限被拒绝，无法导入配置", Toast.LENGTH_LONG).show();
            }
        }
    }


    // 更新 showImportDialog 方法
    private void showImportDialog() {
        // 只有 Android 6.0 (M) 到 Android 9.0 (P) 且 targetSdkVersion < 30 的应用才需要运行时权限
        // 对于 content:// URI，新版系统不再需要 READ_EXTERNAL_STORAGE
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q && !ConfigManager.hasExternalStoragePermission(this)) {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            	requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
							   REQUEST_READ_EXTERNAL_STORAGE);
			}
			
            return;
        }

        openFilePicker();
    }

    // 启动文件选择器
    private void openFilePicker() {
        // 使用 ACTION_GET_CONTENT
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        // 限制只显示 .mcfg 文件（如果系统支持）
        String[] mimetypes = {"application/octet-stream", "application/zip", "application/x-mcfg"};
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            intent.putExtra(Intent.EXTRA_MIME_TYPES, mimetypes);
        }

        try {
            startActivityForResult(Intent.createChooser(intent, "选择配置文件"), REQUEST_PICK_FILE);
        } catch (Exception e) {
            Log.e(TAG, "无法打开文件选择器", e);
            Toast.makeText(this, "无法打开文件选择器", Toast.LENGTH_SHORT).show();
        }
    }


    // 处理文件选择结果
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_PICK_FILE && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) {
                // 重置暂存变量
                pendingImportUri = uri;
                pendingImportName = null;

                // 尝试从 URI 获取文件名
                String fileName = getFileNameFromUri(uri);

                if (fileName != null && fileName.toLowerCase().endsWith(".mcfg")) {
                    // 如果文件名有效，直接导入
                    String configName = fileName.replace(".mcfg", "");
                    importConfigFromUri(uri, configName);
                } else {
                    // 文件名无效或无法获取，提示用户手动输入配置名称
                    showInputConfigNameDialog(uri);
                }
            }
        }
    }

    // 显示手动输入配置名称对话框
    private void showInputConfigNameDialog(final Uri uri) {
        final EditText input = new EditText(this);
        String defaultName = "导入配置_" + new SimpleDateFormat("HHmmss").format(new Date());
        input.setText(defaultName);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("配置名称");
            builder.setMessage("无法自动识别配置文件名，请输入配置名称:");
            builder.setView(input);
            builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    String configName = input.getText().toString().trim();
                    if (!configName.isEmpty()) {
                        // 暂存配置名称并进行导入
                        pendingImportName = configName;
                        importConfigFromUri(uri, configName);
                    } else {
                        Toast.makeText(ConfigManagerActivity.this, "配置名称不能为空", Toast.LENGTH_SHORT).show();
                    }
                }
            });
            builder.setNegativeButton("取消", null);
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


    // 从 URI 导入配置 (新的实现，直接读取内容流)
    private void importConfigFromUri(Uri uri, String configName) {

        // 检查配置名是否以 .mcfg 结尾，如果是，则移除
        if (configName.toLowerCase().endsWith(".mcfg")) {
            configName = configName.substring(0, configName.length() - 5);
        }

        // 目标文件路径 (应用内部私有存储)
        File configDir = new File(getExternalFilesDir(null), "mouse_configs");
        if (!configDir.exists()) {
			configDir.mkdirs();
        }

        File destFile = new File(configDir, configName + ".mcfg");

        try {
            // 检查是否已存在
            if (destFile.exists()) {
                showOverwriteDialog(configName, uri); // 传入 URI
            } else {
                copyAndImportConfig(uri, destFile, configName);
            }
        } catch (Exception e) {
            Log.e(TAG, "导入配置失败", e);
            Toast.makeText(this, "导入失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    // 从 Content URI 获取文件名 (新的健壮方法)
    private String getFileNameFromUri(Uri uri) {
        if (uri == null) return null;
        String fileName = null;

        if ("file".equals(uri.getScheme())) {
            // 对于 file:// URI，直接获取路径的最后一部分
            fileName = uri.getLastPathSegment();
        } else if ("content".equals(uri.getScheme())) {
            // 对于 content:// URI
            Cursor cursor = null;
            try {
                // 使用 OpenableColumns 获取文件名和大小，这是推荐的做法
                cursor = getContentResolver().query(uri, new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null);
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (nameIndex != -1) {
                        fileName = cursor.getString(nameIndex);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "获取文件名失败", e);
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }

        // 检查扩展名
        if (fileName != null && !fileName.toLowerCase().endsWith(".mcfg")) {
			// 如果获取到的文件名扩展名不对，我们返回 null，让用户手动输入
			return null;
        }

        return fileName;
    }


    // 显示覆盖确认对话框
    private void showOverwriteDialog(final String configName, final Uri uri) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("覆盖配置");
            builder.setMessage("配置 \"" + configName + "\" 已存在，是否覆盖？");
            builder.setPositiveButton("覆盖", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // 目标文件路径 (应用内部私有存储)
                    File configDir = new File(getExternalFilesDir(null), "mouse_configs");
                    File destFile = new File(configDir, configName + ".mcfg");
                    copyAndImportConfig(uri, destFile, configName);
                }
            });
            builder.setNegativeButton("取消", null);
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


    // 复制并导入配置 (使用 InputStream/OutputStream)
    private void copyAndImportConfig(Uri sourceUri, File destFile, String configName) {
        InputStream inputStream = null;
        OutputStream outputStream = null;

        try {
            // 打开 Content URI 的输入流
            inputStream = getContentResolver().openInputStream(sourceUri);
            if (inputStream == null) {
                throw new Exception("无法打开输入流");
            }

            // 打开目标文件的输出流
            outputStream = new FileOutputStream(destFile);

            // 复制文件内容
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }
            outputStream.flush();

            // 刷新列表
            loadConfigList();
            Toast.makeText(this, "配置导入成功: " + configName + "。别忘了手动应用配置", Toast.LENGTH_SHORT).show();
			//showConfigSelectedDialog(pos);
        } catch (Exception e) {
            Log.e(TAG, "复制配置文件失败", e);
            Toast.makeText(this, "导入失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
        } finally {
            // 关闭流
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (Exception e) {
                    // 忽略关闭错误
                }
            }
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (Exception e) {
                    // 忽略关闭错误
                }
            }
        }
    }
	
	
	@Override
	public void onBackPressed() {
		super.onBackPressed();
		setResult(RESULT_OK);
		finish();
		overridePendingTransition(R.anim.slide_in,R.anim.slide_out);
	}
	
	
	// 选择一下神奇小功能
    private void showFunctionSelectionDialog(final int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("选择功能类型");
		
        final String[] pressTypes;
       
        pressTypes = new String[]{"应用配置", "删除配置","导出配置"};
       
        builder.setItems(pressTypes, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					if (which == 0) { 
						showConfigSelectedDialog(position);
					} else if(which == 1){
						showDeleteDialog(position);
					} else if (which == 2) {
						currentExportPosition = position;
						ConfigItem item = configList.get(position);
						String defaultExportName = ConfigManager.getDefaultExportName(item.getConfigName());
						showExportDialog(item.getConfigName(), defaultExportName);
					}
				}
			});
        builder.show();
    }
	
	
	
	// 工具方法：dp转px（避免不同分辨率设备间距不一致）
	private int dp2px(Context context, float dpValue) {
		final float scale = context.getResources().getDisplayMetrics().density;
		return (int) (dpValue * scale + 0.5f); // 四舍五入避免精度丢失
	}
	
	
	
	
	
}
