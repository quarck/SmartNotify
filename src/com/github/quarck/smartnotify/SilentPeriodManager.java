package com.github.quarck.smartnotify;

import java.util.Calendar;

public class SilentPeriodManager
{
	private static final String TAG = "SilentPeriodManager";
	
	public static boolean isEnabled(Settings settings)
	{
		return settings.isSilencePeriodEnabled() && settings.hasSilencePeriod();
	}

	// returns time in millis, when silent period ends, 
	// or 0 if we are not on silent 
	public static long getSilentUntil(Settings settings)
	{
		if (!isEnabled(settings))
			return 0;
		
		long ret = 0;
		
		Calendar cal = Calendar.getInstance();
		int hour = cal.get(Calendar.HOUR_OF_DAY);
		int minute = cal.get(Calendar.MINUTE);
		int currentTm = hour * 60 + minute;

		int silenceFrom = settings.getSilenceFrom();
		int silenceTo = settings.getSilenceTo();

		Lw.d(TAG, "have silent period from " + silenceFrom + " to " + silenceTo);
		Lw.d(TAG, "Current time is " + currentTm);

		if (silenceTo < silenceFrom)
			silenceTo += 24 * 60;

		if ( inRange(currentTm, silenceFrom, silenceTo)
			|| inRange(currentTm+24*60, silenceFrom, silenceTo))
		{
			long silentLenghtMins = (silenceTo + 24*60 - currentTm) % (24*60);
			
			ret = System.currentTimeMillis() + silentLenghtMins * 60 * 1000; // convert minutes to milliseconds

			Lw.d(TAG, "We are in the silent zone until " + ret + " (it is " + silentLenghtMins + " minutes from now)");
		}
		else
		{
			Lw.d(TAG, "We are not in the silent mode");		
		}
		
		return ret;
	}
	
	private static boolean inRange(int value, int low, int high)
	{
		return (low <= value && value <= high);
	}
}
