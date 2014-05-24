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

		abstract void onNotificationList(String[] notificationList);
		
		abstract void onRecetNotificationsList(String[] recentNotifications);
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
		case NotificationReceiverService.MSG_LIST_RECENT_NOTIFICATIONS:
			if (callback != null)
				callback.onRecetNotificationsList((String[]) msg.obj);
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

//			if (callback != null)
//				callback.onConnected();
		}

		public void onServiceDisconnected(ComponentName className)
		{
			Lw.d(TAG, "Service disconnected");
			
			mService = null;

//			if (callback != null)
//				callback.onDisconnected();
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

	public void getListRecentNotifications()
	{
		sendServiceReq(NotificationReceiverService.MSG_LIST_RECENT_NOTIFICATIONS);
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
