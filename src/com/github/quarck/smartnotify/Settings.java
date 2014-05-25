/*
 * Copyright (c) 2014, Sergey Parshin, quarck@gmail.com
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of developer (Sergey Parshin) nor the
 *       names of other project contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */


package com.github.quarck.smartnotify;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.Uri;
import android.preference.PreferenceManager;

public class Settings
{
	private Context context = null;

	private static final String IS_ENABLED_KEY = "pref_key_is_enabled";
	private static final String VIBRATION_PATTERN_KEY = "pref_key_vibration_pattern";
	private static final String RINGTONE_KEY = "pref_key_ringtone";

	// number of minutes since 00:00:00 
	private static final String SILENCE_FROM_KEY = "pref_key_time_silence_from"; 
	private static final String SILENCE_TO_KEY = "pref_key_time_silence_to";
	
	private static final String ENABLE_SILENCE_HOURS_KEY = "pref_key_enable_silence_hours";

	private static final String INITIAL_POPULATED_KEY = "initial_populated";
	
	private SharedPreferences prefs = null;

	public Settings(Context ctx)
	{
		context = ctx;
		prefs = PreferenceManager.getDefaultSharedPreferences(context);
	}

	public void setInitialPopulated(boolean value)
	{
		SharedPreferences.Editor editor = prefs.edit();
		editor.putBoolean(INITIAL_POPULATED_KEY, value);
		editor.commit();
	}

	public boolean isInitialPopulated()
	{
		return prefs.getBoolean(INITIAL_POPULATED_KEY, false);
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
		
		editor.commit();
	}
	
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

	public static long[] getDefaultVibrationPattern()
	{
		return new long[]{0,80,30,100,40,110,50,120,50,150,30,150,150,1500};
	}
	
	public static String getDefaultVibrationPatternStr()
	{
		long[] pattern = getDefaultVibrationPattern();
		
		StringBuilder sb = new StringBuilder();
		
		boolean isFirst = true;
		
		for (long length : pattern)
		{
			if (!isFirst)
				sb.append(",");
			
			isFirst = false;
			
			sb.append(length);
		}
		
		return sb.toString();
	}

	public static long[] parseVibrationPattern(String pattern)
	{
		long[] ret = null;

		try
		{
			String[] times = pattern.split(",");

			ret = new long[times.length];

			for (int idx = 0; idx < times.length; ++idx)
			{
				ret[idx] = Long.parseLong(times[idx]);
			}
		}
		catch (Exception ex)
		{
			Lw.d("Got exception while parsing vibration pattern " + pattern);
			ret = null;
		}

		return ret;
	}
	
	public long[] getVibrationPattern()
	{
		String patternStr = prefs.getString(VIBRATION_PATTERN_KEY,
				getDefaultVibrationPatternStr());

		long[] pattern = parseVibrationPattern(patternStr);
		
		if (pattern != null)
			return pattern;
		
		return getDefaultVibrationPattern(); // fail back
	}
	
	public Uri getRingtoneURI()
	{
		Uri notification = null;
		try
		{
			String uriValue = prefs.getString(RINGTONE_KEY, "");

			if (uriValue != null && !uriValue.isEmpty())
				notification = Uri.parse(uriValue);			
		} 
		catch (Exception e) 
		{
		    e.printStackTrace();
		}
		finally
		{
			if (notification == null)
				notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);		    
		}

		return notification;
	}
}
