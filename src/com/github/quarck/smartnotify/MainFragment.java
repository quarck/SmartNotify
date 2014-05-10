package com.github.quarck.smartnotify;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

@SuppressLint("UseValueOf")
public class MainFragment extends Fragment implements ServiceClient.Callback
{
	protected static final String TAG = "MainFragment";
	private ServiceClient serviceClient = null;

	private CheckBox cbEnableService = null;
	private CheckBox cbHandleCal = null;
	private TextView editCalInterval = null;
	private CheckBox cbHandleSMS = null;
	private TextView editSMSInterval = null;
	private CheckBox cbHandlePhone = null;
	private TextView editPhoneInterval = null;
	private CheckBox cbHandleGmail = null;
	private TextView editGmailInterval = null;

	public void checkService()
	{

		// final PackageManager pm =
		// getActivity().getPackageManager();
		// get a list of installed apps.
		// List<ApplicationInfo> packages =
		// pm.getInstalledApplications(PackageManager.GET_META_DATA);

		// for (ApplicationInfo packageInfo : packages)
		// {
		// Log.d(TAG, "Installed package :" +
		// packageInfo.packageName);
		// Log.d(TAG, "Source dir : " +
		// packageInfo.sourceDir);
		// Log.d(TAG, "Launch Activity :" +
		// pm.getLaunchIntentForPackage(packageInfo.packageName));
		// }

		serviceClient.getListNotifications();
	}

	@Override
	public void onNoPermissions()
	{
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setMessage(R.string.application_has_no_access)
				.setPositiveButton(R.string.open_settings, new DialogInterface.OnClickListener()
				{
					public void onClick(DialogInterface dialog, int id)
					{
						Intent intent = new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS");
						startActivity(intent);
					}
				}).setNegativeButton(R.string.cancel_quit, new DialogInterface.OnClickListener()
				{
					public void onClick(DialogInterface dialog, int id)
					{
						getActivity().finish();
					}
				});
		// Create the AlertDialog object and return it
		builder.create().show();
	}

	private void set(PackageSettings s, String packageName, int delay, boolean enabled)
	{
		PackageSettings.Package pkg = s.getPackage(packageName);

		if (pkg == null)
		{
			s.addPackage(s.new Package(packageName, enabled, delay));
		}
		else
		{
			Log.d(TAG, "Updating to send reminders every minute");
			pkg.setRemindIntervalSeconds(delay);
			pkg.setHandlingThis(enabled);
			s.updatePackage(pkg);
		}
	}

	private boolean is(PackageSettings s, String packageName)
	{
		PackageSettings.Package pkg = s.getPackage(packageName);
		return pkg != null ? pkg.isHandlingThis() : false;
	}

	private int interval(PackageSettings s, String packageName)
	{
		PackageSettings.Package pkg = s.getPackage(packageName);
		return pkg != null ? pkg.getRemindIntervalSeconds() : 60 * 5;
	}

	private void saveSettings(Settings settings, PackageSettings pkgSettings)
	{
		settings.setServiceEnabled(cbEnableService.isChecked());

		try
		{
			set(pkgSettings, "com.google.android.calendar",
					Integer.parseInt(editCalInterval.getText().toString()) * 60, cbHandleCal.isChecked());
			set(pkgSettings, "com.android.mms", Integer.parseInt(editSMSInterval.getText().toString()) * 60,
					cbHandleSMS.isChecked());
			set(pkgSettings, "com.android.phone", Integer.parseInt(editPhoneInterval.getText().toString()) * 60,
					cbHandlePhone.isChecked());
			set(pkgSettings, "com.google.android.gm", Integer.parseInt(editGmailInterval.getText().toString()) * 60,
					cbHandleGmail.isChecked());

			Log.d(TAG, "Currently known packages: ");

			for (PackageSettings.Package p : pkgSettings.getAllPackages())
			{
				Log.d(TAG, ">> " + p);
			}
		}
		catch (Exception ex)
		{
			AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
			builder.setMessage("Invalid number").setPositiveButton("OK", new DialogInterface.OnClickListener()
			{
				public void onClick(DialogInterface dialog, int id)
				{
				}
			});
			builder.create().show();
		}
		
		if (serviceClient != null)
			serviceClient.forceReloadConfig();
	}

	private void loadSettings(Settings settings, PackageSettings pkgSettings)
	{
		cbEnableService.setChecked(settings.isServiceEnabled());

		cbHandleCal.setChecked(is(pkgSettings, "com.google.android.calendar"));
		editCalInterval.setText((new Integer(interval(pkgSettings, "com.google.android.calendar") / 60)).toString());
		cbHandleSMS.setChecked(is(pkgSettings, "com.android.mms"));
		editSMSInterval.setText((new Integer(interval(pkgSettings, "com.android.mms") / 60)).toString());
		cbHandlePhone.setChecked(is(pkgSettings, "com.android.phone"));
		editPhoneInterval.setText((new Integer(interval(pkgSettings, "com.android.phone") / 60)).toString());
		cbHandleGmail.setChecked(is(pkgSettings, "com.google.android.gm"));
		editGmailInterval.setText((new Integer(interval(pkgSettings, "com.google.android.gm") / 60)).toString());
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		Settings settings = new Settings(getActivity());
		PackageSettings pkgSettings = new PackageSettings(getActivity());

		View rootView = inflater.inflate(R.layout.fragment_main, container, false);

		cbEnableService = (CheckBox) rootView.findViewById(R.id.checkBoxEnableService);
		cbHandleCal = (CheckBox) rootView.findViewById(R.id.checkBoxHandleCalendar);
		editCalInterval = (TextView) rootView.findViewById(R.id.editTextCalendarRemindInterval);
		cbHandleSMS = (CheckBox) rootView.findViewById(R.id.checkBoxHandleSMS);
		editSMSInterval = (TextView) rootView.findViewById(R.id.editTextSMSRemindInterval);
		cbHandlePhone = (CheckBox) rootView.findViewById(R.id.checkBoxHandlePhone);
		editPhoneInterval = (TextView) rootView.findViewById(R.id.editTextPhoneRemindInterval);
		cbHandleGmail = (CheckBox) rootView.findViewById(R.id.checkBoxHandleGmail);
		editGmailInterval = (TextView) rootView.findViewById(R.id.editTextGmailRemindInterval);

		loadSettings(settings, pkgSettings);

		OnClickListener saveSettingsOnClickListener = new OnClickListener()
		{
			public void onClick(View arg0)
			{
				PackageSettings pkgSettings = new PackageSettings(getActivity());
				Settings settings = new Settings(getActivity());
				
				saveSettings(settings, pkgSettings);

				if (cbEnableService.isChecked())
					serviceClient.checkPermissions();
			}
		};

		cbEnableService.setOnClickListener(saveSettingsOnClickListener);
		cbHandleCal.setOnClickListener(saveSettingsOnClickListener);
		cbHandleSMS.setOnClickListener(saveSettingsOnClickListener);
		cbHandlePhone.setOnClickListener(saveSettingsOnClickListener);
		cbHandleGmail.setOnClickListener(saveSettingsOnClickListener);

		((Button) rootView.findViewById(R.id.buttonCheckService)).setOnClickListener(new OnClickListener()
		{
			public void onClick(View arg0)
			{
				checkService();
			}

		});

		((Button) rootView.findViewById(R.id.buttonConfigure)).setOnClickListener(new OnClickListener()
		{
			public void onClick(View arg0)
			{
				Vibrator v = (Vibrator) getActivity().getSystemService(Context.VIBRATOR_SERVICE);
				long[] pattern = {0,80,30,80,30,80,30,80,30,80,30,80,30,80,150,900};
				Log.d(TAG, "Vibration pattern: " + pattern);
				v.vibrate(pattern, -1);

			}
		});

		return rootView;
	}

	public void onStart()
	{
		super.onStart();

		serviceClient = new ServiceClient(this);
		serviceClient.bindService(getActivity().getApplicationContext());
	}

	public void onStop()
	{
		serviceClient.unbindService(getActivity().getApplicationContext());

		super.onStop();
	}

	@Override
	public void onNotificationList(String[] notifications)
	{
		if (notifications != null)
		{
			StringBuilder sb = new StringBuilder();

			if (notifications != null)
				for (String ntf : notifications)
				{
					sb.append(ntf);
					sb.append("\n");
				}

			Toast.makeText(getActivity(), sb.toString(), Toast.LENGTH_LONG).show();
		}
		else
		{
			onNoPermissions();
		}
	}

	@Override
	public void onConnected()
	{
	}

	@Override
	public void onDisconnected()
	{
	}

}
