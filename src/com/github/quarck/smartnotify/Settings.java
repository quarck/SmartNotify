package com.github.quarck.smartnotify;

import android.content.Context;
import android.content.SharedPreferences;

public class Settings 
{
	private Context context = null;
	
	private static final String SHARED_PREF = "com.github.quarck.SmartNotify.Settings";
	
	private static final String IS_ENABLED_KEY ="IsEnabled";
	private static final String REMIND_INTERVAL_KEY = "RemindIntervalSec";
	private static final String VIBRATION_LENGTH_KEY = "VibrationLengthMSec";
	
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

	public void setVibrationLengthMilliseconds(int value)
	{
		SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(VIBRATION_LENGTH_KEY, value);
        editor.commit();		
	}
	
	public int getVibrationLengthMilliseconds()
	{
		return prefs.getInt(VIBRATION_LENGTH_KEY, 600);		
	}
}
