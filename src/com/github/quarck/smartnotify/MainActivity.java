//http://developer.android.com/about/versions/android-4.3.html#NotificationListener

package com.github.quarck.smartnotify;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.app.ActionBar;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.FragmentTransaction;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ToggleButton;

public class MainActivity extends Activity implements ServiceClient.Callback
{
	private static String TAG = "MainActivity";
	
	private ServiceClient serviceClient = null;

	private class ApplicationPkgInfo
	{
		PackageSettings.Package pkgInfo;
		Drawable icon;
		String name;
	}

	private ArrayList<ApplicationPkgInfo> handledApplications = null;
	
	private ToggleButton tbEnableService = null;

	private ListView listHandledApplications = null;

	
	private OnClickListener saveSettingsOnClickListener = null;

	private PackageSettings pkgSettings = null;
	private Settings settings = null;

	private LoadPackagesTask listApplicationsLoader = null;
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		Lw.d("main activity created");
		
		Lw.d(TAG, "onCreateView");

		setContentView(R.layout.activity_main);
		
	//	View rootView = inflater.inflate(R.layout.fragment_main, container, false);

		tbEnableService = (ToggleButton) findViewById(R.id.toggleButtonEnableService);

		listHandledApplications = (ListView) findViewById(R.id.listApplications);
		
		settings = new Settings(this);
		pkgSettings = new PackageSettings(this);

		

/*		listHandledApplications.setOnItemClickListener(new AdapterView.OnItemClickListener()
		{

			@Override
			public void onItemClick(AdapterView<?> parent, final View view, int position, long id)
			{
				final String item = (String) parent.getItemAtPosition(position);
				view.animate().setDuration(2000).alpha(0).withEndAction(new Runnable()
				{
					@Override
					public void run()
					{
						list.remove(item);
						adapter.notifyDataSetChanged();
						view.setAlpha(1);
					}
				});
			}
		}); */
		
		loadSettings(settings, pkgSettings);

		
		synchronized(this)
		{
			listApplicationsLoader = new LoadPackagesTask();
			listApplicationsLoader.execute();
		}
		
		saveSettingsOnClickListener = new OnClickListener()
		{
			public void onClick(View arg0)
			{
				Lw.d("saveSettingsOnClickListener.onClick()");

				saveSettings();

				if (tbEnableService.isChecked())
					serviceClient.checkPermissions();
			}
		};

		tbEnableService.setOnClickListener(saveSettingsOnClickListener);

		/*
		 * ((Button)
		 * rootView.findViewById(R.id.buttonCheckService)).setOnClickListener
		 * (new OnClickListener() { public void onClick(View arg0) {
		 * checkService(); }
		 * 
		 * });
		 * 
		 * ((Button)
		 * rootView.findViewById(R.id.buttonConfigure)).setOnClickListener(new
		 * OnClickListener() { public void onClick(View arg0) { Vibrator v =
		 * (Vibrator) getActivity().getSystemService(Context.VIBRATOR_SERVICE);
		 * long[] pattern = {0,80,30,80,30,80,30,80,30,80,30,80,30,80,150,900};
		 * v.vibrate(pattern, -1);
		 * 
		 * } });
		 */	
//		return rootView;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		int id = item.getItemId();
		if (id == R.id.action_settings)
		{
			return true;
		}
		else if (id == R.id.action_edit_applications)
		{
			Intent intent = new Intent(this, EditApplicationsActivity.class);
			startActivity(intent);
		}
		return super.onOptionsItemSelected(item);
	}
	
	
	@Override
	public void onNoPermissions()
	{
		Lw.d(TAG, "onNoPermissions()!!!");

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(R.string.application_has_no_access).setCancelable(false)
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
						finish();
					}
				});

		// Create the AlertDialog object and return it
		builder.create().show();
	}

	private void saveSettings()
	{
		Lw.d(TAG, "Saving current settings");

		settings.setServiceEnabled(tbEnableService.isChecked());

		try
		{
			for (ApplicationPkgInfo ai : handledApplications)
			{
				pkgSettings.updatePackage(ai.pkgInfo);
			}

			/*
			
			pkgSettings.set("com.google.android.calendar",
					Integer.parseInt(editCalInterval.getText().toString()) * 60, cbHandleCal.isChecked());
			pkgSettings.set("com.android.mms", Integer.parseInt(editSMSInterval.getText().toString()) * 60,
					cbHandleSMS.isChecked());
			pkgSettings.set("com.android.phone", Integer.parseInt(editPhoneInterval.getText().toString()) * 60,
					cbHandlePhone.isChecked());
			pkgSettings.set("com.google.android.gm", Integer.parseInt(editGmailInterval.getText().toString()) * 60,
					cbHandleGmail.isChecked());

			Lw.d(TAG, "Currently known packages: ");

			for (PackageSettings.Package p : pkgSettings.getAllPackages())
			{
				Lw.d(TAG, ">> " + p);
			}*/
		}
		catch (Exception ex)
		{
			Lw.e(TAG, "Got exception while saving settings " + ex);

			AlertDialog.Builder builder = new AlertDialog.Builder(this);
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
		Lw.d(TAG, "loading settings");

		tbEnableService.setChecked(settings.isServiceEnabled());
	}

	
	
	private void loadPackages()
	{
		PackageSettings pkgSettings = new PackageSettings(this);
	
		PackageManager packageManager = getPackageManager();

		List<PackageSettings.Package> allPackages = pkgSettings.getAllPackages();
		
		ArrayList<ApplicationPkgInfo> applications = new ArrayList<ApplicationPkgInfo>();
		
		for (PackageSettings.Package pkg : allPackages)
		{
			ApplicationPkgInfo ai = new ApplicationPkgInfo();
			ai.pkgInfo = pkg;
			
			ApplicationInfo pmAppInfo;
			try
			{
				pmAppInfo = packageManager.getApplicationInfo(pkg.getPackageName(), PackageManager.GET_META_DATA);
				ai.name = packageManager.getApplicationLabel(pmAppInfo).toString();
				ai.icon = pmAppInfo.loadIcon(packageManager);
			}
			catch (NameNotFoundException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}			
			
			applications.add(ai);
		}
		
		synchronized(this)
		{
			handledApplications =  applications;
		}
	}
	

	public class LoadPackagesTask extends AsyncTask<Void, Void, Void>
	{
		@Override
		protected Void doInBackground(Void... params)
		{
			loadPackages();
			return null;
		}

		@Override
		protected void onPreExecute()
		{
		}

		@Override
		protected void onPostExecute(Void result)
		{
			ArrayList<ApplicationPkgInfo> applications = null;
			
			synchronized (MainActivity.this)
			{
				applications = handledApplications;
			}
			
			listHandledApplications.setAdapter(
					new ListApplicationsAdapter(
							MainActivity.this, R.layout.list_item, applications));
			

			listHandledApplications.setSelection(0);
			
			synchronized (MainActivity.this)
			{
				listApplicationsLoader = null; // job is done, dispose 
			}
		}

		@Override
		protected void onCancelled()
		{
		}
	}

	
	
	@Override
	public void onStart()
	{
		Lw.d(TAG, "onStart()");
		super.onStart();
		serviceClient = new ServiceClient(this);
		serviceClient.bindService(getApplicationContext());
	}

	@Override
	public void onStop()
	{
		Lw.d(TAG, "onStop()");
		serviceClient.unbindService(getApplicationContext());
		super.onStop();
	}
	
	@Override 
	public void onPause()
	{
		LoadPackagesTask loader;
		
		synchronized(this)
		{
			loader = listApplicationsLoader;
			listApplicationsLoader = null;
		}
		
		if (loader != null)
			loader.cancel(false);

		super.onPause();
	}
	
	@Override 
	public void onResume()
	{
		super.onResume();
		
		synchronized(this)
		{
			listApplicationsLoader = new LoadPackagesTask();
			listApplicationsLoader.execute();
		}
	}

	@Override
	public void onNotificationList(String[] notifications)
	{
		Lw.d(TAG, "OnNotificationList()");

		if (notifications != null)
		{
			StringBuilder sb = new StringBuilder();

			if (notifications != null)
				for (String ntf : notifications)
				{
					sb.append(ntf);
					sb.append("\n");
				}

			Toast.makeText(this, sb.toString(), Toast.LENGTH_LONG).show();
		}
		else
		{
			onNoPermissions();
		}
	}

	@Override
	public void onConnected()
	{
		Lw.d(TAG, "onConnected()");
	}

	@Override
	public void onDisconnected()
	{
		Lw.d(TAG, "onDisconnected");
	}

	private class ListApplicationsAdapter extends BaseAdapter
	{
		private final Context context;
		
		ArrayList<ApplicationPkgInfo> listApplications;

		public ListApplicationsAdapter(Context ctx, int textViewResourceId, ArrayList<ApplicationPkgInfo> applications)
		{
			super();
			
			context = ctx;
			
			listApplications = applications;
		}

		public int getCount()
		{
			return listApplications.size();
		}

		public Object getItem(int position)
		{
			return listApplications.get(position);
		}

		public long getItemId(int position)
		{
			return position;
		}

		
		@Override
		public View getView(int position, View convertView, ViewGroup parent)
		{
			View rowView = convertView;
			
			if (rowView == null)
			{
				LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				
				rowView = inflater.inflate(R.layout.list_item, parent, false);

				ViewHolder viewHolder = new ViewHolder();

				viewHolder.textViewRemindInterval = (TextView) rowView.findViewById(R.id.textViewRemindInterval);				
				viewHolder.textViewAppName = (TextView) rowView.findViewById(R.id.textViewAppName);				
				viewHolder.imageViewAppIcon = (ImageView) rowView.findViewById(R.id.icon);
				//viewHolder.checkBoxEnableForApp = (CheckBox) rowView.findViewById(R.id.checkBoxEnableForApp);				
				viewHolder.btnEnableForApp = (ToggleButton) rowView.findViewById(R.id.toggleButtonEnableForApp);				
				
				rowView.setTag(viewHolder);
			}
			
			final ApplicationPkgInfo appInfo = listApplications.get(position); // this would not change as well - why lookup twice then?
			
			ViewHolder viewHolder = (ViewHolder)rowView.getTag();
			
			//viewHolder.checkBoxEnableForApp.setChecked( appInfo.pkgInfo.isHandlingThis());
			viewHolder.btnEnableForApp.setChecked( appInfo.pkgInfo.isHandlingThis() );
			viewHolder.textViewRemindInterval.setText("every " + (appInfo.pkgInfo.getRemindIntervalSeconds() / 60) + " mins (click to change)");
			
			if (appInfo.name != null)
				viewHolder.textViewAppName.setText(appInfo.name);
			else
				viewHolder.textViewAppName.setText(appInfo.pkgInfo.getPackageName());
			
			if ( appInfo.icon != null)
				viewHolder.imageViewAppIcon.setImageDrawable( appInfo.icon );
			
//			viewHolder.checkBoxEnableForApp.setOnClickListener(new OnClickListener()
			viewHolder.btnEnableForApp.setOnClickListener(new OnClickListener()
			{
				public void onClick(View btn)
				{
					Lw.d("saveSettingsOnClickListener.onClick()");
//					appInfo.pkgInfo.setHandlingThis( ! appInfo.pkgInfo.isHandlingThis() );
					appInfo.pkgInfo.setHandlingThis( ((ToggleButton)btn).isChecked() );
				
					pkgSettings.updatePackage(appInfo.pkgInfo);
					
					saveSettings();
				}
			});

			return rowView;
		}	
	}

	public class ViewHolder
	{
//		CheckBox checkBoxEnableForApp;
		ToggleButton btnEnableForApp;
		TextView textViewRemindInterval;
		TextView textViewAppName;
		ImageView imageViewAppIcon;
		
//		ApplicationPkgInfo appInfo;
	}
	
	
	/*private class ProcessListAdapter
	{
		public void toggle(int position)
		{
			mBoolView.set(position, !mBoolView.get(position));
			notifyDataSetChanged();
		}

		private Context mContext;

	} */
}
