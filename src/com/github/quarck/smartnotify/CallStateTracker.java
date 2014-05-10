package com.github.quarck.smartnotify;

import android.content.Context;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;

public class CallStateTracker extends PhoneStateListener
{
	private static final String TAG = "CallStateTracker";
	
	private static CallStateTracker instance = null;

	private GlobalState global = null;
	
	@Override
	public void onCallStateChanged(int state, String incomingNumber)
	{
		boolean isOnCall = false;
		
		Lw.d(TAG, "onCallStateChanged, new state: " + state );
		
		switch (state)
		{
		case TelephonyManager.CALL_STATE_RINGING:
			isOnCall = true;
			Lw.d(TAG, "CALL_STATE_RINGING");
			break;
		case TelephonyManager.CALL_STATE_OFFHOOK:
			isOnCall = true;
			Lw.d(TAG, "CALL_STATE_OFFHOOK");
			break;
			
		case TelephonyManager.CALL_STATE_IDLE:
			isOnCall = false;
			Lw.d(TAG, "CALL_STATE_IDLE");
			break;
		}

		if (incomingNumber != null)
			Lw.d(TAG, "incomingNumber = " + incomingNumber);

		if (global != null)
			global.setIsOnCall(isOnCall);
		else
			Lw.e(TAG, "Can't access global state");

	}
	
	private void register(Context ctx)
	{
		Lw.d(TAG, "Registering listener");
		
		TelephonyManager tm = (TelephonyManager) ctx.getSystemService(Context.TELEPHONY_SERVICE);
		tm.listen(this, PhoneStateListener.LISTEN_CALL_STATE);
		
		global = GlobalState.instance(ctx);
	}

	@SuppressWarnings("unused")
	private void unregister(Context ctx)
	{
		Lw.d(TAG, "Unregistering listener");
		
		TelephonyManager tm = (TelephonyManager) ctx.getSystemService(Context.TELEPHONY_SERVICE);
		tm.listen(this, PhoneStateListener.LISTEN_NONE);
	}
	
	public static void start(Context ctx)
	{
		Lw.d(TAG, "start()");
		synchronized(CallStateTracker.class)
		{
			if (instance == null)
				instance = new CallStateTracker();
			
			instance.register(ctx);			
		}
	}
}