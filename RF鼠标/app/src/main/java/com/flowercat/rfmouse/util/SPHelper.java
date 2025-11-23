package com.flowercat.rfmouse.util;

import android.content.Context;
import android.content.SharedPreferences;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Map;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.List;
import com.flowercat.rfmouse.key.KeyAction;
import java.util.ArrayList;
import java.io.Serializable;
import android.util.Log;
import android.os.Build;
import java.io.FileReader;

public class SPHelper {

    /**
     * 导出SharedPreferences所有内容为JSON字符串
     * @param context 上下文
     * @param prefName SharedPreferences名称
     * @return JSON格式的字符串
     */
    public static String exportToJson(Context context, String prefName) {
        try {
            SharedPreferences sharedPreferences = context.getSharedPreferences(prefName, Context.MODE_PRIVATE);
            Map<String, ?> allEntries = sharedPreferences.getAll();

            JSONObject jsonObject = new JSONObject();

            for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
				
				
				// 特殊处理序列化对象
				if (SPUtil.KEY_ACTIONS_DATA.equals(key) && value instanceof String) {
					// 尝试反序列化后再序列化为JSON
					try {
						List<KeyAction> keyActions = (List<KeyAction>) SPUtil.fromString((String) value);
						JSONArray keyActionsArray = new JSONArray();
						for (KeyAction action : keyActions) {
							JSONObject actionObj = new JSONObject();
							actionObj.put("name", action.getKeyName());
							actionObj.put("shortPressAction", action.getShortPressAction());
							actionObj.put("longPressAction", action.getLongPressAction());
							keyActionsArray.put(actionObj);
						}
						jsonObject.put(key, keyActionsArray);
					} catch (Exception e) {
						// 如果反序列化失败，保存原始字符串
						jsonObject.put(key, value);
					}
				} else if (SPUtil.APPS_KEY.equals(key) && value instanceof String) {
					// 应用列表已经是JSON字符串，直接解析为JSONArray
					try {
						JSONArray appsArray = new JSONArray((String) value);
						jsonObject.put(key, appsArray);
					} catch (JSONException e) {
						jsonObject.put(key, value);
					}
				// 根据值的类型进行处理
				} else if (value instanceof String) {
                    jsonObject.put(key, (String) value);
                } else if (value instanceof Integer) {
                    jsonObject.put(key, (Integer) value);
                } else if (value instanceof Long) {
                    jsonObject.put(key, (Long) value);
                } else if (value instanceof Float) {
                    jsonObject.put(key, (Float) value);
                } else if (value instanceof Boolean) {
                    jsonObject.put(key, (Boolean) value);
                } else {
                    // 其他类型转为字符串
                    jsonObject.put(key, value.toString());
                }
            }

            // 添加元数据
            jsonObject.put("_metadata", createMetadata(prefName, allEntries.size()));

            return jsonObject.toString(2); // 缩进2个空格，便于阅读

        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

  
	
	/**
	 * 从JSON字符串导入数据到SharedPreferences
	 * @param context 上下文
	 * @param prefName SharedPreferences名称
	 * @param jsonString JSON格式的字符串
	 * @return 是否导入成功
	 */
	public static boolean importFromJson(Context context, String prefName, String jsonString) {
		try {
			SharedPreferences sharedPreferences = context.getSharedPreferences(prefName, Context.MODE_PRIVATE);
			SharedPreferences.Editor editor = sharedPreferences.edit();

			JSONObject jsonObject = new JSONObject(jsonString);

			// 使用迭代器遍历JSON对象的所有键
			java.util.Iterator<String> keys = jsonObject.keys();
			while (keys.hasNext()) {
				String key = keys.next();

				// 跳过元数据
				if ("_metadata".equals(key)) {
					continue;
				}

				Object value = jsonObject.get(key);
				
				// 特殊处理序列化对象
				if (SPUtil.KEY_ACTIONS_DATA.equals(key) && value instanceof JSONArray) {
					try {
						JSONArray keyActionsArray = (JSONArray) value;
						List<KeyAction> keyActions = new ArrayList<>();
						for (int i = 0; i < keyActionsArray.length(); i++) {
							JSONObject actionObj = keyActionsArray.getJSONObject(i);
							KeyAction action = new KeyAction(actionObj.getString("name"));
							if (actionObj.has("shortPressAction")) {
								action.setShortPressAction(actionObj.getString("shortPressAction"));
							}
							if (actionObj.has("longPressAction")) {
								action.setLongPressAction(actionObj.getString("longPressAction"));
							}
							keyActions.add(action);
						}
						String serializedData = SPUtil.mtoString((Serializable) keyActions);
						editor.putString(SPUtil.KEY_ACTIONS_DATA, serializedData);
					} catch (Exception e) {
						e.printStackTrace();
					}
				} else if (SPUtil.APPS_KEY.equals(key) && value instanceof JSONArray) {
					// 应用列表直接保存为JSON字符串
					editor.putString(SPUtil.APPS_KEY, value.toString());
				
				} else if (value instanceof String) {
					editor.putString(key, (String) value);
				} else if (value instanceof Integer) {
					editor.putInt(key, (Integer) value);
				} else if (value instanceof Long) {
					editor.putLong(key, (Long) value);
				} else if (value instanceof Float) {
					editor.putFloat(key, (Float) value);
				} else if (value instanceof Boolean) {
					editor.putBoolean(key, (Boolean) value);
				} else if (value instanceof Double) {
					// 处理Double类型（转为Float）
					editor.putFloat(key, ((Double) value).floatValue());
				}
			}

			return editor.commit();

		} catch (JSONException e) {
			e.printStackTrace();
			return false;
		}
	}
	
	
	

    /**
     * 获取SharedPreferences的所有键列表
     * @param context 上下文
     * @param prefName SharedPreferences名称
     * @return 键的数组
     */
    public static String[] getAllKeys(Context context, String prefName) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(prefName, Context.MODE_MULTI_PROCESS);
        Map<String, ?> allEntries = sharedPreferences.getAll();
        return allEntries.keySet().toArray(new String[0]);
    }

    /**
     * 清空SharedPreferences
     * @param context 上下文
     * @param prefName SharedPreferences名称
     * @return 是否成功
     */
    public static boolean clearSharedPreferences(Context context, String prefName) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(prefName, Context.MODE_MULTI_PROCESS);
        return sharedPreferences.edit().clear().commit();
    }

    /**
     * 创建元数据
     */
    private static JSONObject createMetadata(String prefName, int entryCount) throws JSONException {
        JSONObject metadata = new JSONObject();
        metadata.put("preferences_name", prefName);
        metadata.put("export_timestamp", System.currentTimeMillis());
        metadata.put("entry_count", entryCount);
        metadata.put("version", "1.0");
		metadata.put("model", getPhoneModel());
		metadata.put("advice", "不建议手动修改此文件。");
        return metadata;
    }

 
	

	/**
	 * 将JSON字符串写入本地文件
	 * @param context 上下文
	 * @param jsonString JSON字符串
	 * @param fileName 文件名（不含路径）
	 * @return 是否写入成功
	 */
	public static boolean writeJsonToFile(Context context, String jsonString, String fileName) {
		FileOutputStream fileOutputStream = null;
		OutputStreamWriter outputStreamWriter = null;

		try {
			File file = new File("sdcard", fileName);
			fileOutputStream = new FileOutputStream(file);
			outputStreamWriter = new OutputStreamWriter(fileOutputStream, "UTF-8");
			outputStreamWriter.write(jsonString);
			outputStreamWriter.flush();
			return true;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		} finally {
			try {
				if (outputStreamWriter != null) {
					outputStreamWriter.close();
				}
				if (fileOutputStream != null) {
					fileOutputStream.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	

	/**
	 * 从本地文件读取JSON字符串
	 * @param context 上下文
	 * @param fileName 文件名（不含路径）
	 * @return JSON字符串，读取失败返回null
	 */
	public static String readJsonFromFile(Context context, String fileName) {
		FileInputStream fileInputStream = null;
		InputStreamReader inputStreamReader = null;
		BufferedReader bufferedReader = null;

		try {
			File file = new File(context.getFilesDir(), fileName);
			if (!file.exists()) {
				return null;
			}

			fileInputStream = new FileInputStream(file);
			inputStreamReader = new InputStreamReader(fileInputStream, "UTF-8");
			bufferedReader = new BufferedReader(inputStreamReader);

			StringBuilder stringBuilder = new StringBuilder();
			String line;
			while ((line = bufferedReader.readLine()) != null) {
				stringBuilder.append(line);
			}

			return stringBuilder.toString();
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		} finally {
			try {
				if (bufferedReader != null) {
					bufferedReader.close();
				}
				if (inputStreamReader != null) {
					inputStreamReader.close();
				}
				if (fileInputStream != null) {
					fileInputStream.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * 导出SharedPreferences到文件（一步完成）
	 * @param context 上下文
	 * @param prefName SharedPreferences名称
	 * @param fileName 文件名
	 * @return 是否成功
	 */
	public static boolean exportToFile(Context context, String prefName, String fileName) {
		String json = exportToJson(context, prefName);
		if (json != null) {
			return writeJsonToFile(context, json, fileName);
		}
		return false;
	}

	/**
	 * 从文件导入SharedPreferences（一步完成）
	 * @param context 上下文
	 * @param prefName SharedPreferences名称
	 * @param fileName 文件名
	 * @return 是否成功
	 */
	public static boolean importFromFile(Context context, String prefName, String fileName) {
		String json = readJsonFromFile(context, fileName);
		if (json != null) {
			return importFromJson(context, prefName, json);
		}
		return false;
	}

	/**
	 * 检查文件是否存在
	 * @param context 上下文
	 * @param fileName 文件名
	 * @return 是否存在
	 */
	public static boolean isFileExists(Context context, String fileName) {
		File file = new File(context.getFilesDir(), fileName);
		return file.exists();
	}

	/**
	 * 删除文件
	 * @param context 上下文
	 * @param fileName 文件名
	 * @return 是否删除成功
	 */
	public static boolean deleteFile(Context context, String fileName) {
		File file = new File(context.getFilesDir(), fileName);
		return file.delete();
	}

	
	/**
	 * 获取手机型号（优先系统API，备选读取系统文件）
	 * @return 手机型号字符串，异常时返回"未知型号"
	 */
	public static String getPhoneModel() {
		// 方案1：通过Android系统API获取（主流机型优先推荐）
		try {
			String model = Build.MODEL;
			if (model != null && !model.trim().isEmpty()) {
				return model.trim();
			}
		} catch (SecurityException e) {
			// 捕获可能的权限异常（部分定制系统限制访问）
			e.printStackTrace();
		} catch (Exception e) {
			// 捕获其他未知异常
			e.printStackTrace();
		}

		// 方案2：读取系统文件兜底（应对部分API获取失败的机型）
		try {
			String model = readSystemFile("/system/build.prop", "ro.product.model");
			if (model != null && !model.trim().isEmpty()) {
				return model.trim();
			}
		} catch (IOException e) {
			// 捕获文件读取IO异常
			e.printStackTrace();
		} catch (SecurityException e) {
			// 捕获文件访问权限异常
			e.printStackTrace();
		} catch (Exception e) {
			// 捕获其他未知异常
			e.printStackTrace();
		}

		// 所有方案失败时返回默认值
		return "未知型号";
	}

	
	/**
	 * 读取系统配置文件中的指定属性
	 * @param filePath 文件路径
	 * @param key 要查询的属性键
	 * @return 属性值，未找到返回null
	 */
	private static String readSystemFile(String filePath, String key) throws IOException {
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader(filePath));
			String line;
			// 逐行读取文件，匹配目标键
			while ((line = reader.readLine()) != null) {
				line = line.trim();
				if (line.startsWith(key + "=")) {
					// 截取等号后的属性值
					return line.substring(key.length() + 1);
				}
			}
		} finally {
			// 确保流资源关闭
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return null;
	}





}




	

	
