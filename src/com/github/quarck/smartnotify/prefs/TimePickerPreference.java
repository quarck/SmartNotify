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