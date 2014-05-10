package com.github.quarck.smartnotify;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;

public class ServiceClient implements Handler.Callback
{
	private static final String TAG = "ServiceClient";
	
	private Messenger mService = null;

	private boolean mIsBound;

	private final Messenger mMessenger = new Messenger(new Handler(this));

	interface Callback
	{
		abstract void onNoPermissions();

		abstract void onConnected();

		abstract void onDisconnected();

		abstract void onNotificationList(String[] notificationList);
	}

	private Callback callback;

	public ServiceClient(Callback cb)
	{
		callback = cb;
	}

	@Override
	public boolean handleMessage(Message msg)
	{
		Lw.d(TAG, "handleMessage: " + msg.what);
		
		switch (msg.what)
		{
		case NotificationReceiverService.MSG_LIST_NOTIFICATIONS:
			if (callback != null)
				callback.onNotificationList((String[]) msg.obj);
			break;
		case NotificationReceiverService.MSG_NO_PERMISSIONS:
			if (callback != null)
				callback.onNoPermissions();
			break;
		}
		
		if (callback == null)
			Lw.e(TAG, "No callback attached");

		return true;
	}

	private ServiceConnection mConnection = new ServiceConnection()
	{
		public void onServiceConnected(ComponentName className, IBinder service)
		{
			Lw.d(TAG, "Got connection to the service");
			
			mService = new Messenger(service);

			if (callback != null)
				callback.onConnected();
		}

		public void onServiceDisconnected(ComponentName className)
		{
			Lw.d(TAG, "Service disconnected");
			
			mService = null;

			if (callback != null)
				callback.onDisconnected();
		}
	};

	public void bindService(Context ctx)
	{
		Lw.d(TAG, "Binding service");
		
		Intent it = new Intent(ctx, NotificationReceiverService.class);
		it.putExtra(NotificationReceiverService.configServiceExtra, true);

		ctx.bindService(it, mConnection, Context.BIND_AUTO_CREATE);
		mIsBound = true;
	}

	public void unbindService(Context ctx)
	{
		if (mIsBound)
		{
			Lw.d(TAG, "unbinding service");
			ctx.unbindService(mConnection);
			mIsBound = false;
		}
		else
		{
			Lw.e(TAG, "unbind request, but service is not bound!");
		}
	}

	private void sendServiceReq(int code)
	{
		Lw.d(TAG, "Sending request " + code + " to the service" );
		if (mIsBound)
		{
			if (mService != null)
			{
				try
				{
					Message msg = Message.obtain(null, code);
					msg.replyTo = mMessenger;
					mService.send(msg);
				}
				catch (RemoteException e)
				{
					Lw.e(TAG, "Failed to send req - got exception " + e);
				}
			}
		}
		else
		{
			Lw.e(TAG, "Failed to send req - service is not bound!");
		}
	}

	public void getListNotifications()
	{
		sendServiceReq(NotificationReceiverService.MSG_LIST_NOTIFICATIONS);
	}

	public void checkPermissions()
	{
		sendServiceReq(NotificationReceiverService.MSG_CHECK_PERMISSIONS);
	}
	
	public void forceReloadConfig()
	{
		sendServiceReq(NotificationReceiverService.MSG_RELOAD_SETTINGS);
	}
}
