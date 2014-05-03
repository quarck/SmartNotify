package com.github.quarck.smartnotify;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.widget.Toast;

public class NotifyService extends NotificationListenerService 
{
	boolean alarmIsActive = false;
	Alarm alarm = new Alarm();

	@Override
	public IBinder onBind(Intent intent) 
	{
		return super.onBind(intent);
	}

	private void update()
	{
		StatusBarNotification[] notifications = this.getActiveNotifications ();
		
		if (notifications.length == 0 && alarmIsActive)
		{
			alarmIsActive = false;
			alarm.CancelAlarm(this);
		}
		else if (notifications.length != 0 && !alarmIsActive)
		{
			alarmIsActive = true;
			alarm.SetAlarm(this, 10*60);
		}
	}
	
	@Override
	public void onNotificationPosted(StatusBarNotification arg0) 
	{
		update();
	}

	@Override
	public void onNotificationRemoved(StatusBarNotification arg0) 
	{
		update();
	}
}

