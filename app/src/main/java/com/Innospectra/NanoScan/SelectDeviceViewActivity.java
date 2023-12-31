package com.Innospectra.NanoScan;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.ISCSDK.ISCNIRScanSDK;

import java.util.ArrayList;

import static com.ISCSDK.ISCNIRScanSDK.storeStringPref;

/**
 * Activity for scanning for advertising Nano devices over BLE
 * This allows the user to specify a preferred Nano to use in the future.
 * The preferred Nano will be connected to first in environments with more than one Nano
 */
public class SelectDeviceViewActivity extends Activity {

    private Handler mHandler;
    private BluetoothAdapter mBluetoothAdapter;
    public BluetoothLeScanner mBluetoothLeScanner;
    private static String DEVICE_NAME = "NIR";
    private ArrayList<ISCNIRScanSDK.NanoDevice> nanoDeviceList = new ArrayList<>();
    private NanoScanAdapter nanoScanAdapter;
    private static Context mContext;
    private AlertDialog alertDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);
        mContext = this;
        DEVICE_NAME = ISCNIRScanSDK.getStringPref(mContext,ISCNIRScanSDK.SharedPreferencesKeys.DeviceFilter,"NIR");

        //Set up action bar title and enable back button
        ActionBar ab = getActionBar();
        if (ab != null) {
            ab.setTitle(getString(R.string.select_nano));
            ab.setDisplayHomeAsUpEnabled(true);
        }
        final BluetoothManager bluetoothManager =
               (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();

        ListView lv_nanoDevices = (ListView) findViewById(R.id.lv_nanoDevices);
        //Create adapter for the NanoDevice objects returned from a BLE scan
        nanoScanAdapter = new NanoScanAdapter(this, nanoDeviceList);
        lv_nanoDevices.setAdapter(nanoScanAdapter);
        lv_nanoDevices.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                confirmationDialog(nanoDeviceList.get(i).getNanoMac(),nanoDeviceList.get(i).getNanoName());
            }
        });

        mHandler = new Handler();
        scanLeDevice(true);
    }

    /**
     * Provide user with a dialog that asks if they are sure they want to use the Nano with the
     * specified mac as their preferred device
     *
     * @param mac MAC address of Nano
     */
    public void confirmationDialog(String mac, final String name) {

        final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(mContext);
        final String deviceMac = mac;
        alertDialogBuilder.setTitle(mContext.getResources().getString(R.string.nano_confirmation_title));
        alertDialogBuilder.setMessage(mContext.getResources().getString(R.string.nano_confirmation_msg, mac));

        alertDialogBuilder.setPositiveButton(getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface arg0, int arg1) {
                alertDialog.dismiss();
                storeStringPref(mContext, ISCNIRScanSDK.SharedPreferencesKeys.preferredDevice, deviceMac);
                storeStringPref(mContext, ISCNIRScanSDK.SharedPreferencesKeys.preferredDeviceModel,name);
                finish();
            }
        });

        alertDialogBuilder.setNegativeButton(getResources().getString(R.string.cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                alertDialog.dismiss();
            }
        });

        alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    /**
     * Callback function for Bluetooth scanning. This function provides the instance of the
     * Bluetooth device {@link BluetoothDevice} that was found, it's rssi, and advertisement
     * data (scanRecord).
     * <p>
     * When a Bluetooth device with the advertised name matching the
     * string DEVICE_NAME {@link SelectDeviceViewActivity#DEVICE_NAME} is found, a call is made to connect
     * to the device. Also, the Bluetooth should stop scanning, even if
     * the {@link ISCNIRScanSDK#SCAN_PERIOD} has not expired
     */
    private ScanCallback mLeScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);

            BluetoothDevice device = result.getDevice();
            @SuppressLint("MissingPermission") String name = device.getName();
            if (name != null && name.contains(DEVICE_NAME) && result.getScanRecord() != null) {
                Boolean isDeviceInList = false;
                ISCNIRScanSDK.NanoDevice nanoDevice = new ISCNIRScanSDK.NanoDevice(device, result.getRssi(), result.getScanRecord().getBytes());
                for (ISCNIRScanSDK.NanoDevice d : nanoDeviceList) {
                    if (d.getNanoMac().equals(device.getAddress())) {
                        isDeviceInList = true;
                        d.setRssi(result.getRssi());
                        nanoScanAdapter.notifyDataSetChanged();
                    }
                }
                if (!isDeviceInList) {
                    nanoDeviceList.add(nanoDevice);
                    nanoScanAdapter.notifyDataSetChanged();
                }
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            System.out.println("BLE// onScanFailed");
            Log.e("Scan Failed", "Error Code: " + errorCode);
        }

    };
    /**
     * Handle the selection of a menu item.
     * In this case, there is only the up indicator. If selected, this activity should finish.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            this.finish();
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Scans for Bluetooth devices on the specified interval {@link ISCNIRScanSDK#SCAN_PERIOD}.
     * This function uses the handler {@link SelectDeviceViewActivity#mHandler} to delay call to stop
     * scanning until after the interval has expired. The start and stop functions take an
     * LeScanCallback parameter that specifies the callback function when a Bluetooth device
     * @param enable Tells the Bluetooth adapter {@link SelectDeviceViewActivity#mBluetoothAdapter} if
     *               it should start or stop scanning
     */
    @SuppressLint("MissingPermission")
    private void scanLeDevice(final boolean enable) {
        if(mBluetoothLeScanner == null){
            Toast.makeText(SelectDeviceViewActivity.this, "Could not open LE scanner", Toast.LENGTH_SHORT).show();
        }else {
            if (enable) {
                // Stops scanning after a pre-defined scan period.
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mBluetoothLeScanner.stopScan(mLeScanCallback);
                    }
                }, ISCNIRScanSDK.SCAN_PERIOD);
                mBluetoothLeScanner.startScan(mLeScanCallback);
            } else {
                mBluetoothLeScanner.stopScan(mLeScanCallback);
            }
        }
    }

    /**
     * Custom adapter that holds {@link ISCNIRScanSDK.NanoDevice} objects to be used in a listview.
     * This adapter contains device name, MAC, and RSSI
     */
    private class NanoScanAdapter extends ArrayAdapter<ISCNIRScanSDK.NanoDevice> {
        private final ArrayList<ISCNIRScanSDK.NanoDevice> nanoDevices;
        public NanoScanAdapter(Context context, ArrayList<ISCNIRScanSDK.NanoDevice> values) {
            super(context, -1, values);
            this.nanoDevices = values;
        }
        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            ViewHolder viewHolder;
            if (convertView == null) {
                convertView = LayoutInflater.from(this.getContext())
                        .inflate(R.layout.row_nano_scan_item, parent, false);
                viewHolder = new ViewHolder();
                viewHolder.nanoName = (TextView) convertView.findViewById(R.id.tv_nano_name);
                viewHolder.nanoMac = (TextView) convertView.findViewById(R.id.tv_nano_mac);
                viewHolder.nanoRssi = (TextView) convertView.findViewById(R.id.tv_rssi);
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }

            final ISCNIRScanSDK.NanoDevice device = getItem(position);
            if (device != null) {
                viewHolder.nanoName.setText(device.getNanoName());
                viewHolder.nanoMac.setText(device.getNanoMac());
                viewHolder.nanoRssi.setText(device.getRssiString());
            }
            return convertView;
        }
    }

    /**
     * View holder for {@link ISCNIRScanSDK.NanoDevice} objects
     */
    private class ViewHolder {
        private TextView nanoName;
        private TextView nanoMac;
        private TextView nanoRssi;
    }
}
