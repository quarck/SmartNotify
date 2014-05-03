package com.github.quarck.smartnotify;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.os.Vibrator;
import android.widget.Toast;

public class Alarm extends BroadcastReceiver 
{   
	int vibrationLengthMillis = 500;
	
	public Alarm(int _vibrationLenghtMillis)
	{
		super();
		vibrationLengthMillis = _vibrationLenghtMillis;
	}

	public Alarm()
	{
		this(500);
	}
	
    @Override
    public void onReceive(Context context, Intent intent) 
    {   
	    Vibrator v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
	    v.vibrate(500);
    }

    public void SetAlarm(Context context, int timeoutSec)
    {
		AlarmManager am=(AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
		Intent i = new Intent(context, Alarm.class);
		PendingIntent pi = PendingIntent.getBroadcast(context, 0, i, PendingIntent.FLAG_UPDATE_CURRENT);
		am.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), 1000 * timeoutSec, pi); // Millisec * Second 
    }

    public void CancelAlarm(Context context)
    {
	    Intent intent = new Intent(context, Alarm.class);
	    PendingIntent sender = PendingIntent.getBroadcast(context, 0, intent, 0);
	    AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
	    alarmManager.cancel(sender);
    }
}