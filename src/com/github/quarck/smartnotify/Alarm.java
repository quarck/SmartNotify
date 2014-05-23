package com.github.quarck.smartnotify;

import java.util.Calendar;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
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

			if (SilentPeriodManager.isEnabled(settings))
			{
				long tmUntilEnd = SilentPeriodManager.getSilentUntil(settings);
				
				if (tmUntilEnd != 0)
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
				checkPhoneSilentAndFire(context, settings);
			}
		}
		else
		{
			Lw.d(TAG, "Service is now got disabled, cancelling alarm");
			cancelAlarm(context);
		}
	}
	
	private void checkPhoneSilentAndFire(Context ctx, Settings settings)
	{
		boolean mayFireVibration = false;
		boolean mayFireSound = false;
		
		AudioManager am = (AudioManager)ctx.getSystemService(Context.AUDIO_SERVICE);

		switch (am.getRingerMode()) 
		{
		    case AudioManager.RINGER_MODE_SILENT:
				Lw.d(TAG, "checkPhoneSilentAndFire: AudioManager.RINGER_MODE_SILENT");
		        break;
		    case AudioManager.RINGER_MODE_VIBRATE:
				Lw.d(TAG, "checkPhoneSilentAndFire: AudioManager.RINGER_MODE_VIBRATE");
				mayFireVibration = true;
		        break;
		    case AudioManager.RINGER_MODE_NORMAL:
				Lw.d(TAG, "checkPhoneSilentAndFire: AudioManager.RINGER_MODE_NORMAL");
				mayFireVibration = mayFireSound = true;
		        break;
		}
		
		if (mayFireVibration)
		{
			Lw.d(TAG, "Firing vibro-alarm finally");
			Vibrator v = (Vibrator) ctx.getSystemService(Context.VIBRATOR_SERVICE);
			long[] pattern = settings.getVibrationPattern();
			v.vibrate(pattern, -1);
		}

		if (mayFireSound)
		{
			Lw.d(TAG, "Playing sound notification, if URI is not null");
			
			try
			{
				Uri notificationUri = settings.getRingtoneURI();

				MediaPlayer mediaPlayer = new MediaPlayer();

				mediaPlayer.setDataSource(ctx, notificationUri);
				mediaPlayer.setAudioStreamType(AudioManager.STREAM_NOTIFICATION);
				mediaPlayer.prepare();
				mediaPlayer.setOnCompletionListener(
					new OnCompletionListener() 
					{
						@Override
						public void onCompletion(MediaPlayer mp)
						{
							mp.release();
						}
					});
				mediaPlayer.start();
			}
			catch (Exception e)
			{
				Lw.e(TAG, "Exception while playing notification");
				e.printStackTrace();
			}
		}
	}

	public void setAlarmMillis(Context context, long whenMillis, int repeatMillis)
	{
		cancelAlarm(context); // cancel previous alarm, if any, so we would not have two alarms
		
		Lw.d(TAG, "Setting alarm for " + whenMillis + ", repeat " + repeatMillis);
		
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
	
}
