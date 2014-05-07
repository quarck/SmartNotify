package com.github.quarck.smartnotify;

import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

public class NotifyService 
	extends NotificationListenerService
	implements Handler.Callback
{
	public final static String configServiceExtra = "configService";
	
	private Alarm alarm = new Alarm(this);

	private final Messenger mMessenger = new Messenger(new Handler(this));
    
    public static final int MSG_CHECK_PERMISSIONS = 1;
    public static final int MSG_NO_PERMISSIONS = 2;
    public static final int MSG_LIST_NOTIFICATIONS = 3;

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
        }
		
        return ret;
    }

    private boolean handleCheckPermissions(Message msg) 
    {
    	try
    	{
    		getActiveNotifications();
    	}           		
    	catch(NullPointerException ex)
    	{
    		reply(msg, Message.obtain(null, MSG_NO_PERMISSIONS, 0, 0));
    	}
    	
    	return true;
	}

	private boolean handleListNotifications(Message msg) 
	{
    	StatusBarNotification[] notifications = getActiveNotifications ();
		String[] val = new String[notifications.length];
		
		int idx = 0;
		for(StatusBarNotification ntf: notifications)
			val[idx++] = ntf.getPackageName();
				
		reply(msg, Message.obtain(null, MSG_LIST_NOTIFICATIONS, 0, 0, val));
		
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
    }

    @Override
    public void onDestroy() 
    {
    	super.onDestroy();
    }

	@Override
	public IBinder onBind(Intent intent) 
	{
		if (intent.getBooleanExtra(configServiceExtra, false))
			return mMessenger.getBinder();
		
		return super.onBind(intent);
	}

	private void update()
	{
		StatusBarNotification[] notifications = this.getActiveNotifications ();
		
		if (notifications.length == 0 && alarm.IsSet())
		{
			alarm.CancelAlarm(this);
		}
		else if (notifications.length != 0 && !alarm.IsSet())
		{
			alarm.SetAlarm(this, 60);
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

	public boolean onAlarmReceived() 
	{
		// check for (isServiceEnabled()), 
		// if false - disable alarm and quit with false retval
		
		// TODO Auto-generated method stub
		
		return true; // returning false here would disable alarm for this occasion 
	}
}

