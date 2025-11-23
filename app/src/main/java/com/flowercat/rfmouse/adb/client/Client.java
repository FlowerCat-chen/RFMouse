package com.flowercat.rfmouse.adb.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;

public class Client {
    private String host;
    private int port;
    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;
    private boolean connected = false;
    private boolean autoReconnect = true;
    private int reconnectInterval = 5000; // 重连间隔5秒
    private int maxRetryCount = 3; // 最大重试次数，默认5次
    private int currentRetryCount = 0; // 当前重试次数
    private MessageListener messageListener;
    private ConnectionListener connectionListener;

    // 【优化点 1：保存消息接收线程的引用】
    private MessageReceiver messageReceiver; 

    public Client(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public Client(String host, int port, boolean autoReconnect, int reconnectInterval, int maxRetryCount) {
        this.host = host;
        this.port = port;
        this.autoReconnect = autoReconnect;
        this.reconnectInterval = reconnectInterval;
        this.maxRetryCount = maxRetryCount;
    }

    public boolean connect() {
        System.out.println("尝试连接服务端: " + host + ":" + port);

        // 【修复点 1：每次连接尝试时重置重试计数】
        currentRetryCount = 0;

        if(connected && isConnected()){
            System.out.println("已经连接到服务端，无需重复连接");
            return true;
        }

        while (!connected && (autoReconnect || currentRetryCount == 0)) {
            try {
                // 先清理可能的旧连接
                cleanupResources();

                socket = new Socket(host, port);
                reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                writer = new PrintWriter(socket.getOutputStream(), true); // autoflush
                connected = true;
                currentRetryCount = 0; // 连接成功时重置重试计数

                System.out.println("成功连接到服务端: " + host + ":" + port);

                // 触发连接成功回调
                if (connectionListener != null) {
                    connectionListener.onConnectionSuccess();
                }

                // 【优化点 2：创建并保存 MessageReceiver 实例】
                messageReceiver = new MessageReceiver();
                messageReceiver.start();

                return true;

            } catch (UnknownHostException e) {
                String errorMsg = "未知的主机: " + host + ", 错误: " + e.getMessage();
                System.out.println(errorMsg);
                handleConnectionFailure(errorMsg, e);
                if (!autoReconnect) break;
            } catch (IOException e) {
                String errorMsg = "连接服务端失败: " + e.getMessage();
                System.out.println(errorMsg);
                handleConnectionFailure(errorMsg, e);
                if (!autoReconnect) break;
            } catch (Exception e) {
                String errorMsg = "连接时发生未知异常: " + e.getMessage();
                System.out.println(errorMsg);
                handleConnectionFailure(errorMsg, e);
                if (!autoReconnect) break;
            }

            // 检查是否达到最大重试次数
            if (autoReconnect && !connected) {
                currentRetryCount++;
                if (currentRetryCount >= maxRetryCount) {
                    System.out.println("已达到最大重试次数 (" + maxRetryCount + ")，停止重连");
                    if (connectionListener != null) {
                        connectionListener.onMaxRetryReached(maxRetryCount);
                    }
                    break;
                }

                System.out.println("第 " + currentRetryCount + "/" + maxRetryCount + " 次重试，" + 
								   reconnectInterval + "毫秒后尝试重新连接...");

                // 触发重试回调
                if (connectionListener != null) {
                    connectionListener.onRetryAttempt(currentRetryCount, maxRetryCount, reconnectInterval);
                }

                try {
                    Thread.sleep(reconnectInterval);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    System.out.println("重连等待被中断");
                    break;
                }
            }
        }

        return connected;
    }

    private void handleConnectionFailure(String errorMsg, Exception e) {
        if (connectionListener != null) {
            connectionListener.onConnectionFailure(errorMsg, e);
        }
    }

    public void sendMessage(String message) {
        if (!isConnected()) {
            System.out.println("连接未建立，无法发送消息");
            if (connectionListener != null) {
                connectionListener.onSendMessageFailure("连接未建立", null);
            }
            return;
        }

        if (writer != null) {
            try {
                writer.println(message);
                System.out.println("发送消息到服务端: " + message);
                if (connectionListener != null) {
                    connectionListener.onSendMessageSuccess(message);
                }
            } catch (Exception e) {
                System.out.println("发送消息失败: " + e.getMessage());
                // 这里不再设置 connected = false，而是等待 MessageReceiver 线程因 Socket 异常而退出
                if (connectionListener != null) {
                    connectionListener.onSendMessageFailure("发送失败: " + e.getMessage(), e);
                }
            }
        } else {
            System.out.println("Writer未初始化，无法发送消息");
            if (connectionListener != null) {
                connectionListener.onSendMessageFailure("Writer未初始化", null);
            }
        }
    }

    public void disconnect() {
        boolean wasConnected = connected;
        connected = false;
        autoReconnect = false; // 手动断开时不自动重连

        // 【优化点 3.1：尝试中断消息接收线程】
        if (messageReceiver != null) {
            messageReceiver.interrupt();
            messageReceiver = null;
        }

        cleanupResources();

        System.out.println("已断开与服务端的连接");

        if (wasConnected && connectionListener != null) {
            connectionListener.onDisconnected();
        } 
    }

    // 【新增方法：清理资源】
    private void cleanupResources() {
        // 优先关闭 Socket，确保阻塞的 I/O 被中断
        try {
            if (socket != null) {
                socket.close();
                socket = null;
            }
        } catch (IOException e) {
            System.out.println("关闭 Socket 时发生异常: " + e.getMessage());
        }

        // 关闭 Reader
        try {
            if (reader != null) {
                reader.close();
                reader = null;
            }
        } catch (IOException e) {
            System.out.println("关闭 Reader 时发生异常: " + e.getMessage());
        }

        // 关闭 Writer
        if (writer != null) {
            writer.close();
            writer = null;
        }
    }

    public void forceReconnect() {
        System.out.println("强制重新连接...");
        // 【修复点 2：在强制重连时确保重置所有状态】
        boolean oldAutoReconnect = this.autoReconnect;
        this.autoReconnect = true; // 启用自动重连
        disconnect(); 
        currentRetryCount = 0; // 重置重试计数
        // 【优化点 4：使用新线程调用 connect】
        reconnect(); 
        this.autoReconnect = oldAutoReconnect; // 恢复原来的设置
    }

    public boolean isConnected() {
        return connected && socket != null && !socket.isClosed() && socket.isConnected();
    }

    public void setMessageListener(MessageListener listener) {
        this.messageListener = listener;
    }

    public void setConnectionListener(ConnectionListener listener) {
        this.connectionListener = listener;
    }

    // Getters and Setters
    public int getMaxRetryCount() {
        return maxRetryCount;
    }

    public void setMaxRetryCount(int maxRetryCount) {
        this.maxRetryCount = maxRetryCount;
        // 【修复点 3：当最大重试次数改变时，如果当前计数超过新值，重置当前计数】
        if (currentRetryCount > maxRetryCount) {
            currentRetryCount = 0;
        }
    }

    public int getCurrentRetryCount() {
        return currentRetryCount;
    }

    public int getReconnectInterval() {
        return reconnectInterval;
    }

    public void setReconnectInterval(int reconnectInterval) {
        this.reconnectInterval = reconnectInterval;
    }

    public boolean isAutoReconnect() {
        return autoReconnect;
    }

    public void setAutoReconnect(boolean autoReconnect) {
        this.autoReconnect = autoReconnect;
        // 【修复点 4：当关闭自动重连时，重置重试计数】
        if (!autoReconnect) {
            currentRetryCount = 0;
        }
    }

    // 消息接收线程
    class MessageReceiver extends Thread {
        public void run() {
            String line;
            try {
                // reader.readLine() 在 Socket 关闭时会抛出 IOException
                while (connected && (line = reader.readLine()) != null) {
                    System.out.println("收到服务端消息: " + line);
                    if (messageListener != null) {
                        messageListener.onMessageReceived(line);
                    }
                }
            } catch (IOException e) {
                // 【优化点 5：区分主动断开和连接丢失】
                if (connected) {
                    // connected 仍为 true，说明是连接意外丢失
                    System.out.println("读取服务端消息时发生异常，连接丢失: " + e.getMessage());
                    if (connectionListener != null) {
                        connectionListener.onMessageReceiveFailure("读取消息失败: " + e.getMessage(), e);
                    }
                } else {
                    // connected 为 false，说明是主动断开时 Socket 关闭导致的预期异常
                    System.out.println("消息接收线程因连接断开而终止 (预期): " + e.getMessage());
                }
            } finally {
                boolean wasConnected = connected;
                connected = false;

                // 仅在之前是"连接状态"时才触发连接丢失回调和重连
                if (wasConnected) {
                    System.out.println("与服务端的连接已断开");
                    if (connectionListener != null) {
                        connectionListener.onConnectionLost("连接断开");
                    }

                    // 自动重连
                    if (autoReconnect) {
                        System.out.println("启动自动重连机制");
                        reconnect();
                    }
                }
            }
        }
    }

    private void reconnect() {
        new Thread(new Runnable() {
				public void run() {
					connect();
				}
			}).start();
    }

    public interface MessageListener {
        void onMessageReceived(String message);
    }

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
}
