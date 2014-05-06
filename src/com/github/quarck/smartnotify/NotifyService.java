package com.github.quarck.smartnotify;

import java.util.ArrayList;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.widget.Toast;

public class NotifyService 
	extends NotificationListenerService
	implements Handler.Callback
{
	public final static String configServiceExtra = "configService";
	
	boolean alarmIsActive = false;
	Alarm alarm = new Alarm();

	ArrayList<Messenger> mClients = new ArrayList<Messenger>();
    
    static final int MSG_REGISTER_CLIENT = 1;
    static final int MSG_UNREGISTER_CLIENT = 2;
    static final int MSG_SET_VALUE = 3;
    static final int MSG_LIST_NOTIFICATIONS = 4;

    @Override
    public boolean handleMessage(Message msg) 
    {
    	boolean ret = true;
    	
        switch (msg.what) 
        {
            case MSG_REGISTER_CLIENT:
                mClients.add(msg.replyTo);
                break;
            case MSG_UNREGISTER_CLIENT:
                mClients.remove(msg.replyTo);
                break;
            case MSG_LIST_NOTIFICATIONS: 
 
            	StatusBarNotification[] notifications = getActiveNotifications ();
        		String[] val = new String[notifications.length];
        		
        		int idx = 0;
        		for(StatusBarNotification ntf: notifications)
        			val[idx++] = ntf.getPackageName();
        		
				try 
				{
					msg.replyTo.send(Message.obtain(null, MSG_LIST_NOTIFICATIONS, 0, 0, val));
				} 
				catch (RemoteException e) 
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				ret = true;
        		
            	break;
            case MSG_SET_VALUE:
//                   mValue = msg.arg1;
                for (int i=mClients.size()-1; i>=0; i--) 
                {
//                    try 
 //                   {
//                         mClients.get(i).send(Message.obtain(null, MSG_SET_VALUE, mValue, 0));
   //                 } 
     //               catch (RemoteException e) 
       //             {
         //               mClients.remove(i);
           //         }
                }
                break;
        }
		return ret;
    }

    final Messenger mMessenger = new Messenger(new Handler(this));

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
		
		if (notifications.length == 0 && alarmIsActive)
		{
			alarmIsActive = false;
			alarm.CancelAlarm(this);
		}
		else if (notifications.length != 0 && !alarmIsActive)
		{
			alarmIsActive = true;
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
}

