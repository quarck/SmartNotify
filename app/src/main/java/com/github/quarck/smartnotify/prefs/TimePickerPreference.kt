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

package com.github.quarck.smartnotify.prefs

import com.github.quarck.smartnotify.Lw
import com.github.quarck.smartnotify.R

import android.content.Context
import android.content.res.TypedArray
import android.preference.DialogPreference
import android.provider.Settings
import android.util.AttributeSet
import android.view.View
import android.widget.TimePicker

class TimePickerPreference(context: Context, attrs: AttributeSet) : DialogPreference(context, attrs)
{
	internal var timeValue = 0

	internal var picker: TimePicker? = null

	internal var is24hr = true

	init
	{

		dialogLayoutResource = R.layout.dlg_pref_time_picker
		setPositiveButtonText(android.R.string.ok)
		setNegativeButtonText(android.R.string.cancel)
		dialogIcon = null

		val clockType = Settings.System.getString(context.contentResolver, Settings.System.TIME_12_24)

		if (clockType != null)
		{
			Lw.d("SMART_NOTIFY_LOG_TYPE_IS " + clockType)
			is24hr = (clockType.compareTo("24") == 0)
		}
		else
		{
			Lw.d("SMART_NOTIFY_LOG_TYPE_IS is unknown")
		}
	}

	override fun onBindDialogView(view: View)
	{
		super.onBindDialogView(view)

		picker = view.findViewById(R.id.timePickerTime) as TimePicker

		if (picker != null)
		{
			picker!!.setIs24HourView(is24hr)
			picker!!.currentHour = timeValue / 60
			picker!!.currentMinute = timeValue % 60
		}
	}

	override fun onDialogClosed(positiveResult: Boolean)
	{
		// When the user selects "OK", persist the new value
		if (positiveResult)
		{
			if (picker != null)
			{
				timeValue = picker!!.currentHour * 60 + picker!!.currentMinute
				persistInt(timeValue)
			}
		}
	}

	override fun onSetInitialValue(restorePersistedValue: Boolean, defaultValue: Any?)
	{
		if (restorePersistedValue)
		{
			// Restore existing state
			timeValue = this.getPersistedInt(0)
		}
		else
		{
			// Set default state from the XML attribute
			timeValue = defaultValue as Int
			persistInt(timeValue)
		}
	}

	override fun onGetDefaultValue(a: TypedArray, index: Int): Any
	{
		return a.getInteger(index, 0)
	}
}