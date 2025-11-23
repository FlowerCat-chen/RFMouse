package com.flowercat.rfmouse.key;

import java.io.Serializable;

public class KeyAction implements Serializable {
    private String keyName;
    private String shortPressAction;
    private String longPressAction;

    public KeyAction(String keyName) {
        this.keyName = keyName;
        this.shortPressAction = "默认";
        this.longPressAction = "默认";
    }

    // Getter and Setter methods
    public String getKeyName() {
        return keyName;
    }

    public void setKeyName(String keyName) {
        this.keyName = keyName;
    }

    public String getShortPressAction() {
        return shortPressAction;
    }

    public void setShortPressAction(String shortPressAction) {
        this.shortPressAction = shortPressAction;
    }

    public String getLongPressAction() {
        return longPressAction;
    }

    public void setLongPressAction(String longPressAction) {
        this.longPressAction = longPressAction;
    }
}
