package com.github.quarck.smartnotify;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Vibrator;

public class Alarm extends BroadcastReceiver 
{   
	private NotifyService service;
	
	private int vibrationLengthMillis = 500;
	private boolean isSet = false;

	public Alarm(NotifyService svc, int _vibrationLenghtMillis)
	{
		super();
		service = svc;
		vibrationLengthMillis = _vibrationLenghtMillis;
	}

	public Alarm(NotifyService svc)
	{
		this(svc, 500);
	}
	
    @Override
    public void onReceive(Context context, Intent intent) // alarm fired 
    {
    	if (service.onAlarmReceived())
    	{
		    Vibrator v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
		    v.vibrate(vibrationLengthMillis);
    	}
    }

    public void SetAlarm(Context context, int timeoutSec)
    {
		Intent intent = new Intent(context, Alarm.class);
		PendingIntent pendIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
	
		alarmManager(context).setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), 1000 * timeoutSec, pendIntent); 
    
		isSet = true;
    }

    public void CancelAlarm(Context context)
    {
	    Intent intent = new Intent(context, Alarm.class);
	    PendingIntent sender = PendingIntent.getBroadcast(context, 0, intent, 0);
	    
	    alarmManager(context).cancel(sender);
	    
	    isSet = false;
    }

    private AlarmManager alarmManager(Context context)
    {
    	return (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
    }
    
	public boolean IsSet() 
	{
		return isSet;
	}
}