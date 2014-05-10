package com.github.quarck.smartnotify;

import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

public class NotificationReceiverService extends NotificationListenerService implements Handler.Callback
{
	public static final String TAG = "Service";

	public static final String configServiceExtra = "configService";

	public static final int MSG_CHECK_PERMISSIONS = 1;
	public static final int MSG_NO_PERMISSIONS = 2;
	public static final int MSG_LIST_NOTIFICATIONS = 3;
	public static final int MSG_RELOAD_SETTINGS = 4;

	private Alarm alarm = null;

	private final Messenger mMessenger = new Messenger(new Handler(this));

	private Settings settings = null;

	private PackageSettings pkgSettings = null;

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
			return mMessenger.getBinder();

		return super.onBind(intent);
	}

	private void update(StatusBarNotification addedOrRemoved)
	{
		Lw.d(TAG, "update");

		if (!settings.isServiceEnabled())
		{
			Lw.d(TAG, "Service is disabled, cancelling all the alarms and returning");
			alarm.cancelAlarm(this);
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
			boolean restartTimer = false;
			long nextAlarm = GlobalState.getNextAlarmTime(this);
			
			if (addedOrRemoved == null 
					|| pkgSettings.isPackageHandled(addedOrRemoved.getPackageName()))
			{
				Lw.d(TAG, "Either explicit restart request or handled package notification added / removed - restarting timer");
				restartTimer = true;
			}
			else if (System.currentTimeMillis() + 300 >= nextAlarm)
			{
				Lw.d(TAG, "Previous deadline has expired, re-starting timer");
				restartTimer = true;
			}

			if (!restartTimer)
			{
				Lw.d(TAG, "Re-Ensuring alarm with interval " + minReminderInterval + " seconds to run at " + nextAlarm);
				alarm.setAlarmMillis(this, nextAlarm, minReminderInterval * 1000);
			}
			else
			{
				Lw.d(TAG, "(Re)Setting alarm with interval " + minReminderInterval + " seconds");
				alarm.setAlarmMillis(this, minReminderInterval * 1000);	
			}
		}
		else
		{
			Lw.d(TAG, "Nothing to notify about, cancelling alarm, if any");
			alarm.cancelAlarm(this);
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
