package shellService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

public class Service {
    private ServiceGetText mServiceGetText;
    private ServerSocket serverSocket;
    private boolean isRunning = false;
    private int port;

    public Service(ServiceGetText serviceGetText, int port) {
        this.mServiceGetText = serviceGetText;
        this.port = port;
    }

    public void start() {
        if (isRunning) {
            System.out.println("服务端已经在运行中");
            return;
        }

        isRunning = true;
        System.out.println("启动服务端，端口: " + port);

        try {
            serverSocket = new ServerSocket(port);
            System.out.println("服务端成功启动，监听端口: " + port);

            while (isRunning) {
                try {
                    Socket socket = serverSocket.accept();
                    System.out.println("客户端连接成功: " + socket.getInetAddress().getHostAddress());

                    // 为每个客户端连接创建处理线程
                    new ClientHandler(socket).start();

                } catch (SocketException e) {
                    if (isRunning) {
                        System.out.println("Socket异常: " + e.getMessage());
                    } else {
                        System.out.println("服务端正常关闭");
                    }
                } catch (IOException e) {
                    System.out.println("接受客户端连接时发生IO异常: " + e.getMessage());
                }
            }

        } catch (IOException e) {
            System.out.println("启动服务端失败，端口 " + port + " 可能已被占用: " + e.getMessage());
        } finally {
            stop();
        }
    }

    public void stop() {
        isRunning = false;
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
                System.out.println("服务端已关闭");
            } catch (IOException e) {
                System.out.println("关闭服务端时发生异常: " + e.getMessage());
            }
        }
    }

    // 客户端连接处理线程
    class ClientHandler extends Thread {
        private Socket socket;
        private BufferedReader reader;
        private PrintWriter writer;
        private boolean connected = true;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        public void run() {
            try {
                reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                writer = new PrintWriter(socket.getOutputStream(), true); // autoflush

                System.out.println("开始处理客户端通信: " + socket.getInetAddress().getHostAddress());

                String line;
                // 持续读取客户端消息
                while (connected && (line = reader.readLine()) != null) {
                    System.out.println("收到客户端消息: " + line);
					line = line.trim();//注意这一句
                    String response = mServiceGetText.getText(line);
                    System.out.println("发送响应: " + response);
                    writer.println(response);
                }

            } catch (SocketException e) {
                System.out.println("客户端连接断开: " + e.getMessage());
            } catch (IOException e) {
                System.out.println("处理客户端通信时发生IO异常: " + e.getMessage());
            } finally {
                closeConnection();
            }
        }

        private void closeConnection() {
            connected = false;
            try {
                if (reader != null) reader.close();
                if (writer != null) writer.close();
                if (socket != null) socket.close();
                System.out.println("客户端连接已关闭: " + 
								   (socket != null ? socket.getInetAddress().getHostAddress() : "unknown"));
            } catch (IOException e) {
                System.out.println("关闭连接时发生异常: " + e.getMessage());
            }
        }

        // 向客户端发送消息的方法
        public void sendMessage(String message) {
            if (writer != null && connected) {
                writer.println(message);
                System.out.println("主动向客户端发送消息: " + message);
            }
        }
    }

    public interface ServiceGetText {
        String getText(String text);
    }
}
