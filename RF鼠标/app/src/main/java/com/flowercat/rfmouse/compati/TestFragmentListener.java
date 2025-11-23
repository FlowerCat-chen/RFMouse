package com.flowercat.rfmouse.compati;

// TestFragmentListener.java

public interface TestFragmentListener {
    // nextTestId: 2 -> TestTwo, 3 -> TestThree, -1 -> 退出/完成
    void onTestComplete(int nextTestId);
}
