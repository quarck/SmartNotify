package com.github.quarck.smartnotify;

import java.util.ArrayList;

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
	Messenger mService = null;

	boolean mIsBound;

	final Messenger mMessenger = new Messenger(new Handler(this));

	interface Callback
	{
		abstract void onNotificationList(String[] notificationList);
	}
	
	Callback callback;

	
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
        }
        
        return true;
    }

	private ServiceConnection mConnection = 
			new ServiceConnection() 
			{
			    public void onServiceConnected(ComponentName className, IBinder service) {
			        // This is called when the connection with the service has been
			        // established, giving us the service object we can use to
			        // interact with the service.  We are communicating with our
			        // service through an IDL interface, so get a client-side
			        // representation of that from the raw service object.
			        mService = new Messenger(service);
		//	        mCallbackText.setText("Attached.");
		
			        // We want to monitor the service for as long as we are
			        // connected to it.
			        //try 
			        //{
			          //  Message msg = Message.obtain(null, NotifyService.MSG_REGISTER_CLIENT);
			           // msg.replyTo = mMessenger;
	//		            mService.send(msg);
		
		//	            // Give it some value as an example.
			//            msg = Message.obtain(null, MessengerService.MSG_SET_VALUE, this.hashCode(), 0);
			 //           mService.send(msg);
			       // }
			        //catch (RemoteException e) 
			        //{
			            // In this case the service has crashed before we could even
			            // do anything with it; we can count on soon being
			            // disconnected (and then reconnected if it can be restarted)
			            // so there is no need to do anything here.
			        //}
		
		//	        // As part of the sample, tell the user what happened.
		//	        Toast.makeText(Binding.this, R.string.remote_service_connected,
		//	                Toast.LENGTH_SHORT).show();
			    }
		
			    public void onServiceDisconnected(ComponentName className) 
			    {
			        mService = null;
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
	        if (mService != null) 
	        {
	            try 
	            {
	                Message msg = Message.obtain(null, NotifyService.MSG_UNREGISTER_CLIENT);
	                msg.replyTo = mMessenger;
	                mService.send(msg);
	            }
	            catch (RemoteException e) 
	            {
	            }
	        }

	        ctx.unbindService(mConnection);
	        mIsBound = false;
	    }
	}

	public void getListNotifications() 
	{
	    if (mIsBound) 
	    {
	        if (mService != null) 
	        {
	            try 
	            {
	                Message msg = Message.obtain(null, NotifyService.MSG_LIST_NOTIFICATIONS);
	                msg.replyTo = mMessenger;
	                mService.send(msg);
	            }
	            catch (RemoteException e) 
	            {
	            } 
	        }
	    }
	}
}
