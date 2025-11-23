package com.flowercat.rfmouse.key;

// 事件处理策略枚举，用于控制按键事件是否被消耗
public enum KeyEventPolicy {
	DEFAULT, // 不消耗事件，让系统继续处理
	CONSUME  // 消耗事件，阻止系统处理
}
