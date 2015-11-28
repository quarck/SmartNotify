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
		builder.setMessage(context.getString(R.string.error_cannot_parse)).setCancelable(false)
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
