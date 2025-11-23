package com.flowercat.rfmouse.adb; 

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import com.flowercat.rfmouse.R;
import com.flowercat.rfmouse.adb.client.ClientManager;


public class ShellTestActivity extends Activity {

    private TextView tvStatus;
    private TextView tvMessages;
    private EditText etHost;
    private EditText etPort;
    private EditText etMessage;
    private Button btnConnect;
    private Button btnDisconnect;
    private Button btnSend;
    private ScrollView scrollView;

    private ClientManager clientManager;
    private Handler mainHandler;
	//监听器
	public ClientManager.ConnectionListener connectionListener;
	//消息监听器
	public ClientManager.MessageListener messageListener;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		
		requestWindowFeature(Window.FEATURE_NO_TITLE);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                             WindowManager.LayoutParams.FLAG_FULLSCREEN);
		
        setContentView(R.layout.shell_test);
		
        
        mainHandler = new Handler(Looper.getMainLooper());

        // 获取ClientManager实例
        clientManager = ClientManager.getInstance();
		
		initViews();
		
        // 设置监听器
        setupListeners();
    }

    private void initViews() {
        tvStatus = findViewById(R.id.tvStatus);
        tvMessages = findViewById(R.id.tvMessages);
        etHost = findViewById(R.id.etHost);
        etPort = findViewById(R.id.etPort);
        etMessage = findViewById(R.id.etMessage);
        btnConnect = findViewById(R.id.btnConnect);
        btnDisconnect = findViewById(R.id.btnDisconnect);
        btnSend = findViewById(R.id.btnSend);
        scrollView = findViewById(R.id.msgscrollView);

        btnConnect.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					connectToServer();
				}
			});

        btnDisconnect.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					//由于鼠标的功能还要用，这里我们提醒一下。
					disconnectFromServer();
					Toast.makeText(ShellTestActivity.this,"为了保证鼠标功能的正常使用，请在使用鼠标之前重新连接。",Toast.LENGTH_LONG).show();
				}
			});

        btnSend.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					sendMessage();
				}
			});
			
			//如果已经连上了
			if(clientManager.isConnected()){
				updateStatus("已连接", 0xFF4CAF50);
				addMessage("系统: 连接服务器成功");
				setConnectionState(false, true, true);
			}
    }
	

    private void setupListeners() {
		
		if(connectionListener != null){
			clientManager.removeConnectionListener(connectionListener);
			connectionListener = null;
		}

		if(messageListener != null){
			clientManager.removeMessageListener(messageListener);
			messageListener = null;
		}
		
		connectionListener = new ClientManager.ConnectionListener() {
				@Override
				public void onConnectionSuccess() {
					runOnUiThread(new Runnable() {
							@Override
							public void run() {
								updateStatus("已连接", 0xFF4CAF50);
								addMessage("系统: 连接服务器成功");
								setConnectionState(false, true, true);
							}
						});
				}

				@Override
				public void onConnectionFailure(final String errorMsg, Exception e) {
					runOnUiThread(new Runnable() {
							@Override
							public void run() {
								updateStatus("连接失败", 0xFFF44336);
								addMessage("系统: 连接失败 - " + errorMsg);
								setConnectionState(true, false, false);
							}
						});
				}

				@Override
				public void onConnectionLost(final String reason) {
					runOnUiThread(new Runnable() {
							@Override
							public void run() {
								updateStatus("连接丢失", 0xFFFF9800);
								addMessage("系统: 连接丢失 - " + reason);
								setConnectionState(true, false, false);
							}
						});
				}

				@Override
				public void onDisconnected() {
					runOnUiThread(new Runnable() {
							@Override
							public void run() {
								updateStatus("未连接", 0xFFF44336);
								addMessage("系统: 已断开连接");
								setConnectionState(true, false, false);
							}
						});
				}

				@Override
				public void onDisconnectFailure(final String errorMsg, Exception e) {
					runOnUiThread(new Runnable() {
							@Override
							public void run() {
								addMessage("系统: 断开连接失败 - " + errorMsg);
							}
						});
				}

				@Override
				public void onRetryAttempt(int currentRetry, int maxRetry, int nextInterval) {
					final String msg = "正在重试连接 (" + currentRetry + "/" + maxRetry + ")，下次间隔: " + nextInterval + "ms";
					runOnUiThread(new Runnable() {
							@Override
							public void run() {
								addMessage("系统: " + msg);
							}
						});
				}

				@Override
				public void onMaxRetryReached(int maxRetryCount) {
					runOnUiThread(new Runnable() {
							@Override
							public void run() {
								updateStatus("重试超限", 0xFFF44336);
								addMessage("系统: 达到最大重试次数，停止重连");
								setConnectionState(true, false, false);
							}
						});
				}

				@Override
				public void onSendMessageSuccess(final String message) {
					runOnUiThread(new Runnable() {
							@Override
							public void run() {
								addMessage("发送: " + message);
							}
						});
				}

				@Override
				public void onSendMessageFailure(final String errorMsg, Exception e) {
					runOnUiThread(new Runnable() {
							@Override
							public void run() {
								addMessage("系统: 发送失败 - " + errorMsg);
								showToast("发送消息失败");
							}
						});
				}

				@Override
				public void onMessageReceiveFailure(final String errorMsg, Exception e) {
					runOnUiThread(new Runnable() {
							@Override
							public void run() {
								addMessage("系统: 接收消息失败 - " + errorMsg);
							}
						});
				}
			};

		// 添加连接状态监听器
        clientManager.addConnectionListener(connectionListener);
			
     
		messageListener = new ClientManager.MessageListener() {
				@Override
				public void onMessageReceived(final String message) {
					runOnUiThread(new Runnable() {
							@Override
							public void run() {
								addMessage("接收: " + message);
							}
						});
				}
			};
			
		// 添加消息监听器
        clientManager.addMessageListener(messageListener);
    }

    private void connectToServer() {
        final String host = etHost.getText().toString().trim();
        final String portStr = etPort.getText().toString().trim();

        if (host.isEmpty() || portStr.isEmpty()) {
            showToast("请输入服务器地址和端口");
            return;
        }

        try {
            final int port = Integer.parseInt(portStr);

            // 禁用连接按钮，启用断开按钮
            setConnectionState(false, true, false);
			clientManager.setConfig(host,port,true,3);
            // 连接到服务器
            clientManager.connect();

        } catch (NumberFormatException e) {
            showToast("端口号格式错误");
            setConnectionState(true, false, false);
        }
    }

    private void disconnectFromServer() {
        clientManager.disconnect();
		//clientManager.client.disconnect();
        setConnectionState(true, false, false);
        updateStatus("未连接", 0xFFF44336);
        //addMessage("系统: 正在断开连接...");
    }

    private void sendMessage() {
        final String message = etMessage.getText().toString().trim();
        if (message.isEmpty()) {
            showToast("请输入要发送的消息");
            return;
        }

        if (!clientManager.isConnected()) {
            showToast("未连接到服务器");
            return;
        }

        clientManager.sendMessage(message);
        etMessage.setText("");
    }

    private void setConnectionState(boolean connectEnabled, boolean disconnectEnabled, boolean sendEnabled) {
        btnConnect.setEnabled(connectEnabled);
        btnDisconnect.setEnabled(disconnectEnabled);
        btnSend.setEnabled(sendEnabled);

        // 更新按钮透明度以反映状态
        btnConnect.setAlpha(connectEnabled ? 1.0f : 0.5f);
        btnDisconnect.setAlpha(disconnectEnabled ? 1.0f : 0.5f);
        btnSend.setAlpha(sendEnabled ? 1.0f : 0.5f);
    }

    private void updateStatus(String status, int color) {
        tvStatus.setText(status);
        tvStatus.setTextColor(color);
    }

    private void addMessage(String message) {
        String currentText = tvMessages.getText().toString();
        String newText = currentText + "\n" + message;
        tvMessages.setText(newText);

        // 滚动到底部
        scrollView.post(new Runnable() {
				@Override
				public void run() {
					scrollView.fullScroll(ScrollView.FOCUS_DOWN);
				}
			});
    }

    private void showToast(final String message) {
        runOnUiThread(new Runnable() {
				@Override
				public void run() {
					Toast.makeText(ShellTestActivity.this, message, Toast.LENGTH_SHORT).show();
				}
			});
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // 移除监听器
        if (clientManager != null) {
            // 注意：这里需要保存监听器的引用才能正确移除
            // 在实际使用中，你可能需要维护监听器的引用
            // 或者根据业务需求决定是否在Activity销毁时断开连接
			//clientManager.release();
        }

        if (mainHandler != null) {
            mainHandler.removeCallbacksAndMessages(null);
        }
		
		if(connectionListener != null){
			clientManager.removeConnectionListener(connectionListener);
			connectionListener = null;
		}

		if(messageListener != null){
			clientManager.removeMessageListener(messageListener);
			messageListener = null;
		}
		
		clientManager.removeAllListeners();
		
    }

	@Override
	public void onBackPressed() {
		super.onBackPressed();
		finish();
	}
	
	
	
	
	
	
}
