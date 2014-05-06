//http://developer.android.com/about/versions/android-4.3.html#NotificationListener

package com.github.quarck.smartnotify;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActionBar;
import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.os.Vibrator;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;
import android.os.Build;

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
                    .add(R.id.container, new MainFragment())
                    .commit();
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

	public static class MainFragment 
		extends Fragment 
		implements ServiceClient.Callback
    {
    	ServiceClient serviceClient = null;
    	        
        public void checkService() 
        {        
        	serviceClient.getListNotifications();
        }
        
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) 
        {
        	View rootView = inflater.inflate(R.layout.fragment_main, container, false);
            
        	((Button)rootView.findViewById(R.id.buttonCheckService)).setOnClickListener(
    			new OnClickListener()
    			{
					public void onClick(View arg0) 
					{
			        	checkService();
					}
    				
    			}
    		);
        	
        	final ServiceClient.Callback cb = this;
        	
        	((Button)rootView.findViewById(R.id.buttonConfigure)).setOnClickListener(
        			new OnClickListener()
        			{
    					public void onClick(View arg0) 
    					{
    			        	Context ctx = getActivity().getApplicationContext();
    			        	serviceClient = new ServiceClient(cb);

    			        	serviceClient.bindService(ctx);
    					}
        				
        			}
        		);
       
            return rootView;
        }

        public void onStart()
        {
        	super.onStart();
        	//Context ctx = getActivity().getApplicationContext();
        	//serviceClient = new ServiceClient();

        	//serviceClient.bindService(ctx);
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
        	StringBuilder sb = new StringBuilder();
        	
        	if (notifications != null)
	        	for(String ntf: notifications)
	        	{
	        		sb.append(ntf);
	        		sb.append("\n");
	        	}
        	
    		Toast.makeText(getActivity(), sb.toString(), Toast.LENGTH_LONG).show();
			
		}
    }
}
