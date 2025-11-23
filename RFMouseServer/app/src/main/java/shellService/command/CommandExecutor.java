package shellService.command;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import shellService.scrcpy.InputUtil;

/**
 * 指令执行器 - 单例模式
 * 支持格式: command:param1,param2,param3
 * 特殊处理shell指令，不分割逗号
 */
public class CommandExecutor {
    // 单例实例
    private static CommandExecutor instance;

    // 指令处理器映射
    private Map<String, CommandHandler> handlers;

    /**
     * 私有构造函数
     */
    private CommandExecutor() {
        handlers = new HashMap<String, CommandHandler>();
        registerDefaultHandlers();
    }

    /**
     * 获取单例实例
     */
    public static synchronized CommandExecutor getInstance() {
        if (instance == null) {
            instance = new CommandExecutor();
        }
        return instance;
    }

    /**
     * 注册默认指令处理器
     */
    private void registerDefaultHandlers() {
		//命令行
        registerHandler("shell", new ShellCommandHandler());
		//点击屏幕
        registerHandler("click", new ClickCommandHandler());
		//滚动屏幕
        registerHandler("scroll", new ScrollCommandHandler());
		//按键注入
		registerHandler("key", new KeyCommandHandler());
		//拖动
		registerHandler("drag", new DragCommandHandler());
        // 可以继续添加其他默认处理器
    }

    /**
     * 注册指令处理器
     * @param commandName 指令名称
     * @param handler 处理器
     */
    public void registerHandler(String commandName, CommandHandler handler) {
        if (commandName != null && handler != null) {
            handlers.put(commandName.toLowerCase(), handler);
        }
    }

    /**
     * 执行指令 - 主要对外接口
     * @param input 指令字符串
     * @return 执行结果
     * @throws CommandException 指令异常
     */
    public String execute(String input) throws CommandException {
        try {
            Command command = CommandParser.parse(input);
            CommandHandler handler = handlers.get(command.getCommandName().toLowerCase());

            if (handler == null) {
                throw new CommandException("未知指令: " + command.getCommandName());
            }

            return handler.handle(command);

        } catch (IllegalArgumentException e) {
            throw new CommandException("指令格式错误: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new CommandException("执行指令时发生错误: " + e.getMessage(), e);
        }
    }

    /**
     * 指令解析器
     */
    private static class CommandParser {
        /**
         * 解析客户端指令
         */
        public static Command parse(String input) {
            if (input == null || input.trim().isEmpty()) {
                throw new IllegalArgumentException("输入不能为空");
            }

            String trimmedInput = input.trim();
            int colonIndex = trimmedInput.indexOf(':');

            if (colonIndex == -1) {
                throw new IllegalArgumentException("无效的指令格式，缺少冒号分隔符");
            }

            // 提取指令名称
            String commandName = trimmedInput.substring(0, colonIndex).trim();
            if (commandName.isEmpty()) {
                throw new IllegalArgumentException("指令名称不能为空");
            }

            // 提取参数字符串
            String paramsString = trimmedInput.substring(colonIndex + 1).trim();

            // 根据指令类型解析参数
            List<String> parameters = parseParameters(commandName, paramsString);

            return new Command(commandName, parameters);
        }

        /**
         * 根据指令类型解析参数
         */
        private static List<String> parseParameters(String commandName, String paramsString) {
            List<String> parameters = new ArrayList<String>();

            if (paramsString.isEmpty()) {
                return parameters;
            }

            // 特殊处理shell指令 - 不分割逗号
            if ("shell".equalsIgnoreCase(commandName)) {
                parameters.add(paramsString);
                return parameters;
            }

            // 其他指令按逗号分割，但保留空格
            String[] parts = splitByComma(paramsString);
            for (String part : parts) {
                String trimmedPart = part.trim();
                if (!trimmedPart.isEmpty()) {
                    parameters.add(trimmedPart);
                }
            }

            return parameters;
        }

        /**
         * 按逗号分割字符串，处理带引号的情况
         */
        private static String[] splitByComma(String input) {
            List<String> result = new ArrayList<String>();
            StringBuilder current = new StringBuilder();
            boolean inQuotes = false;

            for (int i = 0; i < input.length(); i++) {
                char c = input.charAt(i);

                if (c == '"') {
                    inQuotes = !inQuotes;
                    current.append(c);
                } else if (c == ',' && !inQuotes) {
                    // 遇到逗号且不在引号内，分割
                    result.add(current.toString());
                    current = new StringBuilder();
                } else {
                    current.append(c);
                }
            }

            // 添加最后一个参数
            if (current.length() > 0) {
                result.add(current.toString());
            }

            return result.toArray(new String[result.size()]);
        }
    }

    /**
     * 指令类 - 封装解析后的指令信息
     */
    private static class Command {
        private final String commandName;
        private final List<String> parameters;

        public Command(String commandName, List<String> parameters) {
            this.commandName = commandName;
            this.parameters = parameters;
        }

        public String getCommandName() {
            return commandName;
        }

        public List<String> getParameters() {
            return parameters;
        }

        public String getParameter(int index) {
            if (index >= 0 && index < parameters.size()) {
                return parameters.get(index);
            }
            return null;
        }

        public int getParameterCount() {
            return parameters.size();
        }

        @Override
        public String toString() {
            return "Command{command='" + commandName + "', parameters=" + parameters + "}";
        }
    }

    /**
     * 指令处理器接口
     */
    public interface CommandHandler {
        String handle(Command command) throws CommandException;
    }

    /**
     * 自定义异常类
     */
    public static class CommandException extends Exception {
        public CommandException(String message) {
            super(message);
        }

        public CommandException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    // ========== 具体指令处理器实现 ==========

    /**
     * Shell指令处理器
     */
    private static class ShellCommandHandler implements CommandHandler {
        @Override
        public String handle(Command command) throws CommandException {
            if (command.getParameterCount() == 0) {
                throw new CommandException("Shell指令需要参数");
            }

            String shellCommand = command.getParameter(0);
            // 这里执行实际的Shell命令
			ServiceShellUtils.ServiceShellCommandResult sr =  ServiceShellUtils.execCommand(shellCommand, false);
			if (sr.result == 0){
				return "■ShellOK:" + sr.successMsg;
			} else {
				return "■ShellError:" + sr.errorMsg;
			}
        }
    }

    /**
     * 点击指令处理器
     */
    private static class ClickCommandHandler implements CommandHandler {
        @Override
        public String handle(Command command) throws CommandException {
            if (command.getParameterCount() < 3) {
                throw new CommandException("触摸指令需要3个参数(X,Y坐标,持续时间)");
            }

            try {
                int x = Integer.parseInt(command.getParameter(0));
                int y = Integer.parseInt(command.getParameter(1));
				int time = Integer.parseInt(command.getParameter(2));
				
				//点击
                InputUtil.virtualMouseClick(x,y,time);

                return "触摸完成: X=" + x + ", Y=" + y + " T=" + time;
            } catch (NumberFormatException e) {
                throw new CommandException("点击坐标以及时间参数必须是数字");
            }
        }
    }


    /**
     * 滚动指令处理器
     */
    private static class ScrollCommandHandler implements CommandHandler {
        @Override
        public String handle(Command command) throws CommandException {
            if (command.getParameterCount() < 6) {
                throw new CommandException("滚动指令需要6个参数(起始x,起始y,终点x,终点y,点击事件time,步数step)");
            }

            try {
                int stX = Integer.parseInt(command.getParameter(0));
                int stY = Integer.parseInt(command.getParameter(1));
				int edX = Integer.parseInt(command.getParameter(2));
				int edY = Integer.parseInt(command.getParameter(3));
				int time = Integer.parseInt(command.getParameter(4));
				int step = Integer.parseInt(command.getParameter(5));
				
				//平滑滚动
				InputUtil.virtualMouseSmoothScroll(stX,stY,edX,edY,time,step);
				
                return "滚动操作完成:" + " T=" + time;
            } catch (NumberFormatException e) {
                throw new CommandException("滚动距离参数必须是数字");
            }
        }
    }
	
	/**
     * 按键注入处理器
     */
    private static class KeyCommandHandler implements CommandHandler {
        @Override
        public String handle(Command command) throws CommandException {
            if (command.getParameterCount() == 0) {
                throw new CommandException("按键注入需要键值");
            }

            String code = command.getParameter(0);
			
            // 这里执行实际的keycode
			InputUtil.quickKeyPress(Integer.parseInt(code),0);

            return "按键注入完成: " + code;
        }
    }
	
	
	/**
     * 拖动指令处理器
     */
    private static class DragCommandHandler implements CommandHandler {
        @Override
        public String handle(Command command) throws CommandException {
            if (command.getParameterCount() < 3) {
                throw new CommandException("拖动指令需要3个参数(模式,X,Y坐标)");
            }

            try {
				String mode = command.getParameter(0);
                int x = Integer.parseInt(command.getParameter(1));
                int y = Integer.parseInt(command.getParameter(2));
				
				switch(mode){
					case "down":
						InputUtil.virtualMouseDown(x,y);
						break;
					case "move":
						InputUtil.virtualMouseMove(x,y);
						break;
					case "up":
						InputUtil.virtualMouseUp(x,y);
						break;
				}

                return "拖动完成: Mode =" + mode + ", X=" + x + ", Y=" + y;
            } catch (NumberFormatException e) {
                throw new CommandException("点击坐标以及时间参数必须是数字");
            }
        }
    }
	
	
	
	
	
	
	
}
