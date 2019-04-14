package cron;

import org.springframework.util.StringUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * CRON表达式工具类
 * @author Ramon
 */
public class CronUtils {
	private static volatile ConcurrentHashMap<String, CronExpression> caches = new ConcurrentHashMap<>();

	private static CronExpression getCronExpression(String cron) {
		CronExpression exp = caches.get(cron);
		if (exp == null) {
			exp = new CronExpression(cron);
			CronExpression old = caches.putIfAbsent(cron, exp);
			if (old != null) {
				exp = old;
			}
		}
		return exp;
	}

	/**
	 * 获取下一次执行时间
	 * @param cron
	 * @param time
	 * @return
	 */
	public static Date getTimeAfter(String cron, Date time) {
		CronExpression exp = getCronExpression(cron);
		return exp.getTimeAfter(time);
	}

	/**
	 * 获取前一次执行时间
	 * @param cron
	 * @param time
	 * @return
	 */
	public static Date getTimeBefore(String cron, Date time) {
		CronExpression exp = getCronExpression(cron);
		return exp.getTimeBefore(time);
	}
	
	/**
	 * 获取基准时间后最近的执行时间
	 * @param crons
	 * @param time
	 * @return
	 */
	public static Date getTimeAfter(String[] crons, Date time) {
		long mintime = Long.MAX_VALUE;
		for (String cron : crons) {
			Date afterTime = getTimeAfter(cron, time);
			mintime = Math.min(mintime, afterTime.getTime());
		}
		return new Date(mintime);
	}
	
	/**
	 * 获取基准时间前最近的执行时间
	 * @param crons
	 * @param time
	 * @return
	 */
	public static Date getTimeBefore(String[] crons, Date time) {
		long mintime = Long.MIN_VALUE;
		for (String cron : crons) {
			Date beforeTime = getTimeBefore(cron, time);
			mintime = Math.max(mintime, beforeTime.getTime());
		}
		return new Date(mintime);
	}

}

// /////////////////////////////////////////////////////////////////////////

class CronExpression {
	private CronSequenceGenerator generator;
	private FastCronParser parser;
	private String expression;

	/**
	 * 构造器
	 * @param expression cron表达式
	 */
	public CronExpression(String expression) {
		this.expression = expression;
		this.generator = new CronSequenceGenerator(expression);
		this.parser = new FastCronParser(expression);
	}

	/**
	 * 获取指定时间的下一个时间
	 * @param time 指定时间
	 * @return
	 */
	public Date getTimeAfter(Date time) {
		return generator.next(time);
	}

	/**
	 * 获取指定时间的上一个时间
	 * @param time 指定时间
	 * @return
	 */
	public Date getTimeBefore(Date time) {
		return parser.getTimeBefore(time);
	}

	/**
	 * cron表达式
	 */
	public String getExpression() {
		return expression;
	}

}

class CronSequenceGenerator {
	private final BitSet seconds = new BitSet(60);
	private final BitSet minutes = new BitSet(60);
	private final BitSet hours = new BitSet(24);
	private final BitSet daysOfWeek = new BitSet(7);
	private final BitSet daysOfMonth = new BitSet(31);
	private final BitSet months = new BitSet(12);
	private final String expression;
	private final TimeZone timeZone;

	/**
	 * Construct a {@link CronSequenceGenerator} from the pattern provided, using the default {@link TimeZone}.
	 * @param expression a space-separated list of time fields
	 * @throws IllegalArgumentException if the pattern cannot be parsed
	 * @see TimeZone#getDefault()
	 */
	public CronSequenceGenerator(String expression) {
		this(expression, TimeZone.getDefault());
	}

	/**
	 * Construct a {@link CronSequenceGenerator} from the pattern provided, using the specified {@link TimeZone}.
	 * @param expression a space-separated list of time fields
	 * @param timeZone the TimeZone to use for generated trigger times
	 * @throws IllegalArgumentException if the pattern cannot be parsed
	 */
	public CronSequenceGenerator(String expression, TimeZone timeZone) {
		this.expression = expression;
		this.timeZone = timeZone;
		parse(expression);
	}

	/**
	 * Get the next {@link Date} in the sequence matching the Cron pattern and after the value provided. The return
	 * value will have a whole number of seconds, and will be after the input value.
	 * @param date a seed value
	 * @return the next value matching the pattern
	 */
	public Date next(Date date) {
		/* The plan:
		 * 
		 * 1 Round up to the next whole second
		 * 
		 * 2 If seconds match move on, otherwise find the next match: 2.1 If next match is in the next minute then roll
		 * forwards
		 * 
		 * 3 If minute matches move on, otherwise find the next match 3.1 If next match is in the next hour then roll
		 * forwards 3.2 Reset the seconds and go to 2
		 * 
		 * 4 If hour matches move on, otherwise find the next match 4.1 If next match is in the next day then roll
		 * forwards, 4.2 Reset the minutes and seconds and go to 2
		 * 
		 * ... */

		Calendar calendar = new GregorianCalendar();
		calendar.setTimeZone(this.timeZone);
		calendar.setTime(date);

		// First, just reset the milliseconds and try to calculate from there...
		calendar.set(Calendar.MILLISECOND, 0);
		long originalTimestamp = calendar.getTimeInMillis();
		doNext(calendar, calendar.get(Calendar.YEAR));

		if (calendar.getTimeInMillis() == originalTimestamp) {
			// We arrived at the original timestamp - round up to the next whole second and try again...
			calendar.add(Calendar.SECOND, 1);
			doNext(calendar, calendar.get(Calendar.YEAR));
		}

		return calendar.getTime();
	}

	private void doNext(Calendar calendar, int dot) {
		List<Integer> resets = new ArrayList<Integer>();

		int second = calendar.get(Calendar.SECOND);
		List<Integer> emptyList = Collections.emptyList();
		int updateSecond = findNext(this.seconds, second, calendar, Calendar.SECOND, Calendar.MINUTE, emptyList);
		if (second == updateSecond) {
			resets.add(Calendar.SECOND);
		}

		int minute = calendar.get(Calendar.MINUTE);
		int updateMinute = findNext(this.minutes, minute, calendar, Calendar.MINUTE, Calendar.HOUR_OF_DAY, resets);
		if (minute == updateMinute) {
			resets.add(Calendar.MINUTE);
		} else {
			doNext(calendar, dot);
		}

		int hour = calendar.get(Calendar.HOUR_OF_DAY);
		int updateHour = findNext(this.hours, hour, calendar, Calendar.HOUR_OF_DAY, Calendar.DAY_OF_WEEK, resets);
		if (hour == updateHour) {
			resets.add(Calendar.HOUR_OF_DAY);
		} else {
			doNext(calendar, dot);
		}

		int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
		int dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH);
		int updateDayOfMonth = findNextDay(calendar, this.daysOfMonth, dayOfMonth, daysOfWeek, dayOfWeek, resets);
		if (dayOfMonth == updateDayOfMonth) {
			resets.add(Calendar.DAY_OF_MONTH);
		} else {
			doNext(calendar, dot);
		}

		int month = calendar.get(Calendar.MONTH);
		int updateMonth = findNext(this.months, month, calendar, Calendar.MONTH, Calendar.YEAR, resets);
		if (month != updateMonth) {
			if (calendar.get(Calendar.YEAR) - dot > 4) {
				throw new IllegalArgumentException("Invalid cron expression \"" + this.expression
						+ "\" led to runaway search for next trigger");
			}
			doNext(calendar, dot);
		}

	}

	private int findNextDay(Calendar calendar, BitSet daysOfMonth, int dayOfMonth, BitSet daysOfWeek, int dayOfWeek,
			List<Integer> resets) {

		int count = 0;
		int max = 366;
		// the DAY_OF_WEEK values in java.util.Calendar start with 1 (Sunday),
		// but in the cron pattern, they start with 0, so we subtract 1 here
		while ((!daysOfMonth.get(dayOfMonth) || !daysOfWeek.get(dayOfWeek - 1)) && count++ < max) {
			calendar.add(Calendar.DAY_OF_MONTH, 1);
			dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH);
			dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
			reset(calendar, resets);
		}
		if (count >= max) {
			throw new IllegalArgumentException("Overflow in day for expression \"" + this.expression + "\"");
		}
		return dayOfMonth;
	}

	/**
	 * Search the bits provided for the next set bit after the value provided, and reset the calendar.
	 * @param bits a {@link BitSet} representing the allowed values of the field
	 * @param value the current value of the field
	 * @param calendar the calendar to increment as we move through the bits
	 * @param field the field to increment in the calendar (@see {@link Calendar} for the static constants defining
	 *            valid fields)
	 * @param lowerOrders the Calendar field ids that should be reset (i.e. the ones of lower significance than the
	 *            field of interest)
	 * @return the value of the calendar field that is next in the sequence
	 */
	private int findNext(BitSet bits, int value, Calendar calendar, int field, int nextField, List<Integer> lowerOrders) {
		int nextValue = bits.nextSetBit(value);
		// roll over if needed
		if (nextValue == -1) {
			calendar.add(nextField, 1);
			reset(calendar, Arrays.asList(field));
			nextValue = bits.nextSetBit(0);
		}
		if (nextValue != value) {
			calendar.set(field, nextValue);
			reset(calendar, lowerOrders);
		}
		return nextValue;
	}

	/**
	 * Reset the calendar setting all the fields provided to zero.
	 */
	private void reset(Calendar calendar, List<Integer> fields) {
		for (int field : fields) {
			calendar.set(field, field == Calendar.DAY_OF_MONTH ? 1 : 0);
		}
	}

	// Parsing logic invoked by the constructor

	/**
	 * Parse the given pattern expression.
	 */
	private void parse(String expression) throws IllegalArgumentException {
		String[] fields = StringUtils.tokenizeToStringArray(expression, " ");
		if (fields.length != 6) {
			throw new IllegalArgumentException(String.format(
					"Cron expression must consist of 6 fields (found %d in \"%s\")", fields.length, expression));
		}
		setNumberHits(this.seconds, fields[0], 0, 60);
		setNumberHits(this.minutes, fields[1], 0, 60);
		setNumberHits(this.hours, fields[2], 0, 24);
		setDaysOfMonth(this.daysOfMonth, fields[3]);
		setMonths(this.months, fields[4]);
		setDays(this.daysOfWeek, replaceOrdinals(fields[5], "SUN,MON,TUE,WED,THU,FRI,SAT"), 8);
		if (this.daysOfWeek.get(7)) {
			// Sunday can be represented as 0 or 7
			this.daysOfWeek.set(0);
			this.daysOfWeek.clear(7);
		}
	}

	/**
	 * Replace the values in the commaSeparatedList (case insensitive) with their index in the list.
	 * @return a new string with the values from the list replaced
	 */
	private String replaceOrdinals(String value, String commaSeparatedList) {
		String[] list = StringUtils.commaDelimitedListToStringArray(commaSeparatedList);
		for (int i = 0; i < list.length; i++) {
			String item = list[i].toUpperCase();
			value = StringUtils.replace(value.toUpperCase(), item, "" + i);
		}
		return value;
	}

	private void setDaysOfMonth(BitSet bits, String field) {
		int max = 31;
		// Days of month start with 1 (in Cron and Calendar) so add one
		setDays(bits, field, max + 1);
		// ... and remove it from the front
		bits.clear(0);
	}

	private void setDays(BitSet bits, String field, int max) {
		if (field.contains("?")) {
			field = "*";
		}
		setNumberHits(bits, field, 0, max);
	}

	private void setMonths(BitSet bits, String value) {
		int max = 12;
		value = replaceOrdinals(value, "FOO,JAN,FEB,MAR,APR,MAY,JUN,JUL,AUG,SEP,OCT,NOV,DEC");
		BitSet months = new BitSet(13);
		// Months start with 1 in Cron and 0 in Calendar, so push the values first into a longer bit set
		setNumberHits(months, value, 1, max + 1);
		// ... and then rotate it to the front of the months
		for (int i = 1; i <= max; i++) {
			if (months.get(i)) {
				bits.set(i - 1);
			}
		}
	}

	private void setNumberHits(BitSet bits, String value, int min, int max) {
		String[] fields = StringUtils.delimitedListToStringArray(value, ",");
		for (String field : fields) {
			if (!field.contains("/")) {
				// Not an incrementer so it must be a range (possibly empty)
				int[] range = getRange(field, min, max);
				bits.set(range[0], range[1] + 1);
			} else {
				String[] split = StringUtils.delimitedListToStringArray(field, "/");
				if (split.length > 2) {
					throw new IllegalArgumentException("Incrementer has more than two fields: '" + field
							+ "' in expression \"" + this.expression + "\"");
				}
				int[] range = getRange(split[0], min, max);
				if (!split[0].contains("-")) {
					range[1] = max - 1;
				}
				int delta = Integer.valueOf(split[1]);
				for (int i = range[0]; i <= range[1]; i += delta) {
					bits.set(i);
				}
			}
		}
	}

	private int[] getRange(String field, int min, int max) {
		int[] result = new int[2];
		if (field.contains("*")) {
			result[0] = min;
			result[1] = max - 1;
			return result;
		}
		if (!field.contains("-")) {
			result[0] = result[1] = Integer.valueOf(field);
		} else {
			String[] split = StringUtils.delimitedListToStringArray(field, "-");
			if (split.length > 2) {
				throw new IllegalArgumentException("Range has more than two fields: '" + field + "' in expression \""
						+ this.expression + "\"");
			}
			result[0] = Integer.valueOf(split[0]);
			result[1] = Integer.valueOf(split[1]);
		}
		if (result[0] >= max || result[1] >= max) {
			throw new IllegalArgumentException("Range exceeds maximum (" + max + "): '" + field + "' in expression \""
					+ this.expression + "\"");
		}
		if (result[0] < min || result[1] < min) {
			throw new IllegalArgumentException("Range less than minimum (" + min + "): '" + field
					+ "' in expression \"" + this.expression + "\"");
		}
		return result;
	}

	String getExpression() {
		return this.expression;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof CronSequenceGenerator)) {
			return false;
		}
		CronSequenceGenerator cron = (CronSequenceGenerator) obj;
		return cron.months.equals(this.months) && cron.daysOfMonth.equals(this.daysOfMonth)
				&& cron.daysOfWeek.equals(this.daysOfWeek) && cron.hours.equals(this.hours)
				&& cron.minutes.equals(this.minutes) && cron.seconds.equals(this.seconds);
	}

	@Override
	public int hashCode() {
		return 37 + 17 * this.months.hashCode() + 29 * this.daysOfMonth.hashCode() + 37 * this.daysOfWeek.hashCode()
				+ 41 * this.hours.hashCode() + 53 * this.minutes.hashCode() + 61 * this.seconds.hashCode();
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + ": " + this.expression;
	}
}

/**
 * Parses cron expressions and determines at what time in the past is the most recent match for the supplied expression.
 * @author <a href="&#109;a&#105;&#108;&#116;&#111;:chris&#64;swebtec.&#99;&#111;&#109;">Chris Miller</a>
 * @author $Author: ltorunski $
 * @version $Revision: 340 $
 */
class FastCronParser {
	private static final int NUMBER_OF_CRON_FIELDS = 6;
	private static final int SECEND = 0;
	private static final int MINUTE = 1;
	private static final int HOUR = 2;
	private static final int DAY_OF_MONTH = 3;
	private static final int MONTH = 4;
	private static final int DAY_OF_WEEK = 5;

	// Lookup tables that hold the min/max/size of each of the above field types.
	// These tables are precalculated for performance.
	private static final int[] MIN_VALUE = { 0, 0, 0, 1, 1, 0 };
	private static final int[] MAX_VALUE = { 59, 59, 23, 31, 12, 6 };

	/**
	 * A lookup table holding the number of days in each month (with the obvious exception that February requires
	 * special handling).
	 */
	private static final int[] DAYS_IN_MONTH = { 31, 29, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31 };

	/**
	 * Holds the raw cron expression that this parser is handling.
	 */
	private String cronExpression = null;

	/**
	 * This is the main lookup table that holds a parsed cron expression. each long represents one of the above field
	 * types. Bits in each long value correspond to one of the possbile field values - eg, for the minute field, bits 0
	 * -> 59 in <code>lookup[MINUTE]</code> map to minutes 0 -> 59 respectively. Bits are set if the corresponding value
	 * is enabled. So if the minute field in the cron expression was <code>"0,2-8,50"</code>, bits 0, 2, 3, 4, 5, 6, 7,
	 * 8 and 50 will be set. If the cron expression is <code>"*"</code>, the long value is set to
	 * <code>Long.MAX_VALUE</code>.
	 */
	private long[] lookup = { Long.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE,
		Long.MAX_VALUE };

	/**
	 * This is based on the contents of the <code>lookup</code> table. It holds the <em>highest</em> valid field value
	 * for each field type.
	 */
	private int[] lookupMax = { -1, -1, -1, -1, -1, -1 };

	/**
	 * This is based on the contents of the <code>lookup</code> table. It holds the <em>lowest</em> valid field value
	 * for each field type.
	 */
	private int[] lookupMin = { Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE,
		Integer.MAX_VALUE, Integer.MAX_VALUE };

	/**
	 * Creates a FastCronParser that uses a default cron expression of <code>"* * * * *"</cron>.
	 * This will match any time that is supplied.
	 */
	public FastCronParser() {
	}

	/**
	 * Constructs a new FastCronParser based on the supplied expression.
	 * @throws IllegalArgumentException if the supplied expression is not a valid cron expression.
	 */
	public FastCronParser(String cronExpression) throws IllegalArgumentException {
		setCronExpression(cronExpression);
	}

	/**
	 * Resets the cron expression to the value supplied.
	 * @param cronExpression the new cron expression.
	 * @throws IllegalArgumentException if the supplied expression is not a valid cron expression.
	 */
	public void setCronExpression(String cronExpression) throws IllegalArgumentException {
		if (cronExpression == null) {
			throw new IllegalArgumentException("Cron time expression cannot be null");
		}

		this.cronExpression = cronExpression;
		parseExpression(cronExpression);
	}

	/**
	 * Retrieves the current cron expression.
	 * @return the current cron expression.
	 */
	public String getCronExpression() {
		return this.cronExpression;
	}

	/**
	 * Determines whether this cron expression matches a date/time that is more recent than the one supplied.
	 * @param time The time to compare the cron expression against.
	 * @return <code>true</code> if the cron expression matches a time that is closer to the current time than the
	 *         supplied time is, <code>false</code> otherwise.
	 */
	public boolean hasMoreRecentMatch(Date time) {
		return time.before(getTimeBefore(new Date()));
	}

	/**
	 * Find the most recent time that matches this cron expression. This time will always be in the past, ie a lower
	 * value than the supplied time.
	 * @param time The time (in milliseconds) that we're using as our upper bound.
	 * @return The time (in milliseconds) when this cron event last occurred.
	 */
	public Date getTimeBefore(Date time) {
		// It would be nice to get rid of the Calendar class for speed, but it's a lot of work...
		// We create this
		Calendar cal = new GregorianCalendar();
		cal.setTime(time);

		int second = cal.get(Calendar.SECOND);
		int minute = cal.get(Calendar.MINUTE);
		int hour = cal.get(Calendar.HOUR_OF_DAY);
		int dayOfMonth = cal.get(Calendar.DAY_OF_MONTH);
		int month = cal.get(Calendar.MONTH) + 1; // Calendar is 0-based for this field, and we are 1-based
		int year = cal.get(Calendar.YEAR);

		long validSecend = lookup[SECEND];
		long validMinutes = lookup[MINUTE];
		long validHours = lookup[HOUR];
		long validDaysOfMonth = lookup[DAY_OF_MONTH];
		long validMonths = lookup[MONTH];
		long validDaysOfWeek = lookup[DAY_OF_WEEK];

		// Find out if we have a Day of Week or Day of Month field
		boolean haveDOM = validDaysOfMonth != Long.MAX_VALUE;
		boolean haveDOW = validDaysOfWeek != Long.MAX_VALUE;

		boolean skippedNonLeapYear = false;

		while (true) {
			boolean retry = false;

			// Clean up the month if it was wrapped in a previous iteration
			if (month < 1) {
				month += 12;
				year--;
			}

			// get month...................................................
			boolean found = false;

			if (validMonths != Long.MAX_VALUE) {
				for (int i = month + 11; i > (month - 1); i--) {
					int testMonth = (i % 12) + 1;

					// Check if the month is valid
					if (((1L << (testMonth - 1)) & validMonths) != 0) {
						if ((testMonth > month) || skippedNonLeapYear) {
							year--;
						}

						// Check there are enough days in this month (catches non leap-years trying to match the 29th
						// Feb)
						int numDays = numberOfDaysInMonth(testMonth, year);

						if (!haveDOM || (numDays >= lookupMin[DAY_OF_MONTH])) {
							if ((month != testMonth) || skippedNonLeapYear) {
								// New DOM = min(maxDOM, prevDays); ie, the highest valid value
								dayOfMonth = (numDays <= lookupMax[DAY_OF_MONTH]) ? numDays : lookupMax[DAY_OF_MONTH];
								hour = lookupMax[HOUR];
								minute = lookupMax[MINUTE];
								month = testMonth;
							}

							found = true;
							break;
						}
					}
				}

				skippedNonLeapYear = false;

				if (!found) {
					// The only time we drop out here is when we're searching for the 29th of February and no other
					// date!
					skippedNonLeapYear = true;
					continue;
				}
			}

			// Clean up if the dayOfMonth was wrapped. This takes leap years into account.
			if (dayOfMonth < 1) {
				month--;
				dayOfMonth += numberOfDaysInMonth(month, year);
				hour = lookupMax[HOUR];
				continue;
			}

			// get day...................................................
			if (haveDOM && !haveDOW) { // get day using just the DAY_OF_MONTH token

				int daysInThisMonth = numberOfDaysInMonth(month, year);
				int daysInPreviousMonth = numberOfDaysInMonth(month - 1, year);

				// Find the highest valid day that is below the current day
				for (int i = dayOfMonth + 30; i > (dayOfMonth - 1); i--) {
					int testDayOfMonth = (i % 31) + 1;

					// Skip over any days that don't actually exist (eg 31st April)
					if ((testDayOfMonth <= dayOfMonth) && (testDayOfMonth > daysInThisMonth)) {
						continue;
					}

					if ((testDayOfMonth > dayOfMonth) && (testDayOfMonth > daysInPreviousMonth)) {
						continue;
					}

					if (((1L << (testDayOfMonth - 1)) & validDaysOfMonth) != 0) {
						if (testDayOfMonth > dayOfMonth) {
							// We've found a valid day, but we had to move back a month
							month--;
							retry = true;
						}

						if (dayOfMonth != testDayOfMonth) {
							hour = lookupMax[HOUR];
							minute = lookupMax[MINUTE];
						}

						dayOfMonth = testDayOfMonth;
						break;
					}
				}

				if (retry) {
					continue;
				}
			} else if (haveDOW && !haveDOM) { // get day using just the DAY_OF_WEEK token

				int daysLost = 0;
				int currentDOW = dayOfWeek(dayOfMonth, month, year);

				for (int i = currentDOW + 7; i > currentDOW; i--) {
					int testDOW = i % 7;

					if (((1L << testDOW) & validDaysOfWeek) != 0) {
						dayOfMonth -= daysLost;

						if (dayOfMonth < 1) {
							// We've wrapped back a month
							month--;
							dayOfMonth += numberOfDaysInMonth(month, year);
							retry = true;
						}

						if (currentDOW != testDOW) {
							hour = lookupMax[HOUR];
							minute = lookupMax[MINUTE];
						}

						break;
					}

					daysLost++;
				}

				if (retry) {
					continue;
				}
			}

			// Clean up if the hour has been wrapped
			if (hour < 0) {
				hour += 24;
				dayOfMonth--;
				continue;
			}

			// get hour...................................................
			if (validHours != Long.MAX_VALUE) {
				// Find the highest valid hour that is below the current hour
				for (int i = hour + 24; i > hour; i--) {
					int testHour = i % 24;

					if (((1L << testHour) & validHours) != 0) {
						if (testHour > hour) {
							// We've found an hour, but we had to move back a day
							dayOfMonth--;
							retry = true;
						}

						if (hour != testHour) {
							minute = lookupMax[MINUTE];
						}

						hour = testHour;
						break;
					}
				}

				if (retry) {
					continue;
				}
			}

			if (minute < 0) {
				minute += 60;
				hour--;
				continue;
			}

			// get minute.................................................
			if (validMinutes != Long.MAX_VALUE) {
				// Find the highest valid minute that is below the current minute
				for (int i = minute + 60; i > minute; i--) {
					int testMinute = i % 60;

					if (((1L << testMinute) & validMinutes) != 0) {
						if (testMinute > minute) {
							// We've found a minute, but we had to move back an hour
							hour--;
							retry = true;
						}

						if (minute != testMinute) {
							second = lookupMax[SECEND];
						}

						minute = testMinute;
						break;
					}
				}

				if (retry) {
					continue;
				}
			}

			// get secend.................................................
			if (validSecend != Long.MAX_VALUE) {
				// Find the highest valid second that is below the current second
				for (int i = second + 60; i > second; i--) {
					int testSecend = i % 60;

					if (((1L << testSecend) & validSecend) != 0) {
						if (testSecend > second) {
							// We've found a second, but we had to move back an minute
							minute--;
							retry = true;
						}

						second = testSecend;
						break;
					}
				}

				if (retry) {
					continue;
				}
			}

			break;
		}

		// OK, all done. Return the adjusted time value (adjusting this is faster than creating a new Calendar object)
		cal.set(Calendar.YEAR, year);
		cal.set(Calendar.MONTH, month - 1); // Calendar is 0-based for this field, and we are 1-based
		cal.set(Calendar.DAY_OF_MONTH, dayOfMonth);
		cal.set(Calendar.HOUR_OF_DAY, hour);
		cal.set(Calendar.MINUTE, minute);
		cal.set(Calendar.SECOND, second);
		cal.set(Calendar.MILLISECOND, 0);

		return cal.getTime();
	}

	/**
	 * Takes a cron expression as an input parameter, and extracts from it the relevant minutes/hours/days/months that
	 * the expression matches.
	 * @param expression A valid cron expression.
	 * @throws IllegalArgumentException If the supplied expression could not be parsed.
	 */
	private void parseExpression(String expression) throws IllegalArgumentException {
		try {
			// Reset all the lookup data
			for (int i = 0; i < lookup.length; lookup[i++] = 0) {
				lookupMin[i] = Integer.MAX_VALUE;
				lookupMax[i] = -1;
			}

			// Create some character arrays to hold the extracted field values
			char[][] token = new char[NUMBER_OF_CRON_FIELDS][];

			// Extract the supplied expression into another character array
			// for speed
			int length = expression.length();
			char[] expr = new char[length];
			expression.getChars(0, length, expr, 0);

			int field = 0;
			int startIndex = 0;
			boolean inWhitespace = true;

			// Extract the various cron fields from the expression
			for (int i = 0; (i < length) && (field < NUMBER_OF_CRON_FIELDS); i++) {
				boolean haveChar = (expr[i] != ' ') && (expr[i] != '\t');

				if (haveChar) {
					// We have a text character of some sort
					if (inWhitespace) {
						startIndex = i; // Remember the start of this token
						inWhitespace = false;
					}
				}

				if (i == (length - 1)) { // Adjustment for when we reach the end of the expression
					i++;
				}

				if (!(haveChar || inWhitespace) || (i == length)) {
					// We've reached the end of a token. Copy it into a new char array
					token[field] = new char[i - startIndex];
					System.arraycopy(expr, startIndex, token[field], 0, i - startIndex);
					inWhitespace = true;
					field++;
				}
			}

			if (field < NUMBER_OF_CRON_FIELDS) {
				throw new IllegalArgumentException("Unexpected end of expression while parsing \"" + expression
						+ "\". Cron expressions require 5 separate fields.");
			}

			// OK, we've broken the string up into the 5 cron fields, now lets add
			// each field to their lookup table.
			for (field = 0; field < NUMBER_OF_CRON_FIELDS; field++) {
				startIndex = 0;

				boolean inDelimiter = true;

				// We add each comma-delimited element seperately.
				int elementLength = token[field].length;

				for (int i = 0; i < elementLength; i++) {
					boolean haveElement = token[field][i] != ',';

					if (haveElement) {
						// We have a character from an element in the token
						if (inDelimiter) {
							startIndex = i;
							inDelimiter = false;
						}
					}

					if (i == (elementLength - 1)) { // Adjustment for when we reach the end of the token
						i++;
					}

					if (!(haveElement || inDelimiter) || (i == elementLength)) {
						// We've reached the end of an element. Copy it into a new char array
						char[] element = new char[i - startIndex];
						System.arraycopy(token[field], startIndex, element, 0, i - startIndex);

						// Add the element to our datastructure.
						storeExpressionValues(element, field);

						inDelimiter = true;
					}
				}

				if (lookup[field] == 0) {
					throw new IllegalArgumentException("Token " + new String(token[field])
							+ " contains no valid entries for this field.");
				}
			}

			// Remove any months that will never be valid
			switch (lookupMin[DAY_OF_MONTH]) {
			case 31:
				lookup[MONTH] &= (0xFFF - 0x528); // Binary 010100101000 - the months that have 30 days
			case 30:
				lookup[MONTH] &= (0xFFF - 0x2); // Binary 000000000010 - February

				if (lookup[MONTH] == 0) {
					throw new IllegalArgumentException("The cron expression \"" + expression
							+ "\" will never match any months - the day of month field is out of range.");
				}
			}

			// Check that we don't have both a day of month and a day of week field.
			if ((lookup[DAY_OF_MONTH] != Long.MAX_VALUE) && (lookup[DAY_OF_WEEK] != Long.MAX_VALUE)) {
				throw new IllegalArgumentException("The cron expression \"" + expression
						+ "\" is invalid. Having both a day-of-month and day-of-week field is not supported.");
			}
		} catch (Exception e) {
			if (e instanceof IllegalArgumentException) {
				throw (IllegalArgumentException) e;
			} else {
				throw new IllegalArgumentException("Illegal cron expression format (" + e.toString() + ")");
			}
		}
	}

	/**
	 * Stores the values for the supplied cron element into the specified field.
	 * @param element The cron element to store. A cron element is a single component of a cron expression. For example,
	 *            the complete set of elements for the cron expression <code>30 0,6,12,18 * * *</code> would be
	 *            <code>{"30", "0", "6", "12", "18", "*", "*", "*"}</code>.
	 * @param field The field that this expression belongs to. Valid values are {@link #MINUTE}, {@link #HOUR},
	 *            {@link #DAY_OF_MONTH}, {@link #MONTH} and {@link #DAY_OF_WEEK}.
	 * @throws IllegalArgumentException if there was a problem parsing the supplied element.
	 */
	private void storeExpressionValues(char[] element, int field) throws IllegalArgumentException {
		int i = 0;

		int start = -99;
		int end = -99;
		int interval = -1;
		boolean wantValue = true;
		boolean haveInterval = false;

		while ((interval < 0) && (i < element.length)) {
			char ch = element[i++];

			// Handle the wildcard character - it can only ever occur at the start of an element
			if ((i == 1) && (ch == '*')) {
				// Handle the special case where we have '*' and nothing else
				if (i >= element.length) {
					addToLookup(-1, -1, field, 1);
					return;
				}

				start = -1;
				end = -1;
				wantValue = false;
				continue;
			}

			if (wantValue) {
				// Handle any numbers
				if ((ch >= '0') && (ch <= '9')) {
					ValueSet vs = getValue(ch - '0', element, i);

					if (start == -99) {
						start = vs.value;
					} else if (!haveInterval) {
						end = vs.value;
					} else {
						if (end == -99) {
							end = MAX_VALUE[field];
						}

						interval = vs.value;
					}

					i = vs.pos;
					wantValue = false;
					continue;
				}

				if (!haveInterval && (end == -99)) {
					// Handle any months that have been suplied as words
					if (field == MONTH) {
						if (start == -99) {
							start = getMonthVal(ch, element, i++);
						} else {
							end = getMonthVal(ch, element, i++);
						}

						wantValue = false;

						// Skip past the rest of the month name
						while (++i < element.length) {
							int c = element[i] | 0x20;

							if ((c < 'a') || (c > 'z')) {
								break;
							}
						}

						continue;
					} else if (field == DAY_OF_WEEK) {
						if (start == -99) {
							start = getDayOfWeekVal(ch, element, i++);
						} else {
							end = getDayOfWeekVal(ch, element, i++);
						}

						wantValue = false;

						// Skip past the rest of the day name
						while (++i < element.length) {
							int c = element[i] | 0x20;

							if ((c < 'a') || (c > 'z')) {
								break;
							}
						}

						continue;
					}
				}
			} else {
				// Handle the range character. A range character is only valid if we have a start but no end value
				if ((ch == '-') && (start != -99) && (end == -99)) {
					wantValue = true;
					continue;
				}

				// Handle an interval. An interval is valid as long as we have a start value
				if ((ch == '/') && (start != -99)) {
					wantValue = true;
					haveInterval = true;
					continue;
				}
			}

			throw makeIllegalArgumentException("Invalid character encountered while parsing element", element);
		}

		if (element.length > i) {
			throw makeIllegalArgumentException("Extraneous characters found while parsing element", element);
		}

		if (end == -99) {
			end = start;
		}

		if (interval < 0) {
			interval = 1;
		}

		addToLookup(start, end, field, interval);
	}

	/**
	 * Extracts a numerical value from inside a character array.
	 * @param value The value of the first character
	 * @param element The character array we're extracting the value from
	 * @param i The index into the array of the next character to process
	 * @return the new index and the extracted value
	 */
	private ValueSet getValue(int value, char[] element, int i) {
		ValueSet result = new ValueSet();
		result.value = value;

		if (i >= element.length) {
			result.pos = i;
			return result;
		}

		char ch = element[i];

		while ((ch >= '0') && (ch <= '9')) {
			result.value = (result.value * 10) + (ch - '0');

			if (++i >= element.length) {
				break;
			}

			ch = element[i];
		}

		result.pos = i;

		return result;
	}

	/**
	 * Adds a group of valid values to the lookup table for the specified field. This method handles ranges that
	 * increase in arbitrary step sizes. It is also possible to add a single value by specifying a range with the same
	 * start and end values.
	 * @param start The starting value for the range. Supplying a value that is less than zero will cause the minimum
	 *            allowable value for the specified field to be used as the start value.
	 * @param end The maximum value that can be added (ie the upper bound). If the step size is greater than one, this
	 *            maximum value may not necessarily end up being added. Supplying a value that is less than zero will
	 *            cause the maximum allowable value for the specified field to be used as the upper bound.
	 * @param field The field that the values should be added to.
	 * @param interval Specifies the step size for the range. Any values less than one will be treated as a single step
	 *            interval.
	 */
	private void addToLookup(int start, int end, int field, int interval) throws IllegalArgumentException {
		// deal with the supplied range
		if (start == end) {
			if (start < 0) {
				// We're setting the entire range of values
				start = lookupMin[field] = MIN_VALUE[field];
				end = lookupMax[field] = MAX_VALUE[field];

				if (interval <= 1) {
					lookup[field] = Long.MAX_VALUE;
					return;
				}
			} else {
				// We're only setting a single value - check that it is in range
				if (start < MIN_VALUE[field]) {
					throw new IllegalArgumentException("Value " + start + " in field " + field
							+ " is lower than the minimum allowable value for this field (min=" + MIN_VALUE[field]
							+ ")");
				} else if (start > MAX_VALUE[field]) {
					throw new IllegalArgumentException("Value " + start + " in field " + field
							+ " is higher than the maximum allowable value for this field (max=" + MAX_VALUE[field]
							+ ")");
				}
			}
		} else {
			// For ranges, if the start is bigger than the end value then swap them over
			if (start > end) {
				end ^= start;
				start ^= end;
				end ^= start;
			}

			if (start < 0) {
				start = MIN_VALUE[field];
			} else if (start < MIN_VALUE[field]) {
				throw new IllegalArgumentException("Value " + start + " in field " + field
						+ " is lower than the minimum allowable value for this field (min=" + MIN_VALUE[field] + ")");
			}

			if (end < 0) {
				end = MAX_VALUE[field];
			} else if (end > MAX_VALUE[field]) {
				throw new IllegalArgumentException("Value " + end + " in field " + field
						+ " is higher than the maximum allowable value for this field (max=" + MAX_VALUE[field] + ")");
			}
		}

		if (interval < 1) {
			interval = 1;
		}

		int i = start - MIN_VALUE[field];

		// Populate the lookup table by setting all the bits corresponding to the valid field values
		for (i = start - MIN_VALUE[field]; i <= (end - MIN_VALUE[field]); i += interval) {
			lookup[field] |= (1L << i);
		}

		// Make sure we remember the minimum value set so far
		// Keep track of the highest and lowest values that have been added to this field so far
		if (lookupMin[field] > start) {
			lookupMin[field] = start;
		}

		i += (MIN_VALUE[field] - interval);

		if (lookupMax[field] < i) {
			lookupMax[field] = i;
		}
	}

	/**
	 * Indicates if a year is a leap year or not.
	 * @param year The year to check
	 * @return <code>true</code> if the year is a leap year, <code>false</code> otherwise.
	 */
	private boolean isLeapYear(int year) {
		return (((year % 4) == 0) && ((year % 100) != 0)) || ((year % 400) == 0);
	}

	/**
	 * Calculate the day of the week. Sunday = 0, Monday = 1, ... , Saturday = 6. The formula used is an optimized
	 * version of Zeller's Congruence.
	 * @param day The day of the month (1-31)
	 * @param month The month (1 - 12)
	 * @param year The year
	 * @return
	 */
	private int dayOfWeek(int day, int month, int year) {
		day += ((month < 3) ? year-- : (year - 2));
		return ((((23 * month) / 9) + day + 4 + (year / 4)) - (year / 100) + (year / 400)) % 7;
	}

	/**
	 * Retrieves the number of days in the supplied month, taking into account leap years. If the month value is outside
	 * the range <code>MIN_VALUE[MONTH] - MAX_VALUE[MONTH]</code> then the year will be adjusted accordingly and the
	 * correct number of days will still be returned.
	 * @param month The month of interest.
	 * @param year The year we are checking.
	 * @return The number of days in the month.
	 */
	private int numberOfDaysInMonth(int month, int year) {
		while (month < 1) {
			month += 12;
			year--;
		}

		while (month > 12) {
			month -= 12;
			year++;
		}

		if (month == 2) {
			return isLeapYear(year) ? 29 : 28;
		} else {
			return DAYS_IN_MONTH[month - 1];
		}
	}

	/**
	 * Quickly retrieves the day of week value (Sun = 0, ... Sat = 6) that corresponds to the day name that is specified
	 * in the character array. Only the first 3 characters are taken into account; the rest are ignored.
	 * @param element The character array
	 * @param i The index to start looking at
	 * @return The day of week value
	 */
	private int getDayOfWeekVal(char ch1, char[] element, int i) throws IllegalArgumentException {
		if ((i + 1) >= element.length) {
			throw makeIllegalArgumentException("Unexpected end of element encountered while parsing a day name",
					element);
		}

		int ch2 = element[i] | 0x20;
		int ch3 = element[i + 1] | 0x20;

		switch (ch1 | 0x20) {
		case 's': // Sunday, Saturday

			if ((ch2 == 'u') && (ch3 == 'n')) {
				return 0;
			}

			if ((ch2 == 'a') && (ch3 == 't')) {
				return 6;
			}

			break;
		case 'm': // Monday

			if ((ch2 == 'o') && (ch3 == 'n')) {
				return 1;
			}

			break;
		case 't': // Tuesday, Thursday

			if ((ch2 == 'u') && (ch3 == 'e')) {
				return 2;
			}

			if ((ch2 == 'h') && (ch3 == 'u')) {
				return 4;
			}

			break;
		case 'w': // Wednesday

			if ((ch2 == 'e') && (ch3 == 'd')) {
				return 3;
			}

			break;
		case 'f': // Friday

			if ((ch2 == 'r') && (ch3 == 'i')) {
				return 5;
			}

			break;
		}

		throw makeIllegalArgumentException("Unexpected character while parsing a day name", element);
	}

	/**
	 * Quickly retrieves the month value (Jan = 1, ..., Dec = 12) that corresponds to the month name that is specified
	 * in the character array. Only the first 3 characters are taken into account; the rest are ignored.
	 * @param element The character array
	 * @param i The index to start looking at
	 * @return The month value
	 */
	private int getMonthVal(char ch1, char[] element, int i) throws IllegalArgumentException {
		if ((i + 1) >= element.length) {
			throw makeIllegalArgumentException("Unexpected end of element encountered while parsing a month name",
					element);
		}

		int ch2 = element[i] | 0x20;
		int ch3 = element[i + 1] | 0x20;

		switch (ch1 | 0x20) {
		case 'j': // January, June, July

			if ((ch2 == 'a') && (ch3 == 'n')) {
				return 1;
			}

			if (ch2 == 'u') {
				if (ch3 == 'n') {
					return 6;
				}

				if (ch3 == 'l') {
					return 7;
				}
			}

			break;
		case 'f': // February

			if ((ch2 == 'e') && (ch3 == 'b')) {
				return 2;
			}

			break;
		case 'm': // March, May

			if (ch2 == 'a') {
				if (ch3 == 'r') {
					return 3;
				}

				if (ch3 == 'y') {
					return 5;
				}
			}

			break;
		case 'a': // April, August

			if ((ch2 == 'p') && (ch3 == 'r')) {
				return 4;
			}

			if ((ch2 == 'u') && (ch3 == 'g')) {
				return 8;
			}

			break;
		case 's': // September

			if ((ch2 == 'e') && (ch3 == 'p')) {
				return 9;
			}

			break;
		case 'o': // October

			if ((ch2 == 'c') && (ch3 == 't')) {
				return 10;
			}

			break;
		case 'n': // November

			if ((ch2 == 'o') && (ch3 == 'v')) {
				return 11;
			}

			break;
		case 'd': // December

			if ((ch2 == 'e') && (ch3 == 'c')) {
				return 12;
			}

			break;
		}

		throw makeIllegalArgumentException("Unexpected character while parsing a month name", element);
	}

	/**
	 * Recreates the original human-readable cron expression based on the internal datastructure values.
	 * @return A cron expression that corresponds to the current state of the internal data structure.
	 */
	public String getExpressionSummary() {
		StringBuffer buf = new StringBuffer();
		buf.append(getExpressionSetSummary(SECEND)).append(' ');
		buf.append(getExpressionSetSummary(MINUTE)).append(' ');
		buf.append(getExpressionSetSummary(HOUR)).append(' ');
		buf.append(getExpressionSetSummary(DAY_OF_MONTH)).append(' ');
		buf.append(getExpressionSetSummary(MONTH)).append(' ');
		buf.append(getExpressionSetSummary(DAY_OF_WEEK));

		return buf.toString();
	}

	/**
	 * <p>
	 * Converts the internal datastructure that holds a particular cron field into a human-readable list of values of
	 * the field's contents. For example, if the <code>DAY_OF_WEEK</code> field was submitted that had Sunday and Monday
	 * specified, the string <code>0,1</code> would be returned.
	 * </p>
	 * <p>
	 * If the field contains all possible values, <code>*</code> will be returned.
	 * @param field The field.
	 * @return A human-readable string representation of the field's contents.
	 */
	private String getExpressionSetSummary(int field) {
		if (lookup[field] == Long.MAX_VALUE) {
			return "*";
		}

		StringBuffer buf = new StringBuffer();

		boolean first = true;

		for (int i = MIN_VALUE[field]; i <= MAX_VALUE[field]; i++) {
			if ((lookup[field] & (1L << (i - MIN_VALUE[field]))) != 0) {
				if (!first) {
					buf.append(",");
				} else {
					first = false;
				}

				buf.append(String.valueOf(i));
			}
		}

		return buf.toString();
	}

	/**
	 * Makes a <code>IllegalArgumentException</code>. The exception message is constructed by taking the given message
	 * parameter and appending the supplied character data to the end of it. for example, if
	 * <code>msg == "Invalid character
	 * encountered"</code> and <code>data == {'A','g','u','s','t'}</code>, the resultant error message would be
	 * <code>"Invalid character encountered [Agust]"</code>.
	 * @param msg The error message
	 * @param data The data that the message
	 * @return a newly created <code>IllegalArgumentException</code> object.
	 */
	private IllegalArgumentException makeIllegalArgumentException(String msg, char[] data) {
		char[] buf = new char[msg.length() + data.length + 3];
		int msgLen = msg.length();
		System.arraycopy(msg.toCharArray(), 0, buf, 0, msgLen);
		buf[msgLen] = ' ';
		buf[msgLen + 1] = '[';
		System.arraycopy(data, 0, buf, msgLen + 2, data.length);
		buf[buf.length - 1] = ']';
		return new IllegalArgumentException(new String(buf));
	}
}

class ValueSet {
	public int value;
	public int pos;
}
