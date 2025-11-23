// SectionItem.java
package com.flowercat.rfmouse.adapter;

public class SectionItem {
    private String title;
    private int textColor;
    private int backgroundColor; // 这个背景色我们会应用到布局的内部元素上

    public SectionItem(String title, int textColor, int backgroundColor) {
        this.title = title;
        this.textColor = textColor;
        this.backgroundColor = backgroundColor;
    }

    public String getTitle() {
        return title;
    }

    public int getTextColor() {
        return textColor;
    }

    public int getBackgroundColor() {
        return backgroundColor;
    }

    // 添加setter方法以便动态修改颜色
    public void setTextColor(int textColor) {
        this.textColor = textColor;
    }

    public void setBackgroundColor(int backgroundColor) {
        this.backgroundColor = backgroundColor;
    }
}
