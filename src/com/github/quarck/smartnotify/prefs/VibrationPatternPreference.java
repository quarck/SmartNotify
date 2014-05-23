package com.github.quarck.smartnotify.prefs;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.os.Vibrator;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.EditText;

import com.github.quarck.smartnotify.R;


public class VibrationPatternPreference extends DialogPreference
{
	String patternValue;
	
	EditText edit = null;

	Vibrator vibrator = null; 
	
	Context context = null;
	
	public VibrationPatternPreference(Context ctx, AttributeSet attrs)
	{
		super(ctx, attrs);
		
		context = ctx;
		
		setDialogLayoutResource(R.layout.dlg_vibration_pattern);
		setPositiveButtonText(android.R.string.ok);
		setNegativeButtonText(android.R.string.cancel); 
		setDialogIcon(null);
		
		vibrator  = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
	}
	
	@Override
	protected void onBindDialogView (View view)
	{
		super.onBindDialogView(view);
		
		edit = (EditText)view.findViewById(R.id.editTextVibrationPattern);
		edit.setText(patternValue);
	}

	protected void onDialogClosed(boolean positiveResult)
	{
		// When the user selects "OK", persist the new value
		if (positiveResult)
		{
			if (edit != null)
			{
				String value = edit.getText().toString();

				long[] pattern = com.github.quarck.smartnotify.Settings.parseVibrationPattern(value);
				
				if (pattern != null)
				{
					patternValue = value;
					persistString(patternValue);
					vibrator.vibrate(pattern, -1);
				}
				else
				{
					onInvalidPattern();
				}
			}			
		}
	}

	public void onInvalidPattern()
	{
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setMessage("Cannot parse pattern").setCancelable(false)
			.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener()
			{
				public void onClick(DialogInterface dialog, int id)
				{
				}
			});
		
		builder.create().show();
	}
	
	@Override
	protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue)
	{
		if (restorePersistedValue)
		{
			// Restore existing state
			patternValue = this.getPersistedString(
					com.github.quarck.smartnotify.Settings.getDefaultVibrationPatternStr()
				);
		}
		else
		{
			// Set default state from the XML attribute
			patternValue = (String) defaultValue;
			persistString(patternValue);
		}
	}

	@Override
	protected Object onGetDefaultValue(TypedArray a, int index) 
	{
	    return a.getString(index);
	}
}
