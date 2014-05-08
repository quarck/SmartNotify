package com.github.quarck.smartnotify;

import java.util.Calendar;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Vibrator;
import android.util.Log;

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
		Log.d(TAG, "Alarm received");
	
		Settings settings = new Settings(context);
		
		if (settings.isServiceEnabled())
		{
			boolean fireReminder = true;
			
			if (settings.hasSilencePeriod())
			{
				Calendar cal = Calendar.getInstance();
				int hour = cal.get(Calendar.HOUR_OF_DAY);
				int minute = cal.get(Calendar.MINUTE);
				int currentTm = hour * 60 + minute;
				
				int silenceFrom = settings.getSilenceFrom();
				int silenceTo = settings.getSilenceTo();
				
				if (silenceTo < silenceFrom)
					silenceTo += 24 * 60;
				
				if (silenceFrom <= currentTm && currentTm <= silenceTo)
				{
					Log.d(TAG, "Service is enabled, but we are in silent zone, not alarming! CurrentTM: " + currentTm + ", silence from " + silenceFrom + " to " + silenceTo);
					fireReminder = false;
				}
				else
				{
					Log.d(TAG, "Service is enabled and not in the silent zone, vibrating");
				}
			}
			else
			{
				Log.d(TAG, "Service is enabled, vibrating");
			}

			if (fireReminder)
			{
				Vibrator v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);				// TODO: XXX: WARNING: CHECK FOR SILENT HOURS!!!
				v.vibrate(settings.getVibrationPattern(), -1);
			}
		}
		else
		{
			Log.d(TAG, "Service is now got disabled, cancelling alarm");
			CancelAlarm(context);
		}
    }

    public void SetAlarm(Context context, int timeoutSec)
    {
    	// Cancel any pending alarms, if any
    	CancelAlarm(context);
    	
		Intent intent = new Intent(context, Alarm.class);
		PendingIntent pendIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
	
		alarmManager(context).setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), 1000 * timeoutSec, pendIntent); 
    }

    public void CancelAlarm(Context context)
    {
	    Intent intent = new Intent(context, Alarm.class);
	    PendingIntent sender = PendingIntent.getBroadcast(context, 0, intent, 0);
	    
	    alarmManager(context).cancel(sender);
    }

    private AlarmManager alarmManager(Context context)
    {
    	return (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
    }
}