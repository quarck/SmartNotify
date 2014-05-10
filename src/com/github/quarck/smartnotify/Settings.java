package com.github.quarck.smartnotify;

import android.content.Context;
import android.content.SharedPreferences;

public class Settings
{
	private Context context = null;

	private static final String SHARED_PREF = "com.github.quarck.SmartNotify.Settings";

	private static final String IS_ENABLED_KEY = "IsEnabled";
	private static final String REMIND_INTERVAL_KEY = "RemindIntervalSec";
	private static final String VIBRATION_PATTERN_KEY = "VibrationPattern";

	private static final String SILENT_FROM_KEY = "SilentStart"; // number of
																	// minutes
																	// since
																	// 00:00:00
	private static final String SILENT_TO_KEY = "SilentEnd";

	private SharedPreferences prefs = null;

	public Settings(Context ctx)
	{
		context = ctx;
		prefs = context.getSharedPreferences(SHARED_PREF, Context.MODE_PRIVATE);
	}

	public void setServiceEnabled(boolean value)
	{
		SharedPreferences.Editor editor = prefs.edit();
		editor.putBoolean(IS_ENABLED_KEY, value);
		editor.commit();
	}

	public boolean isServiceEnabled()
	{
		return prefs.getBoolean(IS_ENABLED_KEY, false);
	}

	public void setSilencePeriod(int from, int to)
	{
		SharedPreferences.Editor editor = prefs.edit();
		editor.putInt(SILENT_FROM_KEY, from);
		editor.putInt(SILENT_TO_KEY, to);
		editor.commit();
	}

	public int getSilenceFrom()
	{
		return prefs.getInt(SILENT_FROM_KEY, 21 * 60 + 0); // 21:00
	}

	public int getSilenceTo()
	{
		return prefs.getInt(SILENT_TO_KEY, 5 * 60 + 30); // 5:30
	}
	
	public boolean hasSilencePeriod()
	{
		return getSilenceFrom() != getSilenceTo();
	}

	public void setRemindIntervalSeconds(int value)
	{
		SharedPreferences.Editor editor = prefs.edit();
		editor.putInt(REMIND_INTERVAL_KEY, value);
		editor.commit();
	}

	public int getRemindIntervalSeconds()
	{
		return prefs.getInt(REMIND_INTERVAL_KEY, 600);
	}

	public void setVibrationPattern(long[] pattern)
	{
		SharedPreferences.Editor editor = prefs.edit();
		StringBuilder sb = new StringBuilder();

		boolean isFirst = true;
		for (long len : pattern)
		{
			if (!isFirst)
				sb.append(",");
			sb.append(len);
			isFirst = false;
		}

		editor.putString(VIBRATION_PATTERN_KEY, sb.toString());
		editor.commit();
	}

	public long[] getVibrationPattern()
	{
		try
		{
			String patternStr = prefs.getString(VIBRATION_PATTERN_KEY, "0,80,30,80,30,80,30,80,30,80,30,80,30,80,150,900");

			String[] times = patternStr.split(",");

			long[] pattern = new long[times.length];

			for (int idx = 0; idx < times.length; ++idx)
			{
				pattern[idx] = Long.parseLong(times[idx]);
			}

			return pattern;
		}
		catch (Exception ex)
		{
		}

		return new long[]
		{
				0, 1500
		}; // very-very failback for the case when we've failed
	}
}
