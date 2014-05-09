//http://developer.android.com/about/versions/android-4.3.html#NotificationListener

package com.github.quarck.smartnotify;

import java.util.List;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

@SuppressLint("UseValueOf")
public class MainActivity extends Activity
{
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		if (savedInstanceState == null)
		{
			getFragmentManager().beginTransaction()
					.add(R.id.container, new MainFragment()).commit();
		}

		CallStateTracker.start(this);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings)
		{
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	/**
	 * A placeholder fragment containing a simple view.
	 */

	public static class MainFragment extends Fragment implements
			ServiceClient.Callback
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
					.setPositiveButton(R.string.open_settings,
							new DialogInterface.OnClickListener()
							{
								public void onClick(DialogInterface dialog,
										int id)
								{
									Intent intent = new Intent(
											"android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS");
									startActivity(intent);
								}
							})
					.setNegativeButton(R.string.cancel_quit,
							new DialogInterface.OnClickListener()
							{
								public void onClick(DialogInterface dialog,
										int id)
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
				set(pkgSettings, "com.google.android.calendar", Integer.parseInt(editCalInterval.getText().toString()) * 60, cbHandleCal.isChecked());
				set(pkgSettings, "com.android.mms", Integer.parseInt(editSMSInterval.getText().toString()) * 60, cbHandleSMS.isChecked());
				set(pkgSettings, "com.android.phone", Integer.parseInt(editPhoneInterval.getText().toString()) * 60, cbHandlePhone.isChecked());
				set(pkgSettings, "com.google.android.gm", Integer.parseInt(editGmailInterval.getText().toString()) * 60, cbHandleGmail.isChecked());
				
				Log.d(TAG, "Currently known packages: ");
		
				for (PackageSettings.Package p : pkgSettings.getAllPackages())
				{
					Log.d(TAG, ">> " + p);
				}
			}
			catch (Exception ex)
			{
				AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
				builder.setMessage("Invalid number")
						.setPositiveButton("OK",
								new DialogInterface.OnClickListener()
								{
									public void onClick(DialogInterface dialog,
											int id)
									{
									}
								});
				builder.create().show();
			}
			
		}

		private void loadSettings(PackageSettings s)
		{
			cbHandleCal.setChecked(is(s, "com.google.android.calendar"));
			editCalInterval.setText((new Integer(interval(s, "com.google.android.calendar")/60)).toString());
			cbHandleSMS.setChecked(is(s, "com.android.mms"));
			editSMSInterval.setText((new Integer(interval(s, "com.android.mms")/60)).toString());
			cbHandlePhone.setChecked(is(s, "com.android.phone"));
			editPhoneInterval.setText((new Integer(interval(s, "com.android.phone")/60)).toString());
			cbHandleGmail.setChecked(is(s, "com.google.android.gm"));
			editGmailInterval.setText((new Integer(interval(s, "com.google.android.gm")/60)).toString());			
		}
		
		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState)
		{
			View rootView = inflater.inflate(R.layout.fragment_main, container,
					false);

			
			cbEnableService = (CheckBox) rootView.findViewById(R.id.checkBoxEnableService);
			cbHandleCal = (CheckBox) rootView.findViewById(R.id.checkBoxHandleCalendar);
			editCalInterval = (TextView) rootView.findViewById(R.id.editTextCalendarRemindInterval);
			cbHandleSMS = (CheckBox) rootView.findViewById(R.id.checkBoxHandleSMS);
			editSMSInterval = (TextView) rootView.findViewById(R.id.editTextSMSRemindInterval);
			cbHandlePhone = (CheckBox) rootView.findViewById(R.id.checkBoxHandlePhone);
			editPhoneInterval = (TextView) rootView.findViewById(R.id.editTextPhoneRemindInterval);
			cbHandleGmail = (CheckBox) rootView.findViewById(R.id.checkBoxHandleGmail);
			editGmailInterval = (TextView) rootView.findViewById(R.id.editTextGmailRemindInterval);

			
			((Button) rootView.findViewById(R.id.buttonCheckService))
					.setOnClickListener(new OnClickListener()
					{
						public void onClick(View arg0)
						{
							checkService();
						}

					});

			Settings settings = new Settings(getActivity());
			
			cbEnableService.setChecked(settings.isServiceEnabled());
			cbEnableService.setOnClickListener(new OnClickListener()
			{
				public void onClick(View arg0)
				{
					PackageSettings s = new PackageSettings(
							getActivity());
					
					saveSettings(new Settings(getActivity()), s);

					if (cbEnableService.isChecked())
						serviceClient.checkPermissions();
				}
			});
			
			PackageSettings s = new PackageSettings(
					getActivity());
			
			loadSettings(s);

			((Button) rootView.findViewById(R.id.buttonConfigure))
					.setOnClickListener(new OnClickListener()
					{
						public void onClick(View arg0)
						{
							PackageSettings s = new PackageSettings(
									getActivity());
							
							saveSettings(new Settings(getActivity()), s);
						}
					});

			return rootView;
		}

		public void onStart()
		{
			super.onStart();

			Context ctx = getActivity().getApplicationContext();
			serviceClient = new ServiceClient(this);

			serviceClient.bindService(ctx);
		}

		public void onStop()
		{
			Context ctx = getActivity().getApplicationContext();
			serviceClient.unbindService(ctx);

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

				Toast.makeText(getActivity(), sb.toString(), Toast.LENGTH_LONG)
						.show();
			}
			else
			{
				onNoPermissions();
			}
		}

		@Override
		public void onConnected()
		{
			// serviceClient.checkPermissions();
		}

		@Override
		public void onDisconnected()
		{
			// TODO Auto-generated method stu
		}

	}
}
