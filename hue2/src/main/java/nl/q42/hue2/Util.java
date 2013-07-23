package nl.q42.hue2;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Util {
	public static String quickMatch(String pattern, String target) {
		Matcher m = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE).matcher(target);
		if (m.find()) {
			return m.group(1);
		} else {
			return null;
		}
	}
}
