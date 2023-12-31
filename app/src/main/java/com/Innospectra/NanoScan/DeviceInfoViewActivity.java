package com.Innospectra.NanoScan;

import android.app.ActionBar;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.ISCSDK.ISCNIRScanSDK;

import static com.Innospectra.NanoScan.ScanViewActivity.isExtendVer;
import static com.Innospectra.NanoScan.ScanViewActivity.isExtendVer_PLUS;

/**
 * This activity controls the view for the device information after the Nano is connected
 * When the activity is created, it will send a broadcast to the {@link ISCNIRScanSDK} to start
 * retrieving device information
 *
 * @author collinmast
 */

public class DeviceInfoViewActivity extends Activity {

    private static Context mContext;
    private TextView tv_manuf;
    private TextView tv_model;
    private TextView tv_serial;
    private TextView tv_hw;
    private TextView tv_tiva;
    private BroadcastReceiver DeviceInfoReceiver;
    private final BroadcastReceiver DisconnReceiver = new DisconnReceiver();
    private final IntentFilter disconnFilter = new IntentFilter(ISCNIRScanSDK.ACTION_GATT_DISCONNECTED);
    private static Boolean GotoOtherPage = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_info);
        mContext = this;
        //Set up the action bar title and enable the back indicator
        ActionBar ab = getActionBar();
        if (ab != null) {
            ab.setDisplayHomeAsUpEnabled(true);
            ab.setTitle(getString(R.string.device_information));
        }

        tv_manuf = (TextView) findViewById(R.id.tv_manuf);
        tv_model = (TextView) findViewById(R.id.tv_model);
        tv_serial = (TextView) findViewById(R.id.tv_serial);
        tv_hw = (TextView) findViewById(R.id.tv_hw);
        tv_tiva = (TextView) findViewById(R.id.tv_tiva);

        //Get device information
        ISCNIRScanSDK.GetDeviceInfo();
        /**
                 * Initialize device information broadcast receiver.
                 * All device information is sent in one broadcast.
                 * Once the information is received, make the progress bar invisible
                 *(ISCNIRScanSDK.GetDeviceInfo() should be called)
                 */
        DeviceInfoReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                tv_manuf.setText(intent.getStringExtra(ISCNIRScanSDK.EXTRA_MANUF_NAME).replace("\n", ""));
                tv_model.setText(intent.getStringExtra(ISCNIRScanSDK.EXTRA_MODEL_NUM).replace("\n", ""));
                String SerialNumber = intent.getStringExtra(ISCNIRScanSDK.EXTRA_SERIAL_NUM);
                if((isExtendVer||isExtendVer_PLUS)&& SerialNumber.length()>8)
                    SerialNumber = SerialNumber.substring(0,8);
                else if(!isExtendVer_PLUS &&!isExtendVer && SerialNumber.length()>7)
                    SerialNumber = SerialNumber.substring(0,7);
                tv_serial.setText(SerialNumber);
                tv_hw.setText(intent.getStringExtra(ISCNIRScanSDK.EXTRA_HW_REV));
                tv_tiva.setText(intent.getStringExtra(ISCNIRScanSDK.EXTRA_TIVA_REV));

                ProgressBar pb = (ProgressBar) findViewById(R.id.pb_info);
                pb.setVisibility(View.INVISIBLE);
            }
        };
        //register the broadcast receivers
        LocalBroadcastManager.getInstance(mContext).registerReceiver(DeviceInfoReceiver, new IntentFilter(ISCNIRScanSDK.ACTION_INFO));
        LocalBroadcastManager.getInstance(mContext).registerReceiver(DisconnReceiver, disconnFilter);
    }

    /**
     * On resume, make a call to the superclass.
     * Nothing else is needed here besides calling
     * the super method.
     */
    @Override
    public void onResume() {
        super.onResume();
        GotoOtherPage = false;
    }

    /**
     * When the activity is destroyed, unregister the BroadcastReceiver
     * handling disconnection events, and the receiver handling the device information
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(DeviceInfoReceiver);
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(DisconnReceiver);
    }
    @Override
    public void onPause() {
        super.onPause();
        if(!GotoOtherPage)
        {
            Intent notifybackground = new Intent(ScanViewActivity.NOTIFY_BACKGROUND);
            LocalBroadcastManager.getInstance(mContext).sendBroadcast(notifybackground);
            Intent notifybackground2 = new Intent(ConfigureViewActivity.NOTIFY_BACKGROUND);
            LocalBroadcastManager.getInstance(mContext).sendBroadcast(notifybackground2);
            finish();
        }
    }
    @Override
    public void onBackPressed() {
        GotoOtherPage = true;
        super.onBackPressed();
    }
    /**
     * Inflate the options menu
     * In this case, there is no menu and only an up indicator,
     * so the function should always return true.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    /**
     * Handle the selection of a menu item.
     * In this case, there is only the up indicator. If selected, this activity should finish.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            GotoOtherPage = true;
            this.finish();
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Broadcast Receiver handling the disconnect event. If the Nano disconnects,
     * this activity should finish so that the user is taken back to the {@link HomeViewActivity}.
     * A toast message should appear so that the user knows why the activity is finishing.
     */
    public class DisconnReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Toast.makeText(mContext, R.string.nano_disconnected, Toast.LENGTH_SHORT).show();
            finish();
        }
    }
}
