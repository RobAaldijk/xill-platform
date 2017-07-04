/**
 * Copyright (C) 2014 Xillio (support@xillio.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nl.xillio.xill.plugins.date.services;

import com.google.common.base.CaseFormat;
import com.google.inject.Singleton;
import nl.xillio.xill.api.data.Date;
import nl.xillio.xill.api.data.DateFactory;
import nl.xillio.xill.api.errors.InvalidUserInputException;

import java.time.*;
import java.time.chrono.Chronology;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAccessor;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Implementation providing {@link DateService} functions.
 *
 * @author Sander Visser
 * @author Geert Konijnendijk
 */
@Singleton
public class DateServiceImpl implements DateService, DateFactory {

    private static final DateTimeFormatter DEFAULT_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public Date now() {
        return new nl.xillio.xill.plugins.date.data.Date(ZonedDateTime.now());
    }

    @Override
    public Date constructDate(int year, int month, int day, int hour, int minute, int second, int nano, ZoneId zone) {
        return new nl.xillio.xill.plugins.date.data.Date(ZonedDateTime.of(year, month, day, hour, minute, second, nano, zone));
    }

    @SuppressWarnings("squid:S1166")
        // DateTimeException is handled correctly
    ZonedDateTime getValueOrDefaultZDT(TemporalAccessor parsed) {
        ZonedDateTime now = ZonedDateTime.now();
        ZonedDateTime _default = ZonedDateTime.of(now.toLocalDate(), LocalTime.MIN, now.getZone());

        ChronoField[] parameters = {ChronoField.YEAR,
                ChronoField.MONTH_OF_YEAR,
                ChronoField.DAY_OF_MONTH,
                ChronoField.HOUR_OF_DAY,
                ChronoField.MINUTE_OF_HOUR,
                ChronoField.SECOND_OF_MINUTE,
                ChronoField.NANO_OF_SECOND};
        int[] p = Arrays.stream(parameters).mapToInt(cf -> parsed.isSupported(cf) ? parsed.get(cf) : _default.get(cf)).toArray();

        ZoneId zone;
        try {
            zone = ZoneId.from(parsed);
        } catch (DateTimeException dte) {
            zone = ZoneId.systemDefault();
        }

        return ZonedDateTime.of(p[0], p[1], p[2], p[3], p[4], p[5], p[6], zone);
    }

    @Override
    public Date parseDate(String date, String format, String locale) {

        if (!isValidLocale(locale))
            throw new InvalidUserInputException("Invalid Locale", locale, "A valid Locale code.");

        Locale loc = locale == null ? Locale.getDefault() : Locale.forLanguageTag(locale);
        DateTimeFormatter formatter = createDateTimeFormatter(format, loc);
        TemporalAccessor parsed = formatter.parse(date);
        return new nl.xillio.xill.plugins.date.data.Date(getValueOrDefaultZDT(parsed));
    }

    private DateTimeFormatter createDateTimeFormatter(String format, Locale locale) {
        return format == null ? DEFAULT_FORMATTER : DateTimeFormatter.ofPattern(format)
                .withLocale(locale)
                .withChronology(Chronology.ofLocale(locale));
    }

    @Override
    public Date add(Date original, Map<ChronoUnit, Long> toAdd) {
        ZonedDateTime value = original.getZoned();
        for (Entry<ChronoUnit, Long> entry : toAdd.entrySet()) {
            value = value.plus(entry.getValue(), entry.getKey());
        }
        return new nl.xillio.xill.plugins.date.data.Date(value);
    }

    @Override
    public Date changeTimeZone(Date original, ZoneId newZone) {
        return new nl.xillio.xill.plugins.date.data.Date(ZonedDateTime.from(original.getZoned().withZoneSameInstant(newZone)));
    }

    @Override
    public String formatDate(Date date, String format) {
        DateTimeFormatter formatter = createDateTimeFormatter(format, Locale.getDefault());
        return formatter.format(date.getZoned());
    }

    @Override
    public String formatDateLocalized(Date date, FormatStyle dateStyle, FormatStyle timeStyle, Locale locale) {
        DateTimeFormatter formatter;
        if (locale == null) {
            formatter = DEFAULT_FORMATTER;
        } else if (dateStyle == null) {
            if (timeStyle == null) {
                throw new IllegalArgumentException("No dateStyle or timeStyle was provided");
            }
            formatter = DateTimeFormatter.ofLocalizedTime(timeStyle).withLocale(locale);
        } else {
            formatter = timeStyle == null ? DateTimeFormatter.ofLocalizedDate(dateStyle).withLocale(locale)
                    : DateTimeFormatter.ofLocalizedDateTime(dateStyle, timeStyle).withLocale(locale);
        }

        return formatter.format(date.getZoned());
    }

    @Override
    public Map<String, Long> getFieldValues(Date date) {
        LinkedHashMap<String, Long> fields = new LinkedHashMap<>();
        for (ChronoField field : ChronoField.values()) {
            if (date.getZoned().isSupported(field)) {
                fields.put(CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, field.toString()), date.getZoned().getLong(field));
            }
        }
        return fields;
    }

    @Override
    public ZoneId getTimezone(Date date) {
        return date.getZoned().getZone();
    }

    @Override
    public boolean isInFuture(Date date) {
        return date.getZoned().isAfter(now().getZoned());
    }

    @Override
    public boolean isInPast(Date date) {
        return date.getZoned().isBefore(now().getZoned());
    }

    @Override
    public Map<String, Number> difference(Date date1, Date date2, boolean absolute) {
        // Calculate difference and convert to seconds
        long milisDifference = date1.getZoned().until(date2.getZoned(), ChronoUnit.MILLIS);
        if (absolute) {
            milisDifference = Math.abs(milisDifference);
        }

        // Calculate the totals
        Map<String, Number> diff = new LinkedHashMap<>();
        for (TimeUnits t : TimeUnits.values()) {
            Double total = (double) milisDifference / (double) t.getNumMilliseconds();
            String name = String.format("total%s", t.getPascalName());
            if (TimeUnits.MILLIS.equals(t)) {
                Long totalLong = total.longValue();
                diff.put(name, totalLong);
            } else {
                diff.put(name, total);
            }
        }
        // Calculate the additive differences by going through the TimeUnits in reverse order and
        for (int i = TimeUnits.values().length - 1; i >= 0; i--) {
            TimeUnits unit = TimeUnits.values()[i];
            Long value = milisDifference / unit.getNumMilliseconds();
            milisDifference -= value * unit.getNumMilliseconds();
            diff.put(unit.getCamelName(), value);
        }
        return diff;
    }

    @Override
    public Date from(Instant instant) {
        long milli = instant.toEpochMilli();

        return new nl.xillio.xill.plugins.date.data.Date(
                ZonedDateTime.from(Instant.ofEpochMilli(milli).atZone(ZoneId.systemDefault()))
        );
    }

    @Override
    public Date fromTimestamp(long timestamp) {
        return from(Instant.ofEpochSecond(timestamp));
    }

    @Override
    public boolean isBefore(Date date1, Date date2) {
        return date1.getZoned().isBefore(date2.getZoned());
    }

    @Override
    public boolean isAfter(Date date1, Date date2) {
        return date1.getZoned().isAfter(date2.getZoned());
    }

    @Override
    public boolean isValidLocale(String locale) {
        String supportedLocales[] = {
            "af-ZA","sq-AL","ar-DZ","ar-BH","ar-EG","ar-IQ","ar-JO","ar-KW","ar-LB",
            "ar-LY","ar-MA","ar-OM","ar-QA","ar-SA","ar-SY","ar-TN","ar-AE","ar-YE",
            "hy-AM","Cy-az-AZ","Lt-az-AZ","eu-ES","be-BY","bg-BG","ca-ES","zh-CN",
            "zh-HK","zh-MO","zh-SG","zh-TW","zh-CHS","zh-CHT","hr-HR","cs-CZ","da-DK",
            "div-MV","nl-BE","nl-NL","en-AU","en-BZ","en-CA","en-CB","en-IE","en-JM",
            "en-NZ","en-PH","en-ZA","en-TT","en-GB","en-US","en-ZW","et-EE","fo-FO",
            "fa-IR","fi-FI","fr-BE","fr-CA","fr-FR","fr-LU","fr-MC","fr-CH","gl-ES",
            "ka-GE","de-AT","de-DE","de-LI","de-LU","de-CH","el-GR","gu-IN","he-IL",
            "hi-IN","hu-HU","is-IS","id-ID","it-IT","it-CH","ja-JP","kn-IN","kk-KZ",
            "kok-IN","ko-KR","ky-KZ","lv-LV","lt-LT","mk-MK","ms-BN","ms-MY","mr-IN",
            "mn-MN","nb-NO","nn-NO","pl-PL","pt-BR","pt-PT","pa-IN","ro-RO","ru-RU",
            "sa-IN","Cy-sr-SP","Lt-sr-SP","sk-SK","sl-SI","es-AR","es-BO","es-CL",
            "es-CO","es-CR","es-DO","es-EC","es-SV","es-GT","es-HN","es-MX","es-NI",
            "es-PA","es-PY","es-PE","es-PR","es-ES","es-UY","es-VE","sw-KE","sv-FI",
            "sv-SE","syr-SY","ta-IN","tt-RU","te-IN","th-TH","tr-TR","uk-UA","ur-PK",
            "Cy-uz-UZ","Lt-uz-UZ","vi-VN"
        };

        for (String loc: supportedLocales) {
            if (loc.equals(locale)) return true;
        }
        return false;
    }

    /**
     * Represents different kinds of time units, containing their name and the amount of nanoseconds they contain.
     * <p>
     * The units should be listed in growing order of length.
     *
     * @author Geert Konijnendijk
     */
    private enum TimeUnits {
        MILLIS(1),
        SECONDS(1000),
        MINUTES(60000),
        HOURS(3600000),
        DAYS(86400000),
        WEEKS(604800000),
        MONTHS(2629746000L),
        YEARS(31556952000L),
        DECADES(315569520000L),
        CENTURIES(3155695200000L),
        MILLENNIA(31556952000000L);

        private final long numMilliseconds;
        private final String camelName;
        private final String pascalName;

        /**
         * @param numSeconds Number of seconds that fit into one unit of this kind in {@link Long} String representation
         */
        TimeUnits(long numSeconds) {
            this.numMilliseconds = numSeconds;
            this.camelName = CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, this.name());
            this.pascalName = CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, this.name());
        }

        public Long getNumMilliseconds() {
            return numMilliseconds;
        }

        public String getCamelName() {
            return camelName;
        }

        public String getPascalName() {
            return pascalName;
        }
    }
}
