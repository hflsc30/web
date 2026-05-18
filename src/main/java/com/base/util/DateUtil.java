package com.base.util;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.MonthDay;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author base
 * @since 2026-05-15
 */
public class DateUtil {

    private static final List<DateTimeFormatter> FORMATTERS = new ArrayList<>();

    static {
        // 纯日期
        FORMATTERS.add(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        FORMATTERS.add(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        FORMATTERS.add(DateTimeFormatter.ofPattern("yyyy.MM.dd"));
        FORMATTERS.add(DateTimeFormatter.ofPattern("yyyyMMdd"));
        FORMATTERS.add(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
        FORMATTERS.add(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        FORMATTERS.add(DateTimeFormatter.ofPattern("MM-dd-yyyy"));
        FORMATTERS.add(DateTimeFormatter.ofPattern("MM/dd/yyyy"));
        FORMATTERS.add(DateTimeFormatter.ISO_LOCAL_DATE);

        // 年月
        FORMATTERS.add(DateTimeFormatter.ofPattern("yyyy-MM"));
        FORMATTERS.add(DateTimeFormatter.ofPattern("yyyy/MM"));
        FORMATTERS.add(DateTimeFormatter.ofPattern("yyyyMM"));

        // 日期 + 时间
        FORMATTERS.add(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        FORMATTERS.add(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"));
        FORMATTERS.add(DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm:ss"));
        FORMATTERS.add(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"));
        FORMATTERS.add(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss.SSS"));
        FORMATTERS.add(DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm:ss.SSS"));
        FORMATTERS.add(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
        FORMATTERS.add(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS"));
        FORMATTERS.add(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        FORMATTERS.add(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
        FORMATTERS.add(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        FORMATTERS.add(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm"));

        // 中文格式
        FORMATTERS.add(DateTimeFormatter.ofPattern("yyyy年MM月dd日"));
        FORMATTERS.add(DateTimeFormatter.ofPattern("yyyy年MM月dd日 HH:mm:ss"));
        FORMATTERS.add(DateTimeFormatter.ofPattern("yyyy年MM月dd日 HH:mm"));

        // 纯时间
        FORMATTERS.add(DateTimeFormatter.ofPattern("HH:mm:ss"));
        FORMATTERS.add(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"));
        FORMATTERS.add(DateTimeFormatter.ofPattern("HH:mm"));

        // ISO
        FORMATTERS.add(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    /**
     * 解析任意常见格式的日期时间字符串为 {@link Date}。
     * <p>支持纯日期、日期+时间、纯时间、ISO 8601、中文日期等数十种格式，
     * 若所有格式均不匹配则抛出 {@link IllegalArgumentException}。</p>
     *
     * @param str 日期时间字符串，可为 {@code null}
     * @return 解析后的 Date，传入 {@code null} 或空白字符串时返回 {@code null}
     */
    public static Date parse(final CharSequence str) {
        if (str == null || str.toString().isBlank()) {
            return null;
        }
        String s = str.toString().trim();

        // ISO instant: 2026-05-15T16:31:00Z
        try {
            return Date.from(Instant.parse(s));
        } catch (DateTimeParseException ignored) {
        }

        // ISO offset: 2026-05-15T16:31:00+08:00
        try {
            return Date.from(OffsetDateTime.parse(s, DateTimeFormatter.ISO_OFFSET_DATE_TIME).toInstant());
        } catch (DateTimeParseException ignored) {
        }

        // ISO zoned: 2026-05-15T16:31:00+08:00[Asia/Shanghai]
        try {
            return Date.from(ZonedDateTime.parse(s, DateTimeFormatter.ISO_ZONED_DATE_TIME).toInstant());
        } catch (DateTimeParseException ignored) {
        }

        for (DateTimeFormatter formatter : FORMATTERS) {
            try {
                TemporalAccessor ta = formatter.parseBest(s,
                        LocalDateTime::from,
                        LocalDate::from,
                        LocalTime::from,
                        YearMonth::from,
                        MonthDay::from);
	            switch (ta) {
		            case LocalDateTime ldt -> {
			            return Date.from(ldt.atZone(ZoneId.systemDefault()).toInstant());
		            }
		            case LocalDate ld -> {
			            return Date.from(ld.atStartOfDay(ZoneId.systemDefault()).toInstant());
		            }
		            case LocalTime lt -> {
			            return Date.from(lt.atDate(LocalDate.now())
					            .atZone(ZoneId.systemDefault()).toInstant());
		            }
		            case YearMonth ym -> {
			            return Date.from(ym.atDay(1)
					            .atStartOfDay(ZoneId.systemDefault()).toInstant());
		            }
		            case MonthDay md -> {
			            return Date.from(md.atYear(LocalDate.now().getYear())
					            .atStartOfDay(ZoneId.systemDefault()).toInstant());
		            }
		            default -> {
		            }
	            }
            } catch (DateTimeParseException ignored) {
            }
        }

        throw new IllegalArgumentException("无法解析日期时间字符串: " + str);
    }
}
