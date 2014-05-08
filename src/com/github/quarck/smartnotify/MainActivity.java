//http://developer.android.com/about/versions/android-4.3.html#NotificationListener

package com.github.quarck.smartnotify;

import java.util.List;

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
import android.widget.Toast;

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

		public void checkService()
		{
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

		private void addOrUpdatedPackage(PackageSettings s, String packageName, int delay)
		{
			PackageSettings.Package pkg = s.getPackage(packageName);

			if (pkg == null)
			{
				s.addPackage(s.new Package(packageName, true, delay));
			}
			else
			{
				Log.d(TAG, "Updating to send reminders every minute");
				pkg.setRemindIntervalSeconds(delay);
				s.updatePackage(pkg);
			}
		}
		
		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState)
		{
			View rootView = inflater.inflate(R.layout.fragment_main, container,
					false);

			((Button) rootView.findViewById(R.id.buttonCheckService))
					.setOnClickListener(new OnClickListener()
					{
						public void onClick(View arg0)
						{
							checkService();
						}

					});

			final CheckBox cbEnabled = ((CheckBox) rootView
					.findViewById(R.id.checkBoxEnableService));

			cbEnabled
					.setChecked(new Settings(getActivity()).isServiceEnabled());

			cbEnabled.setOnClickListener(new OnClickListener()
			{
				public void onClick(View arg0)
				{
					Settings s = new Settings(getActivity());
					s.setServiceEnabled(cbEnabled.isChecked());

					if (cbEnabled.isChecked())
						serviceClient.checkPermissions();
				}
			});

			((Button) rootView.findViewById(R.id.buttonConfigure))
					.setOnClickListener(new OnClickListener()
					{
						public void onClick(View arg0)
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
							
							

							PackageSettings s = new PackageSettings(
									getActivity());

							
							addOrUpdatedPackage(s, "com.google.android.calendar", 60*5);
							addOrUpdatedPackage(s, "com.android.mms", 60*4);
							addOrUpdatedPackage(s, "com.android.phone", 60*3);
							
							Log.d(TAG, "Currently known packages: ");
							List<PackageSettings.Package> all = s
									.getAllPackages();

							for (PackageSettings.Package p : all)
							{
								Log.d(TAG, ">> " + p);
							}

						}
					});

			((Button) rootView.findViewById(R.id.button2))
					.setOnClickListener(new OnClickListener()
					{
						public void onClick(View arg0)
						{
							Vibrator v = (Vibrator) getActivity()
									.getSystemService(Context.VIBRATOR_SERVICE); 
							v.vibrate(new Settings(getActivity())
									.getVibrationPattern(), -1);
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
