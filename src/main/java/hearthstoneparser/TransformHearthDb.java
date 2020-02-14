package hearthstoneparser;

import com.google.common.collect.Lists;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;

public class TransformHearthDb {

	public static void main(String[] args) throws Exception {
		BufferedReader br = new BufferedReader(new InputStreamReader(GenerateCardData.class.getResourceAsStream
				("raw_card_ids.txt"), "UTF-8"));
		StringBuffer sb = new StringBuffer();
		String line;
		List<String> currentNamespaces = Lists.newArrayList();
		while ((line = br.readLine()) != null) {
			if (line.contains("namespace")) {
				continue;
			}
			if (line.contains("{") || line.contains("/*")) {
				continue;
			}
			if (line.contains("public class ")) {
				currentNamespaces.add(line.split("public class ")[1]);
				continue;
			}
			if (line.contains("public const string ")) {
				String cardDeclaration = line.split("public const string ")[1];
			}
			sb.append(line);
			sb.append("\n");
		}
		System.out.println("Contents of File: ");
	}
}
