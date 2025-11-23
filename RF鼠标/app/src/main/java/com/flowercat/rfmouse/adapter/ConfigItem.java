package com.flowercat.rfmouse.adapter;

// ConfigItem.java

import java.io.Serializable;

public class ConfigItem implements Serializable {
    private String configName;
    private String filePath;
    private long fileSize;
    private long createTime;
    private boolean isSelected;

    public ConfigItem(String configName, String filePath, long fileSize, long createTime) {
        this.configName = configName;
        this.filePath = filePath;
        this.fileSize = fileSize;
        this.createTime = createTime;
        this.isSelected = false;
    }

    // Getters and Setters
    public String getConfigName() { return configName; }
    public void setConfigName(String configName) { this.configName = configName; }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public long getFileSize() { return fileSize; }
    public void setFileSize(long fileSize) { this.fileSize = fileSize; }

    public long getCreateTime() { return createTime; }
    public void setCreateTime(long createTime) { this.createTime = createTime; }

    public boolean isSelected() { return isSelected; }
    public void setSelected(boolean selected) { isSelected = selected; }

    public String getFormattedFileSize() {
        if (fileSize < 1024) {
            return fileSize + " B";
        } else if (fileSize < 1024 * 1024) {
            return String.format("%.1f KB", fileSize / 1024.0);
        } else {
            return String.format("%.1f MB", fileSize / (1024.0 * 1024.0));
        }
    }

    public String getFormattedTime() {
        return android.text.format.DateFormat.format("yyyy-MM-dd HH:mm", createTime).toString();
    }
}
