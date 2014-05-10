package com.github.quarck.smartnotify;

import java.util.Calendar;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Vibrator;

public class Alarm extends BroadcastReceiver
{
	public static final String TAG = "Alarm";

	public Alarm()
	{
		super();
	}

	@Override
	public void onReceive(Context context, Intent intent) // alarm fired
	{
		Lw.d(TAG, "Alarm received");

		Settings settings = new Settings(context);
		
		if (settings.isServiceEnabled())
		{
			Lw.d(TAG, "Serivce is enabled");
			
			GlobalState.setNextAlarmTime(context, 
					System.currentTimeMillis() 
						+ GlobalState.getCurrentRemindInterval(context));

			boolean fireReminder = true;

			if (settings.hasSilencePeriod())
			{
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
					Lw.d(TAG, "Service is enabled, but we are in silent zone, not alarming!");
					fireReminder = false;
				}
				else
				{
					Lw.d(TAG, "Service is enabled, we are not in silient zone, vibrating.");		
				}
			}
			else
			{
				Lw.d(TAG, "Service is enabled, no silet period. vibrating");
			}

			if (fireReminder && GlobalState.getIsOnCall(context))
			{
				Lw.d(TAG, "Was going to fire the reminder, but call is currently in progress. Would skip this one.");
				fireReminder = false;
			}

			if (fireReminder)
			{
				Lw.d(TAG, "Firing alarm finally");
				Vibrator v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
				long[] pattern = settings.getVibrationPattern();
				v.vibrate(pattern, -1);
			}
		}
		else
		{
			Lw.d(TAG, "Service is now got disabled, cancelling alarm");
			cancelAlarm(context);
		}
	}

	public void setAlarmMillis(Context context, long whenMillis, int repeatMillis)
	{
		Lw.d(TAG, "Setting alarm for " + whenMillis + ", repeat " + repeatMillis);
		
		cancelAlarm(context); // cancel previous alarm, if any, so we would not have two alarms
		
		GlobalState.setCurrentRemindInterval(context,  repeatMillis);
		GlobalState.setNextAlarmTime(context, whenMillis);

		Intent intent = new Intent(context, Alarm.class);
		PendingIntent pendIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

		alarmManager(context).setRepeating(AlarmManager.RTC_WAKEUP, whenMillis, repeatMillis, pendIntent);
	}

	public void setAlarmMillis(Context context, int repeatMillis)
	{
		Lw.d(TAG, "This (below) is a simple repeating alarm without specific deadline");
		setAlarmMillis(context, System.currentTimeMillis() + repeatMillis, repeatMillis);
	}

	public void cancelAlarm(Context context)
	{
		Lw.d(TAG, "Cancelling alarm");
		Intent intent = new Intent(context, Alarm.class);
		PendingIntent sender = PendingIntent.getBroadcast(context, 0, intent, 0);

		alarmManager(context).cancel(sender);

		GlobalState.setCurrentRemindInterval(context,  0);
		GlobalState.setNextAlarmTime(context, 0);
	}

	private AlarmManager alarmManager(Context context)
	{
		return (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
	}
	
	private static boolean inRange(int value, int low, int high)
	{
		return (low <= value && value <= high);
	}
}
