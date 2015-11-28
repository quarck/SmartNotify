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