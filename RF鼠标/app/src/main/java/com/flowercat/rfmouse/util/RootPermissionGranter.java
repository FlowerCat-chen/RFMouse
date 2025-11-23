package com.flowercat.rfmouse.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Root权限自动授权工具类（增强版）
 * 用于通过Root权限自动授予应用所需的各种权限，包括特殊权限如设备管理器和辅助服务
 */
public class RootPermissionGranter {

    // 常用权限常量
    public static final String PERMISSION_READ_EXTERNAL_STORAGE = "android.permission.READ_EXTERNAL_STORAGE";
    public static final String PERMISSION_WRITE_EXTERNAL_STORAGE = "android.permission.WRITE_EXTERNAL_STORAGE";
    public static final String PERMISSION_SYSTEM_ALERT_WINDOW = "android.permission.SYSTEM_ALERT_WINDOW";
	public static final String PERMISSION_WRITE_SECURE_SETTINGS = "android.permission.WRITE_SECURE_SETTINGS";
	
    // 特殊权限类型
    public static final int PERMISSION_TYPE_NORMAL = 0;
    public static final int PERMISSION_TYPE_DEVICE_ADMIN = 1;
    public static final int PERMISSION_TYPE_ACCESSIBILITY = 2;

    private String packageName;
    private List<PermissionRequest> permissions;
    private RootShellManager rootShellManager;
    private GrantCallback grantCallback;
    private int currentIndex;
    private List<PermissionRequest> failedPermissions;

    /**
     * 权限请求封装类
     */
    private static class PermissionRequest {
        String permission;
        int type;
        String component; // 对于特殊权限，需要组件名称

        PermissionRequest(String permission, int type) {
            this(permission, type, null);
        }

        PermissionRequest(String permission, int type, String component) {
            this.permission = permission;
            this.type = type;
            this.component = component;
        }

        @Override
        public String toString() {
            return permission + (component != null ? "(" + component + ")" : "");
        }
    }

    /**
     * 授权结果回调接口
     */
    public interface GrantCallback {
        void onAllGranted();
        void onPartialGranted(List<String> failedPermissions);
        void onGrantFailed(String permission, String error);
        void onRetryOption(String permission, RetryHandler retryHandler);
    }

    /**
     * 重试处理器接口
     */
    public interface RetryHandler {
        void retry();
        void skip();
    }

    /**
     * 构造函数
     * @param packageName 要授予权限的包名
     * @param rootShellManager RootShellManager实例
     */
    public RootPermissionGranter(String packageName, RootShellManager rootShellManager) {
        this.packageName = packageName;
        this.rootShellManager = rootShellManager;
        this.permissions = new ArrayList<PermissionRequest>();
        this.failedPermissions = new ArrayList<PermissionRequest>();
    }

    /**
     * 添加需要授予的普通权限
     * @param permission 权限名称
     * @return RootPermissionGranter实例，用于链式调用
     */
    public RootPermissionGranter addPermission(String permission) {
        this.permissions.add(new PermissionRequest(permission, PERMISSION_TYPE_NORMAL));
        return this;
    }

    /**
     * 添加设备管理器权限
     * @param adminReceiverClass 设备管理员接收器的完整类名
     * @return RootPermissionGranter实例，用于链式调用
     */
    public RootPermissionGranter addDeviceAdminPermission(String adminReceiverClass) {
        String component = packageName + "/" + packageName + adminReceiverClass;
        this.permissions.add(new PermissionRequest("设备管理员权限", PERMISSION_TYPE_DEVICE_ADMIN, component));
        return this;
    }

    /**
     * 添加辅助服务权限
     * @param accessibilityServiceClass 辅助服务的完整类名
     * @return RootPermissionGranter实例，用于链式调用
     */
    public RootPermissionGranter addAccessibilityServicePermission(String accessibilityServiceClass) {
        String component = packageName + "/" + packageName + accessibilityServiceClass;
        this.permissions.add(new PermissionRequest("辅助服务权限", PERMISSION_TYPE_ACCESSIBILITY, component));
        return this;
    }

    /**
     * 添加多个需要授予的权限
     * @param permissions 权限名称列表
     * @return RootPermissionGranter实例，用于链式调用
     */
    public RootPermissionGranter addPermissions(List<String> permissions) {
        for (String permission : permissions) {
            this.permissions.add(new PermissionRequest(permission, PERMISSION_TYPE_NORMAL));
        }
        return this;
    }

    /**
     * 开始授予权限
     * @param callback 授权结果回调
     */
    public void grantPermissions(final GrantCallback callback) {
        this.grantCallback = callback;
        this.currentIndex = 0;
        this.failedPermissions.clear();

        if (permissions.isEmpty()) {
            callback.onAllGranted();
            return;
        }

        grantNextPermission();
    }

    /**
     * 授予下一个权限
     */
    private void grantNextPermission() {
        if (currentIndex >= permissions.size()) {
            // 所有权限处理完成
            if (failedPermissions.isEmpty()) {
                grantCallback.onAllGranted();
            } else {
                List<String> failedPerms = new ArrayList<String>();
                for (PermissionRequest req : failedPermissions) {
                    failedPerms.add(req.toString());
                }
                grantCallback.onPartialGranted(failedPerms);
            }
            return;
        }

        final PermissionRequest request = permissions.get(currentIndex);

        // 根据权限类型执行不同的授权命令
        switch (request.type) {
            case PERMISSION_TYPE_NORMAL:
                grantNormalPermission(request, grantCallback);
                break;
            case PERMISSION_TYPE_DEVICE_ADMIN:
                grantDeviceAdminPermission(request, grantCallback);
                break;
            case PERMISSION_TYPE_ACCESSIBILITY:
                grantAccessibilityPermission(request, grantCallback);
                break;
        }
    }

    /**
     * 授予普通权限
     */
    private void grantNormalPermission(final PermissionRequest request, final GrantCallback callback) {
        final String command = "pm grant " + packageName + " " + request.permission;

        rootShellManager.executeCommandWithResult(command, new RootShellManager.CommandCallback() {
				@Override
				public void onSuccess(String result) {
					// 权限授予成功
					currentIndex++;
					grantNextPermission();
				}

				@Override
				public void onFailure(String error) {
					// 权限授予失败
					handlePermissionFailure(request, error, callback);
				}
			});
    }

    /**
     * 授予设备管理员权限
     */
    private void grantDeviceAdminPermission(final PermissionRequest request, final GrantCallback callback) {
        // 激活设备管理员
        final String command = "dpm set-active-admin " + request.component;

        rootShellManager.executeCommandWithResult(command, new RootShellManager.CommandCallback() {
				@Override
				public void onSuccess(String result) {
					// 设备管理员激活成功
					currentIndex++;
					grantNextPermission();
				}

				@Override
				public void onFailure(String error) {
					// 设备管理员激活失败
					handlePermissionFailure(request, error, callback);
				}
			});
    }

    /**
     * 授予辅助服务权限
     */
    private void grantAccessibilityPermission(final PermissionRequest request, final GrantCallback callback) {
		
		
        // 获取当前已启用的辅助服务
        final String getCommand = "settings get secure enabled_accessibility_services";

        rootShellManager.executeCommandWithResult(getCommand, new RootShellManager.CommandCallback() {
				@Override
				public void onSuccess(String currentServices) {
					// 添加新的辅助服务到列表
					String newServices;
					if (currentServices == null || currentServices.trim().isEmpty() || "null".equals(currentServices)) {
						newServices = request.component;
					} else {
						// 检查是否已经包含该服务
						if (currentServices.contains(request.component)) {
							// 已经启用，直接跳过
							currentIndex++;
							grantNextPermission();
							return;
						}
						newServices = currentServices.trim() + ":" + request.component;
					}

					// 启用辅助服务
					final String setCommand = "settings put secure enabled_accessibility_services " + newServices;
					final String enableCommand = "settings put secure accessibility_enabled 1";

					rootShellManager.executeCommandWithResult(setCommand, new RootShellManager.CommandCallback() {
							@Override
							public void onSuccess(String result) {
								// 设置辅助服务列表成功，现在启用辅助功能
								rootShellManager.executeCommandWithResult(enableCommand, new RootShellManager.CommandCallback() {
										@Override
										public void onSuccess(String result) {
											// 辅助功能启用成功
											currentIndex++;
											grantNextPermission();
										}

										@Override
										public void onFailure(String error) {
											// 辅助功能启用失败
											handlePermissionFailure(request, "启用辅助功能失败: " + error, callback);
										}
									});
							}

							@Override
							public void onFailure(String error) {
								// 设置辅助服务列表失败
								handlePermissionFailure(request, "设置辅助服务列表失败: " + error, callback);
							}
						});
				}

				@Override
				public void onFailure(String error) {
					// 获取当前辅助服务失败
					handlePermissionFailure(request, "获取当前辅助服务失败: " + error, callback);
				}
			});
			
    }

    /**
     * 处理权限授予失败
     */
    private void handlePermissionFailure(final PermissionRequest request, final String error, final GrantCallback callback) {
        failedPermissions.add(request);

        // 通知UI层，提供重试选项
        if (callback != null) {
            callback.onGrantFailed(request.toString(), error);
            callback.onRetryOption(request.toString(), new RetryHandler() {
					@Override
					public void retry() {
						// 重试当前权限
						retryPermission(request, new SinglePermissionCallback() {
								@Override
								public void onSuccess() {
									// 重试成功，从失败列表中移除
									failedPermissions.remove(request);
									currentIndex++;
									grantNextPermission();
								}

								@Override
								public void onFailure(String errorMsg) {
									// 重试失败，继续下一个权限
									callback.onGrantFailed(request.toString(), "重试失败: " + errorMsg);
									currentIndex++;
									grantNextPermission();
								}
							});
					}

					@Override
					public void skip() {
						// 跳过当前权限，继续下一个
						currentIndex++;
						grantNextPermission();
					}
				});
        }
    }

    /**
     * 重试单个权限
     */
    private void retryPermission(final PermissionRequest request, final SinglePermissionCallback callback) {
        switch (request.type) {
            case PERMISSION_TYPE_NORMAL:
                grantNormalPermission(request, createDummyCallback(callback));
                break;
            case PERMISSION_TYPE_DEVICE_ADMIN:
                grantDeviceAdminPermission(request, createDummyCallback(callback));
                break;
            case PERMISSION_TYPE_ACCESSIBILITY:
                grantAccessibilityPermission(request, createDummyCallback(callback));
                break;
        }
    }

    /**
     * 创建用于重试的虚拟回调
     */
    private GrantCallback createDummyCallback(final SinglePermissionCallback callback) {
        return new GrantCallback() {
            @Override
            public void onAllGranted() {
                callback.onSuccess();
            }

            @Override
            public void onPartialGranted(List<String> failedPermissions) {
                callback.onFailure("部分权限授予失败");
            }

            @Override
            public void onGrantFailed(String permission, String error) {
                callback.onFailure(error);
            }

            @Override
            public void onRetryOption(String permission, RetryHandler retryHandler) {
                // 不会在重试时调用
            }
        };
    }

    /**
     * 授予单个权限的回调接口
     */
    private interface SinglePermissionCallback {
        void onSuccess();
        void onFailure(String errorMsg);
    }

    /**
     * 快速授予常用权限集合
     * @param callback 授权结果回调
     */
    public void grantCommonPermissions(final GrantCallback callback) {
        // 清空现有权限列表
        this.permissions.clear();

        // 添加常用权限
        addPermission(PERMISSION_READ_EXTERNAL_STORAGE);
        addPermission(PERMISSION_WRITE_EXTERNAL_STORAGE);
        addPermission(PERMISSION_SYSTEM_ALERT_WINDOW);
        addPermission(PERMISSION_WRITE_SECURE_SETTINGS);

        // 开始授权
        grantPermissions(callback);
    }

    /**
     * 获取失败权限列表
     * @return 失败权限列表
     */
    public List<String> getFailedPermissions() {
        List<String> result = new ArrayList<String>();
        for (PermissionRequest req : failedPermissions) {
            result.add(req.toString());
        }
        return result;
    }

    /**
     * 清空权限列表
     */
    public void clearPermissions() {
        this.permissions.clear();
    }
}
