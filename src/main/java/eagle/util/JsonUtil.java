package eagle.util;

import com.google.gson.*;
import org.junit.Assert;
import org.springframework.stereotype.Component;

import java.io.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.*;
import java.util.Map.Entry;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@Component
public class JsonUtil {
	final static String TOKEN = "156b8d147a2c11e5b7a9000c29a33e38";
	static Gson gson = new Gson();

	public String createGetParameter(String parameters) {
		String result = "";
		Gson gson = getGson();
		HashMap<String, Object> parametersMap = gson.fromJson(parameters, HashMap.class);
		// Generate current time timestamp and caculate sign
		if (parametersMap.containsKey("timeStamp")) {
			parametersMap.put("timeStamp", System.currentTimeMillis() + "");
		}
		if (parametersMap.containsKey("sign")) {
			parametersMap.remove("sign");
		}
		Map<String, Object> sortMap = new TreeMap<String, Object>(new MapKeyComparator());
		sortMap.putAll(parametersMap);
		for (String key : sortMap.keySet()) {
			result = result + key + "=" + sortMap.get(key) + "&";
		}
		// remove last '&' symbol
		result = result.substring(0, result.length() - 1);
		// if request need sign, generate it
		if (parameters.contains("\"sign\":")) {
			MessageDigest md5;
			String sign = "";
			try {
				md5 = MessageDigest.getInstance("MD5");
				md5.reset();
				md5.update((result + "&token=" + TOKEN).getBytes("UTF-8"));
				BigInteger bigInt = new BigInteger(1, md5.digest());
				sign = bigInt.toString(16);
				// put new new md5 value to paremeter map and remove token
				result = result + "&sign=" + sign;
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		return result;
	}

	public void validateJsonIncludeValues(String actual, String expected) {
		validateJson(actual, expected, true);
	}

	public void validateJsonExcludeValues(String actual, String expected) {
		validateJson(actual, expected, false);
	}

	public void validateJsonNodes(String actual, String expected) {
		// expect = result.data[0].brandId=56249481e4b0550174b7f41b
		// split expect string
		String key = expected.split("=")[0];
		String value = expected.split("=")[1];

		checkValue(actual, key, value);
	}

	private void checkValue(String actual, String key, String value) {
		JsonElement result = null;
		String input = actual;
		String[] nodes = key.split("\\.");
		for (int i = 0; i < nodes.length; i++) {
			// 获取个节点的值
			result = getTreeNode(input, nodes[i]);
			// 替换input为最新值
			input = result + "";
		}
		if (result == null) {
			Assert.fail(key + " element is not found!");
		} else {
			assertThat(result.toString(), is(value));
		}
	}

	public void validateJsonNodesWithQuote(String actual, String expected) {
		// expect = result.data[0].brandId=56249481e4b0550174b7f41b
		// split expect string
		String key = expected.split("=")[0];
		String value = "\"" + expected.split("=")[1] + "\"";
		checkValue(actual, key, value);
	}

	/**
	 * jsonTree: result.data[0].brandId=56249481e4b0550174b7f41b
	 * @param httpResult
	 * @param jsonTree
	 * @return
	 */
	public String getJsonElement(String httpResult, String jsonTree) {
		// jsonTree: result.data[0].brandId=56249481e4b0550174b7f41b
		// split jsonTree string
		JsonElement result = null;
		String[] nodes = jsonTree.split("\\.");
		for (int i = 0; i < nodes.length; i++) {
			// 获取每个节点的值
			result = getTreeNode(httpResult, nodes[i]);
			if (result.isJsonNull()) {
				return "null";
			}
			// 替换httpResult为最新值
			httpResult = result + "";
		}
		// 遍历结束后，返回值
		// primitive类型
		if (result.isJsonPrimitive()) {
			return result.getAsString();
		}
		// null类型
		if (result.isJsonNull()) {
			return result.getAsJsonNull() + "";
		}
		// null类型
		if (result.isJsonArray()) {
			return result.getAsJsonArray() + "";
		}
		// JsonObject类型
		return result.getAsJsonObject() + "";
	}

	private JsonElement getTreeNode(String httpResult, String nodeName) {
		// jsonTree: result.data[0].brandId=56249481e4b0550174b7f41b
		// split jsonTree string
		JsonElement result = getJson(httpResult);
		JsonElement target = null;
		// 数组node
		if (nodeName.endsWith("]")) {
			// 格式[1]
			int index = Integer.parseInt(nodeName.substring(nodeName.indexOf("[") + 1, nodeName.indexOf("]")));
			if (result.isJsonArray()) {
				target = result.getAsJsonArray().get(index);
			} else {
				// 格式names[1]
				String node = nodeName.substring(0, nodeName.indexOf("["));
				target = result.getAsJsonObject().get(node).getAsJsonArray().get(index);
			}
		} else {
			// 非数组，直接取值
			if (result.isJsonNull()) {
				target = new JsonNull();
			}
			target = result.getAsJsonObject().get(nodeName);
		}
		if (target == null) {
			System.out.println(nodeName + "不存在!");
			Assert.fail("参数" + nodeName + "获取不到，请查看依赖用例是否通过！");
		}
		return target;
	}

	public JsonElement getJson(String json) {
		Gson gson = getGson();
		String result = "";
		String expected_string = "";
		JsonElement jo = null;
		if (json.endsWith(".json")) {
			try {
				InputStreamReader isr = new InputStreamReader(new FileInputStream(new File(json)), "UTF-8");
				BufferedReader br = new BufferedReader(isr);
				String tmp = "";
				while ((tmp = br.readLine()) != null) {
					expected_string += tmp;
				}
				result = expected_string;
				isr.close();
				br.close();
			} catch (IOException io) {
				Assert.fail(json + "文件不存在.");
			}
		} else {
			result = json;
		}
		try {
			jo = gson.fromJson(result, JsonElement.class);
		} catch (JsonSyntaxException e) {
			Assert.fail("目标格式不是JOSON格式!");
		}
		return jo;
	}

	public JsonArray getJsonArray(String json) {
		return gson.fromJson(json, JsonArray.class);
	}

	private void validateJson(String actual, String expected, boolean checkvalue) {
		// 获取实际JSON结果
		JsonElement actual_json = getJson(actual);
		// 获取期望JSON结果
		JsonElement expected_json = getJson(expected);
		// 判断实际和目标结果类型是否一致，如果不一致，回复匹配错误
		// Json数组
		System.out.println("expected" + expected);
		System.out.println("actual" + actual);
		if (actual_json.isJsonArray() && expected_json.isJsonArray()) {
			validateArrays("根节点数组", actual_json.getAsJsonArray(), expected_json.getAsJsonArray(), checkvalue);
		}
		// Json对象
		if (actual_json.isJsonObject() && expected_json.isJsonObject()) {
			Set<Entry<String, JsonElement>> expected_set = expected_json.getAsJsonObject().entrySet();
			Set<Entry<String, JsonElement>> actual_set = actual_json.getAsJsonObject().entrySet();
			// 判断实际JSON是否多出额外的Element
			compareSets(expected_set, actual_set);
			// 对比期望JSON和实际JSON的值
			for (Iterator<Entry<String, JsonElement>> iterator = expected_set.iterator(); iterator.hasNext();) {
				Entry<String, JsonElement> expected_entry = (Entry<String, JsonElement>) iterator.next();
				if (!actual_json.getAsJsonObject().has(expected_entry.getKey())) {
					Assert.fail("实际结果中：" + expected_entry.getKey().toString() + " element 不存在!!");
				} else {
						validateValue(checkvalue, actual_json.getAsJsonObject(), expected_entry);
				}
			}
		} else {
			// 其他情况，期望和实际结果类型不匹配，报错！！
			Assert.fail("期望和实际结果类型不匹配！");
		}

	}

	/**
	 * 判断JsonNull和Jsonimitive值
	 *
	 * @param elementName
	 *            比较元素的名称
	 * @param expected
	 * @param actual
	 */
	private void validateJsonElement(String elementName, JsonElement expected, JsonElement actual) {
		if (expected.isJsonPrimitive()) {
			// 如果不需要检查此节点，直接跳过
			if ("uncheck".equals(expected.getAsString())) {
				return;
			}
			// 需要校验
			assertThat(elementName + ":", actual.getAsString(), is(expected.getAsString()));
		} else {
			try {
				assertThat(elementName + ":", actual.getAsJsonNull(), is(expected.getAsJsonNull()));
			} catch (Exception e) {
				Assert.fail("实际结果中:" + elementName + "不是null值");
			}
		}
	}

	private void validateValue(boolean checkvalue, JsonObject actual_json, Entry<String, JsonElement> expected_entry) {
		// 获取节点值
		JsonElement expected_je = expected_entry.getValue();
		JsonElement actual_je = actual_json.get(expected_entry.getKey());
		// 如果是原始元素，判读其值是否和期望值一致
		if (expected_je.isJsonPrimitive() || expected_je.isJsonNull()) {
			if (checkvalue) {
				validateJsonElement(expected_entry.getKey().toString(), expected_je, actual_je);
			}
		} else {
			// 如果不是原始元素，递归处理
			// 如果原始是数组，处理数组里面的值
			if (expected_je.isJsonArray()) {
				JsonArray actual_ja = actual_je.getAsJsonArray();
				JsonArray expect_ja = expected_je.getAsJsonArray();
				validateArrays(expected_entry.getKey().toString(), actual_ja, expect_ja, checkvalue);
			} else {
				try {
					validateJson(actual_je.toString(), expected_je.toString(), checkvalue);
				} catch (IllegalAccessError e) {

				}
			}
		}
	}

	private void validateArrays(String elementKeyName, JsonArray actual_ja, JsonArray expect_ja, boolean checkvalue) {
		// 如果不是原始元素，递归处理
		// 如果原始是数组，处理数组里面的值
		if (actual_ja.size() != expect_ja.size()) {
			if (checkvalue) {
				Assert.fail(elementKeyName + ": array size is not the same as expected size!");
			}
		}
		compareArrays(checkvalue, elementKeyName, actual_ja, expect_ja);
	}

	private void compareArrays(boolean checkvalue, String elementKeyName, JsonArray actuals, JsonArray expects) {
		for (int i = 0; i < expects.size(); i++) {
			if (expects.get(i).isJsonPrimitive() || expects.get(i).isJsonNull()) {
				if (checkvalue) {
					if (!"uncheck".equals(expects.get(i).getAsString())) {
						// 比较
						assertThat(elementKeyName + ":", actuals.get(i).toString(), is(expects.get(i).toString()));
					}
				}
			} else {
				try {
					validateJson(actuals.get(i).toString(), expects.get(i).toString(), checkvalue);
				} catch (IllegalAccessError e) {

				}

			}
		}
	}

	private static void compareSets(Set<Entry<String, JsonElement>> expected_set,
			Set<Entry<String, JsonElement>> actual_set) {
		if (actual_set.size() > expected_set.size()) {
			Assert.fail("实际结果keys数量多出期keys数量 " + (actual_set.size() - expected_set.size()) + "个!");

		}
	}

	synchronized public Gson getGson() {
		// 设定时期格式
		return gson;
	}

	// order by name ascending
	static class MapKeyComparator implements Comparator<String> {

		public int compare(String o1, String o2) {
			// TODO Auto-generated method stub
			return o1.compareTo(o2);
		}

	}

	public String getValueFromBigDecimal(BigDecimal bigDecimal) {
		try {
			return bigDecimal.longValueExact()+"";
		} catch (ArithmeticException e) {
			// TODO: handle exception
			return bigDecimal.doubleValue()+"";
		}
	}
}
