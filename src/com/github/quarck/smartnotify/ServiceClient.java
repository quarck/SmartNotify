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
        switch (msg.what) 
        {
        	case NotifyService.MSG_LIST_NOTIFICATIONS:
        		if (callback != null)
        			callback.onNotificationList((String[]) msg.obj);
        		break;
        	case NotifyService.MSG_NO_PERMISSIONS:
        		if (callback != null)
        			callback.onNoPermissions();
        		break;
        }
        
        return true;
    }

	private ServiceConnection mConnection = 
			new ServiceConnection() 
			{
			    public void onServiceConnected(ComponentName className, IBinder service) 
			    {
			        mService = new Messenger(service);
			        
			        if (callback != null)
			        	callback.onConnected();
			    }
		
			    public void onServiceDisconnected(ComponentName className) 
			    {
			        mService = null;
			        
			        if (callback != null)
			        	callback.onDisconnected();
			    }
			};
		
	public void bindService(Context ctx) 
	{
		Intent it = new Intent(ctx, NotifyService.class);		
		it.putExtra(NotifyService.configServiceExtra, true);
		
	    ctx.bindService(it, mConnection, Context.BIND_AUTO_CREATE);
	    mIsBound = true;
	}

	public void unbindService(Context ctx) 
	{
	    if (mIsBound) 
	    {
	        ctx.unbindService(mConnection);
	        mIsBound = false;
	    }
	}

	private void sendServiceReq(int code) 
	{
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
	            } 
	        }
	    }
	}

	
	public void getListNotifications() 
	{
		sendServiceReq(NotifyService.MSG_LIST_NOTIFICATIONS);
	}

	public void checkPermissions() 
	{
		sendServiceReq(NotifyService.MSG_CHECK_PERMISSIONS);
	}
}
