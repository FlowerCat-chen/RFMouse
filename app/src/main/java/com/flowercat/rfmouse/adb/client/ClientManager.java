package com.flowercat.rfmouse.adb.client;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 客户端管理器 - 单例类
 * 负责管理Client的连接、消息发送和事件监听
 */
public class ClientManager {
    private static final String TAG = "ClientManager";
    private static ClientManager instance;

    // 客户端实例
    public Client client;
    // 应用上下文
    private Context context;
    // 服务器主机地址
    private String host;
    // 服务器端口
    private int port;
    // 是否自动连接
    public boolean autoConnect;
    // 连接状态标志
    private boolean isConnecting = false;
	
	//最大重试次数
	private int retryCount = 3;
	
    // 线程池用于处理消息发送
    private ExecutorService executorService = Executors.newFixedThreadPool(3);

    // 监听器列表
    private List<ConnectionListener> connectionListeners = new ArrayList<ConnectionListener>();
    private List<MessageListener> messageListeners = new ArrayList<MessageListener>();

    // 配置常量
 
    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_PORT = 4521;
    private static final boolean DEFAULT_AUTO_CONNECT = true;
	private static final int DEFAULT_RETRY_COUNT = 3;

    /**
     * 私有构造函数 - 单例模式
     */
    private ClientManager() {
        // 私有构造函数
    }

    /**
     * 获取ClientManager单例实例
     */
    public static synchronized ClientManager getInstance() {
        if (instance == null) {
            instance = new ClientManager();
        }
        return instance;
    }

    /**
     * 初始化方法
     * @param context 应用上下文
     */
    public void initialize(Context context) {
        this.context = context.getApplicationContext();
        loadConfig();
        createClient();
    }

    /**
     ** 加载配置
     */
    private void loadConfig() {
    	this.host = DEFAULT_HOST;
        this.port = DEFAULT_PORT;
        this.autoConnect = DEFAULT_AUTO_CONNECT;
		this.retryCount= DEFAULT_RETRY_COUNT;
    }

	//传入配置
	public void setConfig(String host,int port,boolean auto,int retryCount){
		this.host = host;
        this.port = port;
        this.autoConnect = auto;
		this.retryCount = retryCount;
		createClient();
	}
	
	
    /**
     * 创建客户端实例
     */
    private void createClient() {
        // 先断开现有连接
        if (client != null) {
            try {
                client.disconnect();
            } catch (Exception e) {
                Log.w(TAG, "Error while disconnecting old client: " + e.getMessage());
            }
        }

        try {
            // 创建新的客户端实例
            client = new Client(host, port, true, 5000, retryCount);

            // 设置连接监听器
            client.setConnectionListener(new Client.ConnectionListener() {
					@Override
					public void onConnectionSuccess() {
						Log.i(TAG, "Connection success");
						notifyConnectionSuccess();
					}

					@Override
					public void onConnectionFailure(String errorMsg, Exception e) {
						Log.e(TAG, "Connection failure: " + errorMsg);
						notifyConnectionFailure(errorMsg, e);
					}

					@Override
					public void onConnectionLost(String reason) {
						Log.w(TAG, "Connection lost: " + reason);
						notifyConnectionLost(reason);
					}

					@Override
					public void onDisconnected() {
						Log.i(TAG, "Disconnected");
						notifyDisconnected();
					}

					@Override
					public void onDisconnectFailure(String errorMsg, Exception e) {
						Log.e(TAG, "Disconnect failure: " + errorMsg);
						notifyDisconnectFailure(errorMsg, e);
					}

					@Override
					public void onRetryAttempt(int currentRetry, int maxRetry, int nextInterval) {
						Log.d(TAG, "Retry attempt " + currentRetry + "/" + maxRetry);
						notifyRetryAttempt(currentRetry, maxRetry, nextInterval);
					}

					@Override
					public void onMaxRetryReached(int maxRetryCount) {
						Log.e(TAG, "Max retry reached: " + maxRetryCount);
						notifyMaxRetryReached(maxRetryCount);
					}

					@Override
					public void onSendMessageSuccess(String message) {
						Log.d(TAG, "Send message success: " + message);
						notifySendMessageSuccess(message);
					}

					@Override
					public void onSendMessageFailure(String errorMsg, Exception e) {
						Log.e(TAG, "Send message failure: " + errorMsg);
						notifySendMessageFailure(errorMsg, e);
					}

					@Override
					public void onMessageReceiveFailure(String errorMsg, Exception e) {
						Log.e(TAG, "Message receive failure: " + errorMsg);
						notifyMessageReceiveFailure(errorMsg, e);
					}
				});

            // 设置消息监听器
            client.setMessageListener(new Client.MessageListener() {
					@Override
					public void onMessageReceived(String message) {
						Log.d(TAG, "Message received: " + message);
						notifyMessageReceived(message);
					}
				});

            Log.d(TAG, "Client created successfully");

        } catch (Exception e) {
            Log.e(TAG, "Failed to create client: " + e.getMessage(), e);
            notifyConnectionFailure("Failed to create client: " + e.getMessage(), e);
        }
    }

    /**
     * 连接到服务器
     */
    public void connect() {
        if (isConnecting) {
            Log.d(TAG, "Already connecting, skip duplicate connect request");
            return;
        }

        if (client == null) {
            Log.w(TAG, "Client is null, recreating...");
            createClient();
        }

        if (client == null) {
            Log.e(TAG, "Failed to create client for connection");
            notifyConnectionFailure("Client creation failed", null);
            return;
        }

        isConnecting = true;
        executorService.execute(new Runnable() {
				@Override
				public void run() {
					try {
						boolean connected = client.connect();
						if (!connected) {
							notifyConnectionFailure("Connect method returned false", null);
						}
					} catch (Exception e) {
						Log.e(TAG, "Exception during connect: " + e.getMessage(), e);
						notifyConnectionFailure("Exception during connect: " + e.getMessage(), e);
					} finally {
						isConnecting = false;
					}
				}
			});
    }

    /**
     * 断开连接
     */
    public void disconnect() {
        if (client != null) {
            try {
                client.disconnect();
            } catch (Exception e) {
                Log.e(TAG, "Error during disconnect: " + e.getMessage(), e);
                notifyDisconnectFailure("Error during disconnect: " + e.getMessage(), e);
            }
        }
        isConnecting = false;
    }

    /**
     * 发送消息
     * @param message 要发送的消息
     */
    public void sendMessage(final String message) {
        if (message == null || message.trim().isEmpty()) {
            Log.w(TAG, "Attempt to send empty or null message");
            return;
        }

        if (isConnected()) {
            // 直接发送
            executorService.execute(new Runnable() {
					@Override
					public void run() {
						try {
							client.sendMessage(message);
						} catch (Exception e) {
							Log.e(TAG, "Exception while sending message: " + e.getMessage(), e);
							notifySendMessageFailure("Exception while sending: " + e.getMessage(), e);
						}
					}
				});
        } else {
            // 未连接，记录日志
            Log.d(TAG, "Client not connected, message discarded: " + message);
        }
    }

    /**
     * 立即发送消息（不检查连接状态）
     * @param message 要发送的消息
     */
    public void sendMessageImmediate(final String message) {
        if (message == null || message.trim().isEmpty()) {
            return;
        }

        executorService.execute(new Runnable() {
				@Override
				public void run() {
					try {
						client.sendMessage(message);
					} catch (Exception e) {
						Log.e(TAG, "Exception while sending immediate message: " + e.getMessage(), e);
						notifySendMessageFailure("Exception while sending immediate: " + e.getMessage(), e);
					}
				}
			});
    }

    /**
     * 检查是否已连接
     */
    public boolean isConnected() {
        return client != null && client.isConnected();
    }

    /**
     * 检查是否正在连接中
     */
    public boolean isConnecting() {
        return isConnecting;
    }

    // 监听器管理方法

	/**
	 * 添加连接监听器
	 */
	public void addConnectionListener(ConnectionListener listener) {
		if (listener != null && !connectionListeners.contains(listener)) {
			connectionListeners.add(listener);
		}
	}

	/**
	 * 移除连接监听器
	 */
	public void removeConnectionListener(ConnectionListener listener) {
		connectionListeners.remove(listener);
	}

	/**
	 * 移除所有连接监听器
	 */
	public void removeAllConnectionListeners() {
		connectionListeners.clear();
		Log.d(TAG, "All connection listeners removed");
	}

	/**
	 * 添加消息监听器
	 */
	public void addMessageListener(MessageListener listener) {
		if (listener != null && !messageListeners.contains(listener)) {
			messageListeners.add(listener);
		}
	}

	/**
	 * 移除消息监听器
	 */
	public void removeMessageListener(MessageListener listener) {
		messageListeners.remove(listener);
	}

	/**
	 * 移除所有消息监听器  
	 */
	public void removeAllMessageListeners() {
		messageListeners.clear();
		Log.d(TAG, "All message listeners removed");
	}

	/**
	 * 移除所有监听器（包括连接监听器和消息监听器）
	 */
	public void removeAllListeners() {
		connectionListeners.clear();
		messageListeners.clear();
		Log.d(TAG, "All listeners removed");
	}

    // 监听器通知方法

    /**
     * 通知连接成功
     */
    private void notifyConnectionSuccess() {
        for (ConnectionListener listener : connectionListeners) {
            try {
                listener.onConnectionSuccess();
            } catch (Exception e) {
                Log.e(TAG, "Exception in connection success listener: " + e.getMessage(), e);
            }
        }
    }

    /**
     * 通知连接失败
     */
    private void notifyConnectionFailure(String errorMsg, Exception e) {
        for (ConnectionListener listener : connectionListeners) {
            try {
                listener.onConnectionFailure(errorMsg, e);
            } catch (Exception ex) {
                Log.e(TAG, "Exception in connection failure listener: " + ex.getMessage(), ex);
            }
        }
    }

    /**
     * 通知连接丢失
     */
    private void notifyConnectionLost(String reason) {
        for (ConnectionListener listener : connectionListeners) {
            try {
                listener.onConnectionLost(reason);
            } catch (Exception e) {
                Log.e(TAG, "Exception in connection lost listener: " + e.getMessage(), e);
            }
        }
    }

    /**
     * 通知已断开连接
     */
    private void notifyDisconnected() {
        for (ConnectionListener listener : connectionListeners) {
            try {
                listener.onDisconnected();
            } catch (Exception e) {
                Log.e(TAG, "Exception in disconnected listener: " + e.getMessage(), e);
            }
        }
    }

    /**
     * 通知断开连接失败
     */
    private void notifyDisconnectFailure(String errorMsg, Exception e) {
        for (ConnectionListener listener : connectionListeners) {
            try {
                listener.onDisconnectFailure(errorMsg, e);
            } catch (Exception ex) {
                Log.e(TAG, "Exception in disconnect failure listener: " + ex.getMessage(), ex);
            }
        }
    }

    /**
     * 通知重试尝试
     */
    private void notifyRetryAttempt(int currentRetry, int maxRetry, int nextInterval) {
        for (ConnectionListener listener : connectionListeners) {
            try {
                listener.onRetryAttempt(currentRetry, maxRetry, nextInterval);
            } catch (Exception e) {
                Log.e(TAG, "Exception in retry attempt listener: " + e.getMessage(), e);
            }
        }
    }

    /**
     * 通知达到最大重试次数
     */
    private void notifyMaxRetryReached(int maxRetryCount) {
        for (ConnectionListener listener : connectionListeners) {
            try {
                listener.onMaxRetryReached(maxRetryCount);
            } catch (Exception e) {
                Log.e(TAG, "Exception in max retry reached listener: " + e.getMessage(), e);
            }
        }
    }

    /**
     * 通知发送消息成功
     */
    private void notifySendMessageSuccess(String message) {
        for (ConnectionListener listener : connectionListeners) {
            try {
                listener.onSendMessageSuccess(message);
            } catch (Exception e) {
                Log.e(TAG, "Exception in send message success listener: " + e.getMessage(), e);
            }
        }
    }

    /**
     * 通知发送消息失败
     */
    private void notifySendMessageFailure(String errorMsg, Exception e) {
        for (ConnectionListener listener : connectionListeners) {
            try {
                listener.onSendMessageFailure(errorMsg, e);
            } catch (Exception ex) {
                Log.e(TAG, "Exception in send message failure listener: " + ex.getMessage(), ex);
            }
        }
    }

    /**
     * 通知接收消息失败
     */
    private void notifyMessageReceiveFailure(String errorMsg, Exception e) {
        for (ConnectionListener listener : connectionListeners) {
            try {
                listener.onMessageReceiveFailure(errorMsg, e);
            } catch (Exception ex) {
                Log.e(TAG, "Exception in message receive failure listener: " + ex.getMessage(), ex);
            }
        }
    }

    /**
     * 通知接收到消息
     */
    private void notifyMessageReceived(String message) {
        for (MessageListener listener : messageListeners) {
            try {
                listener.onMessageReceived(message);
            } catch (Exception e) {
                Log.e(TAG, "Exception in message received listener: " + e.getMessage(), e);
            }
        }
    }

    /**
     * 释放资源
     */
    public void release() {
        disconnect();

        if (executorService != null) {
            try {
                executorService.shutdown();
            } catch (Exception e) {
                Log.e(TAG, "Error shutting down executor service: " + e.getMessage(), e);
            }
        }

        connectionListeners.clear();
        messageListeners.clear();
    }

    // 监听器接口

    /**
     * 连接状态监听器接口
     */
    public interface ConnectionListener {
        void onConnectionSuccess();
        void onConnectionFailure(String errorMsg, Exception e);
        void onConnectionLost(String reason);
        void onDisconnected();
        void onDisconnectFailure(String errorMsg, Exception e);
        void onRetryAttempt(int currentRetry, int maxRetry, int nextInterval);
        void onMaxRetryReached(int maxRetryCount);
        void onSendMessageSuccess(String message);
        void onSendMessageFailure(String errorMsg, Exception e);
        void onMessageReceiveFailure(String errorMsg, Exception e);
    }

    /**
     * 消息监听器接口
     */
    public interface MessageListener {
        void onMessageReceived(String message);
    }
}
