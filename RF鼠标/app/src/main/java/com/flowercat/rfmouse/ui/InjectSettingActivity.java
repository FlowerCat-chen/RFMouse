package com.flowercat.rfmouse.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import com.flowercat.rfmouse.R;
import com.flowercat.rfmouse.adb.client.ClientManager;
import com.flowercat.rfmouse.service.FlowerMouseService;
import com.flowercat.rfmouse.util.RootShellManager;
import com.flowercat.rfmouse.util.SPUtil;
import java.io.IOException;

public class InjectSettingActivity extends Activity {

    // 广播相关
    private static final String SERVER_STARTED_ACTION = "com.flowercat.rfmouse.SERVER_STARTED";
    private BroadcastReceiver serverStatusReceiver;
    private boolean isReceiverRegistered = false;
    private static final long SERVER_START_TIMEOUT = 15000; // 15秒超时
    private Handler timeoutHandler = new Handler();
    private ProgressDialog serverStartDialog;
	private AlertDialog adbDialog;

    //触摸注入
    private SeekBar mgr_short_time;
    private SeekBar mgr_long_time;
    private SeekBar mgr_scroll_time;
    private SeekBar mgr_scroll_updown_dis;
    private SeekBar mgr_scroll_leftright_dis;

    private TextView tv_mgr_short_time; // 新增
    private TextView tv_mgr_long_time; // 新增
    private TextView tv_mgr_scroll_time;
    private TextView tv_mgr_scroll_updown_dis; // 新增
    private TextView tv_mgr_scroll_leftright_dis; // 新增

    // 服务器连接相关
    private EditText etServerAddress;
    private EditText etServerPort;
    private Switch switchAutoReconnect;
    private Switch switchCrashLog;
    private SeekBar seekReconnectCount;
    private TextView tvReconnectCount;
	private boolean autoStart = false;
	private boolean acceAutoStart = false;
    //连接与结束服务器
    private Button btnKillServer;
    private Button btnStartServer;

    // Root 操作管理器实例
    public static RootShellManager rootShellManager = RootShellManager.getInstance();
    //连接管理器实例
    private ClientManager clientManager = ClientManager.getInstance();
    //监听器
    public ClientManager.ConnectionListener connectionListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		
		requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                             WindowManager.LayoutParams.FLAG_FULLSCREEN);
		
        setContentView(R.layout.inject_setting);
		
		//当前由广播接收器启动。
		if(getIntent().hasExtra("startMgr")){
			autoStart = true;
		}
		
		if(getIntent().hasExtra("startAcce")){
			acceAutoStart = true;
		}

        try {
            rootShellManager.initializeSuSession();
        } catch (IOException e) {}

        mgr_short_time = findViewById(R.id.mgr_short_time);
        mgr_long_time = findViewById(R.id.mgr_long_time);
        mgr_scroll_time = findViewById(R.id.mgr_scroll_time);
        mgr_scroll_updown_dis = findViewById(R.id.mgr_scroll_updown_dis);
        mgr_scroll_leftright_dis = findViewById(R.id.mgr_scroll_leftright_dis);

        tv_mgr_short_time = findViewById(R.id.tv_mgr_short_time);
        tv_mgr_long_time = findViewById(R.id.tv_mgr_long_time);
        tv_mgr_scroll_updown_dis = findViewById(R.id.tv_mgr_scroll_updown_dis);
        tv_mgr_scroll_leftright_dis = findViewById(R.id.tv_mgr_scroll_leftright_dis);
        tv_mgr_scroll_time = findViewById(R.id.tv_mgr_scroll_time);


        // 服务器连接相关
        etServerAddress = findViewById(R.id.set_server_address);
        etServerPort = findViewById(R.id.set_server_port);
        switchAutoReconnect = findViewById(R.id.switch_auto_reconnect);
        switchCrashLog = findViewById(R.id.switch_crash_log);
        seekReconnectCount = findViewById(R.id.seek_reconnect_count);
        tvReconnectCount = findViewById(R.id.tv_reconnect_count);

        btnKillServer = findViewById(R.id.btn_kill_server);
        btnStartServer = findViewById(R.id.btn_start_server);

        //为所有 SeekBar 和 Switch 的父布局添加按键监听
        setupKeyNavigation();


        loadViewData();

        setupListeners();

        /* 触摸注入*/

        mgr_short_time.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser) {
                        tv_mgr_short_time.setText(String.valueOf(progress));
                        SPUtil.putInt(SPUtil.KEY_MGR_SHORT, progress);
                        if (FlowerMouseService.getInstance() != null) {
                            FlowerMouseService.getInstance().actionManager.setShortClickTimeMgr(progress);
                        }
                    }
                }
                @Override public void onStartTrackingTouch(SeekBar seekBar) {}
                @Override public void onStopTrackingTouch(SeekBar seekBar) {}
            });

        mgr_long_time.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser) {
                        tv_mgr_long_time.setText(String.valueOf(progress));
                        SPUtil.putInt(SPUtil.KEY_MGR_LONG, progress);
                        if (FlowerMouseService.getInstance() != null) {
                            FlowerMouseService.getInstance().actionManager.setLongClickTimeMgr(progress);
                        }

                    }
                }
                @Override public void onStartTrackingTouch(SeekBar seekBar) {}
                @Override public void onStopTrackingTouch(SeekBar seekBar) {}
            });


        mgr_scroll_time.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser) {
                        tv_mgr_scroll_time.setText(String.valueOf(progress));
                        SPUtil.putInt(SPUtil.KEY_MGR_SCROLL, progress);
                        if (FlowerMouseService.getInstance() != null) {
                            FlowerMouseService.getInstance().actionManager.setScrollTimeMgr(progress);
                        }
                    }
                }
                @Override public void onStartTrackingTouch(SeekBar seekBar) {}
                @Override public void onStopTrackingTouch(SeekBar seekBar) {}
            });

        mgr_scroll_updown_dis.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser) {
                        tv_mgr_scroll_updown_dis.setText(String.valueOf(progress));
                        SPUtil.putInt(SPUtil.KEY_MGR_UD_DIS, progress);
                        if (FlowerMouseService.getInstance() != null) {
                            FlowerMouseService.getInstance().actionManager.setScrollDisUDMgr(progress);
                        }
                    }
                }
                @Override public void onStartTrackingTouch(SeekBar seekBar) {}
                @Override public void onStopTrackingTouch(SeekBar seekBar) {}
            });

        mgr_scroll_leftright_dis.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser) {
                        tv_mgr_scroll_leftright_dis.setText(String.valueOf(progress));
                        SPUtil.putInt(SPUtil.KEY_MGR_LR_DIS, progress);
                        if (FlowerMouseService.getInstance() != null) {
                            FlowerMouseService.getInstance().actionManager.setScrollDisLRMgr(progress);
                        }
                    }
                }
                @Override public void onStartTrackingTouch(SeekBar seekBar) {}
                @Override public void onStopTrackingTouch(SeekBar seekBar) {}
            });


        // 服务器地址
        etServerAddress.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    if (!hasFocus && !etServerAddress.getText().toString().isEmpty()) {
                        String address = etServerAddress.getText().toString();
                        SPUtil.putString(SPUtil.KEY_SERVER_ADDRESS, address);
                    }
                }
            });

        // 服务器端口
        etServerPort.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    if (!hasFocus && !etServerPort.getText().toString().isEmpty()) {
                        String port = etServerPort.getText().toString();
                        SPUtil.putString(SPUtil.KEY_SERVER_PORT, port);
                    }
                }
            });

        // 自动重连开关
        switchAutoReconnect.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    SPUtil.putBoolean(SPUtil.KEY_AUTO_RECONNECT, isChecked);
                }
            });

        // 记录崩溃日志开关
        switchCrashLog.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    SPUtil.putBoolean(SPUtil.KEY_CRASH_LOG, isChecked);
                    enableLog(isChecked);
                }
            });

        // 重连次数 SeekBar
        seekReconnectCount.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser) {
                        tvReconnectCount.setText(String.valueOf(progress));
                        SPUtil.putInt(SPUtil.KEY_RECONNECT_COUNT, progress);
                    }
                }
                @Override public void onStartTrackingTouch(SeekBar seekBar) {}
                @Override public void onStopTrackingTouch(SeekBar seekBar) {}
            });

        //重置服务器相关参数    
        Button btnReset = findViewById(R.id.btn_server_default);
        btnReset.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
					showRestoreConfirmationDialog(1,"是否要重置服务器相关参数？");
                 }
            });


        // 开始
        btnStartServer.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startServer();
                }
            });

        //结束
        btnKillServer.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    sendServerCommand("EXIT_NOW");
                }
            });

        // 重置触摸注入设置的按钮监听
        Button btnMgrDefault = findViewById(R.id.btn_mgr_default);
        btnMgrDefault.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showRestoreConfirmationDialog(0,"是否要重置触摸注入相关参数？");
                }
            });

		// 修改连接状态判断逻辑
		if(clientManager.isConnected()){
			btnStartServer.setEnabled(false);
			btnKillServer.setEnabled(true);
		} else {
			// 如果是 Root 环境，检查进程状态
			if (rootShellManager.isRootAvailable()) {
				checkServerProcessRunning(new ProcessCheckCallback() {
						@Override
						public void onCheckResult(final boolean isRunning) {
							runOnUiThread(new Runnable() {
									@Override
									public void run() {
										if (isRunning) {
											btnStartServer.setEnabled(false); // 进程存在但未连接，禁用启动按钮
											btnKillServer.setEnabled(true);   // 允许结束服务
											showToast("检测到服务端进程，请等待连接...");
											// 延迟尝试连接
											new Handler().postDelayed(new Runnable() {
													@Override
													public void run() {
														connectToServer();
													}
												}, 1000);
										} else {
											btnStartServer.setEnabled(true);
											btnKillServer.setEnabled(false);
											if(autoStart){
												btnStartServer.performClick();
											}
										}
									}
								});
						}
					});
			} else {
				// 非 Root 环境保持原有逻辑
				btnStartServer.setEnabled(true);
				btnKillServer.setEnabled(false);
				//自动点击
				if(autoStart){
					btnStartServer.performClick();
				}
			}
		}

        // 初始化广播接收器
        setupBroadcastReceiver();
    }

    /**
     * 设置广播接收器
     */
    private void setupBroadcastReceiver() {
        serverStatusReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (SERVER_STARTED_ACTION.equals(action)) {
                    // 服务器启动成功
                    handleServerStartSuccess();
                }
            }
        };

        // 注册广播接收器
        IntentFilter filter = new IntentFilter();
        filter.addAction(SERVER_STARTED_ACTION);
        registerReceiver(serverStatusReceiver, filter);
        isReceiverRegistered = true;
    }

	/**
	 * 处理服务器启动成功
	 */
	private void handleServerStartSuccess() {
		runOnUiThread(new Runnable() {
				@Override
				public void run() {
					// 取消超时检查
					timeoutHandler.removeCallbacksAndMessages(null);

					// 关闭进度对话框
					if (serverStartDialog != null && serverStartDialog.isShowing()) {
						serverStartDialog.dismiss();
					}

					// 关闭ADB对话框
					if (adbDialog != null && adbDialog.isShowing()) {
						adbDialog.dismiss();
					}

					showToast("服务器启动成功");
					btnStartServer.setEnabled(false);
					btnKillServer.setEnabled(true);

					// 尝试连接服务器
					connectToServer();
				}
			});
	}
	
	

    /**
     * 处理服务器启动失败
     */
    private void handleServerStartFailure(final String errorMessage) {
        runOnUiThread(new Runnable() {
				@Override
				public void run() {
					// 取消超时检查
					timeoutHandler.removeCallbacksAndMessages(null);

					// 关闭进度对话框
					if (serverStartDialog != null && serverStartDialog.isShowing()) {
						serverStartDialog.dismiss();
					}

					showToast("服务器启动失败: " + errorMessage);
					btnStartServer.setEnabled(true);
					btnKillServer.setEnabled(false);
				}
			});
    }

    /**
     * 显示服务器启动进度对话框
     */
    private void showServerStartDialog() {
        runOnUiThread(new Runnable() {
				@Override
				public void run() {
					serverStartDialog = new ProgressDialog(InjectSettingActivity.this);
					serverStartDialog.setMessage("正在启动服务器...");
					serverStartDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
					serverStartDialog.setCancelable(false);
					serverStartDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "取消", new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								handleServerStartCancelled();
							}
						});
					serverStartDialog.show();

					// 设置超时检查
					timeoutHandler.postDelayed(new Runnable() {
							@Override
							public void run() {
								handleServerStartTimeout();
							}
						}, SERVER_START_TIMEOUT);
				}
			});
    }

    /**
     * 处理服务器启动取消
     */
    private void handleServerStartCancelled() {
        // 取消超时检查
        timeoutHandler.removeCallbacksAndMessages(null);

        // 关闭进度对话框
        if (serverStartDialog != null && serverStartDialog.isShowing()) {
            serverStartDialog.dismiss();
        }

        showToast("已取消服务器启动");
        btnStartServer.setEnabled(true);
    }

    /**
     * 处理服务器启动超时
     */
    private void handleServerStartTimeout() {
        runOnUiThread(new Runnable() {
				@Override
				public void run() {
					// 关闭进度对话框
					if (serverStartDialog != null && serverStartDialog.isShowing()) {
						serverStartDialog.dismiss();
					}

					showToast("服务器启动超时，请检查服务端状态");
					btnStartServer.setEnabled(true);
					btnKillServer.setEnabled(false);
				}
			});
    }

    private void loadViewData() {
        // 从SharedPreferences加载数据并更新UI

        int mgr_short= SPUtil.getInt(SPUtil.KEY_MGR_SHORT, 50);
        int mgr_long = SPUtil.getInt(SPUtil.KEY_MGR_LONG, 2);
        int scroll_time = SPUtil.getInt(SPUtil.KEY_MGR_SCROLL, 300);
        int mgr_scroll_ud = SPUtil.getInt(SPUtil.KEY_MGR_UD_DIS, 25);
        int mgr_scroll_lr = SPUtil.getInt(SPUtil.KEY_MGR_LR_DIS, 25);



        tv_mgr_short_time.setText(String.valueOf(mgr_short));
        mgr_short_time.setProgress(mgr_short);

        tv_mgr_long_time.setText(String.valueOf(mgr_long));
        mgr_long_time.setProgress(mgr_long);

        tv_mgr_scroll_time.setText(String.valueOf(scroll_time));
        mgr_scroll_time.setProgress(scroll_time);


        tv_mgr_scroll_updown_dis.setText(String.valueOf(mgr_scroll_ud));
        mgr_scroll_updown_dis.setProgress(mgr_scroll_ud);


        tv_mgr_scroll_leftright_dis.setText(String.valueOf(mgr_scroll_lr));
        mgr_scroll_leftright_dis.setProgress(mgr_scroll_lr);



        // 加载服务器地址和端口
        String serverAddress = SPUtil.getString(SPUtil.KEY_SERVER_ADDRESS, "localhost");
        String serverPort = SPUtil.getString(SPUtil.KEY_SERVER_PORT, "4521");
        etServerAddress.setText(serverAddress);
        etServerPort.setText(serverPort);

        // 加载开关状态
        boolean autoReconnect = SPUtil.getBoolean(SPUtil.KEY_AUTO_RECONNECT, true);
        boolean crashLog = SPUtil.getBoolean(SPUtil.KEY_CRASH_LOG, true);
        switchAutoReconnect.setChecked(autoReconnect);
        switchCrashLog.setChecked(crashLog);

        // 加载重连次数
        int reconnectCount = SPUtil.getInt(SPUtil.KEY_RECONNECT_COUNT, 3);
        tvReconnectCount.setText(String.valueOf(reconnectCount));
        seekReconnectCount.setProgress(reconnectCount);

    }


    /**
     * 设置按键导航适配
     */
    private void setupKeyNavigation() {
        // SeekBar 按键适配
        setupSeekBarKeyNavigation(R.id.sett_reconnect_count, R.id.seek_reconnect_count);
        setupSeekBarKeyNavigation(R.id.sett_mgr1, R.id.mgr_short_time);
        setupSeekBarKeyNavigation(R.id.sett_mgr2, R.id.mgr_long_time);
        setupSeekBarKeyNavigation(R.id.mgr_scroll_dur, R.id.mgr_scroll_time);
        setupSeekBarKeyNavigation(R.id.sett_mgr3, R.id.mgr_scroll_updown_dis);
        setupSeekBarKeyNavigation(R.id.sett_mgr4, R.id.mgr_scroll_leftright_dis);

        // Switch 按键适配
        setupSwitchKeyNavigation(R.id.sett_mgr_reconnect, R.id.switch_auto_reconnect);
        setupSwitchKeyNavigation(R.id.sett_mgr_log, R.id.switch_crash_log);
    }



    /**
     * 为 SeekBar 设置按键导航
     */
    private void setupSeekBarKeyNavigation(int layoutId, int seekBarId) {
        View layout = findViewById(layoutId);
        final SeekBar seekBar = findViewById(seekBarId);

        if (layout != null && seekBar != null) {
            layout.setOnKeyListener(new View.OnKeyListener() {
                    @Override
                    public boolean onKey(View v, int keyCode, KeyEvent event) {
                        // 检查按键是否是方向键，并且是按下事件
                        if (event.getAction() == KeyEvent.ACTION_DOWN) {
                            switch (keyCode) {
                                case KeyEvent.KEYCODE_DPAD_RIGHT:
                                case KeyEvent.KEYCODE_DPAD_LEFT:
                                    // 将事件传递给子视图（SeekBar）
                                    seekBar.onKeyDown(keyCode, event);
                                    // 返回 true 表示我们已经处理了这个事件
                                    return true;
                            }
                        }
                        // 返回 false 表示事件未被处理，让它继续向下传递
                        return false;
                    }
                });
        }
    }


    /**
     * 为 Switch 设置按键导航
     */
    private void setupSwitchKeyNavigation(int layoutId, int switchId) {
        View layout = findViewById(layoutId);
        final Switch switchView = findViewById(switchId);

        if (layout != null && switchView != null) {

            // 为布局添加点击监听，点击时切换 Switch
            layout.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        switchView.setChecked(!switchView.isChecked());
                    }
                });
        }

    }

	
	/**
	 * 显示ADB授权对话框
	 */
	private void showADBDialog(final Context context) {
		try {
			AlertDialog.Builder builder = new AlertDialog.Builder(context);
			builder.setTitle("ADB授权");

			// 创建可滚动的命令文本布局
			LinearLayout layout = new LinearLayout(context);
			layout.setOrientation(LinearLayout.VERTICAL);
			int padding = dpToPx(context, 8);
			layout.setPadding(padding, padding, padding, padding);

			// 添加说明文本
			TextView tvInstruction = new TextView(context);
			tvInstruction.setText("请在电脑端执行以下ADB命令来启动服务端：\n等待一小会等待弹窗自动消失，否则按底下的取消按键");
			tvInstruction.setTextSize(10);
			tvInstruction.setPadding(0, 0, 0, dpToPx(context, 12));
			layout.addView(tvInstruction);

			// 创建水平滚动容器
			HorizontalScrollView scrollView = new HorizontalScrollView(context);
			LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT,
				LinearLayout.LayoutParams.WRAP_CONTENT
			);
			scrollView.setLayoutParams(scrollParams);
			scrollView.setPadding(0, 0, 0, dpToPx(context, 8));

			TextView tvCommand = new TextView(context);
			final String adbCommand = "adb shell sh /sdcard/Android/data/com.flowercat.rfmouse/files/MouseStarter.sh";
			tvCommand.setText(adbCommand);
			tvCommand.setBackgroundResource(android.R.drawable.edit_text);
			tvCommand.setPadding(dpToPx(context, 12), dpToPx(context, 8), dpToPx(context, 12), dpToPx(context, 8));
			tvCommand.setTextIsSelectable(true);
			tvCommand.setTextSize(12);
			tvCommand.setSingleLine(true);

			scrollView.addView(tvCommand);
			layout.addView(scrollView);

			// 添加提示文本
			TextView tvHint = new TextView(context);
			tvHint.setText("↑ 长按命令可复制，左右滑动查看完整命令 ↑");
			tvHint.setTextSize(8);
			tvHint.setGravity(Gravity.CENTER);
			tvHint.setTextColor(getResources().getColor(android.R.color.darker_gray));
			tvHint.setPadding(0, 0, 0, dpToPx(context, 8));
			layout.addView(tvHint);

			// 添加底部空白区域，确保内容不被按钮遮挡
			View spacer = new View(context);
			LinearLayout.LayoutParams spacerParams = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT,
				dpToPx(context, 20) // 增加底部空间
			);
			spacer.setLayoutParams(spacerParams);
			layout.addView(spacer);

			builder.setView(layout);

			builder.setPositiveButton("已执行", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
						// 用户点击已执行后，显示等待进度条
						//showServerStartDialog();
						// 开始等待ADB服务端启动广播
						connectToServer();
					}
				});

			builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
						// 取消ADB启动流程
						btnStartServer.setEnabled(true);
					}
				});

			builder.setCancelable(false);

			adbDialog = builder.create();

			// 在显示对话框前设置窗口属性，确保有足够空间
			adbDialog.setOnShowListener(new DialogInterface.OnShowListener() {
					@Override
					public void onShow(DialogInterface dialogInterface) {
						// 确保对话框有足够空间显示内容
						AlertDialog alertDialog = (AlertDialog) dialogInterface;
						Window window = alertDialog.getWindow();
						if (window != null) {
							// 设置对话框宽度为屏幕宽度的90%，高度自适应
							DisplayMetrics displayMetrics = new DisplayMetrics();
							getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
							int dialogWidth = (int) (displayMetrics.widthPixels * 0.9);
							window.setLayout(dialogWidth, WindowManager.LayoutParams.WRAP_CONTENT);
						}
					}
				});

			adbDialog.show();
			
			
			// 2. 获取“确认”按钮（DialogInterface.BUTTON_POSITIVE）并设置样式
			Button positiveBtn = adbDialog.getButton(DialogInterface.BUTTON_POSITIVE);
			if (positiveBtn != null) {
				LinearLayout.LayoutParams positiveParams = (LinearLayout.LayoutParams) positiveBtn.getLayoutParams();
				positiveParams.leftMargin = dp2px(this, 30); // 确认按钮左边距30dp（与取消按钮隔开）
				positiveBtn.setLayoutParams(positiveParams);
				positiveBtn.setBackgroundResource(R.drawable.button_background_selector); // 应用选择器
				//positiveBtn.setPadding(30, 10, 30, 10); // 可选：调整按钮内边距，避免边框紧贴文字

			}

			// 3. 获取“取消”按钮（DialogInterface.BUTTON_NEGATIVE）并设置样式
			Button negativeBtn = adbDialog.getButton(DialogInterface.BUTTON_NEGATIVE);
			if (negativeBtn != null) {
				LinearLayout.LayoutParams negativeParams = (LinearLayout.LayoutParams) negativeBtn.getLayoutParams();
				negativeParams.rightMargin = dp2px(this, 10); // 取消按钮右边距10dp
				negativeBtn.setLayoutParams(negativeParams);
				negativeBtn.setBackgroundResource(R.drawable.button_background_selector); // 应用选择器
				//negativeBtn.setPadding(30, 10, 30, 10); // 可选：同确认按钮，保持样式一致
			}
			

			// 为命令文本添加长按复制功能
			tvCommand.setOnLongClickListener(new View.OnLongClickListener() {
					@Override
					public boolean onLongClick(View v) {
						ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
						if (clipboard != null) {
							ClipData clip = ClipData.newPlainText("ADB命令", adbCommand);
							clipboard.setPrimaryClip(clip);
							Toast.makeText(context, "命令已复制到剪贴板", Toast.LENGTH_SHORT).show();
						}
						return true;
					}
				});
				
			//如果是从辅助服务那边启动的，那证明服务端已经在运行了，我们尝试连接。
			if(acceAutoStart){
				positiveBtn.performClick();
			}

		} catch (Exception e) {
			Log.e("ADBDialog", "显示ADB对话框失败", e);
		}
	}


// 添加dp转px的工具方法
	private int dpToPx(Context context, float dp) {
		float density = context.getResources().getDisplayMetrics().density;
		return Math.round(dp * density);
	}
	
	
	
    // 工具方法：dp转px（避免不同分辨率设备间距不一致）
    private static int dp2px(Context context, float dpValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f); // 四舍五入避免精度丢失
    }


    //向服务器发送指令
    private void sendServerCommand(final String command) {
        if (!clientManager.isConnected()) {
            showToast("未连接到服务器");
            return;
        }

        // 对于kill命令，需要特殊处理确认
        if ("EXIT_NOW".equals(command)) {
            showKillServerConfirmation();
            return;
        }

        clientManager.sendMessage(command);
    }

    //结束服务确认弹窗
    private void showKillServerConfirmation() {
        if (!clientManager.isConnected()) {
            showToast("未连接到服务器");
            return;
        }

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("确认终止服务端");
        builder.setMessage("此操作将终止服务器进程，确定要继续吗？");
        builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    clientManager.sendMessage("EXIT_NOW");

                    if(connectionListener != null){
                        clientManager.removeConnectionListener(connectionListener);
                        connectionListener = null;
                    }

                    clientManager.removeAllListeners();

                    //允许再次启动
                    btnStartServer.setEnabled(true);

                }
            });
         builder.setNegativeButton("取消", null);
         builder.setCancelable(false);
				
		// 1. 先创建对话框（不能直接用builder，需先show()获取实例）
		AlertDialog dia = builder.show();
		
		// 2. 获取“确认”按钮（DialogInterface.BUTTON_POSITIVE）并设置样式
		Button positiveBtn = dia.getButton(DialogInterface.BUTTON_POSITIVE);
		if (positiveBtn != null) {
			LinearLayout.LayoutParams positiveParams = (LinearLayout.LayoutParams) positiveBtn.getLayoutParams();
			positiveParams.leftMargin = dp2px(this, 30); // 确认按钮左边距30dp（与取消按钮隔开）
			positiveBtn.setLayoutParams(positiveParams);
			positiveBtn.setBackgroundResource(R.drawable.button_background_selector); // 应用选择器
			//positiveBtn.setPadding(30, 10, 30, 10); // 可选：调整按钮内边距，避免边框紧贴文字

		}

		// 3. 获取“取消”按钮（DialogInterface.BUTTON_NEGATIVE）并设置样式
		Button negativeBtn = dia.getButton(DialogInterface.BUTTON_NEGATIVE);
		if (negativeBtn != null) {
			LinearLayout.LayoutParams negativeParams = (LinearLayout.LayoutParams) negativeBtn.getLayoutParams();
			negativeParams.rightMargin = dp2px(this, 10); // 取消按钮右边距10dp
			negativeBtn.setLayoutParams(negativeParams);
			negativeBtn.setBackgroundResource(R.drawable.button_background_selector); // 应用选择器
			//negativeBtn.setPadding(30, 10, 30, 10); // 可选：同确认按钮，保持样式一致
		}
		
    }


    //服务器连接状态监听
    private void setupListeners() {

        if(connectionListener != null){
            clientManager.removeConnectionListener(connectionListener);
            connectionListener = null;
        }

        connectionListener = new ClientManager.ConnectionListener() {

			@Override
			public void onConnectionLost(String reason) {
			}

			@Override
			public void onDisconnectFailure(String errorMsg, Exception e) {
			}

			@Override
			public void onRetryAttempt(int currentRetry, int maxRetry, int nextInterval) {
				showToast("injject尝试重连中…");
			}

			@Override
			public void onMaxRetryReached(int maxRetryCount) {
			}

			@Override
			public void onSendMessageSuccess(String message) {
			}

			@Override
			public void onSendMessageFailure(String errorMsg, Exception e) {
			}

			@Override
			public void onMessageReceiveFailure(String errorMsg, Exception e) {
			}

			// 在setupListeners方法的ConnectionListener中补充
			@Override
			public void onConnectionSuccess() {
				runOnUiThread(new Runnable() {
						@Override
						public void run() {
							btnStartServer.setEnabled(false);
							btnKillServer.setEnabled(true);
							showToast("连接服务器成功");
						}
					});
			}

			@Override
			public void onConnectionFailure(final String errorMsg, Exception e) {
				runOnUiThread(new Runnable() {
						@Override
						public void run() {
							btnStartServer.setEnabled(true);
							btnKillServer.setEnabled(false);
							showToast("连接失败: " + errorMsg);
						}
					});
			}

			@Override
			public void onDisconnected() {
				runOnUiThread(new Runnable() {
						@Override
						public void run() {
							btnStartServer.setEnabled(true);
							btnKillServer.setEnabled(false);
							showToast("已断开连接");
						}
					});
			}
		};

        // 添加连接状态监听器来更新UI状态
        clientManager.addConnectionListener(connectionListener);
    }



    private void showToast(final String message) {
        runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(InjectSettingActivity.this, message, Toast.LENGTH_SHORT).show();
                }
            });
    }


    //开启服务端
    private void startServer() {
      
        if (rootShellManager.isRootAvailable()) {
            // Root方式启动
            startServerWithRoot();
        } else {
            // ADB方式启动
            startServerWithAdb();
        }
    }

	//ROOT权限下开启服务端
	private void startServerWithRoot() {
		// 先检查服务端是否已启动
		checkServerProcessRunning(new ProcessCheckCallback() {
				@Override
				public void onCheckResult(final boolean isRunning) {
					runOnUiThread(new Runnable() {
							@Override
							public void run() {
								if (isRunning) {
									showToast("服务端已经在运行，无需重复启动");
									// 直接连接服务器
									connectToServer();
								} else {
									// 进程不存在，继续启动流程
									String scriptPath = "/sdcard/Android/data/com.flowercat.rfmouse/files/MouseStarter.sh";

									rootShellManager.executeCommandWithResult("sh " + scriptPath, new RootShellManager.CommandCallback() {
											@Override
											public void onSuccess(final String result) {
												// 这里不再直接处理成功，而是等待广播通知
											}

											@Override
											public void onFailure(final String error) {
												runOnUiThread(new Runnable() {
														@Override
														public void run() {
															// 取消超时检查
															timeoutHandler.removeCallbacksAndMessages(null);

															// 关闭进度对话框
															if (serverStartDialog != null && serverStartDialog.isShowing()) {
																serverStartDialog.dismiss();
															}

															showToast("服务器启动失败: " + error);
															btnStartServer.setEnabled(true);
														}
													});
											}
										});

									// 显示启动进度对话框
									showServerStartDialog();
								}
							}
						});
				}
			});
	}


    //ADB下开启服务端
    private void startServerWithAdb() {
        // 发送广播通知电脑端启动服务器
        showADBDialog(this);
        // 对于ADB方式，我们无法直接知道启动结果，所以依赖超时机制
    }


    //连接服务器
    private void connectToServer() {
        String address = etServerAddress.getText().toString();
        String portStr = etServerPort.getText().toString();
        boolean auto = switchAutoReconnect.isChecked();
        int retryCount = seekReconnectCount.getProgress();
        try {
            setupListeners();
            int port = Integer.parseInt(portStr);
            clientManager.setConfig(address,port,auto,retryCount);
            clientManager.connect();
        } catch (NumberFormatException e) {
            showToast("端口号格式错误");
        }
    }

    //重置触摸注入
    private void resetTouchInjectionSettings() {
        // 重置短按时长
        int defaultShort = 50;
        mgr_short_time.setProgress(defaultShort);
        tv_mgr_short_time.setText(String.valueOf(defaultShort));
        SPUtil.putInt(SPUtil.KEY_MGR_SHORT, defaultShort);

        // 重置长按时长
        int defaultLong = 2;
        mgr_long_time.setProgress(defaultLong);
        tv_mgr_long_time.setText(String.valueOf(defaultLong));
        SPUtil.putInt(SPUtil.KEY_MGR_LONG, defaultLong);

        // 重置滚动时长
        int defaultScroll = 300;
        mgr_scroll_time.setProgress(defaultScroll);
        tv_mgr_scroll_time.setText(String.valueOf(defaultScroll));
        SPUtil.putInt(SPUtil.KEY_MGR_SCROLL, defaultScroll);

        // 重置上下滑动距离
        int defaultUD = 25;
        mgr_scroll_updown_dis.setProgress(defaultUD);
        tv_mgr_scroll_updown_dis.setText(String.valueOf(defaultUD));
        SPUtil.putInt(SPUtil.KEY_MGR_UD_DIS, defaultUD);

        // 重置左右滑动距离
        int defaultLR = 25;
        mgr_scroll_leftright_dis.setProgress(defaultLR);
        tv_mgr_scroll_leftright_dis.setText(String.valueOf(defaultLR));
        SPUtil.putInt(SPUtil.KEY_MGR_LR_DIS, defaultLR);

        // 更新服务中的设置
        if (FlowerMouseService.getInstance() != null) {
            FlowerMouseService.getInstance().actionManager.setShortClickTimeMgr(defaultShort);
            FlowerMouseService.getInstance().actionManager.setLongClickTimeMgr(defaultLong);
            FlowerMouseService.getInstance().actionManager.setScrollTimeMgr(defaultScroll);
            FlowerMouseService.getInstance().actionManager.setScrollDisUDMgr(defaultUD);
            FlowerMouseService.getInstance().actionManager.setScrollDisLRMgr(defaultLR);
        }

        showToast("触摸注入设置已重置");
    }

    //是否开启日志
    private void enableLog(boolean en) {
        if(en){
            sendServerCommand("ENABLE_LOG");
        } else {
            sendServerCommand("DISABLE_LOG");
        }
    }
	
	
	
	/**
	 * 检查服务端进程是否正在运行
	 */
	private void checkServerProcessRunning(final ProcessCheckCallback callback) {
		rootShellManager.executeCommandWithResult("ps | grep flowermouse", new RootShellManager.CommandCallback() {
				@Override
				public void onSuccess(String result) {
					boolean isRunning = result != null && !result.trim().isEmpty() && result.contains("flowermouse");
					if (callback != null) {
						callback.onCheckResult(isRunning);
					}
				}

				@Override
				public void onFailure(String error) {
					Log.e("ServerCheck", "检查服务端进程时出错: " + error);
					if (callback != null) {
						callback.onCheckResult(false);
					}
				}
			});
	}

	// 进程检查回调接口
	interface ProcessCheckCallback {
		void onCheckResult(boolean isRunning);
	}
	
	
	//显示恢复默认对话框
    private void showRestoreConfirmationDialog(final int id ,String content) {

		AlertDialog.Builder builder = new AlertDialog.Builder(this);

		builder
			.setTitle("确定恢复默认？")
			.setMessage(content)
			.setPositiveButton("确定", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					switch(id){
						case 0:
							resetTouchInjectionSettings();
							break;
						case 1:
							restServerInfo();
							break;
					}

				}
			})
			.setNegativeButton("取消", null);

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
	
	//重置服务器参数
	public void restServerInfo(){
		// 重置服务器设置到默认值
		etServerAddress.setText("localhost");
		etServerPort.setText("4521");
		switchAutoReconnect.setChecked(true);
		switchCrashLog.setChecked(true);
		seekReconnectCount.setProgress(3);
		tvReconnectCount.setText("3");

		// 保存默认值
		SPUtil.putString(SPUtil.KEY_SERVER_ADDRESS, "localhost");
		SPUtil.putString(SPUtil.KEY_SERVER_PORT, "4521");
		SPUtil.putBoolean(SPUtil.KEY_AUTO_RECONNECT, true);
		SPUtil.putBoolean(SPUtil.KEY_CRASH_LOG, true);
		SPUtil.putInt(SPUtil.KEY_RECONNECT_COUNT, 3);

		Toast.makeText(InjectSettingActivity.this, "已重置所有设置", Toast.LENGTH_SHORT).show();
	}

	

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // 取消注册广播接收器
        if (isReceiverRegistered && serverStatusReceiver != null) {
            try {
                unregisterReceiver(serverStatusReceiver);
                isReceiverRegistered = false;
            } catch (Exception e) {
                // 忽略可能的异常，如接收器未注册
            }
        }

        // 取消所有超时检查
        if (timeoutHandler != null) {
            timeoutHandler.removeCallbacksAndMessages(null);
        }

        // 关闭进度对话框
        if (serverStartDialog != null && serverStartDialog.isShowing()) {
            serverStartDialog.dismiss();
        }

		// 关闭ADB对话框
		if (adbDialog != null && adbDialog.isShowing()) {
			adbDialog.dismiss();
		}
		
		
		
        // 移除连接监听器
        if (connectionListener != null) {
            clientManager.removeConnectionListener(connectionListener);
            connectionListener = null;
        }
		
		
		clientManager.removeAllListeners();
    }

}
