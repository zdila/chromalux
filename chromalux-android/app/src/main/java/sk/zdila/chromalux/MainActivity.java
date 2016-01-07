package sk.zdila.chromalux;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.SeekBar;
import android.widget.Toast;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public final class MainActivity extends Activity {

    private static final String TAG = "BluetoothChatFragment";

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
    private static final int REQUEST_ENABLE_BT = 3;

    private BluetoothAdapter mBluetoothAdapter;

    private BluetoothChatService mChatService;

    private String mConnectedDeviceName;
    private Toast toast;

    private long last = System.currentTimeMillis();

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        toast = Toast.makeText(this, "Not connected", Toast.LENGTH_SHORT);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
        }

        final ColorPicker colorPicker = (ColorPicker) findViewById(R.id.colorPicker);
        final BrightnessPicker brightnessPicker = (BrightnessPicker) findViewById(R.id.brightnessPicker);

        colorPicker.setColor(Color.GREEN);
        brightnessPicker.setColor(Color.GREEN);

        brightnessPicker.setOnColorChangeListener(new BrightnessPicker.OnColorChangeListener() {
            @Override
            public void onColorChanged(final BrightnessPicker brightnessPicker, final int color, float[] colorHSV) {
                colorPicker.setValue(colorHSV[2]);
                sendColor(colorHSV);
            }
        });

        colorPicker.setOnColorChangeListener(new ColorPicker.OnColorChangeListener() {
            @Override
            public void onColorChanged(final ColorPicker colorPicker, final int color, float[] colorHSV) {
                brightnessPicker.setHS(colorHSV[0], colorHSV[1]);
                sendColor(colorHSV);
            }
        });

        ((SeekBar) findViewById(R.id.amplitudeSeekBar)).setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(final SeekBar seekBar, final int progress, final boolean fromUser) {
                final ByteBuffer buffer = ByteBuffer.allocate(7);
                buffer.order(ByteOrder.LITTLE_ENDIAN);
                buffer.put((byte) 0xAA); // sync
                buffer.put((byte) 5); // len
                buffer.put((byte) 2);
                buffer.putFloat(progress / 1000000f);
                sendMessage(buffer.array());

            }

            @Override
            public void onStartTrackingTouch(final SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(final SeekBar seekBar) {

            }
        });

        ((SeekBar) findViewById(R.id.frequencySeekBar)).setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(final SeekBar seekBar, final int progress, final boolean fromUser) {
                final ByteBuffer buffer = ByteBuffer.allocate(7);
                buffer.order(ByteOrder.LITTLE_ENDIAN);
                buffer.put((byte) 0xAA); // sync
                buffer.put((byte) 5); // len
                buffer.put((byte) 3);
                buffer.putFloat((float) Math.pow(progress / 1000000.0, 2.0));
                sendMessage(buffer.array());

            }

            @Override
            public void onStartTrackingTouch(final SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(final SeekBar seekBar) {

            }
        });

    }

    private void sendColor(final float[] colorHSV) {
        final long t = System.currentTimeMillis();
        if (t - last > 20) {

            final ByteBuffer buffer = ByteBuffer.allocate(15);
            buffer.order(ByteOrder.LITTLE_ENDIAN);
            buffer.put((byte) 0xAA); // sync
            buffer.put((byte) 13); // len
            buffer.put((byte) 1);
            buffer.putFloat(colorHSV[0]);
            buffer.putFloat(colorHSV[1]);
            buffer.putFloat(colorHSV[2]);
            sendMessage(buffer.array());

            last = t;
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
            // Otherwise, setup the chat session
        } else if (mChatService == null) {
            setupChat();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mChatService != null) {
            mChatService.stop();
        }
    }

    /**
     * Set up the UI and background operations for chat.
     */
    private void setupChat() {
        Log.d(TAG, "setupChat()");

        // Initialize the BluetoothChatService to perform bluetooth connections
        mChatService = new BluetoothChatService(this, mHandler);

    }

    /**
     * The Handler that gets information back from the BluetoothChatService
     */
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Constants.MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case BluetoothChatService.STATE_CONNECTED:
                            Log.i(TAG, "Status: Connected to " + mConnectedDeviceName);
//                            mConversationArrayAdapter.clear();
                            break;
                        case BluetoothChatService.STATE_CONNECTING:
                            Log.i(TAG, "Status: connecting");
                            break;
                        case BluetoothChatService.STATE_NONE:
                            Log.i(TAG, "Status: Not connected");
                            break;
                    }
                    break;
                case Constants.MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;
                    // construct a string from the buffer
                    //String writeMessage = new String(writeBuf);
                    //Log.i(TAG, "MW: " + writeMessage);
//                    mConversationArrayAdapter.add("Me:  " + writeMessage);
                    break;
                case Constants.MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    // construct a string from the valid bytes in the buffer
                    String readMessage = new String(readBuf, 0, msg.arg1);
                    Log.i(TAG, "MR: " + readMessage);
                    break;
                case Constants.MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    mConnectedDeviceName = msg.getData().getString(Constants.DEVICE_NAME);
                    Toast.makeText(MainActivity.this, "Connected to "
                            + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                    break;
                case Constants.MESSAGE_TOAST:
                    Toast.makeText(MainActivity.this, msg.getData().getString(Constants.TOAST),
                            Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CONNECT_DEVICE_SECURE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    connectDevice(data);
                }
                break;
            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    // Bluetooth is now enabled, so set up a chat session
                    setupChat();
                } else {
                    // User did not enable Bluetooth or an error occurred
                    Log.d(TAG, "BT not enabled");
                    Toast.makeText(this, "BT not enabled, leaving", Toast.LENGTH_SHORT).show();
                    finish();
                }
        }
    }

    /**
     * Establish connection with other device
     *
     * @param data   An {@link Intent} with {@link DeviceListActivity#EXTRA_DEVICE_ADDRESS} extra.
     */
    private void connectDevice(Intent data) {
        // Get the device MAC address
        String address = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
        // Get the BluetoothDevice object
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        // Attempt to connect to the device
        mChatService.connect(device);
    }


    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.bluetooth_chat, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.secure_connect_scan:
                // Launch the DeviceListActivity to see devices and do scan
                Intent serverIntent = new Intent(this, DeviceListActivity.class);
                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_SECURE);
                return true;
            default:
                return false;
        }
    }

    private void sendMessage(final byte[] message) {
        // Check that we're actually connected before trying anything
        if (mChatService.getState() != BluetoothChatService.STATE_CONNECTED) {
            if (!toast.getView().isShown()) {
                toast.show();
            }
            return;
        }

        // Check that there's actually something to send
        if (message.length > 0) {
            mChatService.write(message);
        }
    }
}
