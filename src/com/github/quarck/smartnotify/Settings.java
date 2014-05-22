package com.github.quarck.smartnotify;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class Settings
{
	private Context context = null;

	private static final String SHARED_PREF = "com.github.quarck.SmartNotify.Settings";

	private static final String IS_ENABLED_KEY = "pref_key_is_enabled";
	private static final String VIBRATION_PATTERN_KEY = "pref_key_vibration_pattern";

	private static final String SILENCE_FROM_KEY = "pref_key_time_silence_from"; // number of
																	// minutes
																	// since
																	// 00:00:00
	private static final String SILENCE_TO_KEY = "pref_key_time_silence_to";
	
	private static final String ENABLE_SILENCE_HOURS_KEY = "pref_key_enable_silence_hours";

	private SharedPreferences prefs = null;

	public Settings(Context ctx)
	{
		context = ctx;
		//prefs = context.getSharedPreferences(SHARED_PREF, Context.MODE_PRIVATE);
		prefs = PreferenceManager.getDefaultSharedPreferences(context);
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
	
		editor.putInt(SILENCE_FROM_KEY, from);
		editor.putInt(SILENCE_TO_KEY, to);
		
//		editor.putString(SILENCE_FROM_KEY, String.format("%1$02:%2$02", from / 60, from % 60));
//		editor.putString(SILENCE_TO_KEY, String.format("%1$02:%2$02", to / 60, to % 60));
		editor.commit();
	}

	/*
	private int getPrefTime(String key, int defaultValue)
	{
		int ret = defaultValue;
		
		String strPref = prefs.getString(key, "");
		
		if (!strPref.equals(""))
		{
			String[] parts = strPref.split(":", 2);
			try 
			{
				int hr, min;
				hr = Integer.valueOf(parts[0]);
				min = Integer.valueOf(parts[1]);
				ret = hr * 60 + min;				
			}
			catch (Exception ex)
			{
				ex.printStackTrace();
			}
		}
		
		return ret;
	}*/
	
	public int getSilenceFrom()
	{
		return prefs.getInt(SILENCE_FROM_KEY, 21 * 60 + 0); // 21:00
	}

	public int getSilenceTo()
	{
		return prefs.getInt(SILENCE_TO_KEY, 5 * 60 + 30); // 5:30
	}

	public boolean hasSilencePeriod()
	{
		return getSilenceFrom() != getSilenceTo();
	}
	
	public boolean isSilencePeriodEnabled()
	{
		return prefs.getBoolean(ENABLE_SILENCE_HOURS_KEY, false);		
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
			String patternStr = prefs.getString(VIBRATION_PATTERN_KEY,
					"0,80,30,80,30,80,30,80,30,80,30,80,30,80,150,900");

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
			Lw.d("Got exception while reading vibration pattern");
		}

		return new long[] { 0, 1500 }; // very-very failback for the case when
										// we've failed
	}
}
