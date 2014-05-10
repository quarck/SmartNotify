package com.github.quarck.smartnotify;

import java.util.List;

import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

public class NotificationReceiverService extends NotificationListenerService implements Handler.Callback
{
	public static final String TAG = "SmartNotify Service";

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

		switch (msg.what)
		{
		case MSG_CHECK_PERMISSIONS:
			ret = handleCheckPermissions(msg);
			break;

		case MSG_LIST_NOTIFICATIONS:
			ret = handleListNotifications(msg);
			break;
			
		case MSG_RELOAD_SETTINGS:
			Log.d(TAG, "Explicit request to reload config");
			update(null);
			break;
		}

		return ret;
	}

	private boolean handleCheckPermissions(Message msg)
	{
		Log.d(TAG, "handleCheckPermissions");
		try
		{
			getActiveNotifications();
		}
		catch (NullPointerException ex)
		{
			Log.e(TAG, "Got exception, have no permissions!");
			reply(msg, Message.obtain(null, MSG_NO_PERMISSIONS, 0, 0));
		}

		return true;
	}

	private boolean handleListNotifications(Message msg)
	{
		Log.d(TAG, "handleListNotifications");
		try
		{
			StatusBarNotification[] notifications = getActiveNotifications();
			String[] val = new String[notifications.length];

			int idx = 0;
			for (StatusBarNotification notification : notifications)
			{
				Log.d(TAG, "Sending info about notification " + notification);
				val[idx++] = notification.getPackageName();
			}

			reply(msg, Message.obtain(null, MSG_LIST_NOTIFICATIONS, 0, 0, val));
		}
		catch (NullPointerException ex)
		{
			Log.e(TAG, "Got exception, have no permissions!");
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
		Log.d(TAG, "onCreate()");

		Log.d(TAG, "Alarm");
		alarm = new Alarm();

		Log.d(TAG, "Settings");
		settings = new Settings(this);

		Log.d(TAG, "PackageSettings");
		pkgSettings = new PackageSettings(this);

		CallStateTracker.start(this);
	}

	@Override
	public void onDestroy()
	{
		Log.d(TAG, "onDestroy (??)");
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
		Log.d(TAG, "update");

		if (!settings.isServiceEnabled())
		{
			Log.d(TAG, "Service is disabled, cancelling all the alarms and returning");
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
			Log.e(TAG, "Got exception, have no permissions!");
		}

		int cntHandledNotifications = 0;

		int minReminderInterval = Integer.MAX_VALUE;

		if (notifications != null)
		{
			Log.d(TAG, "Total number of notifications currently active: " + notifications.length);

			for (StatusBarNotification notification : notifications)
			{
				// Log.d(TAG, "Checking notification" + notification);

				String packageName = notification.getPackageName();

				Log.d(TAG, "Package name is " + packageName);

				PackageSettings.Package pkg = pkgSettings.getPackage(packageName);

				if (pkg != null && pkg.isHandlingThis())
				{
					Log.d(TAG, "We are handling this!");

					++cntHandledNotifications;
					if (pkg.getRemindIntervalSeconds() < minReminderInterval)
					{
						minReminderInterval = pkg.getRemindIntervalSeconds();
						Log.d(TAG, "remind interval updated to " + minReminderInterval + " seconds");
					}
				}
				else
				{
					if (pkg == null)
						Log.d(TAG, "No settings for package " + packageName);
					else
						Log.d(TAG, "There are settings for packageName: " + pkg);
				}
			}

			Log.d(TAG, "Currently known packages: ");
			List<PackageSettings.Package> all = pkgSettings.getAllPackages();

			for (PackageSettings.Package pkg : all)
			{
				Log.d(TAG, ">> " + pkg);
			}
		}
		else
		{
			Log.e(TAG, "Can't get list of notifications. ");
		}

		if (cntHandledNotifications != 0)
		{
			// only (re)set alarm if added or removed notification is one of the
			// handled notifications.
			// otherwise alarm would be re-started each time the non-handled
			// notification appears or disappears
			// (which is incorrect)
			//
			// also we would force re-set alarm if addedOrUpdated is null, this means
			// it is not being called from the callback, but it is an explicit request 
			// from the UI

			if (addedOrRemoved == null 
					|| pkgSettings.isPackageHandled(addedOrRemoved.getPackageName()))
			{
				Log.d(TAG, "Firing alarm with interval " + minReminderInterval + " seconds");
				alarm.setAlarm(this, minReminderInterval);
			}
			else
			{
				Log.d(TAG, "Added/Removed notification is not handled, - not (Re)setting the alarm");
			}
		}
		else
		{
			Log.d(TAG, "Cancelling alarm, if any");
			alarm.cancelAlarm(this);
		}
	}

	@Override
	public void onNotificationPosted(StatusBarNotification arg0)
	{
		Log.d(TAG, "Notification posted: " + arg0);
		update(arg0);
	}

	@Override
	public void onNotificationRemoved(StatusBarNotification arg0)
	{
		Log.d(TAG, "Notification removed: " + arg0);
		update(arg0);
	}
}
