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

import java.util.ArrayList;
import java.util.HashMap;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.widget.RemoteViews;
import android.widget.RemoteViews.RemoteView;

public class NotificationReceiverService extends NotificationListenerService implements Handler.Callback
{
	public static final String TAG = "Service";

	public static final String configServiceExtra = "configService";

	public static final int MSG_CHECK_PERMISSIONS = 1;
	public static final int MSG_NO_PERMISSIONS = 2;
	public static final int MSG_LIST_NOTIFICATIONS = 3;
	public static final int MSG_RELOAD_SETTINGS = 4;
	public static final int MSG_LIST_RECENT_NOTIFICATIONS = 5;
	public static final int MSG_TOGGLE_MUTE = 6;
	
	private Alarm alarm = null;

	private final Messenger messenger = new Messenger(new Handler(this));

	private Settings settings = null;

	private PackageSettings pkgSettings = null;
	
	private static HashMap<String,Long> recentNotifications = new HashMap<String,Long>();
	
	@Override
	public boolean handleMessage(Message msg)
	{
		boolean ret = true;

		Lw.d(TAG, "handleMessage, msg=" + msg.what);
		
		switch (msg.what)
		{
		case MSG_CHECK_PERMISSIONS:
			ret = handleCheckPermissions(msg);
			break;

		case MSG_LIST_NOTIFICATIONS:
			ret = handleListNotifications(msg);
			break;
			
		case MSG_RELOAD_SETTINGS:
			Lw.d(TAG, "Explicit request to reload config");
			update(null);
			break;
			
		case MSG_LIST_RECENT_NOTIFICATIONS:
			Lw.d(TAG, "Req for recent notifications");
			sendRecent(msg);
			break;
			
		case MSG_TOGGLE_MUTE:
			Lw.d(TAG, "Toggling mute");			
			GlobalState.setIsMuted(this, ! GlobalState.getIsMuted(this));
			update(null);
			break;
		}

		return ret;
	}

	private boolean handleCheckPermissions(Message msg)
	{
		Lw.d(TAG, "handleCheckPermissions");
		try
		{
			getActiveNotifications();
		}
		catch (NullPointerException ex)
		{
			Lw.e(TAG, "Got exception, have no permissions!");
			reply(msg, Message.obtain(null, MSG_NO_PERMISSIONS, 0, 0));
		}

		return true;
	}

	private boolean handleListNotifications(Message msg)
	{
		Lw.d(TAG, "handleListNotifications");
		try
		{
			StatusBarNotification[] notifications = getActiveNotifications();
			String[] val = new String[notifications.length];

			int idx = 0;
			for (StatusBarNotification notification : notifications)
			{
				Lw.d(TAG, "Sending info about notification " + notification);
				val[idx++] = notification.getPackageName();
			}

			reply(msg, Message.obtain(null, MSG_LIST_NOTIFICATIONS, 0, 0, val));
		}
		catch (NullPointerException ex)
		{
			Lw.e(TAG, "Got exception, have no permissions!");
			reply(msg, Message.obtain(null, MSG_NO_PERMISSIONS, 0, 0));
		}
		return true;
	}

	private static void cleanupRecentNotifications()
	{
		long timeNow = System.currentTimeMillis();
		
		ArrayList<String> listToCleanup = new ArrayList<String>();
		
		for (HashMap.Entry<String, Long> entry : recentNotifications.entrySet()) 
		{
		    String key = entry.getKey();
		    Long value = entry.getValue();
		    
		    if (timeNow - value.longValue() > 1000 * 3600 * 24 ) // older than 1 day
		    {
		    	listToCleanup.add(key);
		    	recentNotifications.remove(key);
		    }
		}
		
		for (String key: listToCleanup)
			recentNotifications.remove(key);
	}
	
	public static String[] getRecentNotifications()
	{
		Lw.d(TAG, "getRecentNotifications");

		String[] notifications = null;
				
		synchronized (NotificationReceiverService.class)
		{
			cleanupRecentNotifications();

			notifications = new String[recentNotifications.size()];
			
			int idx = 0;
			for (String key : recentNotifications.keySet()) 
			{
			    notifications[idx++] = key;
			}
		}
		
		return notifications;
	}

	private boolean sendRecent(Message msg)
	{
		Lw.d(TAG, "sendRecent");

		String[] notifications = getRecentNotifications();

		if (notifications != null)
			reply(msg, Message.obtain(null, MSG_LIST_RECENT_NOTIFICATIONS, 0, 0, notifications));
		
		return true;
	}

	private void reply(Message msgIn, Message msgOut)
	{
		try
		{
			msgIn.replyTo.send(msgOut);
		}
		catch (RemoteException e)
		{
			e.printStackTrace();
		}
	}

	@Override
	public void onCreate()
	{
		super.onCreate();
		Lw.d(TAG, "onCreate()");

		Lw.d(TAG, "AlarmReceiver");
		alarm = new Alarm();

		Lw.d(TAG, "Settings");
		settings = new Settings(this);

		Lw.d(TAG, "PackageSettings");
		pkgSettings = new PackageSettings(this);

		CallStateTracker.start(this);
	}

	@Override
	public void onDestroy()
	{
		Lw.d(TAG, "onDestroy (??)");
		super.onDestroy();
	}

	@Override
	public IBinder onBind(Intent intent)
	{
		if (intent.getBooleanExtra(configServiceExtra, false))
			return messenger.getBinder();

		return super.onBind(intent);
	}
	
	private void update(StatusBarNotification addedOrRemoved)
	{
		Lw.d(TAG, "update");

		if (addedOrRemoved != null)
		{
			synchronized (NotificationReceiverService.class)
			{
				recentNotifications.put(
						addedOrRemoved.getPackageName(), 
						System.currentTimeMillis()
					);
				
				if (recentNotifications.size() > 100)
					cleanupRecentNotifications();
			}
		}
		
		if (!settings.isServiceEnabled())
		{
			Lw.d(TAG, "Service is disabled, cancelling all the alarms and returning");
			alarm.cancelAlarm(this);
			if ( GlobalState.getLastCountHandledNotifications(this) != 0 )
			{
				OngoingNotificationManager.cancelOngoingNotification(this);
				GlobalState.setLastCountHandledNotifications(this, 0);
			}
			return;
		}

		StatusBarNotification[] notifications = null;

		try
		{
			notifications = getActiveNotifications();
		}
		catch (NullPointerException ex)
		{
			Lw.e(TAG, "Got exception while obtaining list of notifications, have no permissions!");
		}

		int cntHandledNotifications = 0;

		int minReminderInterval = Integer.MAX_VALUE;

		if (notifications != null)
		{
			Lw.d(TAG, "Total number of notifications currently active: " + notifications.length);

			for (StatusBarNotification notification : notifications)
			{
				Lw.d(TAG, "Checking notification" + notification);
				String packageName = notification.getPackageName();

				if (packageName == "com.github.quarck.smartnotify")
				{
					Lw.d(TAG, "That's ours, ignoring");
					continue;
				}
				
				Lw.d(TAG, "Package name is " + packageName);

				PackageSettings.Package pkg = pkgSettings.getPackage(packageName);

				if (pkg != null && pkg.isHandlingThis())
				{
					Lw.d(TAG, "We are handling this!");

					++cntHandledNotifications;
					if (pkg.getRemindIntervalSeconds() < minReminderInterval)
					{
						minReminderInterval = pkg.getRemindIntervalSeconds();
						Lw.d(TAG, "remind interval updated to " + minReminderInterval + " seconds");
					}
				}
				else
				{
					if (pkg == null)
						Lw.d(TAG, "No settings for package " + packageName);
					else
						Lw.d(TAG, "There are settings for packageName: " + pkg + ", but it is not currently handled");
				}
			}

			Lw.d(TAG, "Currently known packages: ");
			for (PackageSettings.Package pkg : pkgSettings.getAllPackages())
			{
				Lw.d(TAG, ">> " + pkg);
			}
		}
		else
		{
			Lw.e(TAG, "Can't get list of notifications. WE HAVE NO PERMISSION!! ");
		}

		if (cntHandledNotifications != 0)
		{
			Lw.d(TAG, "(Re)Setting alarm with interval " + minReminderInterval + " seconds");
			alarm.setAlarmMillis(this,  minReminderInterval * 1000);
			
			if ( ( GlobalState.getLastCountHandledNotifications(this) != cntHandledNotifications ) && 
					settings.isOngoingNotificationEnabled()) 
			{
				GlobalState.setLastCountHandledNotifications(this, cntHandledNotifications);
				OngoingNotificationManager.showUpdateOngoingNotification(this);
			}
		}
		else if (GlobalState.getIsMuted(this)) // if we are muted - always show the notification also
		{
			Lw.d(TAG, "Nothing to notify about, cancelling alarm, if any, but setting notification since we are muted");
			alarm.cancelAlarm(this);			

			if ( ( GlobalState.getLastCountHandledNotifications(this) != cntHandledNotifications ) && 
					settings.isOngoingNotificationEnabled()) 
			{
				GlobalState.setLastCountHandledNotifications(this, 0);
				OngoingNotificationManager.showUpdateOngoingNotification(this);
			}
		}
		else
		{
			Lw.d(TAG, "Nothing to notify about, cancelling alarm, if any");
			alarm.cancelAlarm(this);

			if ( GlobalState.getLastCountHandledNotifications(this) != 0)
			{
				GlobalState.setLastCountHandledNotifications(this, 0);
				OngoingNotificationManager.cancelOngoingNotification(this);
			}
		}
	}

	@Override
	public void onNotificationPosted(StatusBarNotification arg0)
	{
		Lw.d(TAG, "Notification posted: " + arg0);
		update(arg0);
	}

	@Override
	public void onNotificationRemoved(StatusBarNotification arg0)
	{
		Lw.d(TAG, "Notification removed: " + arg0);
		update(arg0);
	}
}
