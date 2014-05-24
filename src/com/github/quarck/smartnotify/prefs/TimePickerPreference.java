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

package com.github.quarck.smartnotify.prefs;

import com.github.quarck.smartnotify.Lw;
import com.github.quarck.smartnotify.R;

import android.content.Context;
import android.content.res.TypedArray;
import android.preference.DialogPreference;
import android.provider.Settings;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TimePicker;

public class TimePickerPreference extends DialogPreference
{
	int timeValue = 0;
	
	TimePicker picker = null;

	String clockType = null;
	
	public TimePickerPreference(Context context, AttributeSet attrs)
	{
		super(context, attrs);

		setDialogLayoutResource(R.layout.dlg_pref_time_picker);
		setPositiveButtonText(android.R.string.ok);
		setNegativeButtonText(android.R.string.cancel);
		setDialogIcon(null);

		clockType = Settings.System.getString(context.getContentResolver(), Settings.System.TIME_12_24);
		
		Lw.d("SMART_NOTIFY_LOG_TYPE_IS " + clockType);
	}
	
	@Override
	protected void onBindDialogView (View view)
	{
		super.onBindDialogView(view);
		
		picker = (TimePicker)view.findViewById(R.id.timePickerTime);
		
		if (picker != null)
		{
			picker.setIs24HourView(clockType.compareTo("24") == 0);
			picker.setCurrentHour(timeValue / 60);
			picker.setCurrentMinute(timeValue % 60);
		}
	}

	protected void onDialogClosed(boolean positiveResult)
	{
		// When the user selects "OK", persist the new value
		if (positiveResult)
		{
			if (picker != null)
			{
				timeValue = picker.getCurrentHour() * 60 + picker.getCurrentMinute();
				persistInt(timeValue);
			}			
		}
	}

	@Override
	protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue)
	{
		if (restorePersistedValue)
		{
			// Restore existing state
			timeValue = this.getPersistedInt(0);
		}
		else
		{
			// Set default state from the XML attribute
			timeValue = (Integer) defaultValue;
			persistInt(timeValue);
		}
	}

	@Override
	protected Object onGetDefaultValue(TypedArray a, int index) 
	{
	    return a.getInteger(index, 0);
	}	
}