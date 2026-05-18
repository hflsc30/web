package com.base.util;

import java.util.function.Predicate;

/**
 * @author base
 * @since 2026-05-15
 */
public class StrUtil {

	public static boolean isBlankChar(final int c) {
		return Character.isWhitespace(c)
				|| Character.isSpaceChar(c)
				|| c == '\ufeff'
				|| c == '\u202a'
				|| c == '\u0000'
				// Hangul Filler
				|| c == 'ㅤ'
				// Braille Pattern Blank
				|| c == '⠀'
				// Zero Width Non-Joiner, ZWNJ
				|| c == '\u200c'
				// MONGOLIAN VOWEL SEPARATOR
				|| c == '\u180e';
	}

	public static String toStringOrNull(final Object obj) {
		return null == obj ? null : obj.toString();
	}

	public static boolean isEmpty(final CharSequence str) {
		return str == null || str.isEmpty();
	}

	public static boolean contains(final CharSequence str, final CharSequence searchStr) {
		if (str == null || searchStr == null) {
			return false;
		}
		return str.toString().contains(searchStr);
	}

	public static String replace(final CharSequence str, final CharSequence oldStr, final CharSequence newStr) {
		if (str == null || oldStr == null || newStr == null) {
			return toStringOrNull(str);
		}
		return str.toString().replace(oldStr, newStr);
	}

	public static String trim(final CharSequence str) {
		Predicate<Character> predicate = StrUtil::isBlankChar;

		if (StrUtil.isEmpty(str)) {
			return StrUtil.toStringOrNull(str);
		}

		final int length = str.length();
		int begin = 0;
		int end = length;

		// 扫描字符串头部
		while ((begin < end) && (predicate.test(str.charAt(begin)))) {
			begin++;
		}

		// 扫描字符串尾部
		while ((begin < end) && (predicate.test(str.charAt(end - 1)))) {
			end--;
		}

		final String result;
		if ((begin > 0) || (end < length)) {
			result = str.toString().substring(begin, end);
		} else {
			result = str.toString();
		}

		return result;
	}

	public static boolean isBlank(final CharSequence str) {
		final int length;

		if ((str == null) || ((length = str.length()) == 0)) {
			return true;
		}

		for (int i = 0; i < length; i++) {
			// 只要有一个非空字符即为非空字符串
			if (!isBlankChar(str.charAt(i))) {
				return false;
			}
		}

		return true;
	}

	public static boolean isNotBlank(final CharSequence str) {
		final int length;

		if ((str == null) || ((length = str.length()) == 0)) {
			// empty
			return false;
		}

		for (int i = 0; i < length; i++) {
			// 只要有一个非空字符即为非空字符串
			if (!isBlankChar(str.charAt(i))) {
				return true;
			}
		}

		return false;
	}
}
