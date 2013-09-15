package com.example.testapplication;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

public class MainActivity extends Activity {

	private static final boolean DEBUG = true;
	
	public final static String EXTRA_MESSAGE = "com.example.myfirstapp.MESSAGE"; // good practice to use app name as a key

	// Key names received from the BluetoothChatService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";
	
	
	
	// Message types sent from the BluetoothDataReceptionService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;
    
    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
    private static final int REQUEST_CONNECT_DEVICE_INSECURE = 2;
    private static final int REQUEST_ENABLE_BT = 3;
    
    
    // Array adapter for the conversation thread
    private ArrayAdapter<String> mConversationArrayAdapter;
    // Layout Views
    private ListView mConversationView;
	
	
 // Name of the connected device
    private String mConnectedDeviceName = null;
	
    
	// Local Bluetooth adapter
    private BluetoothAdapter mBluetoothAdapter = null;
    private BluetoothDataReceptionService mDataService = null;
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		if (mBluetoothAdapter == null){
			// device does not support bluetooth... that's lame
			Log.e("MainActivity", "no bluetooth");
		}
    	Log.v("MainActivity", "create");

	}
	
	@Override
	public void onStart() {
		super.onStart();
		
		if(DEBUG) Log.e("MainActivity", "++ ON START ++");

        // If BT is not on, request that it be enabled.
        // setupChat() will then be called during onActivityResult
        if (!mBluetoothAdapter.isEnabled()) {
        	Log.v("MainActivity", "not enabled");

            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        // Otherwise, setup the chat session
        } else {
        	Log.v("MainActivity", "enabled");

        	//setup data grabbing
            //if (mChatService == null) setupChat();
        	// Initialize the BluetoothDataReceptionService to perform bluetooth connections
            mDataService = new BluetoothDataReceptionService(this, mHandler);
        }
    	Log.v("MainActivity", "start");

		
	}
	
	
	// TODO
	@Override
    public synchronized void onResume() {
        super.onResume();
        if(DEBUG) Log.e("MainActivity", "+ ON RESUME +");

        // Performing this check in onResume() covers the case in which BT was
        // not enabled during onStart(), so we were paused to enable it...
        // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
        if (mDataService != null) {
            // Only if the state is STATE_NONE, do we know that we haven't started already
            if (mDataService.getState() == BluetoothDataReceptionService.STATE_NONE) {
              // Start the Bluetooth chat services
              mDataService.start();
            }
        }
        connectDevice(new Intent(), false);
    }
	
	

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    // Handle presses on the action bar items
	    switch (item.getItemId()) {
	        case R.id.sign_out:
	        	//should have a real function called... or something
	            Log.v("MainActivity", "I'm outta here!");
	            return true;
	        case R.id.action_settings:
	            //openSettings();
	            Log.v("MainActivity", "Settings?");
	            return true;
	        default:
	            return super.onOptionsItemSelected(item);
	    }
	}
	
	/* send a message when the user clicks a button */
	public void sendMessage(View view){
		Intent intent = new Intent(this, DisplayMessageActivity.class);
		EditText editText = (EditText) findViewById(R.id.edit_message);
		String message = editText.getText().toString();
		intent.putExtra(EXTRA_MESSAGE, message);
		startActivity(intent);
	}
	
	
	private final void setStatus(int resId) {
		Log.v("Status", String.valueOf(resId));
        //final ActionBar actionBar = getActionBar();
        //actionBar.setSubtitle(resId);
    }
	
	//wtf is this for?!
    private final void setStatus(CharSequence subTitle) {
		Log.v("Status", subTitle.toString());
        //final ActionBar actionBar = getActionBar();
        //actionBar.setSubtitle(subTitle);
    }
	
	
	
	
	// The Handler that gets information back from the BluetoothDataReceptionService
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MESSAGE_STATE_CHANGE:
                if(DEBUG) Log.i("Handler", "MESSAGE_STATE_CHANGE: " + msg.arg1);
                switch (msg.arg1) {
                case BluetoothDataReceptionService.STATE_CONNECTED:
                    setStatus(getString(R.string.title_connected_to, mConnectedDeviceName));
                    mConversationArrayAdapter.clear();
                    break;
                case BluetoothDataReceptionService.STATE_CONNECTING:
                    setStatus(R.string.title_connecting);
                    break;
                case BluetoothDataReceptionService.STATE_LISTEN:
                case BluetoothDataReceptionService.STATE_NONE:
                    setStatus(R.string.title_not_connected);
                    break;
                }
                break;
            case MESSAGE_WRITE:
                byte[] writeBuf = (byte[]) msg.obj;
                // construct a string from the buffer
                String writeMessage = new String(writeBuf);
                mConversationArrayAdapter.add("Me:  " + writeMessage);
                break;
            case MESSAGE_READ:
                byte[] readBuf = (byte[]) msg.obj;
                // construct a string from the valid bytes in the buffer
                String readMessage = new String(readBuf, 0, msg.arg1);
                mConversationArrayAdapter.add(mConnectedDeviceName+":  " + readMessage);
                break;
            case MESSAGE_DEVICE_NAME:
                // save the connected device's name
                mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
                Toast.makeText(getApplicationContext(), "Connected to "
                               + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                break;
            case MESSAGE_TOAST:
                Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
                               Toast.LENGTH_SHORT).show();
                break;
            }
        }
    };
	
	
	
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(DEBUG) Log.d("MainActivity", "onActivityResult " + resultCode);
        switch (requestCode) {
        case REQUEST_CONNECT_DEVICE_SECURE:
            // When DeviceListActivity returns with a device to connect
            if (resultCode == Activity.RESULT_OK) {
                connectDevice(data, true);
            }
            break;
        case REQUEST_CONNECT_DEVICE_INSECURE:
            // When DeviceListActivity returns with a device to connect
            if (resultCode == Activity.RESULT_OK) {
                connectDevice(data, false);
            }
            break;
        case REQUEST_ENABLE_BT:
            // When the request to enable Bluetooth returns
            if (resultCode == Activity.RESULT_OK) {
                // Bluetooth is now enabled, so set up a chat session
                //setupChat();
                mDataService = new BluetoothDataReceptionService(this, mHandler);
            } else {
                // User did not enable Bluetooth or an error occurred
                Log.d("MainActivity", "BT not enabled");
                Toast.makeText(this, R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }
	
	

    private void connectDevice(Intent data, boolean secure) {
        // Get the device MAC address
        //String address = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
        // Get the BluetoothDevice object
        //BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        // Attempt to connect to the device
    	Log.v("MainActivity", "devices/connecting");
    	BluetoothDevice device = null;
    	
    	Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
    	// If there are paired devices
    	if (pairedDevices.size() > 0) {
    	    // Loop through paired devices
    	    for (BluetoothDevice adevice : pairedDevices) {
    	        // Add the name and address to an array adapter to show in a ListView
    	        //mArrayAdapter.add(device.getName() + "\n" + device.getAddress());
    	    	if (adevice.getAddress().equals("00:06:66:61:E7:8B")){
        	    	Log.v("MainActivity", "meh!!!!");
        	    	device = adevice;
    	    	}
    	    	else {
        	    	Log.v("MainActivity", adevice.getAddress()+" -- "+adevice.getName());
    	    	}
    	    }
    	}
    	
    	
    	//BluetoothDevice device = mBluetoothAdapter.getRemoteDevice();
        mDataService.connect(device, secure);
    }
	
	
	
	
}




