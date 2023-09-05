package com.Innospectra.NanoScan;

import static com.ISCSDK.ISCNIRScanSDK.Interpret_intensity;
import static com.ISCSDK.ISCNIRScanSDK.Interpret_length;
import static com.ISCSDK.ISCNIRScanSDK.Interpret_uncalibratedIntensity;
import static com.ISCSDK.ISCNIRScanSDK.Interpret_wavelength;
import static com.ISCSDK.ISCNIRScanSDK.getBooleanPref;
import static com.ISCSDK.ISCNIRScanSDK.getStringPref;
import static com.ISCSDK.ISCNIRScanSDK.storeBooleanPref;
import static com.ISCSDK.ISCNIRScanSDK.storeStringPref;
import static com.Innospectra.NanoScan.DeviceStatusViewActivity.GetLampTimeString;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.FragmentTransaction;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.provider.MediaStore;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.ISCSDK.ISCNIRScanSDK;
import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.opencsv.CSVWriter;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

/**
 * Activity controlling the Nano once it is connected
 * This activity allows a user to initiate a scan, as well as access other "connection-only"
 * settings. When first launched, the app will scan for a preferred device
 * for {@link com.ISCSDK.ISCNIRScanSDK#SCAN_PERIOD}, if it is not found, then it will start another "open"
 * scan for any Nano.
 * <p>
 * If a preferred Nano has not been set, it will start a single scan. If at the end of scanning, a
 * Nano has not been found, a message will be presented to the user indicating and error, and the
 * activity will finish
 * <p>
 * WARNING: This activity uses JNI function calls for communicating with the Spectrum C library, It
 * is important that the name and file structure of this activity remain unchanged, or the functions
 * will NOT work
 *
 * @author collinmast
 */
public class ScanViewActivityForUsers extends Activity {

    public static final String NOTIFY_BACKGROUND = "com.Innospectra.NanoScan.ScanViewActivity.notifybackground";
    //! Tiva version is extend wavelength version or not
    public static Boolean isExtendVer = false;
    public static Boolean isExtendVer_PLUS = false;
    //! Control FW level to implement function
    public static ISCNIRScanSDK.FW_LEVEL_STANDARD fw_level_standard = ISCNIRScanSDK.FW_LEVEL_STANDARD.LEVEL_0;
    public static ISCNIRScanSDK.FW_LEVEL_EXT fw_level_ext = ISCNIRScanSDK.FW_LEVEL_EXT.LEVEL_EXT_1;
    public static ISCNIRScanSDK.FW_LEVEL_EXT_PLUS fw_level_ext_plus = ISCNIRScanSDK.FW_LEVEL_EXT_PLUS.LEVEL_EXT_PLUS_1;
    public static boolean showActiveconfigpage = false;
    public static byte[] passSpectrumCalCoefficients = new byte[144];
    //Is go to Scan Configuration page or not
    public static boolean GotoScanConfigFlag = false;
    public static Boolean isOldTiva = false;
    //region parameter
    private static Context mContext;
    //The name filter for BLE
    private static String DEVICE_NAME = "NIR";
    private static String API_URL = null;
    private static String API_KEY = null;
    //On pause event trigger is go to other page or not
    private static Boolean GotoOtherPage = false;
    //region broadcast parameter
    private final BroadcastReceiver ScanDataReadyReceiver = new ScanDataReadyReceiver();
    private final BroadcastReceiver RefDataReadyReceiver = new RefDataReadyReceiver();
    private final BroadcastReceiver NotifyCompleteReceiver = new NotifyCompleteReceiver();
    private final BroadcastReceiver ScanStartedReceiver = new ScanStartedReceiver();
    private final BroadcastReceiver RefCoeffDataProgressReceiver = new RefCoeffDataProgressReceiver();
    private final BroadcastReceiver CalMatrixDataProgressReceiver = new CalMatrixDataProgressReceiver();
    private final BroadcastReceiver DisconnReceiver = new DisconnReceiver();
    private final BroadcastReceiver SpectrumCalCoefficientsReadyReceiver = new SpectrumCalCoefficientsReadyReceiver();
    private final BroadcastReceiver RetrunReadActivateStatusReceiver = new RetrunReadActivateStatusReceiver();
    private final BroadcastReceiver RetrunActivateStatusReceiver = new RetrunActivateStatusReceiver();
    //endregion
    private final BroadcastReceiver DeviceInfoReceiver = new DeviceInfoReceiver();
    private final BroadcastReceiver GetUUIDReceiver = new GetUUIDReceiver();
    private final BroadcastReceiver GetDeviceStatusReceiver = new GetDeviceStatusReceiver();
    private final BroadcastReceiver ScanConfReceiver = new ScanConfReceiver();
    private final BroadcastReceiver WriteScanConfigStatusReceiver = new WriteScanConfigStatusReceiver();
    private final BroadcastReceiver ScanConfSizeReceiver = new ScanConfSizeReceiver();
    private final BroadcastReceiver GetActiveScanConfReceiver = new GetActiveScanConfReceiver();
    private final BroadcastReceiver ReturnLampRampUpADCReceiver = new ReturnLampRampUpADCReceiver();
    private final BroadcastReceiver ReturnLampADCAverageReceiver = new ReturnLampADCAverageReceiver();
    private final BroadcastReceiver ReturnLampRampUpADCTimeStampReceiver = new ReturnLampRampUpADCTimeStampReceiver();
    private final BroadcastReceiver ReturnMFGNumReceiver = new ReturnMFGNumReceiver();
    private final BroadcastReceiver ReturnSetLampReceiver = new ReturnSetLampReceiver();
    private final BroadcastReceiver ReturnSetPGAReceiver = new ReturnSetPGAReceiver();
    private final BroadcastReceiver ReturnSetScanRepeatsReceiver = new ReturnSetScanRepeatsReceiver();
    private final BroadcastReceiver ReturnHWModelReceiver = new ReturnHWModelReceiver();
    private final BroadcastReceiver BackgroundReciver = new BackGroundReciver();
    private final IntentFilter scanDataReadyFilter = new IntentFilter(ISCNIRScanSDK.SCAN_DATA);
    private final IntentFilter refReadyFilter = new IntentFilter(ISCNIRScanSDK.REF_CONF_DATA);
    private final IntentFilter notifyCompleteFilter = new IntentFilter(ISCNIRScanSDK.ACTION_NOTIFY_DONE);
    private final IntentFilter requestCalCoeffFilter = new IntentFilter(ISCNIRScanSDK.ACTION_REQ_CAL_COEFF);
    private final IntentFilter requestCalMatrixFilter = new IntentFilter(ISCNIRScanSDK.ACTION_REQ_CAL_MATRIX);
    private final IntentFilter disconnFilter = new IntentFilter(ISCNIRScanSDK.ACTION_GATT_DISCONNECTED);
    private final IntentFilter scanStartedFilter = new IntentFilter(ISCNIRScanSDK.ACTION_SCAN_STARTED);
    private final IntentFilter SpectrumCalCoefficientsReadyFilter = new IntentFilter(ISCNIRScanSDK.SPEC_CONF_DATA);
    private final IntentFilter RetrunReadActivateStatusFilter = new IntentFilter(ISCNIRScanSDK.ACTION_RETURN_READ_ACTIVATE_STATE);
    private final IntentFilter scanConfFilter = new IntentFilter(ISCNIRScanSDK.SCAN_CONF_DATA);
//    private final BroadcastReceiver GetPGAReceiver = new GetPGAReceiver();
    private final IntentFilter RetrunActivateStatusFilter = new IntentFilter(ISCNIRScanSDK.ACTION_RETURN_ACTIVATE);
    private final IntentFilter ReturnCurrentScanConfigurationDataFilter = new IntentFilter(ISCNIRScanSDK.RETURN_CURRENT_CONFIG_DATA);
    private final IntentFilter WriteScanConfigStatusFilter = new IntentFilter(ISCNIRScanSDK.ACTION_RETURN_WRITE_SCAN_CONFIG_STATUS);
    private final IntentFilter ReturnLampRampUpFilter = new IntentFilter(ISCNIRScanSDK.ACTION_RETURN_LAMP_RAMPUP_ADC);
    private final IntentFilter ReturnLampADCAverageFilter = new IntentFilter(ISCNIRScanSDK.ACTION_RETURN_LAMP_AVERAGE_ADC);
    private final IntentFilter ReturnLampRampUpADCTimeStampFilter = new IntentFilter(ISCNIRScanSDK.ACTION_RETURN_LAMP_ADC_TIMESTAMP);
    private final IntentFilter ReturnMFGNumFilter = new IntentFilter(ISCNIRScanSDK.ACTION_RETURN_MFGNUM);
    private final IntentFilter ReturnHWModelFilter = new IntentFilter(ISCNIRScanSDK.ACTION_RETURN_HWMODEL);
    private final OkHttpClient client = new OkHttpClient();
    LampInfo Lamp_Info = LampInfo.ManualLamp;
    Boolean init_viewpage_valuearray = false;
    //Record chart view page select
    int tabPosition = 0;
    //Record the active config index
    int ActiveConfigindex;
    ArrayAdapter<CharSequence> adapter_width;
    Boolean downloadspecFlag = false;
    byte[] SpectrumCalCoefficients = new byte[144];
    boolean stop_continuous = false;
    int MaxPattern = 0;
    Boolean isScan = false;
    //Record is set config for reference scan or not
    boolean reference_set_config = false;
    String model_name = "";
    String serial_num = "";
    String HWrev = "";
    String Tivarev = "";
    String Specrev = "";
    /**
     * Get the device uuid(ISCNIRScanSDK.GetUUID()should be called)
     */
    String uuid = "";
    boolean continuous = false;
    ISCNIRScanSDK.ReferenceCalibration reference_calibration;
    String CurrentTime;
    long MesureScanTime = 0;
    /**
     * GetDeviceStatusReceiver to get  the device status(ISCNIRScanSDK.GetDeviceStatus()should be called)
     */
    String battery = "";
    String TotalLampTime;
    byte[] devbyte;
    byte[] errbyte;
    float temprature;
    float humidity;
    Boolean saveReference = false;
    private ProgressDialog barProgressDialog;
    private ProgressBar calProgress;
    private TextView progressBarinsideText;
    private AlertDialog alertDialog;
    private Menu mMenu;
    private ProgressBar progressBar;
    private EditText fileNumber;
    private ViewPager mViewPager;
    private final String GraphLabel = "ISC Scan";
    private ArrayList<String> mXValues;
    private ArrayList<Entry> mIntensityFloat;
    private ArrayList<Entry> mAbsorbanceFloat;
    private ArrayList<Entry> mReflectanceFloat;
    private ArrayList<Entry> mReferenceFloat;
    private ArrayList<Float> mWavelengthFloat;
    //    private ArrayList<String> components;
    private ListView resultListView;
    private ArrayAdapter<String> adapter;
    private final String NOTIFY_ISEXTVER = "com.Innospectra.NanoScan.ISEXTVER";

    private ISCNIRScanSDK.ScanResults Scan_Spectrum_Data;
    private ISCNIRScanSDK mNanoBLEService;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeScanner mBluetoothLeScanner;
    private Handler mHandler;
    //Check the device is connected or not
    private boolean connected;
    //Get the device name you want to connect from the Settings page
    private String preferredDevice;
    //The active config of the device
    private ISCNIRScanSDK.ScanConfiguration activeConf;
    //Several configs in device were received
    private int receivedConfSize = -1;
    //Record the nnumber of the configs in the device
    private int storedConfSize;
    private final ArrayList<ISCNIRScanSDK.ScanConfiguration> ScanConfigList = new ArrayList<ISCNIRScanSDK.ScanConfiguration>();
    //Record the scan config list detail from scan configuration page
    private ArrayList<ISCNIRScanSDK.ScanConfiguration> ScanConfigList_from_ScanConfiguration = new ArrayList<ISCNIRScanSDK.ScanConfiguration>();
    private final List<String[]> post_data = new ArrayList<>();
    private String[] row_data;
    private final ArrayList<String> showResult = new ArrayList<>();
    //Record the active config byte
    private byte[] ActiveConfigByte;
    //Record the scan config list byte
    private final ArrayList<byte[]> ScanConfig_Byte_List = new ArrayList<>();
    //Record the scan config list byte from scan configuration page
    private ArrayList<byte[]> ScanConfig_Byte_List_from_ScanConfiuration = new ArrayList<>();
    private float minWavelength = 900;
    private float maxWavelength = 1700;
    private int MINWAV = 900;
    private int MAXWAV = 1700;
    private float minAbsorbance = 0;
    private float maxAbsorbance = 2;
    private float minReflectance = -2;
    private float maxReflectance = 2;
    private float minIntensity = -7000;
    private float maxIntensity = 7000;
    private float minReference = -7000;
    private float maxReference = 7000;
    private int numSections = 0;

    // 双面扫描切换按钮
    private ToggleButton btn_doubleside;
    private ToggleButton btn_verification;
    // 双面扫描结果校验按钮
    // 开始扫描按钮
    private Button btn_scanAndPredict;    //region Scan device and connect
    //Manage service lifecycle
    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            //Get a reference to the service from the service connection
            mNanoBLEService = ((ISCNIRScanSDK.LocalBinder) service).getService();

            //initialize bluetooth, if BLE is not available, then finish
            if (!mNanoBLEService.initialize()) {
                finish();
            }
            //Start scanning for devices that match DEVICE_NAME
            final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            mBluetoothAdapter = bluetoothManager.getAdapter();
            mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
            if (mBluetoothLeScanner == null) {
                finish();
                Toast.makeText(ScanViewActivityForUsers.this, "Please ensure Bluetooth is enabled and try again", Toast.LENGTH_SHORT).show();
            }
            mHandler = new Handler();
            if (getStringPref(mContext, ISCNIRScanSDK.SharedPreferencesKeys.preferredDevice, null) != null) {
                preferredDevice = getStringPref(mContext, ISCNIRScanSDK.SharedPreferencesKeys.preferredDevice, null);
                scanPreferredLeDevice(true);
            } else {
                scanLeDevice(true);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mNanoBLEService = null;
        }
    };
    //Normal scan setting
    private LinearLayout ly_normal_config;    /**
     * Callback function for Bluetooth scanning. This function provides the instance of the
     * Bluetooth device {@link BluetoothDevice} that was found, it's rssi, and advertisement
     * data (scanRecord).
     * When a Bluetooth device with the advertised name matching the
     * string DEVICE_NAME {@link ScanViewActivityForUsers#DEVICE_NAME} is found, a call is made to connect
     * to the device. Also, the Bluetooth should stop scanning, even if
     * the {@link  ISCNIRScanSDK#SCAN_PERIOD} has not expired
     */
    private final ScanCallback mLeScanCallback = new ScanCallback() {
        @SuppressLint("MissingPermission")
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            BluetoothDevice device = result.getDevice();
            String name = device.getName();
            String preferredNano = getStringPref(mContext, ISCNIRScanSDK.SharedPreferencesKeys.preferredDevice, null);

            if (name != null) {
                if (device.getName().contains(DEVICE_NAME) && device.getAddress().equals(preferredNano)) {
                    mNanoBLEService.connect(device.getAddress());
                    connected = true;
                    scanLeDevice(false);
                }
            }
        }
    };
    private EditText filePrefix;    /**
     * Callback function for preferred Nano scanning. This function provides the instance of the
     * Bluetooth device {@link BluetoothDevice} that was found, it's rssi, and advertisement
     * data (scanRecord).
     * When a Bluetooth device with the advertised name matching the
     * string DEVICE_NAME {@link ScanViewActivityForUsers#DEVICE_NAME} is found, a call is made to connect
     * to the device. Also, the Bluetooth should stop scanning, even if
     * the {@link ISCNIRScanSDK#SCAN_PERIOD} has not expired
     */
    @SuppressLint("MissingPermission")
    private final ScanCallback mPreferredLeScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            BluetoothDevice device = result.getDevice();
            String name = device.getName();
            String preferredNano = getStringPref(mContext, ISCNIRScanSDK.SharedPreferencesKeys.preferredDevice, null);
            if (name != null) {
                if (device.getName().contains(DEVICE_NAME) && device.getAddress().equals(preferredNano)) {
                    if (device.getAddress().equals(preferredDevice)) {
                        mNanoBLEService.connect(device.getAddress());
                        connected = true;
                        scanPreferredLeDevice(false);
                    }
                }
            }
        }
    };
    private TextView tv_normal_scan_conf;
    private ConstraintLayout constraintLayout;
    //endregion
    //region After connect to device
    private ScanMethod Current_Scan_Method = ScanMethod.Normal;
    //When read the activate status of the device, check this from HomeViewActivity->ScanViewActivity trigger or not
    private String mainflag = "";
    private byte[] refCoeff;
    private byte[] refMatrix;
    private Boolean WarmUp = false;
    // 是否启用双面扫描
    private Boolean doubleSidedScanning = false;
    // 是否启用双面含量校对
    private Boolean doubleSidedContentVerification = false;
    // 当前是否是双面扫描第一阶段
    private Boolean IsScanPhase_1 = true;
    private final Button.OnClickListener Continuous_Scan_Stop_Click = new Button.OnClickListener() {
        @Override
        public void onClick(View view) {
            stop_continuous = true;
        }
    };
    /**
     * Get MFG Num (ISCNIRScanSDK.GetMFGNumber() should be called)
     */
    private byte[] MFG_NUM;
    /**
     * Get HW Model (ISCNIRScanSDK.GetHWModel() should be called)
     */
    private String HW_Model = "";
    //endregion
    //region Normal UI component Event
    private final LinearLayout.OnClickListener Normal_Config_Click = new LinearLayout.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (activeConf != null) {
                GotoOtherPage = true;
                Intent activeConfIntent = new Intent(mContext, ActiveConfigDetailViewActivity.class);
                activeConfIntent.putExtra("conf", activeConf);
                startActivity(activeConfIntent);
            }
        }
    };
    //
    //endregion
    //region QuickSet UI component Event
    private final EditText.OnEditorActionListener Quickset_Lamp_Time_OnEditor = new EditText.OnEditorActionListener() {
        @Override
        public boolean onEditorAction(TextView textView, int actionId, KeyEvent event) {
            if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE || event.getAction() == KeyEvent.ACTION_DOWN && event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                return false; // consume.
            }
            return false;
        }
    };
    /**
     * Get  the  current scan config  in the device(ISCNIRScanSDK.ReadCurrentScanConfig(data) should be called)
     */

    private final LinearLayout.OnClickListener Manual_Config_Click = new LinearLayout.OnClickListener() {
        @Override
        public void onClick(View view) {
            if (activeConf != null) {
                GotoOtherPage = true;
                Intent activeConfIntent = new Intent(mContext, ActiveConfigDetailViewActivity.class);
                activeConfIntent.putExtra("conf", activeConf);
                startActivity(activeConfIntent);
            }
        }
    };
    //region Scan
    // 点击了扫描按钮
    private final Button.OnClickListener Button_Scan_Click = new Button.OnClickListener() {
        @Override
        public void onClick(View view) {
            storeStringPref(mContext, ISCNIRScanSDK.SharedPreferencesKeys.prefix, filePrefix.getText().toString());
            long delaytime = 300;
            if (fileNumber.getText().toString().isEmpty()) {
                Dialog_Pane("错误", "请填写文件编号！");
                return;
            }
            // TODO: 2023/8/25 这里记得清空row_data和post_data
            row_data = new String[229];
            showResult.clear();
            adapter.notifyDataSetChanged();
            if (IsScanPhase_1){
                post_data.clear();
            }
            // 关闭scrollview的背景图
//            constraintLayout.setBackgroundColor(Color.TRANSPARENT);
            // 关闭结果背景图
            constraintLayout.setBackgroundColor(Color.TRANSPARENT);
            // 隐藏上次扫描结果listview
            resultListView.setVisibility(View.INVISIBLE);


            // 开始扫描
            PerformScan(delaytime);//Normal
            DisableAllComponent();
            calProgress.setVisibility(View.VISIBLE);
            progressBar.setVisibility(View.VISIBLE);

            btn_scanAndPredict.setText(getString(R.string.scanning));

        }
    };
    /**
     * Get lamp ramp up adc data (ISCNIRScanSDK.GetScanLampRampUpADC() should be called)
     */
    private byte[] Lamp_RAMPUP_ADC_DATA;
    /**
     * Get lamp average adc data (ISCNIRScanSDK.GetLampADCAverage() should be called)
     */
    private byte[] Lamp_AVERAGE_ADC_DATA;
    /**
     * Get lamp ramp up adc timestamp (ISCNIRScanSDK.GetLampADCTimeStamp() should be called)
     */
    private byte[] Lamp_RAMPUP_ADC_TIMESTAMP;

    /*** Filter out all non-numeric, / and-characters***/
    public static String filterDate(String Str) {
        String filter = "[^0-9^A-Z^a-z]"; // Specify the characters to be filtered
        Pattern p = Pattern.compile(filter);
        Matcher m = p.matcher(Str);
        return m.replaceAll("").trim(); // Replace all characters other than those set above
    }

    public static byte[] hexToBytes(String hexString) {

        char[] hex = hexString.toCharArray();
        //change to rawData length by half
        int length = hex.length / 2;
        byte[] rawData = new byte[length];
        for (int i = 0; i < length; i++) {
            //Convert hex data to decimal value
            int high = Character.digit(hex[i * 2], 16);
            int low = Character.digit(hex[i * 2 + 1], 16);
            int value = (high << 4) | low;
            //Complementary with FFFFFFFF
            if (value > 127) value -= 256;
            //Finally change to byte
            rawData[i] = (byte) value;
        }
        return rawData;
    }

    public static String getBytetoString(byte[] configName) {
        byte[] byteChars = new byte[40];
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        byte[] var3 = byteChars;
        int i = byteChars.length;
        for (int var5 = 0; var5 < i; ++var5) {
            byte b = var3[var5];
            byteChars[b] = 0;
        }
        String s = null;
        for (i = 0; i < configName.length; ++i) {
            byteChars[i] = configName[i];
            if (configName[i] == 0) {
                break;
            }
            os.write(configName[i]);
        }
        s = new String(os.toByteArray(), StandardCharsets.UTF_8);
        return s;
    }

    //endregion
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_new_scan);
        mContext = this;
        DEVICE_NAME = ISCNIRScanSDK.getStringPref(mContext, ISCNIRScanSDK.SharedPreferencesKeys.DeviceFilter, "NIR");
        Bundle bundle = getIntent().getExtras();
        mainflag = bundle.getString("main");
        WarmUp = bundle.getBoolean("warmup");
        storeBooleanPref(mContext, ISCNIRScanSDK.SharedPreferencesKeys.LockButton, false);
        // Set up the action bar.
        // 载入服务器API地址和key的自定义数据、启用双面扫描状态、启用双面含量校对、检验员姓名
        loadData();
        calProgress = findViewById(R.id.calProgress);
        calProgress.setVisibility(View.VISIBLE);
        progressBarinsideText = findViewById(R.id.progressBarinsideText);
        connected = false;

        filePrefix = findViewById(R.id.et_prefix);
        btn_scanAndPredict = findViewById(R.id.btn_scanAndPredict);
        btn_scanAndPredict.setClickable(false);
        // 将按钮设置为不可用状态
        btn_scanAndPredict.setBackground(ContextCompat.getDrawable(mContext, R.drawable.scan_button_disabled));
        btn_scanAndPredict.setOnClickListener(Button_Scan_Click);

        // 将界面设置为不可用状态
        setActivityTouchDisable(true);
        row_data = new String[229];
        InitialNormalComponent();
        InitialFabricComposition();
        TitleBarEvent();

        //Bind to the service. This will start it, and call the start command function
        Intent gattServiceIntent = new Intent(this, ISCNIRScanSDK.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
        //region Register all needed broadcast receivers
        LocalBroadcastManager.getInstance(mContext).registerReceiver(ScanDataReadyReceiver, scanDataReadyFilter);
        LocalBroadcastManager.getInstance(mContext).registerReceiver(RefDataReadyReceiver, refReadyFilter);
        LocalBroadcastManager.getInstance(mContext).registerReceiver(NotifyCompleteReceiver, notifyCompleteFilter);
        LocalBroadcastManager.getInstance(mContext).registerReceiver(RefCoeffDataProgressReceiver, requestCalCoeffFilter);
        LocalBroadcastManager.getInstance(mContext).registerReceiver(CalMatrixDataProgressReceiver, requestCalMatrixFilter);
        LocalBroadcastManager.getInstance(mContext).registerReceiver(DisconnReceiver, disconnFilter);
        LocalBroadcastManager.getInstance(mContext).registerReceiver(ScanConfReceiver, scanConfFilter);
        LocalBroadcastManager.getInstance(mContext).registerReceiver(ScanStartedReceiver, scanStartedFilter);
        LocalBroadcastManager.getInstance(mContext).registerReceiver(SpectrumCalCoefficientsReadyReceiver, SpectrumCalCoefficientsReadyFilter);
        LocalBroadcastManager.getInstance(mContext).registerReceiver(RetrunReadActivateStatusReceiver, RetrunReadActivateStatusFilter);
        LocalBroadcastManager.getInstance(mContext).registerReceiver(RetrunActivateStatusReceiver, RetrunActivateStatusFilter);
        LocalBroadcastManager.getInstance(mContext).registerReceiver(DeviceInfoReceiver, new IntentFilter(ISCNIRScanSDK.ACTION_INFO));
        LocalBroadcastManager.getInstance(mContext).registerReceiver(GetUUIDReceiver, new IntentFilter(ISCNIRScanSDK.SEND_DEVICE_UUID));
        LocalBroadcastManager.getInstance(mContext).registerReceiver(ReturnLampRampUpADCReceiver, ReturnLampRampUpFilter);
        LocalBroadcastManager.getInstance(mContext).registerReceiver(ReturnLampADCAverageReceiver, ReturnLampADCAverageFilter);
        LocalBroadcastManager.getInstance(mContext).registerReceiver(ReturnLampRampUpADCTimeStampReceiver, ReturnLampRampUpADCTimeStampFilter);
        LocalBroadcastManager.getInstance(mContext).registerReceiver(ReturnMFGNumReceiver, ReturnMFGNumFilter);
        LocalBroadcastManager.getInstance(mContext).registerReceiver(ReturnHWModelReceiver, ReturnHWModelFilter);
        LocalBroadcastManager.getInstance(mContext).registerReceiver(BackgroundReciver, new IntentFilter(NOTIFY_BACKGROUND));
        LocalBroadcastManager.getInstance(mContext).registerReceiver(ReturnSetLampReceiver, new IntentFilter(ISCNIRScanSDK.SET_LAMPSTATE_COMPLETE));
        LocalBroadcastManager.getInstance(mContext).registerReceiver(ReturnSetPGAReceiver, new IntentFilter(ISCNIRScanSDK.SET_PGA_COMPLETE));
        LocalBroadcastManager.getInstance(mContext).registerReceiver(ReturnSetScanRepeatsReceiver, new IntentFilter(ISCNIRScanSDK.SET_SCANREPEATS_COMPLETE));
    }

    private void loadData() {
        SharedPreferences sharedPreferences = getSharedPreferences("settingsViewStatus", Context.MODE_PRIVATE);
        API_URL = sharedPreferences.getString("API_URL", "http://192.168.115.230:8000/predict/");
        API_KEY = sharedPreferences.getString("API_KEY", "your-secret-api-key");
        sharedPreferences = getSharedPreferences("scanViewUserStatus", Context.MODE_PRIVATE);
        doubleSidedScanning = sharedPreferences.getBoolean("doubleSidedScanning", true);
        doubleSidedContentVerification = sharedPreferences.getBoolean("doubleSidedContentVerification", false);
    }
    private void saveData(){
        SharedPreferences sharedPreferences = getSharedPreferences("scanViewUserStatus", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("doubleSidedScanning", doubleSidedScanning);
        editor.putBoolean("doubleSidedContentVerification", doubleSidedContentVerification);
        editor.apply();
    }

    /**
     * Scans for Bluetooth devices on the specified interval {@link ISCNIRScanSDK#SCAN_PERIOD}.
     * This function uses the handler {@link ScanViewActivityForUsers#mHandler} to delay call to stop
     * scanning until after the interval has expired. The start and stop functions take an
     * LeScanCallback parameter that specifies the callback function when a Bluetooth device
     * has been found {@link ScanViewActivityForUsers#mLeScanCallback}
     *
     * @param enable Tells the Bluetooth adapter {@link ISCNIRScanSDK#mBluetoothAdapter} if
     *               it should start or stop scanning
     */
    @SuppressLint("MissingPermission")
    private void scanLeDevice(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (mBluetoothLeScanner != null) {
                        mBluetoothLeScanner.stopScan(mLeScanCallback);
                        if (!connected) {
                            notConnectedDialog();
                        }
                    }
                }
            }, ISCNIRScanSDK.SCAN_PERIOD);
            if (mBluetoothLeScanner != null) {
                mBluetoothLeScanner.startScan(mLeScanCallback);
            } else {
                finish();
                Toast.makeText(ScanViewActivityForUsers.this, "Please ensure Bluetooth is enabled and try again", Toast.LENGTH_SHORT).show();
            }
        } else {
            mBluetoothLeScanner.stopScan(mLeScanCallback);
        }
    }

    /**
     * Scans for preferred Nano devices on the specified interval {@link ISCNIRScanSDK#SCAN_PERIOD}.
     * This function uses the handler {@link ScanViewActivityForUsers#mHandler} to delay call to stop
     * scanning until after the interval has expired. The start and stop functions take an
     * LeScanCallback parameter that specifies the callback function when a Bluetooth device
     * has been found {@link ScanViewActivityForUsers#mPreferredLeScanCallback}
     *
     * @param enable Tells the Bluetooth adapter {@link ISCNIRScanSDK#mBluetoothAdapter} if
     *               it should start or stop scanning
     */
    @SuppressLint("MissingPermission")
    private void scanPreferredLeDevice(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mBluetoothLeScanner.stopScan(mPreferredLeScanCallback);
                    if (!connected) {

                        scanLeDevice(true);
                    }
                }
            }, ISCNIRScanSDK.SCAN_PERIOD);
            if (mBluetoothLeScanner == null) {
                notConnectedDialog();
            } else {
                mBluetoothLeScanner.startScan(mPreferredLeScanCallback);
            }

        } else {
            mBluetoothLeScanner.stopScan(mPreferredLeScanCallback);
        }
    }

    /**
     * Define FW LEVEL according to tiva version
     *
     * @param Tivarev Tiva version of the device
     */
    private ISCNIRScanSDK.FW_LEVEL_STANDARD GetFWLevelStandard(String Tivarev) {
        String[] TivaArray = Tivarev.split(Pattern.quote("."));
        String[] split_hw = HWrev.split("\\.");
        fw_level_standard = ISCNIRScanSDK.FW_LEVEL_STANDARD.LEVEL_0;
        if (Integer.parseInt(TivaArray[1]) >= 5 && split_hw[0].equals("F")) {
                /*New Applications:
                  1. Use new command to read ADC value and timestamp
                 */
            fw_level_standard = ISCNIRScanSDK.FW_LEVEL_STANDARD.LEVEL_5;//>=2.5.X and main board ="F"
        } else if (Integer.parseInt(TivaArray[1]) >= 5) {
                /*New Applications:
                  1. Support get pga
                 */
            fw_level_standard = ISCNIRScanSDK.FW_LEVEL_STANDARD.LEVEL_4;//>=2.5.X
        } else if (Integer.parseInt(TivaArray[1]) >= 4 && Integer.parseInt(TivaArray[2]) >= 3 && split_hw[0].equals("F")) {
                /*New Applications:
                  1. Support read ADC value
                 */
            fw_level_standard = ISCNIRScanSDK.FW_LEVEL_STANDARD.LEVEL_3;//>=2.4.4 and main board ="F"
        } else if ((Integer.parseInt(TivaArray[1]) >= 4 && Integer.parseInt(TivaArray[2]) >= 3) || Integer.parseInt(TivaArray[1]) >= 5) {
                /*New Applications:
                  1. Add Lock Button
                 */
            fw_level_standard = ISCNIRScanSDK.FW_LEVEL_STANDARD.LEVEL_2;//>=2.4.4
        } else if ((TivaArray.length == 3 && Integer.parseInt(TivaArray[1]) >= 1) || (TivaArray.length == 4 && Integer.parseInt(TivaArray[3]) >= 67))//>=2.1.0.67
        {
            //New Applications:
            // 1. Support activate state

            fw_level_standard = ISCNIRScanSDK.FW_LEVEL_STANDARD.LEVEL_1;
        } else {
            fw_level_standard = ISCNIRScanSDK.FW_LEVEL_STANDARD.LEVEL_0;
        }
        return fw_level_standard;
    }

    private ISCNIRScanSDK.FW_LEVEL_EXT GetFWLevelEXT(String Tivarev) {
        String[] TivaArray = Tivarev.split(Pattern.quote("."));
        String[] split_hw = HWrev.split("\\.");
        fw_level_ext = ISCNIRScanSDK.FW_LEVEL_EXT.LEVEL_EXT_1;
        if (Integer.parseInt(TivaArray[1]) >= 5 && split_hw[0].equals("O")) {
                 /*New Applications:
                  1. Use new command to read ADC value and timestamp
                 */
            fw_level_ext = ISCNIRScanSDK.FW_LEVEL_EXT.LEVEL_EXT_4;//>=3.5.X and main board = "O"
        } else if (Integer.parseInt(TivaArray[1]) >= 5) {
                 /*New Applications:
                  1. Support get pga
                 */
            fw_level_ext = ISCNIRScanSDK.FW_LEVEL_EXT.LEVEL_EXT_3;//>=3.5.X
        } else if (Integer.parseInt(TivaArray[1]) >= 3 && split_hw[0].equals("O")) {
                 /*New Applications:
                  1. Support read ADC value
                 */
            fw_level_ext = ISCNIRScanSDK.FW_LEVEL_EXT.LEVEL_EXT_2;//>=3.3.0 and main board = "O"
        } else if (Integer.parseInt(TivaArray[1]) >= 3) {
                /*New Applications:
                  1. Add Lock Button
                 */
            fw_level_ext = ISCNIRScanSDK.FW_LEVEL_EXT.LEVEL_EXT_1;//>=3.3.0
        } else if (Integer.parseInt(TivaArray[1]) == 2 && Integer.parseInt(TivaArray[2]) == 1)
            fw_level_ext = ISCNIRScanSDK.FW_LEVEL_EXT.LEVEL_EXT_1;//==3.2.1

        return fw_level_ext;
    }

    private ISCNIRScanSDK.FW_LEVEL_EXT_PLUS GetFWLevelEXTPLUS(String Tivarev) {
        String[] TivaArray = Tivarev.split(Pattern.quote("."));
        String[] split_hw = HWrev.split("\\.");
        fw_level_ext_plus = ISCNIRScanSDK.FW_LEVEL_EXT_PLUS.LEVEL_EXT_PLUS_1;
        return fw_level_ext_plus;
    }


    /**
     * Determine the wavelength range of the device and parameter initialization
     */
    private void InitParameter() {
        if (isExtendVer) {
            minWavelength = 1350;
            maxWavelength = 2150;
            MINWAV = 1350;
            MAXWAV = 2150;
        } else if (isExtendVer_PLUS) {
            minWavelength = 1600;
            maxWavelength = 2400;
            MINWAV = 1600;
            MAXWAV = 2400;
        } else {
            minWavelength = 900;
            maxWavelength = 1700;
            MINWAV = 900;
            MAXWAV = 1700;
        }
       //not support lock button
        if (!isExtendVer_PLUS && !isExtendVer && fw_level_standard.compareTo(ISCNIRScanSDK.FW_LEVEL_STANDARD.LEVEL_1) <= 0)
            storeBooleanPref(mContext, ISCNIRScanSDK.SharedPreferencesKeys.LockButton, false);
    }

    private void CheckIsOldTIVA() {
        String[] TivaArray = Tivarev.split(Pattern.quote("."));
        try {
            if (!isExtendVer_PLUS && !isExtendVer && (Integer.parseInt(TivaArray[1]) < 4 || Integer.parseInt(TivaArray[1]) < 4))//Tiva <2.4.4(the newest version)
            {
                isOldTiva = true;
                Dialog_Pane_OldTIVA("Firmware Out of Date", "You must update the firmware on your NIRScan Nano to make this App working correctly!\n" + "FW required version at least V2.4.4\nDetected version is V" + Tivarev + "\nDo you still want to continue?");
            } else isOldTiva = false;
        } catch (Exception e) {

        }
    }

    /**
     * Initial chart view pager and title bar event
     */
    private void TitleBarEvent() {
        //Set up title bar and  enable tab navigation
        ActionBar ab = getActionBar();
        if (ab != null) {
            ab.setDisplayHomeAsUpEnabled(true);
            ab.setTitle(getString(R.string.new_scan));
            ab.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

            mViewPager = findViewById(R.id.viewpager);
            mViewPager.setOffscreenPageLimit(2);

            // Create a tab listener that is called when the user changes tabs.
            ActionBar.TabListener tl = new ActionBar.TabListener() {
                @Override
                public void onTabSelected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
                    //1.if select tab0 then scan, onTabSelected can't invoke. But select other tab can invoke.
                    if (isScan) {
                        if (tabPosition == 0) //2. select tab0 then scan. Choose tab1.at this time isscan =true but tabPosition will be equal to 0 will cause page error.
                        //So if tabPosition is 0, it will choose to do mViewPager.setCurrentItem (tab.getPosition ()); to see the current state
                        {
                            mViewPager.setCurrentItem(tab.getPosition());
                        } else//The tabPosition will record the current tab and then update after the scan
                        {
                            mViewPager.setCurrentItem(tabPosition);
                        }
                        isScan = false;
                    } else {
                        mViewPager.setCurrentItem(tab.getPosition());
                    }
                }

                @Override
                public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
                }

                @Override
                public void onTabReselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
                }
            };
            // Add 3 tabs, specifying the tab's text and TabListener
            for (int i = 0; i < 4; i++) {
                ab.addTab(ab.newTab().setText(getResources().getStringArray(R.array.graph_tab_index)[i]).setTabListener(tl));
            }
        }
    }

    /**
     * Inflate the options menu so that user actions are present
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_new_scan, menu);
        mMenu = menu;
        mMenu.findItem(R.id.action_settings).setEnabled(false);
        mMenu.findItem(R.id.action_key).setEnabled(false);
        return true;
    }

    /**
     * Handle the selection of a menu item.
     * In this case, the user has the ability to access settings while the Nano is connected
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            GotoOtherPage = true;
            ChangeLampState();
            //avoid conflict when go to scan config page
            LocalBroadcastManager.getInstance(mContext).unregisterReceiver(ScanConfReceiver);
            LocalBroadcastManager.getInstance(mContext).unregisterReceiver(ScanConfSizeReceiver);
            LocalBroadcastManager.getInstance(mContext).unregisterReceiver(GetActiveScanConfReceiver);
            LocalBroadcastManager.getInstance(mContext).unregisterReceiver(WriteScanConfigStatusReceiver);
            LocalBroadcastManager.getInstance(mContext).unregisterReceiver(GetDeviceStatusReceiver);

            Intent configureIntent = new Intent(mContext, ConfigureViewActivity.class);
            startActivity(configureIntent);
        }
        if (id == R.id.action_key) {
            GotoOtherPage = true;
            ChangeLampState();
            LocalBroadcastManager.getInstance(mContext).unregisterReceiver(RetrunReadActivateStatusReceiver);
            Intent configureIntent = new Intent(mContext, ActivationViewActivity.class);
            startActivity(configureIntent);
            LocalBroadcastManager.getInstance(mContext).unregisterReceiver(RetrunActivateStatusReceiver);
        }
        if (id == android.R.id.home) {
            this.finish();
        }
        return super.onOptionsItemSelected(item);
    }

    //endregion
    //region Initial Component and control
    // 初始化织物成分录入界面
    private void InitialFabricComposition() {
        // 初始化双面扫描按钮
        btn_doubleside = findViewById(R.id.btn_doubleside);
        btn_doubleside.setChecked(doubleSidedScanning);
        btn_doubleside.setOnClickListener((e) -> {
            // 更新双面扫描状态
            doubleSidedScanning = btn_doubleside.isChecked();
        });
        // 初始化双面扫描结果验证
        btn_verification = findViewById(R.id.btn_verification);
        btn_verification.setChecked(doubleSidedContentVerification);
        btn_verification.setOnClickListener((e) -> {
            // 更新双面扫描结果验证状态
            doubleSidedContentVerification = btn_verification.isChecked();
        });

        adapter = new ArrayAdapter<>(this, R.layout.result_list_item, showResult);
        fileNumber = findViewById(R.id.fileNumber);
        // 扫描结果ListView
        resultListView = findViewById(R.id.resultListView);
        progressBar = findViewById(R.id.progressBar);
        constraintLayout = findViewById(R.id.cl_resultView);
        resultListView.setAdapter(adapter);

        progressBar.setVisibility(View.INVISIBLE);
        resultListView.setVisibility(View.VISIBLE);

    }

    private int getMaxNumberFromFilenames() {
        int maxNumber = -1; // 初始化为-1，表示未找到任何匹配的整数

        // 构建目标路径
        File directory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "/ISC_Report/");

        // 获取该目录下的所有文件
        File[] files = directory.listFiles();

        if (files != null) {
            // 定义正则表达式来匹配"_数字.csv"模式的字符串
            Pattern pattern = Pattern.compile("_([0-9]+)\\.csv$");

            for (File file : files) {
//                System.out.println("FileName:" + file.getName());
                if (file.isFile() && file.getName().endsWith(".csv")) {
                    Matcher matcher = pattern.matcher(file.getName());
                    if (matcher.find()) {
                        int number = Integer.parseInt(matcher.group(1));
                        if (number > maxNumber) {
                            maxNumber = number;
                        }
                    }
                }
            }
        }

        return maxNumber;
    }

    /**
     * Initial the component of the normal scan mode
     */
    private void InitialNormalComponent() {
        // 扫描配置内容
        tv_normal_scan_conf = findViewById(R.id.tv_scan_conf);
        // 扫描配置
        ly_normal_config = findViewById(R.id.ll_conf);
        ly_normal_config.setClickable(false);
        ly_normal_config.setOnClickListener(Normal_Config_Click);

    }

    private void DisableLinearComponet(LinearLayout layout) {

        for (int i = 0; i < layout.getChildCount(); i++) {
            View child = layout.getChildAt(i);
            child.setEnabled(false);
        }
    }

    private void DisableAllComponent() {
        //normal------------------------------------------------
        LinearLayout layout = findViewById(R.id.ll_fileNumberLayout);
        DisableLinearComponet(layout);
        layout = findViewById(R.id.ll_user);
        DisableLinearComponet(layout);
        layout = findViewById(R.id.ll_conf);
        DisableLinearComponet(layout);
        layout = findViewById(R.id.ll_doubleSideScan);
        DisableLinearComponet(layout);
        layout = findViewById(R.id.ll_double_side);
        DisableLinearComponet(layout);

        ly_normal_config.setClickable(false);
        btn_scanAndPredict.setClickable(false);
        btn_scanAndPredict.setBackground(ContextCompat.getDrawable(mContext, R.drawable.scan_button_disabled));

        mMenu.findItem(R.id.action_settings).setEnabled(false);
        mMenu.findItem(R.id.action_key).setEnabled(false);
    }

    private void EnableLinearComponet(LinearLayout layout) {

        for (int i = 0; i < layout.getChildCount(); i++) {
            View child = layout.getChildAt(i);
            child.setEnabled(true);
        }
    }

    private void EnableAllComponent() {
        //normal------------------------------------------
        LinearLayout layout = findViewById(R.id.ll_fileNumberLayout);
        DisableLinearComponet(layout);
        layout = findViewById(R.id.ll_user);
        EnableLinearComponet(layout);
        layout = findViewById(R.id.ll_conf);
        EnableLinearComponet(layout);
        layout = findViewById(R.id.ll_doubleSideScan);
        EnableLinearComponet(layout);
        layout = findViewById(R.id.ll_double_side);
        EnableLinearComponet(layout);


        //------------------------------------------
        ly_normal_config.setClickable(true);
        btn_scanAndPredict.setClickable(true);
        btn_scanAndPredict.setBackground(ContextCompat.getDrawable(mContext, R.drawable.scan_button));

        mMenu.findItem(R.id.action_settings).setEnabled(true);
        mMenu.findItem(R.id.action_key).setEnabled(true);
    }

    /**
     * Unlock device will open all scan mode
     */
    private void openFunction() {

        Current_Scan_Method = ScanMethod.Normal;
        btn_scanAndPredict.setClickable(true);
        btn_scanAndPredict.setBackground(ContextCompat.getDrawable(mContext, R.drawable.scan_button));
        setActivityTouchDisable(false);

    }

    /**
     * Lock device can only use normal scan
     */
    private void closeFunction() {
        findViewById(R.id.layout_normal).setVisibility(View.VISIBLE);//
        Current_Scan_Method = ScanMethod.Normal;
        btn_scanAndPredict.setClickable(true);
        btn_scanAndPredict.setBackground(ContextCompat.getDrawable(mContext, R.drawable.scan_button));
        setActivityTouchDisable(false);
    }

    private void ChangeLampState() {
        if (WarmUp) {
            ISCNIRScanSDK.ControlLamp(ISCNIRScanSDK.LampState.AUTO);
            WarmUp = false;
        }
    }
    /**
     * Finish scan will write scan data to .csv and setting UI
     * Continuous scan will trigger scan event to scan data
     */
    private void DoScanComplete() {
        long delaytime = 0;
        Boolean isLockButton = getBooleanPref(mContext, ISCNIRScanSDK.SharedPreferencesKeys.LockButton, false);
        if (isLockButton) //User open lock button on scan setting
            ISCNIRScanSDK.ControlPhysicalButton(ISCNIRScanSDK.PhysicalButton.Lock);
        else ISCNIRScanSDK.ControlPhysicalButton(ISCNIRScanSDK.PhysicalButton.Unlock);
        delaytime = 300;
        if (!getStringPref(mContext, ISCNIRScanSDK.SharedPreferencesKeys.Activacatestatus, "").contains("Activated")) {
            closeFunction();
        }
        writeCSV(Scan_Spectrum_Data);
        //------------------------------------------------------------------------------------------------------------
        calProgress.setVisibility(View.GONE);
        progressBarinsideText.setVisibility(View.GONE);
        // 当启用了双面扫描，且当前为第一阶段时
        if (doubleSidedScanning && IsScanPhase_1) {
            IsScanPhase_1 = false;
            btn_scanAndPredict.setText("请扫描另一面");
            btn_scanAndPredict.setClickable(true);
            btn_scanAndPredict.setBackground(ContextCompat.getDrawable(mContext, R.drawable.scan_button));
            setActivityTouchDisable(false);
            Toast.makeText(mContext, "请将样品翻面，再次扫描！", Toast.LENGTH_SHORT).show();
        } else {
            // 显示预测结果
            resultListView.setVisibility(View.VISIBLE);
//            resultListView.setBackgroundColor(Color.TRANSPARENT);
            // 发送POST请求到API接口
            postCsvData(post_data);
            // 将当前的文件编号加1
            int fileNum = Integer.parseInt(fileNumber.getText().toString());
            fileNumber.setText(String.valueOf(fileNum + 1));
            // 设置为双面扫描的第一阶段
            IsScanPhase_1 = true;
            btn_scanAndPredict.setText(getString(R.string.scan_predict));
//            EnableAllComponent();
        }
        //Tiva version <2.1.0.67
        if (!isExtendVer_PLUS && !isExtendVer && fw_level_standard.compareTo(ISCNIRScanSDK.FW_LEVEL_STANDARD.LEVEL_0) == 0)
            closeFunction();
    }

    public void postCsvData(List<String[]> post_data) {
        StringBuilder csvBuilder = new StringBuilder();

        for (String[] row : post_data) {
            csvBuilder.append(String.join(",", row)).append("\n");
        }

        String csvContent = csvBuilder.toString();

        // Construct the complete URL with query parameters
        String completeUrl = API_URL + "?gpu_count=1&api_key=" + API_KEY;

        // Fix: Create a multipart request body
        RequestBody fileBody = RequestBody.create(MediaType.parse("text/csv"), csvContent);
        MultipartBody requestBody = new MultipartBody.Builder().setType(MultipartBody.FORM).addFormDataPart("file", "data.csv", fileBody).build();

        Request request = new Request.Builder().url(completeUrl).post(requestBody).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                System.out.println("网络异常：" + e.getMessage());
                // 弹出警告窗口
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // 更新UI的代码，例如：
                        Dialog_Pane_Finish("网络异常", "请检查网络连接！\n详情：" + e.getMessage());
                        progressBar.setVisibility(View.INVISIBLE);
                        EnableAllComponent();
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    // Handle successful response
                    if (response.code() == 200) {
                        System.out.println("请求成功！");
                        String result = response.body().string();
//                        System.out.println("response.body().string() = " + result);
                        parsingJSON(result);
//                        showResult.add("预测结果：" + result);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                // 更新UI的代码，例如：
                                adapter.notifyDataSetChanged();
                                progressBar.setVisibility(View.INVISIBLE);
                                // 或其他UI更新操作
                                EnableAllComponent();
                            }
                        });
                    }
                    // Do something with the result
                } else {
                    // Handle error response
                    System.out.println("请求失败:" + response.message());
                    // 弹出警告窗口
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            // 更新UI的代码，例如：
                            Dialog_Pane_Finish("请求失败", "请检查内容格式！\n详情：" + response.message());
                            progressBar.setVisibility(View.INVISIBLE);
                            EnableAllComponent();
                        }
                    });
                }
            }
        });
    }
    private void parsingJSON(String jsonResponse){
        try {
            JSONArray jsonArray = new JSONArray(jsonResponse);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONArray innerArray = jsonArray.getJSONArray(i);
                String fileName = innerArray.getString(0);
                String result =  (i + 1) +"：" + fileName;
                // 提取成分数据
                for (int j = 1; j < innerArray.length(); j++) {
                    String component = innerArray.getString(j);
                    System.out.println("Component: " + component);
                    result += "\n" + component;
                }
                showResult.add(result);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
    private String ErrorByteTransfer() {
        String ErrorMsg = "";
        int ErrorInt = errbyte[0] & 0xFF | (errbyte[1] << 8);
        if ((ErrorInt & 0x00000001) > 0)//Scan Error
        {
            ErrorMsg += "Scan Error : ";
            int ErrDetailInt = errbyte[4] & 0xFF;
            if ((ErrDetailInt & 0x01) > 0) ErrorMsg += "DLPC150 Boot Error Detected.    ";
            if ((ErrDetailInt & 0x02) > 0) ErrorMsg += "DLPC150 Init Error Detected.    ";
            if ((ErrDetailInt & 0x04) > 0) ErrorMsg += "DLPC150 Lamp Driver Error Detected.    ";
            if ((ErrDetailInt & 0x08) > 0) ErrorMsg += "DLPC150 Crop Image Failed.    ";
            if ((ErrDetailInt & 0x10) > 0) ErrorMsg += "ADC Data Error.    ";
            if ((ErrDetailInt & 0x20) > 0) ErrorMsg += "Scan Config Invalid.    ";
            if ((ErrDetailInt & 0x40) > 0) ErrorMsg += "Scan Pattern Streaming Error.    ";
            if ((ErrDetailInt & 0x80) > 0) ErrorMsg += "DLPC150 Read Error.    ";
            ErrorMsg += ",";
        }
        if ((ErrorInt & 0x00000002) > 0)  // ADC Error
        {
            ErrorMsg += "ADC Error : ";
            int ErrDetailInt = errbyte[5] & 0xFF;
            if (ErrDetailInt == 1) ErrorMsg += "Timeout Error.    ";
            else if (ErrDetailInt == 2) ErrorMsg += "PowerDown Error.    ";
            else if (ErrDetailInt == 3) ErrorMsg += "PowerUp Error.    ";
            else if (ErrDetailInt == 4) ErrorMsg += "Standby Error.    ";
            else if (ErrDetailInt == 5) ErrorMsg += "WakeUp Error.    ";
            else if (ErrDetailInt == 6) ErrorMsg += "Read Register Error.    ";
            else if (ErrDetailInt == 7) ErrorMsg += "Write Register Error.    ";
            else if (ErrDetailInt == 8) ErrorMsg += "Configure Error.    ";
            else if (ErrDetailInt == 9) ErrorMsg += "Set Buffer Error.    ";
            else if (ErrDetailInt == 10) ErrorMsg += "Command Error.    ";
            else if (ErrDetailInt == 11) ErrorMsg += "Set PGA Error.    ";
            ErrorMsg += ",";
        }
        if ((ErrorInt & 0x00000004) > 0)  // SD Card Error
        {
            ErrorMsg += "SD Card Error.";
            ErrorMsg += ",";
        }
        if ((ErrorInt & 0x00000008) > 0)  // EEPROM Error
        {
            ErrorMsg += "EEPROM Error.";
            ErrorMsg += ",";
        }
        if ((ErrorInt & 0x00000010) > 0)  // BLE Error
        {
            ErrorMsg += "Bluetooth Error.";
            ErrorMsg += ",";
        }
        if ((ErrorInt & 0x00000020) > 0)  // Spectrum Library Error
        {
            ErrorMsg += "Spectrum Library Error.";
            ErrorMsg += ",";
        }
        if ((ErrorInt & 0x00000040) > 0)  // Hardware Error
        {
            ErrorMsg += "HW Error : ";
            int ErrDetailInt = errbyte[11] & 0xFF;
            if (ErrDetailInt == 1) ErrorMsg += "DLPC150 Error.    ";
            else if (ErrDetailInt == 2) ErrorMsg += "Read UUID Error.    ";
            else if (ErrDetailInt == 3) ErrorMsg += "Flash Initial Error.    ";
            ErrorMsg += ",";
        }
        if ((ErrorInt & 0x00000080) > 0)  // TMP Sensor Error
        {
            ErrorMsg += "TMP Error : ";
            int ErrDetailInt = errbyte[12] & 0xFF;
            if (ErrDetailInt == 1) ErrorMsg += "Invalid Manufacturing ID.    ";
            else if (ErrDetailInt == 2) ErrorMsg += "Invalid Device ID.    ";
            else if (ErrDetailInt == 3) ErrorMsg += "Reset Error.    ";
            else if (ErrDetailInt == 4) ErrorMsg += "Read Register Error.    ";
            else if (ErrDetailInt == 5) ErrorMsg += "Write Register Error.    ";
            else if (ErrDetailInt == 6) ErrorMsg += "Timeout Error.    ";
            else if (ErrDetailInt == 7) ErrorMsg += "I2C Error.    ";
            ErrorMsg += ",";
        }
        if ((ErrorInt & 0x00000100) > 0)  // HDC Sensor Error
        {
            ErrorMsg += "HDC Error : ";
            int ErrDetailInt = errbyte[13] & 0xFF;
            if (ErrDetailInt == 1) ErrorMsg += "Invalid Manufacturing ID.    ";
            else if (ErrDetailInt == 2) ErrorMsg += "Invalid Device ID.    ";
            else if (ErrDetailInt == 3) ErrorMsg += "Reset Error.    ";
            else if (ErrDetailInt == 4) ErrorMsg += "Read Register Error.    ";
            else if (ErrDetailInt == 5) ErrorMsg += "Write Register Error.    ";
            else if (ErrDetailInt == 6) ErrorMsg += "Timeout Error.    ";
            else if (ErrDetailInt == 7) ErrorMsg += "I2C Error.    ";
            ErrorMsg += ",";
        }
        if ((ErrorInt & 0x00000200) > 0)  // Battery Error
        {
            ErrorMsg += "Battery Error : ";
            int ErrDetailInt = errbyte[14] & 0xFF;
            if (ErrDetailInt == 0x01) ErrorMsg += "Battery Low.    ";
            ErrorMsg += ",";
        }
        if ((ErrorInt & 0x00000400) > 0)  // Insufficient Memory Error
        {
            ErrorMsg += "Not Enough Memory.";
            ErrorMsg += ",";
        }
        if ((ErrorInt & 0x00000800) > 0)  // UART Error
        {
            ErrorMsg += "UART Error.";
            ErrorMsg += ",";
        }
        if ((ErrorInt & 0x00001000) > 0)   // System Error
        {
            ErrorMsg += "System Error : ";
            int ErrDetailInt = errbyte[17] & 0xFF;
            if ((ErrDetailInt & 0x01) > 0) ErrorMsg += "Unstable Lamp ADC.    ";
            if ((ErrDetailInt & 0x02) > 0) ErrorMsg += "Unstable Peak Intensity.    ";
            if ((ErrDetailInt & 0x04) > 0) ErrorMsg += "ADS1255 Error.    ";
            if ((ErrDetailInt & 0x08) > 0) ErrorMsg += "Auto PGA Error.    ";

            ErrDetailInt = errbyte[18] & 0xFF;
            if ((ErrDetailInt & 0x01) > 0) ErrorMsg += "Unstable Scan in Repeated times.    ";
            ErrorMsg += ",";
        }
        if (ErrorMsg.equals("")) ErrorMsg = "Not Found";
        return ErrorMsg;
    }

    /**
     * Write scan data to CSV file
     *
     * @param scanResults the {@link ISCNIRScanSDK.ScanResults} structure to save
     */
    private void writeCSV(ISCNIRScanSDK.ScanResults scanResults) {
        Boolean HaveError = false;
        int scanType;
        int numSections;
        String[] widthnm = {"", "", "2.34", "3.51", "4.68", "5.85", "7.03", "8.20", "9.37", "10.54", "11.71", "12.88", "14.05", "15.22", "16.39", "17.56", "18.74", "19.91", "21.08", "22.25", "23.42", "24.59", "25.76", "26.93", "28.10", "29.27", "30.44", "31.62", "32.79", "33.96", "35.13", "36.30", "37.47", "38.64", "39.81", "40.98", "42.15", "43.33", "44.50", "45.67", "46.84", "48.01", "49.18", "50.35", "51.52", "52.69", "53.86", "55.04", "56.21", "57.38", "58.55", "59.72", "60.89"};
        String[] widthnm_plus = {"", "", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20", "21", "22", "23", "24", "25", "26", "27", "28", "29", "30", "31", "32", "33", "34", "35", "36", "37", "38", "39", "40", "41", "42", "43", "44", "45", "46", "47", "48", "49", "50", "51", "52", "53", "54", "55", "56", "57", "58", "59", "60", "61"};
        String[] exposureTime = {"0.635", "1.27", " 2.54", " 5.08", "15.24", "30.48", "60.96"};
        int index = 0;
        double temp;
        double humidity;
        //-------------------------------------------------
        String newdate = "";
        String[][] CSV = new String[35][15];
        for (int i = 0; i < 35; i++)
            for (int j = 0; j < 15; j++)
                CSV[i][j] = ",";

        numSections = ISCNIRScanSDK.ScanConfigInfo.numSections[0];
        scanType = ISCNIRScanSDK.ScanConfigInfo.scanType[0];
        //----------------------------------------------------------------
        String configname = getBytetoString(ISCNIRScanSDK.ScanConfigInfo.configName);
        CSV[14][8] = ISCNIRScanSDK.ReferenceInfo.refday[0] + "/" + ISCNIRScanSDK.ReferenceInfo.refday[1] + "/" + ISCNIRScanSDK.ReferenceInfo.refday[2] + "T" + ISCNIRScanSDK.ReferenceInfo.refday[3] + ":" + ISCNIRScanSDK.ReferenceInfo.refday[4] + ":" + ISCNIRScanSDK.ReferenceInfo.refday[5];

        // Section information field names
        CSV[0][0] = "***Scan Config Information***,";
        CSV[1][0] = "Scan Config Name:,";
        CSV[2][0] = "Scan Config Type:,";
        CSV[3][0] = "Section Config Type:,";
        CSV[4][0] = "Start Wavelength (nm):,";
        CSV[5][0] = "End Wavelength (nm):,";
        CSV[6][0] = "Pattern Width (nm):,";
        CSV[7][0] = "Exposure (ms):,";
        CSV[8][0] = "Digital Resolution:,";
        CSV[9][0] = "Num Repeats:,";
        CSV[10][0] = "PGA Gain:,";
        CSV[11][0] = "System Temp (C):,";
        CSV[12][0] = "Humidity (%):,";
        CSV[13][0] = "Battery Capacity (%):,";
        if ((isExtendVer && fw_level_ext.compareTo(ISCNIRScanSDK.FW_LEVEL_EXT.LEVEL_EXT_2) >= 0) || (!isExtendVer && fw_level_standard.compareTo(ISCNIRScanSDK.FW_LEVEL_STANDARD.LEVEL_3) >= 0))
            CSV[14][0] = "Lamp ADC:,";
        else CSV[14][0] = "Lamp Indicator:,";
        CSV[15][0] = "Data Date-Time:,";
        CSV[16][0] = "Total Measurement Time in sec:,";

        CSV[1][1] = configname + ",";
        CSV[2][1] = "Slew,";
        CSV[2][2] = "Num Section:,";
        CSV[2][3] = numSections + ",";

        for (int i = 0; i < numSections; i++) {
            if (ISCNIRScanSDK.ScanConfigInfo.sectionScanType[i] == 0) CSV[3][i + 1] = "Column,";
            else CSV[3][i + 1] = "Hadamard,";
            CSV[4][i + 1] = ISCNIRScanSDK.ScanConfigInfo.sectionWavelengthStartNm[i] + ",";
            CSV[5][i + 1] = ISCNIRScanSDK.ScanConfigInfo.sectionWavelengthEndNm[i] + ",";
            index = ISCNIRScanSDK.ScanConfigInfo.sectionWidthPx[i];
            if (isExtendVer_PLUS) CSV[6][i + 1] = widthnm_plus[index] + ",";
            else CSV[6][i + 1] = widthnm[index] + ",";
            index = ISCNIRScanSDK.ScanConfigInfo.sectionExposureTime[i];
            CSV[7][i + 1] = exposureTime[index] + ",";
            CSV[8][i + 1] = ISCNIRScanSDK.ScanConfigInfo.sectionNumPatterns[i] + ",";
        }
        CSV[9][1] = ISCNIRScanSDK.ScanConfigInfo.sectionNumRepeats[0] + ",";
        CSV[10][1] = ISCNIRScanSDK.ScanConfigInfo.pga[0] + ",";
        temp = ISCNIRScanSDK.ScanConfigInfo.systemp[0];
        temp = temp / 100;
        CSV[11][1] = temp + ",";
        humidity = ISCNIRScanSDK.ScanConfigInfo.syshumidity[0];
        humidity = humidity / 100;
        CSV[12][1] = humidity + ",";
        CSV[13][1] = battery + ",";
        CSV[14][1] = ISCNIRScanSDK.ScanConfigInfo.lampintensity[0] + ",";
        CSV[15][1] = ISCNIRScanSDK.ScanConfigInfo.day[0] + "/" + ISCNIRScanSDK.ScanConfigInfo.day[1] + "/" + ISCNIRScanSDK.ScanConfigInfo.day[2] + "T" + ISCNIRScanSDK.ScanConfigInfo.day[3] + ":" + ISCNIRScanSDK.ScanConfigInfo.day[4] + ":" + ISCNIRScanSDK.ScanConfigInfo.day[5] + ",";
        CSV[16][1] = Double.toString((double) MesureScanTime / 1000);
        //General Information
        CSV[17][0] = "***General Information***,";
        CSV[18][0] = "Model Name:,";
        CSV[19][0] = "Serial Number:,";
        CSV[20][0] = "APP Version:,";
        CSV[21][0] = "TIVA Version:,";
        //CSV[22][0] = "DLPC Version:,";
        CSV[22][0] = "UUID:,";
        CSV[23][0] = "Main Board Version:,";
        CSV[24][0] = "Detector Board Version:,";

        CSV[18][1] = model_name + ",";
        CSV[19][1] = serial_num + ",";
        String mfg_num = "";
        try {
            mfg_num = new String(MFG_NUM, StandardCharsets.ISO_8859_1);
            if (!mfg_num.contains("70UB1") && !mfg_num.contains("95UB1")) mfg_num = "";
            else if (mfg_num.contains("95UB1"))//Extended 19 words, standard 18 words
            {
                if (isExtendVer && mfg_num.length() >= 19) mfg_num = mfg_num.substring(0, 19);
                else if (!isExtendVer_PLUS && !isExtendVer && mfg_num.length() >= 18)
                    mfg_num = mfg_num.substring(0, 18);
                else mfg_num = mfg_num.substring(0, mfg_num.length() - 2);
            }
        } catch (Exception e) {
            mfg_num = "";
        }
        CSV[19][2] = mfg_num + ",";
        String version = "";
        int versionCode = 0;
        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            version = pInfo.versionName;
            versionCode = pInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
        }
        CSV[20][1] = version + "." + versionCode;
        CSV[21][1] = Tivarev + ",";
        CSV[22][1] = uuid + ",";
        String[] split_hw = HWrev.split("\\.");
        CSV[23][1] = split_hw[0] + ",";
        CSV[24][1] = split_hw[2] + ",";

        //Reference Scan Information
        CSV[0][7] = "***Reference Scan Information***,";
        CSV[1][7] = "Scan Config Name:,";
        CSV[2][7] = "Scan Config Type:,";
        CSV[3][7] = "Section Config Type:,";
        CSV[4][7] = "Start Wavelength (nm):,";
        CSV[5][7] = "End Wavelength (nm):,";
        CSV[6][7] = "Pattern Width (nm):,";
        CSV[7][7] = "Exposure (ms):,";
        CSV[8][7] = "Digital Resolution:,";
        CSV[9][7] = "Num Repeats:,";
        CSV[10][7] = "PGA Gain:,";
        CSV[11][7] = "System Temp (C):,";
        CSV[12][7] = "Humidity (%):,";
        CSV[13][7] = "Lamp Indicator:,";
        CSV[14][7] = "Data Date-Time:,";
        if (getBytetoString(ISCNIRScanSDK.ReferenceInfo.refconfigName).equals("SystemTest"))
            CSV[1][8] = "Built-in Factory Reference";
        else CSV[1][8] = "Built-in User Reference";
        CSV[2][8] = "Slew,";
        CSV[2][9] = "Num Section:,";
        CSV[2][10] = "1,";
        if (ISCNIRScanSDK.ReferenceInfo.refconfigtype[0] == 0) CSV[3][8] = "Column,";
        else CSV[3][8] = "Hadamard,";
        CSV[4][8] = Double.toString(ISCNIRScanSDK.ReferenceInfo.refstartwav[0]);
        CSV[5][8] = Double.toString(ISCNIRScanSDK.ReferenceInfo.refendwav[0]);
        index = ISCNIRScanSDK.ReferenceInfo.width[0];
        if (isExtendVer_PLUS) CSV[6][8] = widthnm_plus[index] + ",";
        else CSV[6][8] = widthnm[index] + ",";
        index = ISCNIRScanSDK.ReferenceInfo.refexposuretime[0];
        CSV[7][8] = exposureTime[index] + ",";
        CSV[8][8] = ISCNIRScanSDK.ReferenceInfo.numpattren[0] + ",";
        CSV[9][8] = ISCNIRScanSDK.ReferenceInfo.numrepeat[0] + ",";
        CSV[10][8] = Integer.toString(ISCNIRScanSDK.ReferenceInfo.refpga[0]);
        temp = ISCNIRScanSDK.ReferenceInfo.refsystemp[0];
        temp = temp / 100;
        CSV[11][8] = temp + ",";
        humidity = ISCNIRScanSDK.ReferenceInfo.refsyshumidity[0] / 100;
        CSV[12][8] = Double.toString(humidity);
        CSV[13][8] = ISCNIRScanSDK.ReferenceInfo.reflampintensity[0] + ",";
        //Calibration Coefficients
        CSV[17][7] = "***Calibration Coefficients***,";
        CSV[18][7] = "Shift Vector Coefficients:,";
        CSV[19][7] = "Pixel to Wavelength Coefficients:,";
        CSV[21][7] = "***Lamp Usage * **,";
        CSV[22][7] = "Total Time(HH:MM:SS):,";
        CSV[23][7] = "***Device/Error Status***,";
        CSV[24][7] = "Device Status:,";
        CSV[25][7] = "Error status:,";
        CSV[18][8] = ISCNIRScanSDK.ScanConfigInfo.shift_vector_coff[0] + ",";
        CSV[18][9] = ISCNIRScanSDK.ScanConfigInfo.shift_vector_coff[1] + ",";
        CSV[18][10] = ISCNIRScanSDK.ScanConfigInfo.shift_vector_coff[2] + ",";
        CSV[19][8] = ISCNIRScanSDK.ScanConfigInfo.pixel_coff[0] + ",";
        CSV[19][9] = ISCNIRScanSDK.ScanConfigInfo.pixel_coff[1] + ",";
        CSV[19][10] = ISCNIRScanSDK.ScanConfigInfo.pixel_coff[2] + ",";
        CSV[22][8] = TotalLampTime + ",";
        final StringBuilder stringBuilder = new StringBuilder(8);
        for (int i = 3; i >= 0; i--)
            stringBuilder.append(String.format("%02X", devbyte[i]));
        CSV[24][8] = "0x" + stringBuilder;
        final StringBuilder stringBuilder_errorstatus = new StringBuilder(8);
        for (int i = 3; i >= 0; i--)
            stringBuilder_errorstatus.append(String.format("%02X", errbyte[i]));
        CSV[25][8] = "0x" + stringBuilder_errorstatus + ",";
        final StringBuilder stringBuilder_errorcode = new StringBuilder(8);
        for (int i = 0; i < 20; i++) {
            stringBuilder_errorcode.append(String.format("%02X", errbyte[i]));
            if (errbyte[i] != 0) HaveError = true;
        }
        CSV[25][9] = "Error Code:,";
        CSV[25][10] = "0x" + stringBuilder_errorcode + ",";
        CSV[27][0] = "***Scan Data***,";
        // 开始写入扫描数据到数组。
        CSVWriter writer;
        // 文件名前缀
        String prefix = filePrefix.getText().toString();
        if (prefix.equals("")) {
            prefix = "ISC";
        }
        if (android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_REMOVED)) {
            Toast.makeText(ScanViewActivityForUsers.this, "没有找到 SD card.", Toast.LENGTH_SHORT).show();
            return;
        }
        //--------------------------------------
        // 定位到手机文档目录
        File mSDFile = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
        File mFile = new File(mSDFile.getParent() + "/" + mSDFile.getName() + "/ISC_Report");
        //No file exist
        if (!mFile.exists()) {
            mFile.mkdirs();
        }
        // 设置路径权限(报错...)
        mFile.setExecutable(true);
        mFile.setReadable(true);
        mFile.setWritable(true);
        // initiate media scan and put the new things into the path array to
        // make the scanner aware of the location and the files you want to see
        MediaScannerConnection.scanFile(this, new String[]{mFile.toString()}, null, null);
        //-------------------------------------------------------------------------
        // 将文件序号加入文件名中
        String fileNum = fileNumber.getText().toString();
        String csvOS = "";
        CSV[26][9] = "Error Details:,";
        // 根据是否发生错误，设置文件名
        if (HaveError) {
            CSV[26][10] = ErrorByteTransfer();
            csvOS = mSDFile.getParent() + "/" + mSDFile.getName() + "/ISC_Report/" + prefix + "_" + configname + "_" + CurrentTime + "_Error_Detected_" + fileNum + ".csv";
        } else {
            CSV[26][10] = "Not Found,";
            csvOS = mSDFile.getParent() + "/" + mSDFile.getName() + "/ISC_Report/" + prefix + "_" + configname + "_" + CurrentTime + "_" + fileNum + ".csv";
        }
        // 将文件名加入单行数据中
        row_data[0] = "\"" + prefix + "_" + configname + "_" + CurrentTime + "_" + fileNum + ".csv" + "\"";
        try {
            List<String[]> data = new ArrayList<String[]>();
            String buf = "";
            for (int i = 0; i < 28; i++) {
                for (int j = 0; j < 15; j++) {
                    buf += CSV[i][j];
                    if (j == 14) {
                        data.add(new String[]{buf});
                    }
                }
                buf = "";
            }
            data.add(new String[]{"Wavelength (nm),Absorbance (AU),Reference Signal (unitless),Sample Signal (unitless)"});
            int csvIndex;
            for (csvIndex = 0; csvIndex < scanResults.getLength(); csvIndex++) {
                double waves = scanResults.getWavelength()[csvIndex];
                int intens = scanResults.getUncalibratedIntensity()[csvIndex];
                float absorb = (-1) * (float) Math.log10((double) scanResults.getUncalibratedIntensity()[csvIndex] / (double) scanResults.getIntensity()[csvIndex]);
                row_data[csvIndex + 1] = String.valueOf(absorb);
                float reference = (float) Scan_Spectrum_Data.getIntensity()[csvIndex];
                data.add(new String[]{String.valueOf(waves), String.valueOf(absorb), String.valueOf(reference), String.valueOf(intens)});
            }
            // 结尾添加属性信息
            data.add(new String[]{"***End of Scan Data***"});
            // 添加row数据到post_data
            post_data.add(row_data);
            if (isExtendVer_PLUS) data = WriteADCNotTimeStamp_PLUS(data, CSV);
            else if ((!isExtendVer_PLUS && !isExtendVer && fw_level_standard.compareTo(ISCNIRScanSDK.FW_LEVEL_STANDARD.LEVEL_5) == 0) || (isExtendVer && fw_level_ext.compareTo(ISCNIRScanSDK.FW_LEVEL_EXT.LEVEL_EXT_4) == 0))
                data = WriteADCTimeStamp(data, CSV);
            else if ((isExtendVer && (fw_level_ext.compareTo(ISCNIRScanSDK.FW_LEVEL_EXT.LEVEL_EXT_2) == 0 || fw_level_ext.compareTo(ISCNIRScanSDK.FW_LEVEL_EXT.LEVEL_EXT_4) == 0)) || (!isExtendVer_PLUS && !isExtendVer && (fw_level_standard.compareTo(ISCNIRScanSDK.FW_LEVEL_STANDARD.LEVEL_3) == 0 || fw_level_standard.compareTo(ISCNIRScanSDK.FW_LEVEL_STANDARD.LEVEL_5) == 0)))
                data = WriteADCNotTimeStamp(data, CSV);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
                MediaStoreWriteCSV(data, configname, prefix);
            else {
                // initiate media scan and put the new things into the path array to
                // make the scanner aware of the location and the files you want to see
                MediaScannerConnection.scanFile(this, new String[]{csvOS}, null, null);
                writer = new CSVWriter(new FileWriter(csvOS), ',', CSVWriter.NO_QUOTE_CHARACTER);
                writer.writeAll(data);
                writer.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void MediaStoreWriteCSV(List<String[]> data, String configname, String prefix) {
        try {
           // 将文件序号加入文件名中
            String fileNum = fileNumber.getText().toString();

            ContentValues values = new ContentValues();
            values.put(MediaStore.MediaColumns.DISPLAY_NAME, prefix + "_" + configname + "_" + CurrentTime + "_" + fileNum + ".csv");       //file name
            values.put(MediaStore.MediaColumns.MIME_TYPE, "text/comma-separated-values");
            values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS + "/ISC_Report/");
            Uri uri = getContentResolver().insert(MediaStore.Files.getContentUri("external"), values);
            OutputStream outputStream = getContentResolver().openOutputStream(uri);
            int Len = data.size();
            for (int i = 0; i < Len; i++) {
                String datacontent = "";
                for (int j = 0; j < data.get(i).length; j++) {
                    datacontent += data.get(i)[j];
                    if (j < data.get(i).length - 1) datacontent += ",";
                }
                datacontent += "\r\n";
                outputStream.write(datacontent.getBytes());
            }
            outputStream.close();
        } catch (Exception e) {

        }
    }
    private List<String[]> WriteADCTimeStamp(List<String[]> data, String[][] CSV) {
        try {
            data.add(new String[]{""});
            data.add(new String[]{"***Lamp Ramp Up ADC***,"});
            data.add(new String[]{"Timestamp(ms),ADC0,ADC1,ADC2,ADC3"});
            String[] ADC = new String[5];
            int count = 0;
            int timeindex = 0;
            List<Integer> time = new ArrayList<Integer>();
            for (int i = 0; i < Lamp_RAMPUP_ADC_TIMESTAMP.length; i += 4) {
                Integer buftime = (Lamp_RAMPUP_ADC_TIMESTAMP[i + 1] & 0xff) << 8 | Lamp_RAMPUP_ADC_TIMESTAMP[i] & 0xff;
                if (buftime == 0) break;
                else time.add(buftime);
            }
            for (int i = 0; i < Lamp_RAMPUP_ADC_DATA.length; i += 2) {
                int adc_value = (Lamp_RAMPUP_ADC_DATA[i + 1] & 0xff) << 8 | Lamp_RAMPUP_ADC_DATA[i] & 0xff;
                if (adc_value == 0) break;
                ADC[count + 1] = Integer.toString(adc_value);
                count++;
                if (count == 4) {
                    ADC[0] = Integer.toString(time.get(timeindex));
                    timeindex++;
                    data.add(ADC);
                    count = 0;
                    ADC = new String[5];
                }
            }
            data.add(new String[]{""});
            data.add(new String[]{"***Lamp ADC among repeated times***,"});
            data.add(new String[]{"Timestamp(ms),ADC0,ADC1,ADC2,ADC3"});
            ADC = new String[5];
            int[] Average_ADC = new int[4];
            int cal_count = 0;
            count = 0;
            for (int i = 0; i < Lamp_AVERAGE_ADC_DATA.length; i += 2) {
                int adc_value = (Lamp_AVERAGE_ADC_DATA[i + 1] & 0xff) << 8 | Lamp_AVERAGE_ADC_DATA[i] & 0xff;
                if (adc_value == 0) break;
                ADC[count + 1] = Integer.toString(adc_value);
                Average_ADC[count] += adc_value;
                count++;
                if (count == 4) {
                    ADC[0] = Integer.toString(time.get(timeindex));
                    timeindex++;
                    data.add(ADC);
                    cal_count++;
                    count = 0;
                    ADC = new String[5];
                }
            }
            String AverageADC = "Lamp ADC:,";

            for (int i = 0; i < 4; i++) {
                double buf_adc = Average_ADC[i];
                AverageADC += Math.round(buf_adc / cal_count) + ",";
            }
            AverageADC += ",," + CSV[14][7] + CSV[14][8];// add ref data-time data
            data.get(14)[0] = AverageADC;
        } catch (Exception e) {

        }
        return data;
    }
    private List<String[]> WriteADCNotTimeStamp(List<String[]> data, String[][] CSV) {
        try {
            data.add(new String[]{""});
            data.add(new String[]{"***Lamp Ramp Up ADC***,"});
            data.add(new String[]{"ADC0,ADC1,ADC2,ADC3"});
            String[] ADC = new String[4];
            int count = 0;
            for (int i = 0; i < Lamp_RAMPUP_ADC_DATA.length; i += 2) {
                int adc_value = (Lamp_RAMPUP_ADC_DATA[i + 1] & 0xff) << 8 | Lamp_RAMPUP_ADC_DATA[i] & 0xff;
                if (adc_value == 0) break;
                ADC[count] = Integer.toString(adc_value);
                count++;
                if (count == 4) {
                    data.add(ADC);
                    count = 0;
                    ADC = new String[4];
                }
            }
            //-----------------------------------
            data.add(new String[]{""});
            data.add(new String[]{"***Lamp ADC among repeated times***,"});
            data.add(new String[]{"ADC0,ADC1,ADC2,ADC3"});
            ADC = new String[4];
            int[] Average_ADC = new int[4];
            int cal_count = 0;
            count = 0;
            for (int i = 0; i < Lamp_AVERAGE_ADC_DATA.length; i += 2) {
                int adc_value = (Lamp_AVERAGE_ADC_DATA[i + 1] & 0xff) << 8 | Lamp_AVERAGE_ADC_DATA[i] & 0xff;
                if (adc_value == 0) break;
                ADC[count] = Integer.toString(adc_value);
                Average_ADC[count] += adc_value;
                count++;
                if (count == 4) {
                    data.add(ADC);
                    cal_count++;
                    count = 0;
                    ADC = new String[4];
                }
            }
            String AverageADC = "Lamp ADC:,";

            for (int i = 0; i < 4; i++) {
                double buf_adc = Average_ADC[i];
                AverageADC += Math.round(buf_adc / cal_count) + ",";
            }
            AverageADC += ",," + CSV[14][7] + CSV[14][8];// add ref data-time data
            data.get(14)[0] = AverageADC;
        } catch (Exception e) {

        }
        return data;
    }
    private List<String[]> WriteADCNotTimeStamp_PLUS(List<String[]> data, String[][] CSV) {
        try {
            data.add(new String[]{""});
            data.add(new String[]{"***Lamp Ramp Up ADC***,"});
            data.add(new String[]{"ADC0,ADC1,ADC2"});
            String[] ADC = new String[3];
            int count = 0;
            for (int i = 0; i < Lamp_RAMPUP_ADC_DATA.length; i += 2) {
                int adc_value = (Lamp_RAMPUP_ADC_DATA[i + 1] & 0xff) << 8 | Lamp_RAMPUP_ADC_DATA[i] & 0xff;
                if (adc_value == 0) break;
                if (count < 3) ADC[count] = Integer.toString(adc_value);
                count++;
                if (count == 4) {
                    data.add(ADC);
                    count = 0;
                    ADC = new String[3];
                }
            }
            //-----------------------------------
            data.add(new String[]{""});
            data.add(new String[]{"***Lamp ADC among repeated times***,"});
            data.add(new String[]{"ADC0,ADC1,ADC2"});
            ADC = new String[3];
            int[] Average_ADC = new int[3];
            int cal_count = 0;
            count = 0;
            for (int i = 0; i < Lamp_AVERAGE_ADC_DATA.length; i += 2) {
                int adc_value = (Lamp_AVERAGE_ADC_DATA[i + 1] & 0xff) << 8 | Lamp_AVERAGE_ADC_DATA[i] & 0xff;
                if (adc_value == 0) break;
                if (count < 3) {
                    ADC[count] = Integer.toString(adc_value);
                    Average_ADC[count] += adc_value;
                }
                count++;
                if (count == 4) {
                    data.add(ADC);
                    cal_count++;
                    count = 0;
                    ADC = new String[3];
                }
            }
            String AverageADC = "Lamp ADC:,";

            for (int i = 0; i < 3; i++) {
                double buf_adc = Average_ADC[i];
                AverageADC += Math.round(buf_adc / cal_count) + ",";
            }
            AverageADC += ",,," + CSV[14][7] + CSV[14][8];// add ref data-time data
            data.get(14)[0] = AverageADC;
        } catch (Exception e) {

        }
        return data;
    }

    private void setData(LineChart mChart, ArrayList<String> xValues, ArrayList<Entry> yValues, ChartType type) {

        if (type == ChartType.REFLECTANCE) {
            int size = yValues.size();
            if (size == 0) {
                return;
            }
            //---------------------------------------------------------
            // create a dataset and give it a type
            LineDataSet set1 = new LineDataSet(yValues, GraphLabel);
            // set the line to be drawn like this "- - - - - -"
            set1.enableDashedLine(10f, 5f, 0f);
            set1.enableDashedHighlightLine(10f, 5f, 0f);
            set1.setColor(Color.BLACK);
            set1.setCircleColor(Color.argb(100, 6, 151, 254));
            set1.setLineWidth(1f);
            set1.setCircleSize(2f);
            set1.setDrawCircleHole(false);
            set1.setValueTextSize(9f);
            set1.setFillAlpha(65);
            set1.setFillColor(Color.argb(100, 6, 151, 254));
            set1.setDrawFilled(true);
            set1.setValues(yValues);
            ArrayList<ILineDataSet> dataSets = new ArrayList<ILineDataSet>();
            dataSets.add(set1);
            LineData data = new LineData(dataSets);
            mChart.setData(data);
            mChart.setMaxVisibleValueCount(20);
        } else if (type == ChartType.ABSORBANCE) {
            int size = yValues.size();
            if (size == 0) {
                return;
            }
            // create a dataset and give it a type
            LineDataSet set1 = new LineDataSet(yValues, GraphLabel);
            // set the line to be drawn like this "- - - - - -"
            set1.enableDashedLine(10f, 5f, 0f);
            set1.enableDashedHighlightLine(10f, 5f, 0f);
            set1.setColor(Color.BLACK);
            set1.setCircleColor(Color.RED);
            set1.setLineWidth(1f);
            set1.setCircleSize(2f);
            set1.setDrawCircleHole(false);
            set1.setValueTextSize(9f);
            set1.setFillAlpha(65);
            set1.setFillColor(Color.RED);
            set1.setDrawFilled(true);
            set1.setValues(yValues);
            ArrayList<ILineDataSet> dataSets = new ArrayList<ILineDataSet>();
            dataSets.add(set1);
            LineData data = new LineData(dataSets);
            mChart.setData(data);
            mChart.setMaxVisibleValueCount(20);
        } else if (type == ChartType.INTENSITY) {
            int size = yValues.size();
            if (size == 0) {
                return;
            }
            // create a dataset and give it a type
            LineDataSet set1 = new LineDataSet(yValues, GraphLabel);

            // set the line to be drawn like this "- - - - - -"
            set1.enableDashedLine(10f, 5f, 0f);
            set1.enableDashedHighlightLine(10f, 5f, 0f);
            set1.setColor(Color.BLACK);
            set1.setCircleColor(Color.BLUE);
            set1.setLineWidth(1f);
            set1.setCircleSize(2f);
            set1.setDrawCircleHole(false);
            set1.setValueTextSize(9f);
            set1.setFillAlpha(65);
            set1.setFillColor(Color.BLUE);
            set1.setDrawFilled(true);
            set1.setValues(yValues);
            ArrayList<ILineDataSet> dataSets = new ArrayList<ILineDataSet>();
            dataSets.add(set1);
            LineData data = new LineData(dataSets);
            mChart.setData(data);
            mChart.setMaxVisibleValueCount(20);
        } else {
            int size = yValues.size();
            if (size == 0) {
                yValues.add(new Entry((float) -10, (float) -10));
            }
            // create a dataset and give it a type
            LineDataSet set1 = new LineDataSet(yValues, GraphLabel);
            // set the line to be drawn like this "- - - - - -"
            set1.enableDashedLine(10f, 5f, 0f);
            set1.enableDashedHighlightLine(10f, 5f, 0f);
            set1.setColor(Color.BLACK);
            set1.setCircleColor(Color.BLACK);
            set1.setLineWidth(1f);
            set1.setCircleSize(3f);
            set1.setDrawCircleHole(true);
            set1.setValueTextSize(9f);
            set1.setFillAlpha(65);
            set1.setFillColor(Color.BLACK);
            set1.setDrawFilled(true);
            set1.setValues(yValues);
            ArrayList<ILineDataSet> dataSets = new ArrayList<ILineDataSet>();
            dataSets.add(set1);
            LineData data = new LineData(dataSets);
            mChart.setData(data);
            mChart.setMaxVisibleValueCount(10);
        }
    }
    private void setDataSlew(LineChart mChart, ArrayList<Entry> yValues, int slewnum) {
        if (yValues.size() <= 1) {
            return;
        }
        ArrayList<Entry> yValues1 = new ArrayList<Entry>();
        ArrayList<Entry> yValues2 = new ArrayList<Entry>();
        ArrayList<Entry> yValues3 = new ArrayList<Entry>();
        ArrayList<Entry> yValues4 = new ArrayList<Entry>();
        ArrayList<Entry> yValues5 = new ArrayList<Entry>();

        for (int i = 0; i < activeConf.getSectionNumPatterns()[0]; i++) {
            if (!Float.isInfinite(yValues.get(i).getY())) {
                yValues1.add(new Entry(yValues.get(i).getX(), yValues.get(i).getY()));
            }
        }
        int offset = activeConf.getSectionNumPatterns()[0];
        for (int i = 0; i < activeConf.getSectionNumPatterns()[1]; i++) {
            if (!Float.isInfinite(yValues.get(offset + i).getY())) {
                yValues2.add(new Entry(yValues.get(offset + i).getX(), yValues.get(offset + i).getY()));
            }
        }
        if (slewnum >= 3) {
            offset = activeConf.getSectionNumPatterns()[0] + activeConf.getSectionNumPatterns()[1];
            for (int i = 0; i < activeConf.getSectionNumPatterns()[2]; i++) {
                if (!Float.isInfinite(yValues.get(offset + i).getY())) {
                    yValues3.add(new Entry(yValues.get(offset + i).getX(), yValues.get(offset + i).getY()));
                }

            }
        }
        if (slewnum >= 4) {
            offset = activeConf.getSectionNumPatterns()[0] + activeConf.getSectionNumPatterns()[1] + activeConf.getSectionNumPatterns()[2];
            for (int i = 0; i < activeConf.getSectionNumPatterns()[3]; i++) {
                if (!Float.isInfinite(yValues.get(offset + i).getY())) {
                    yValues4.add(new Entry(yValues.get(offset + i).getX(), yValues.get(offset + i).getY()));
                }
            }
        }
        if (slewnum == 5) {
            offset = activeConf.getSectionNumPatterns()[0] + activeConf.getSectionNumPatterns()[1] + activeConf.getSectionNumPatterns()[2] + activeConf.getSectionNumPatterns()[3];
            for (int i = 0; i < activeConf.getSectionNumPatterns()[4]; i++) {
                if (!Float.isInfinite(yValues.get(offset + i).getY())) {
                    yValues5.add(new Entry(yValues.get(offset + i).getX(), yValues.get(offset + i).getY()));
                }
            }
        }
        // create a dataset and give it a type
        LineDataSet set1 = new LineDataSet(yValues1, "Slew1");
        LineDataSet set2 = new LineDataSet(yValues2, "Slew2");
        LineDataSet set3 = new LineDataSet(yValues3, "Slew3");
        LineDataSet set4 = new LineDataSet(yValues4, "Slew4");
        LineDataSet set5 = new LineDataSet(yValues5, "Slew5");
        // set the line to be drawn like this "- - - - - -"
        set1.enableDashedLine(10f, 5f, 0f);
        set1.enableDashedHighlightLine(10f, 5f, 0f);
        set1.setColor(Color.BLUE);
        set1.setCircleColor(Color.BLUE);
        set1.setLineWidth(1f);
        set1.setCircleSize(2f);
        set1.setDrawCircleHole(false);
        set1.setValueTextSize(9f);
        set1.setFillAlpha(65);
        set1.setFillColor(Color.BLUE);
        set1.setDrawFilled(true);
        set1.setValues(yValues1);
        // set the line to be drawn like this "- - - - - -"
        set2.enableDashedLine(10f, 5f, 0f);
        set2.enableDashedHighlightLine(10f, 5f, 0f);
        set2.setColor(Color.RED);
        set2.setCircleColor(Color.RED);
        set2.setLineWidth(1f);
        set2.setCircleSize(2f);
        set2.setDrawCircleHole(false);
        set2.setValueTextSize(9f);
        set2.setFillAlpha(65);
        set2.setFillColor(Color.RED);
        set2.setDrawFilled(true);
        set2.setValues(yValues2);
        // set the line to be drawn like this "- - - - - -"
        set3.enableDashedLine(10f, 5f, 0f);
        set3.enableDashedHighlightLine(10f, 5f, 0f);
        set3.setColor(Color.GREEN);
        set3.setCircleColor(Color.GREEN);
        set3.setLineWidth(1f);
        set3.setCircleSize(2f);
        set3.setDrawCircleHole(false);
        set3.setValueTextSize(9f);
        set3.setFillAlpha(65);
        set3.setFillColor(Color.GREEN);
        set3.setDrawFilled(true);
        set3.setValues(yValues3);
        // set the line to be drawn like this "- - - - - -"
        set4.enableDashedLine(10f, 5f, 0f);
        set4.enableDashedHighlightLine(10f, 5f, 0f);
        set4.setColor(Color.YELLOW);
        set4.setCircleColor(Color.YELLOW);
        set4.setLineWidth(1f);
        set4.setCircleSize(2f);
        set4.setDrawCircleHole(false);
        set4.setValueTextSize(9f);
        set4.setFillAlpha(65);
        set4.setFillColor(Color.YELLOW);
        set4.setDrawFilled(true);
        set4.setValues(yValues4);
        // set the line to be drawn like this "- - - - - -"
        set5.enableDashedLine(10f, 5f, 0f);
        set5.enableDashedHighlightLine(10f, 5f, 0f);
        set5.setColor(Color.LTGRAY);
        set5.setCircleColor(Color.LTGRAY);
        set5.setLineWidth(1f);
        set5.setCircleSize(2f);
        set5.setDrawCircleHole(false);
        set5.setValueTextSize(9f);
        set5.setFillAlpha(65);
        set5.setFillColor(Color.LTGRAY);
        set5.setDrawFilled(true);
        set5.setValues(yValues5);
        if (slewnum == 2) {
            LineData data = new LineData(set1, set2);
            mChart.setData(data);
            mChart.setMaxVisibleValueCount(20);
        }
        if (slewnum == 3) {
            LineData data = new LineData(set1, set2, set3);
            mChart.setData(data);
            mChart.setMaxVisibleValueCount(20);
        }

        if (slewnum == 4) {
            LineData data = new LineData(set1, set2, set3, set4);
            mChart.setData(data);
            mChart.setMaxVisibleValueCount(20);
        }

        if (slewnum == 5) {
            LineData data = new LineData(set1, set2, set3, set4, set5);
            mChart.setData(data);
            mChart.setMaxVisibleValueCount(20);
        }
    }
    //endregion
    //region Dialog Pane
    private void Dialog_Pane_Finish(String title, String content) {
        final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(mContext);
        alertDialogBuilder.setTitle(title);
        alertDialogBuilder.setCancelable(false);
        alertDialogBuilder.setMessage(content);
        alertDialogBuilder.setPositiveButton(getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface arg0, int arg1) {
                alertDialog.dismiss();
            }
        });
        alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }
    private void NotValidValueDialog(String title, String content) {
        final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(mContext);
        alertDialogBuilder.setTitle(title);
        alertDialogBuilder.setCancelable(false);
        alertDialogBuilder.setMessage(content);
        alertDialogBuilder.setPositiveButton(getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface arg0, int arg1) {
                alertDialog.dismiss();
            }
        });
        alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }
    private void Dialog_Pane(String title, String content) {
        final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(mContext);
        alertDialogBuilder.setTitle(title);
        alertDialogBuilder.setCancelable(false);
        alertDialogBuilder.setMessage(content);
        alertDialogBuilder.setPositiveButton(getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface arg0, int arg1) {
                alertDialog.dismiss();
            }
        });
        alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }
    private void Dialog_Pane_OpenFunction(String title, String content) {
        final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(mContext);
        alertDialogBuilder.setTitle(title);
        alertDialogBuilder.setCancelable(false);
        alertDialogBuilder.setMessage(content);
        alertDialogBuilder.setPositiveButton(getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface arg0, int arg1) {
                alertDialog.dismiss();
                openFunction();
            }
        });
        alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }
    private void Dialog_Pane_OldTIVA(String title, String content) {
        final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(mContext);
        alertDialogBuilder.setTitle(title);
        alertDialogBuilder.setMessage(content);
        alertDialogBuilder.setNegativeButton(getResources().getString(R.string.yes), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface arg0, int arg1) {
                alertDialog.dismiss();
                NotValidValueDialog("Limited Functions", "Running with older Tiva firmware\nis not recommended and functions\nwill be limited!");
            }
        });
        alertDialogBuilder.setPositiveButton(getResources().getString(R.string.no), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface arg0, int arg1) {
                alertDialog.dismiss();
                finish();
            }
        });
        alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    /**
     * Dialog that tells the user that a Nano is not connected. The activity will finish when the
     * user selects ok
     */
    private void notConnectedDialog() {
        final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(mContext);
        alertDialogBuilder.setTitle(mContext.getResources().getString(R.string.not_connected_title));
        alertDialogBuilder.setCancelable(false);
        alertDialogBuilder.setMessage(mContext.getResources().getString(R.string.not_connected_message));

        alertDialogBuilder.setPositiveButton(getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface arg0, int arg1) {
                alertDialog.dismiss();
                finish();
            }
        });

        alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }
    //endregion
    private void GetActiveConfigOnResume() {
        ScanConfigList_from_ScanConfiguration = ScanConfigurationsViewActivity.bufconfigs;//from scan configuration
        ScanConfig_Byte_List_from_ScanConfiuration = ScanConfigurationsViewActivity.bufEXTRADATA_fromScanConfigurationsViewActivity;
        int storenum = ScanConfigList_from_ScanConfiguration.size();
        if (storenum != 0) {
            if (storenum != ScanConfigList.size()) {
                ScanConfigList.clear();
                ScanConfig_Byte_List.clear();
                for (int i = 0; i < ScanConfigList_from_ScanConfiguration.size(); i++) {
                    ScanConfigList.add(ScanConfigList_from_ScanConfiguration.get(i));
                    ScanConfig_Byte_List.add(ScanConfig_Byte_List_from_ScanConfiuration.get(i));
                }
            }

            for (int i = 0; i < ScanConfigList.size(); i++) {
                int ScanConfigIndextoByte = (byte) ScanConfigList.get(i).getScanConfigIndex();
                if (ActiveConfigindex == ScanConfigIndextoByte) {
                    activeConf = ScanConfigList.get(i);
                    ActiveConfigByte = ScanConfig_Byte_List.get(i);
                    tv_normal_scan_conf.setText(activeConf.getConfigName());
                }
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // 载入数据
        loadData();
        GotoOtherPage = false;
        numSections = 0;
        // TODO: 2023/8/25 这里记得清空row_data和post_data
        row_data = new String[229];
        if (IsScanPhase_1){
            post_data.clear();
        }
        // 更新当前文件名编号
        int fileNum = getMaxNumberFromFilenames();
        if (fileNum != -1) {
            fileNumber.setText(String.valueOf(fileNum + 1));
        } else {
            fileNumber.setText("0");
        }

        if (!init_viewpage_valuearray) {
            init_viewpage_valuearray = true;
            //Initialize view pager
            CustomPagerAdapter pagerAdapter = new CustomPagerAdapter(this);
            mViewPager.setAdapter(pagerAdapter);
            mViewPager.invalidate();
            mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
                @Override
                public void onPageSelected(int position) {
                    // When swiping between pages, select the
                    // corresponding tab.
                    ActionBar ab = getActionBar();
                    if (ab != null) {
                        getActionBar().setSelectedNavigationItem(position);
                    }
                }
            });
            mXValues = new ArrayList<>();
            mIntensityFloat = new ArrayList<>();
            mAbsorbanceFloat = new ArrayList<>();
            mReflectanceFloat = new ArrayList<>();
            mWavelengthFloat = new ArrayList<>();
            mReferenceFloat = new ArrayList<>();
        } else {
            if ((!isExtendVer_PLUS && !isExtendVer && fw_level_standard.compareTo(ISCNIRScanSDK.FW_LEVEL_STANDARD.LEVEL_0) == 0) || isOldTiva)
                closeFunction();
            else if (getStringPref(mContext, ISCNIRScanSDK.SharedPreferencesKeys.Activacatestatus, null).contains("Activated")) {
                if (!showActiveconfigpage) openFunction();
            } else closeFunction();
        }
        if (!showActiveconfigpage) {
            LocalBroadcastManager.getInstance(mContext).registerReceiver(ScanConfSizeReceiver, new IntentFilter(ISCNIRScanSDK.SCAN_CONF_SIZE));
            LocalBroadcastManager.getInstance(mContext).registerReceiver(GetActiveScanConfReceiver, new IntentFilter(ISCNIRScanSDK.SEND_ACTIVE_CONF));
            LocalBroadcastManager.getInstance(mContext).registerReceiver(WriteScanConfigStatusReceiver, WriteScanConfigStatusFilter);
            LocalBroadcastManager.getInstance(mContext).registerReceiver(GetDeviceStatusReceiver, new IntentFilter(ISCNIRScanSDK.ACTION_STATUS));

        }
        //-----------------------------------------------------------------------------------------------------------
        //In active page back to this page,do nothing,don't init scan Configuration text
        if (showActiveconfigpage) {
            showActiveconfigpage = false;
        }
        //First to connect
        else {
            tv_normal_scan_conf.setText(getStringPref(mContext, ISCNIRScanSDK.SharedPreferencesKeys.scanConfiguration, "Column 1"));
        }
        if (!GotoScanConfigFlag && activeConf != null) {
            tv_normal_scan_conf.setText(activeConf.getConfigName());
        } else if (GotoScanConfigFlag) {
            ISCNIRScanSDK.GetActiveConfig();
        }
    }
    /*
     * When the activity is destroyed, unregister all broadcast receivers, remove handler callbacks,
     * and store all user preferences
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        ISCNIRScanSDK.ControlPhysicalButton(ISCNIRScanSDK.PhysicalButton.Unlock);
        ChangeLampState();
        try {
            Thread.sleep(200);
        } catch (Exception e) {

        }
        unbindService(mServiceConnection);
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(ScanDataReadyReceiver);
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(RefDataReadyReceiver);
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(NotifyCompleteReceiver);
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(RefCoeffDataProgressReceiver);
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(CalMatrixDataProgressReceiver);
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(DisconnReceiver);
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(ScanConfReceiver);
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(ScanConfSizeReceiver);
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(GetActiveScanConfReceiver);
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(SpectrumCalCoefficientsReadyReceiver);
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(RetrunReadActivateStatusReceiver);
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(RetrunActivateStatusReceiver);
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(WriteScanConfigStatusReceiver);
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(DeviceInfoReceiver);
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(GetUUIDReceiver);
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(GetDeviceStatusReceiver);
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(ReturnLampRampUpADCReceiver);
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(ReturnLampADCAverageReceiver);
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(ReturnLampRampUpADCTimeStampReceiver);
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(ReturnMFGNumReceiver);
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(ReturnHWModelReceiver);
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(ReturnSetLampReceiver);
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(ReturnSetPGAReceiver);
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(ReturnSetScanRepeatsReceiver);
        mHandler.removeCallbacksAndMessages(null);
}

    @Override
    public void onPause() {
        super.onPause();
        // 保存双面扫描、双面扫描结果校验数据
        saveData();
        // 取消注册广播接收器，避免重复写入文件
        if (!showActiveconfigpage) {
            LocalBroadcastManager.getInstance(mContext).unregisterReceiver(ScanConfSizeReceiver);
            LocalBroadcastManager.getInstance(mContext).unregisterReceiver(GetActiveScanConfReceiver);
            LocalBroadcastManager.getInstance(mContext).unregisterReceiver(WriteScanConfigStatusReceiver);
            LocalBroadcastManager.getInstance(mContext).unregisterReceiver(GetDeviceStatusReceiver);
        }
    }

    private void setActivityTouchDisable(boolean value) {
        if (value) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
        }
    }

    /**
     * Set device physical button status
     */
    private void SetDeviceButtonStatus() {
        if (isExtendVer_PLUS || isExtendVer || (!isExtendVer_PLUS && !isExtendVer && fw_level_standard.compareTo(ISCNIRScanSDK.FW_LEVEL_STANDARD.LEVEL_1) > 0)) {
            Boolean isLockButton = getBooleanPref(mContext, ISCNIRScanSDK.SharedPreferencesKeys.LockButton, false);
            //User open lock button on Configure page
            if (isLockButton) {
                ISCNIRScanSDK.ControlPhysicalButton(ISCNIRScanSDK.PhysicalButton.Lock);
            } else {
                ISCNIRScanSDK.ControlPhysicalButton(ISCNIRScanSDK.PhysicalButton.Unlock);
            }
        }
    }
    /**
     * Perform scan to the device
     *
     * @param delaytime set delay time tto avoid ble hang
     */
    private void PerformScan(long delaytime) {
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                //Send broadcast START_SCAN will trigger to scan data
                ISCNIRScanSDK.StartScan();
            }
        }, delaytime);
    }
public enum ScanMethod {
    Normal, QuickSet, Manual, Maintain
}
public enum LampInfo {
    WarmDevice, ManualLamp, CloseWarmUpLampInScan
}
/**
 * Pager enum to control tab tile and layout resource
 */
public enum CustomPagerEnum {
    REFLECTANCE(R.string.reflectance, R.layout.page_graph_reflectance), ABSORBANCE(R.string.absorbance, R.layout.page_graph_absorbance), INTENSITY(R.string.intensity, R.layout.page_graph_intensity), REFERENCE(R.string.reference_tab, R.layout.page_graph_reference);
    private final int mTitleResId;
    private final int mLayoutResId;

    CustomPagerEnum(int titleResId, int layoutResId) {
        mTitleResId = titleResId;
        mLayoutResId = layoutResId;
    }

    public int getLayoutResId() {
        return mLayoutResId;
    }
}
    /**
     * Custom enum for chart type
     */
    public enum ChartType {
        REFLECTANCE, ABSORBANCE, INTENSITY
    }

    /**
     * Custom receiver that will request the time once all of the GATT notifications have been subscribed to
     * If the connected device has saved the last setting, skip request the time. Read the device's  active config directly.
     */
    public class NotifyCompleteReceiver extends BroadcastReceiver {
        public void onReceive(Context context, Intent intent) {
            if (WarmUp) {
                ISCNIRScanSDK.ControlLamp(ISCNIRScanSDK.LampState.ON);
                Lamp_Info = LampInfo.WarmDevice;
            } else {
                Boolean reference = false;
                if (getStringPref(mContext, ISCNIRScanSDK.SharedPreferencesKeys.ReferenceScan, "Not").equals("ReferenceScan")) {
                    reference = true;
                }
                if (preferredDevice.equals(HomeViewActivity.storeCalibration.device) && !reference) {
                    refCoeff = HomeViewActivity.storeCalibration.storrefCoeff;
                    refMatrix = HomeViewActivity.storeCalibration.storerefMatrix;
                    ArrayList<ISCNIRScanSDK.ReferenceCalibration> refCal = new ArrayList<>();
                    refCal.add(new ISCNIRScanSDK.ReferenceCalibration(refCoeff, refMatrix));
                    ISCNIRScanSDK.ReferenceCalibration.writeRefCalFile(mContext, refCal);
                    calProgress.setVisibility(View.INVISIBLE);
                    barProgressDialog = new ProgressDialog(ScanViewActivityForUsers.this);
                    //Get active config
                    ISCNIRScanSDK.ShouldDownloadCoefficient = false;
                    ISCNIRScanSDK.SetCurrentTime();
                } else {
                    if (reference) {
                        storeStringPref(mContext, ISCNIRScanSDK.SharedPreferencesKeys.ReferenceScan, "Not");
                    }
                    //Synchronize time and download calibration coefficient and calibration matrix
                    ISCNIRScanSDK.ShouldDownloadCoefficient = true;
                    ISCNIRScanSDK.SetCurrentTime();
                }
            }
        }
    }

    /**
     * Custom receiver for receiving calibration coefficient data.(ISCNIRScanSDK.SetCurrentTime()must be called)
     */
    public class RefCoeffDataProgressReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            intent.getIntExtra(ISCNIRScanSDK.EXTRA_REF_CAL_COEFF_SIZE, 0);
            Boolean size = intent.getBooleanExtra(ISCNIRScanSDK.EXTRA_REF_CAL_COEFF_SIZE_PACKET, false);
            if (size) {
                calProgress.setVisibility(View.INVISIBLE);
                barProgressDialog = new ProgressDialog(ScanViewActivityForUsers.this);
                barProgressDialog.setTitle(getString(R.string.dl_ref_cal));
                barProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                barProgressDialog.setProgress(0);
                barProgressDialog.setMax(intent.getIntExtra(ISCNIRScanSDK.EXTRA_REF_CAL_COEFF_SIZE, 0));
                barProgressDialog.setCancelable(false);
                barProgressDialog.show();
            } else {
                barProgressDialog.setProgress(barProgressDialog.getProgress() + intent.getIntExtra(ISCNIRScanSDK.EXTRA_REF_CAL_COEFF_SIZE, 0));
            }
        }
    }
    /**
     * Custom receiver for receiving calibration matrix data. When this receiver action complete, it
     * will request the active configuration so that it can be displayed in the listview(ISCNIRScanSDK.SetCurrentTime()must be called)
     */
    public class CalMatrixDataProgressReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            intent.getIntExtra(ISCNIRScanSDK.EXTRA_REF_CAL_MATRIX_SIZE, 0);
            Boolean size = intent.getBooleanExtra(ISCNIRScanSDK.EXTRA_REF_CAL_MATRIX_SIZE_PACKET, false);
            if (size) {
                barProgressDialog.dismiss();
                barProgressDialog = new ProgressDialog(ScanViewActivityForUsers.this);
                barProgressDialog.setTitle(getString(R.string.dl_cal_matrix));
                barProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                barProgressDialog.setProgress(0);
                barProgressDialog.setMax(intent.getIntExtra(ISCNIRScanSDK.EXTRA_REF_CAL_MATRIX_SIZE, 0));
                barProgressDialog.setCancelable(false);
                barProgressDialog.show();
            } else {
                barProgressDialog.setProgress(barProgressDialog.getProgress() + intent.getIntExtra(ISCNIRScanSDK.EXTRA_REF_CAL_MATRIX_SIZE, 0));
            }
            if (barProgressDialog.getProgress() == barProgressDialog.getMax()) {
                //Send broadcast GET_ACTIVE_CONF will trigger to get active config
                ISCNIRScanSDK.GetActiveConfig();
            }
        }
    }

    /**
     * After download reference calibration  matrix will notify and save(ISCNIRScanSDK.SetCurrentTime()must be called)
     */
    public class RefDataReadyReceiver extends BroadcastReceiver {
        public void onReceive(Context context, Intent intent) {
            refCoeff = intent.getByteArrayExtra(ISCNIRScanSDK.EXTRA_REF_COEF_DATA);
            refMatrix = intent.getByteArrayExtra(ISCNIRScanSDK.EXTRA_REF_MATRIX_DATA);
            ArrayList<ISCNIRScanSDK.ReferenceCalibration> refCal = new ArrayList<>();
            refCal.add(new ISCNIRScanSDK.ReferenceCalibration(refCoeff, refMatrix));
            ISCNIRScanSDK.ReferenceCalibration.writeRefCalFile(mContext, refCal);
            calProgress.setVisibility(View.GONE);
            //------------------------------------------------------------------
            HomeViewActivity.storeCalibration.device = preferredDevice;
            HomeViewActivity.storeCalibration.storrefCoeff = refCoeff;
            HomeViewActivity.storeCalibration.storerefMatrix = refMatrix;
        }
    }
    /**
     * Send broadcast  GET_ACTIVE_CONF will  through GetActiveScanConfReceiver to get active config(ISCNIRScanSDK.GetActiveConfig() should be called)
     */
    private class GetActiveScanConfReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            ActiveConfigindex = intent.getByteArrayExtra(ISCNIRScanSDK.EXTRA_ACTIVE_CONF)[0];
            if (ScanConfigList.size() != 0) {
                GetActiveConfigOnResume();
            } else {
                //Get the number of scan config and scan config data
                ISCNIRScanSDK.GetScanConfig();
            }
        }
    }

    /**
     * Get the number of scan config(ISCNIRScanSDK.GetScanConfig() should be called)
     */
    private class ScanConfSizeReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            storedConfSize = intent.getIntExtra(ISCNIRScanSDK.EXTRA_CONF_SIZE, 0);
        }
    }

    /**
     * Get the scan config data(ISCNIRScanSDK.GetScanConfig() should be called)
     */
    private class ScanConfReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            receivedConfSize++;
            ScanConfig_Byte_List.add(intent.getByteArrayExtra(ISCNIRScanSDK.EXTRA_DATA));
            ScanConfigList.add(ISCNIRScanSDK.scanConf);

            if (storedConfSize > 0 && receivedConfSize == 0) {
                barProgressDialog.dismiss();
                barProgressDialog = new ProgressDialog(ScanViewActivityForUsers.this);
                barProgressDialog.setTitle(getString(R.string.reading_configurations));
                barProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                barProgressDialog.setProgress(0);
                barProgressDialog.setMax(storedConfSize);
                barProgressDialog.setCancelable(false);
                barProgressDialog.show();
            } else {
                barProgressDialog.setProgress(receivedConfSize + 1);
            }
            if (barProgressDialog.getProgress() == barProgressDialog.getMax() || barProgressDialog.getMax() == 1) {
                for (int i = 0; i < ScanConfigList.size(); i++) {
                    int ScanConfigIndextoByte = (byte) ScanConfigList.get(i).getScanConfigIndex();
                    if (ActiveConfigindex == ScanConfigIndextoByte) {
                        activeConf = ScanConfigList.get(i);
                        ActiveConfigByte = ScanConfig_Byte_List.get(i);
                    }
                }
                barProgressDialog.dismiss();
                mMenu.findItem(R.id.action_settings).setEnabled(true);
                storeStringPref(mContext, ISCNIRScanSDK.SharedPreferencesKeys.scanConfiguration, ISCNIRScanSDK.scanConf.getConfigName());
                tv_normal_scan_conf.setText(activeConf.getConfigName());
                if (!downloadspecFlag) {
                    //Get spectrum calibration coefficient
                    ISCNIRScanSDK.GetSpectrumCoef();
                    downloadspecFlag = true;
                }
            }
        }
    }

    /**
     * Get  spectrum calibration coefficient from the device then send request to get the device info(ISCNIRScanSDK.GetSpectrumCoef() should be called)
     */
    public class SpectrumCalCoefficientsReadyReceiver extends BroadcastReceiver {
        public void onReceive(Context context, Intent intent) {
            SpectrumCalCoefficients = intent.getByteArrayExtra(ISCNIRScanSDK.EXTRA_SPEC_COEF_DATA);
            passSpectrumCalCoefficients = SpectrumCalCoefficients;
            //Request device information
            ISCNIRScanSDK.GetDeviceInfo();
        }
    }
    /**
     * Send broadcast  GET_INFO will  through DeviceInfoReceiver  to get the device info(ISCNIRScanSDK.GetDeviceInfo() should be called)
     */
    public class DeviceInfoReceiver extends BroadcastReceiver {
        public void onReceive(Context context, Intent intent) {
            model_name = intent.getStringExtra(ISCNIRScanSDK.EXTRA_MODEL_NUM);
            serial_num = intent.getStringExtra(ISCNIRScanSDK.EXTRA_SERIAL_NUM);
            HWrev = intent.getStringExtra(ISCNIRScanSDK.EXTRA_HW_REV);
            Tivarev = intent.getStringExtra(ISCNIRScanSDK.EXTRA_TIVA_REV);
            Specrev = intent.getStringExtra(ISCNIRScanSDK.EXTRA_SPECTRUM_REV);
            if (Tivarev.charAt(0) == '5') {
                isExtendVer_PLUS = true;
                isExtendVer = false;
            } else if (Tivarev.charAt(0) == '3' && (HWrev.charAt(0) == 'E' || HWrev.charAt(0) == 'O')) {
                isExtendVer_PLUS = false;
                isExtendVer = true;
            } else {
                isExtendVer_PLUS = false;
                isExtendVer = false;
            }
            if ((isExtendVer || isExtendVer_PLUS) && serial_num.length() > 8)
                serial_num = serial_num.substring(0, 8);
            else if (!isExtendVer_PLUS && !isExtendVer && serial_num.length() > 7)
                serial_num = serial_num.substring(0, 7);
            if (HWrev.charAt(0) == 'N')
                Dialog_Pane_Finish("Not support", "Not to support the N version of the main board.\nWill go to the home page.");
            else {
                if (isExtendVer_PLUS) {
                    adapter_width = ArrayAdapter.createFromResource(mContext, R.array.scan_width_plus, android.R.layout.simple_spinner_item);
                    adapter_width.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                } else {
                    adapter_width = ArrayAdapter.createFromResource(mContext, R.array.scan_width, android.R.layout.simple_spinner_item);
                    adapter_width.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                }
                if (isExtendVer) ISCNIRScanSDK.TIVAFW_EXT = GetFWLevelEXT(Tivarev);
                else if (isExtendVer_PLUS)
                    ISCNIRScanSDK.TIVAFW_EXT_PLUS = GetFWLevelEXTPLUS(Tivarev);
                else ISCNIRScanSDK.TIVAFW_STANDARD = GetFWLevelStandard(Tivarev);
                InitParameter();
                if (!isExtendVer_PLUS && !isExtendVer && fw_level_standard.compareTo(ISCNIRScanSDK.FW_LEVEL_STANDARD.LEVEL_0) == 0)
                    Dialog_Pane_Finish("Firmware Out of Date", "You must update the firmware on your NIRScan Nano to make this App working correctly!\n" + "FW required version at least V2.4.4.\nDetected version is V" + Tivarev + ".");
                else ISCNIRScanSDK.GetMFGNumber();
            }
        }
    }

    public class ReturnMFGNumReceiver extends BroadcastReceiver {

        public void onReceive(Context context, Intent intent) {
            MFG_NUM = intent.getByteArrayExtra(ISCNIRScanSDK.MFGNUM_DATA);
            //Tiva 2.5.x
            if ((!isExtendVer_PLUS && !isExtendVer && fw_level_standard.compareTo(ISCNIRScanSDK.FW_LEVEL_STANDARD.LEVEL_4) >= 0) || (isExtendVer && fw_level_ext.compareTo(ISCNIRScanSDK.FW_LEVEL_EXT.LEVEL_EXT_3) >= 0))
                ISCNIRScanSDK.GetHWModel();
            else ISCNIRScanSDK.GetUUID();
        }
    }

    public class ReturnHWModelReceiver extends BroadcastReceiver {

        public void onReceive(Context context, Intent intent) {
            byte[] byteHWMDEL = intent.getByteArrayExtra(ISCNIRScanSDK.HWMODEL_DATA);
            int len = 0;
            for (int i = 0; i < byteHWMDEL.length; i++) {
                if (byteHWMDEL[i] == 0) break;
                else len++;
            }
            byte[] HWModel = new byte[len];
            System.arraycopy(byteHWMDEL, 0, HWModel, 0, len);
            HW_Model = new String(HWModel, StandardCharsets.UTF_8);
            //Get the uuid of the device
            ISCNIRScanSDK.GetUUID();
        }
    }

    public class GetUUIDReceiver extends BroadcastReceiver {
        public void onReceive(Context context, Intent intent) {

            byte[] buf = intent.getByteArrayExtra(ISCNIRScanSDK.EXTRA_DEVICE_UUID);
            for (int i = 0; i < buf.length; i++) {
                uuid += Integer.toHexString(0xff & buf[i]);
                if (i != buf.length - 1) {
                    uuid += ":";
                }
            }
            CheckIsOldTIVA();
            if (!isOldTiva) {
                //Get the device is activate or not
                ISCNIRScanSDK.ReadActivateState();
            } else {
                closeFunction();
                mMenu.findItem(R.id.action_key).setVisible(false);
            }
            LocalBroadcastManager.getInstance(mContext).unregisterReceiver(DeviceInfoReceiver);
            LocalBroadcastManager.getInstance(mContext).unregisterReceiver(GetUUIDReceiver);
        }
    }

    /**
     * Get the activate state of the device(ISCNIRScanSDK.ReadActivateState() should be called)
     */
    public class RetrunReadActivateStatusReceiver extends BroadcastReceiver {

        public void onReceive(Context context, Intent intent) {
            if (mainflag != "")//Only from HomeViewActivity->ScanViewActivityForUsers should do this
            {
                ISCNIRScanSDK.SetActiveConfig();
                mainflag = "";
            }
            byte[] state = intent.getByteArrayExtra(ISCNIRScanSDK.RETURN_READ_ACTIVATE_STATE);
            if (state[0] == 1) {
                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {

                    @Override
                    public void run() {
                        SetDeviceButtonStatus();
                        Dialog_Pane_OpenFunction("设备已激活", "设备高级功能全部解锁。");
                    }
                }, 200);
                mMenu.findItem(R.id.action_settings).setEnabled(true);
                mMenu.findItem(R.id.action_key).setEnabled(true);
                storeStringPref(mContext, ISCNIRScanSDK.SharedPreferencesKeys.Activacatestatus, "Activated.");
            } else {
                String licensekey = getStringPref(mContext, ISCNIRScanSDK.SharedPreferencesKeys.licensekey, null);
                //The device is locked but saved license key
                if (licensekey != null && licensekey != "") {
                    calProgress.setVisibility(View.VISIBLE);
                    String filterdata = filterDate(licensekey);
                    final byte[] data = hexToBytes(filterdata);
                    Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            ISCNIRScanSDK.SetLicenseKey(data);
                        }
                    }, 200);
                } else {
                    Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            SetDeviceButtonStatus();
                            Dialog_Pane("Unlock device", "Some functions are locked.");
                        }
                    }, 200);
                    mMenu.findItem(R.id.action_settings).setEnabled(true);
                    mMenu.findItem(R.id.action_key).setEnabled(true);
                    storeStringPref(mContext, ISCNIRScanSDK.SharedPreferencesKeys.Activacatestatus, "Function is locked.");
                    closeFunction();
                }
            }
        }
    }

    /**
     * Get the activate state of the device(ISCNIRScanSDK.SetLicenseKey(data) should be called)
     */
    public class RetrunActivateStatusReceiver extends BroadcastReceiver {
        public void onReceive(Context context, Intent intent) {
            mMenu.findItem(R.id.action_settings).setEnabled(true);
            mMenu.findItem(R.id.action_key).setEnabled(true);
            calProgress.setVisibility(View.GONE);
            byte[] state = intent.getByteArrayExtra(ISCNIRScanSDK.RETURN_ACTIVATE_STATUS);
            if (state[0] == 1) {
                SetDeviceButtonStatus();
                Dialog_Pane_OpenFunction("设备已激活", "设备高级功能全部解锁。");
                storeStringPref(mContext, ISCNIRScanSDK.SharedPreferencesKeys.Activacatestatus, "Activated.");
            } else {
                SetDeviceButtonStatus();
                Dialog_Pane("Unlock device", "Some functions are locked.");
                storeStringPref(mContext, ISCNIRScanSDK.SharedPreferencesKeys.Activacatestatus, "Function is locked.");
                closeFunction();
            }
        }
    }

    /**
     * Send broadcast  ACTION_WRITE_SCAN_CONFIG will  through WriteScanConfigStatusReceiver  to get  the status of set config to the device(SetConfig should be called)
     */
    public class WriteScanConfigStatusReceiver extends BroadcastReceiver {

        public void onReceive(Context context, Intent intent) {
            calProgress.setVisibility(View.GONE);
            byte[] status = intent.getByteArrayExtra(ISCNIRScanSDK.RETURN_WRITE_SCAN_CONFIG_STATUS);
            btn_scanAndPredict.setClickable(true);
            if ((int) status[0] == 1) {
                if ((int) status[2] == -1 && (int) status[3] == -1) {
                    Dialog_Pane("Fail", "Set configuration fail!");
                } else {
                    //Get the scan config of the device
                    ISCNIRScanSDK.ReadCurrentScanConfig();
                }
            } else if ((int) status[0] == -1) {
                Dialog_Pane("Fail", "Set configuration fail!");
            } else if ((int) status[0] == -2) {
                Dialog_Pane("Fail", "Set configuration fail! Hardware not compatible!");
            } else if ((int) status[0] == -3) {
                Dialog_Pane("Fail", "Set configuration fail! Function is currently locked!");
            }
        }
    }
    /**
     * Send broadcast  START_SCAN will  through ScanStartedReceiver  to notify scanning(PerformScan should be called)
     */
    public class ScanStartedReceiver extends BroadcastReceiver {
        public void onReceive(Context context, Intent intent) {
            calProgress.setVisibility(View.VISIBLE);
            btn_scanAndPredict.setText(getString(R.string.scanning));
        }
    }
    /**
     * Custom receiver for handling scan data and setting up the graphs properly(ISCNIRScanSDK.StartScan() should be called)
     */
    public class ScanDataReadyReceiver extends BroadcastReceiver {
        public void onReceive(Context context, Intent intent) {
            long endtime = System.currentTimeMillis();
            MesureScanTime = endtime - ISCNIRScanSDK.startScanTime;
            reference_calibration = ISCNIRScanSDK.ReferenceCalibration.currentCalibration.get(0);
            if (Interpret_length <= 0) {
                Dialog_Pane_Finish("Error", "The scan interpret fail. Please check your device.");
            } else {
                //Get scan spectrum data
                Scan_Spectrum_Data = new ISCNIRScanSDK.ScanResults(Interpret_wavelength, Interpret_intensity, Interpret_uncalibratedIntensity, Interpret_length);

                mXValues.clear();
                mIntensityFloat.clear();
                mAbsorbanceFloat.clear();
                mReflectanceFloat.clear();
                mWavelengthFloat.clear();
                mReferenceFloat.clear();
                int index;
                for (index = 0; index < Scan_Spectrum_Data.getLength(); index++) {
                    mXValues.add(String.format("%.02f", ISCNIRScanSDK.ScanResults.getSpatialFreq(mContext, Scan_Spectrum_Data.getWavelength()[index])));
                    mIntensityFloat.add(new Entry((float) Scan_Spectrum_Data.getWavelength()[index], (float) Scan_Spectrum_Data.getUncalibratedIntensity()[index]));
                    mAbsorbanceFloat.add(new Entry((float) Scan_Spectrum_Data.getWavelength()[index], (-1) * (float) Math.log10((double) Scan_Spectrum_Data.getUncalibratedIntensity()[index] / (double) Scan_Spectrum_Data.getIntensity()[index])));
                    mReflectanceFloat.add(new Entry((float) Scan_Spectrum_Data.getWavelength()[index], (float) Scan_Spectrum_Data.getUncalibratedIntensity()[index] / Scan_Spectrum_Data.getIntensity()[index]));
                    mWavelengthFloat.add((float) Scan_Spectrum_Data.getWavelength()[index]);
                    mReferenceFloat.add(new Entry((float) Scan_Spectrum_Data.getWavelength()[index], (float) Scan_Spectrum_Data.getIntensity()[index]));
                }
                minWavelength = mWavelengthFloat.get(0);
                maxWavelength = mWavelengthFloat.get(0);

                for (Float f : mWavelengthFloat) {
                    if (f < minWavelength) minWavelength = f;
                    if (f > maxWavelength) maxWavelength = f;
                }
                minAbsorbance = mAbsorbanceFloat.get(0).getY();
                maxAbsorbance = mAbsorbanceFloat.get(0).getY();
                for (Entry e : mAbsorbanceFloat) {
                    if (e.getY() < minAbsorbance || Float.isNaN(minAbsorbance))
                        minAbsorbance = e.getY();
                    if (e.getY() > maxAbsorbance || Float.isNaN(maxAbsorbance))
                        maxAbsorbance = e.getY();
                }
                if (minAbsorbance == 0 && maxAbsorbance == 0) {
                    maxAbsorbance = 2;
                }
                minReflectance = mReflectanceFloat.get(0).getY();
                maxReflectance = mReflectanceFloat.get(0).getY();

                for (Entry e : mReflectanceFloat) {
                    if (e.getY() < minReflectance || Float.isNaN(minReflectance))
                        minReflectance = e.getY();
                    if (e.getY() > maxReflectance || Float.isNaN(maxReflectance))
                        maxReflectance = e.getY();
                }
                if (minReflectance == 0 && maxReflectance == 0) {
                    maxReflectance = 2;
                }
                minIntensity = mIntensityFloat.get(0).getY();
                maxIntensity = mIntensityFloat.get(0).getY();

                for (Entry e : mIntensityFloat) {
                    if (e.getY() < minIntensity || Float.isNaN(minIntensity))
                        minIntensity = e.getY();
                    if (e.getY() > maxIntensity || Float.isNaN(maxIntensity))
                        maxIntensity = e.getY();
                }
                if (minIntensity == 0 && maxIntensity == 0) {
                    maxIntensity = 1000;
                }
                minReference = mReferenceFloat.get(0).getY();
                maxReference = mReferenceFloat.get(0).getY();

                for (Entry e : mReferenceFloat) {
                    if (e.getY() < minReference || Float.isNaN(minReference))
                        minReference = e.getY();
                    if (e.getY() > maxReference || Float.isNaN(maxReference))
                        maxReference = e.getY();
                }
                if (minReference == 0 && maxReference == 0) {
                    maxReference = 1000;
                }
                isScan = true;
                tabPosition = mViewPager.getCurrentItem();
                mViewPager.setAdapter(mViewPager.getAdapter());
                mViewPager.invalidate();
                //number of slew
                String slew = "";
                if (activeConf != null && activeConf.getScanType().equals("Slew")) {
                    int numSections = activeConf.getSlewNumSections();
                    int i;
                    for (i = 0; i < numSections; i++) {
                        slew = slew + activeConf.getSectionNumPatterns()[i] + "%";
                    }
                }
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss", java.util.Locale.getDefault());
                SimpleDateFormat filesimpleDateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault());
                String ts = simpleDateFormat.format(new Date());
                CurrentTime = filesimpleDateFormat.format(new Date());
                ActionBar ab = getActionBar();
                if (ab != null) {
                    if (filePrefix.getText().toString().equals("")) {
                        ab.setTitle("ISC" + ts);
                    } else {
                        ab.setTitle(filePrefix.getText().toString() + ts);
                    }
                    ab.setSelectedNavigationItem(0);
                }
                storeStringPref(mContext, ISCNIRScanSDK.SharedPreferencesKeys.prefix, filePrefix.getText().toString());
                if (WarmUp) {
                    ISCNIRScanSDK.ControlLamp(ISCNIRScanSDK.LampState.AUTO);
                    Lamp_Info = LampInfo.CloseWarmUpLampInScan;
                } else
                    //Get Device information from the device
                    ISCNIRScanSDK.GetDeviceStatus();
            }
        }
    }
    public class GetDeviceStatusReceiver extends BroadcastReceiver {

        public void onReceive(Context context, Intent intent) {
            battery = Integer.toString(intent.getIntExtra(ISCNIRScanSDK.EXTRA_BATT, 0));
            long lamptime = intent.getLongExtra(ISCNIRScanSDK.EXTRA_LAMPTIME, 0);
            TotalLampTime = GetLampTimeString(lamptime);
            devbyte = intent.getByteArrayExtra(ISCNIRScanSDK.EXTRA_DEV_STATUS_BYTE);
            errbyte = intent.getByteArrayExtra(ISCNIRScanSDK.EXTRA_ERR_BYTE);
            if (isExtendVer_PLUS || (isExtendVer && (fw_level_ext.compareTo(ISCNIRScanSDK.FW_LEVEL_EXT.LEVEL_EXT_2) == 0 || fw_level_ext.compareTo(ISCNIRScanSDK.FW_LEVEL_EXT.LEVEL_EXT_4) == 0)) || (!isExtendVer_PLUS && !isExtendVer && (fw_level_standard.compareTo(ISCNIRScanSDK.FW_LEVEL_STANDARD.LEVEL_3) == 0 || fw_level_standard.compareTo(ISCNIRScanSDK.FW_LEVEL_STANDARD.LEVEL_5) == 0)))
                ISCNIRScanSDK.GetScanLampRampUpADC();
            else DoScanComplete();
        }
    }
    public class ReturnLampRampUpADCReceiver extends BroadcastReceiver {

        public void onReceive(Context context, Intent intent) {
            Lamp_RAMPUP_ADC_DATA = intent.getByteArrayExtra(ISCNIRScanSDK.LAMP_RAMPUP_DATA);
            ISCNIRScanSDK.GetLampADCAverage();
        }
    }
    public class ReturnLampADCAverageReceiver extends BroadcastReceiver {

        public void onReceive(Context context, Intent intent) {
            Lamp_AVERAGE_ADC_DATA = intent.getByteArrayExtra(ISCNIRScanSDK.LAMP_ADC_AVERAGE_DATA);
            if (isExtendVer_PLUS || (!isExtendVer_PLUS && !isExtendVer && fw_level_standard.compareTo(ISCNIRScanSDK.FW_LEVEL_STANDARD.LEVEL_5) == 0) || (isExtendVer && fw_level_ext.compareTo(ISCNIRScanSDK.FW_LEVEL_EXT.LEVEL_EXT_4) == 0))
                ISCNIRScanSDK.GetLampADCTimeStamp();
            else DoScanComplete();
        }
    }
    public class ReturnLampRampUpADCTimeStampReceiver extends BroadcastReceiver {

        public void onReceive(Context context, Intent intent) {
            Lamp_RAMPUP_ADC_TIMESTAMP = intent.getByteArrayExtra(ISCNIRScanSDK.LAMP_ADC_TIMESTAMP);
            DoScanComplete();
        }
    }
    /**
     * Custom pager adapter to handle changing chart data when pager tabs are changed
     */
    public class CustomPagerAdapter extends PagerAdapter {
        private final Context mContext;
        public CustomPagerAdapter(Context context) {
            mContext = context;
        }
        @Override
        public Object instantiateItem(ViewGroup collection, int position) {
            CustomPagerEnum customPagerEnum = CustomPagerEnum.values()[position];
            LayoutInflater inflater = LayoutInflater.from(mContext);
            ViewGroup layout = (ViewGroup) inflater.inflate(customPagerEnum.getLayoutResId(), collection, false);
            collection.addView(layout);

            if (customPagerEnum.getLayoutResId() == R.layout.page_graph_intensity) {
                LineChart mChart = layout.findViewById(R.id.lineChartInt);
                mChart.setDrawGridBackground(false);

                // enable touch gestures
                mChart.setTouchEnabled(true);

                // enable scaling and dragging
                mChart.setDragEnabled(true);
                mChart.setScaleEnabled(true);

                // if disabled, scaling can be done on x- and y-axis separately
                mChart.setPinchZoom(true);

                // x-axis limit line
                LimitLine llXAxis = new LimitLine(10f, "Index 10");
                llXAxis.setLineWidth(4f);
                llXAxis.enableDashedLine(10f, 10f, 0f);
                llXAxis.setLabelPosition(LimitLine.LimitLabelPosition.RIGHT_BOTTOM);
                llXAxis.setTextSize(10f);

                XAxis xAxis = mChart.getXAxis();
                xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
                xAxis.setAxisMaximum(maxWavelength);
                xAxis.setAxisMinimum(minWavelength);

                YAxis leftAxis = mChart.getAxisLeft();
                leftAxis.removeAllLimitLines(); // reset all limit lines to avoid overlapping lines

                mChart.setAutoScaleMinMaxEnabled(true);

                leftAxis.setStartAtZero(true);
                leftAxis.setAxisMaximum(maxIntensity);
                leftAxis.setAxisMinimum(minIntensity);
                leftAxis.enableGridDashedLine(10f, 10f, 0f);

                leftAxis.setDrawLimitLinesBehindData(true);

                mChart.getAxisRight().setEnabled(false);

                // add data
                int numSections = ISCNIRScanSDK.ScanConfigInfo.numSections[0];
                if (numSections >= 2 && (!Float.isNaN(minIntensity) && !Float.isNaN(maxIntensity)) && Current_Scan_Method != ScanMethod.QuickSet)//Scan method : quickset only one section
                {
                    setDataSlew(mChart, mIntensityFloat, numSections); //scan data section > 1
                } else if (!Float.isNaN(minIntensity) && !Float.isNaN(maxIntensity)) {
                    setData(mChart, mXValues, mIntensityFloat, ChartType.INTENSITY);//scan data section = 1
                }


                mChart.animateX(2500, Easing.EasingOption.EaseInOutQuart);

                // get the legend (only possible after setting data)
                Legend l = mChart.getLegend();

                // modify the legend ...
                l.setForm(Legend.LegendForm.LINE);
                mChart.getLegend().setEnabled(false);
                return layout;
            } else if (customPagerEnum.getLayoutResId() == R.layout.page_graph_absorbance) {

                LineChart mChart = layout.findViewById(R.id.lineChartAbs);
                mChart.setDrawGridBackground(false);

                // enable touch gestures
                mChart.setTouchEnabled(true);

                // enable scaling and dragging
                mChart.setDragEnabled(true);
                mChart.setScaleEnabled(true);

                // if disabled, scaling can be done on x- and y-axis separately
                mChart.setPinchZoom(true);

                // x-axis limit line
                LimitLine llXAxis = new LimitLine(10f, "Index 10");
                llXAxis.setLineWidth(4f);
                llXAxis.enableDashedLine(10f, 10f, 0f);
                llXAxis.setLabelPosition(LimitLine.LimitLabelPosition.RIGHT_BOTTOM);
                llXAxis.setTextSize(10f);

                XAxis xAxis = mChart.getXAxis();
                xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
                xAxis.setAxisMaximum(maxWavelength);
                xAxis.setAxisMinimum(minWavelength);

                YAxis leftAxis = mChart.getAxisLeft();
                leftAxis.removeAllLimitLines(); // reset all limit lines to avoid overlapping lines
                leftAxis.setAxisMaximum(maxAbsorbance);
                leftAxis.setAxisMinimum(minAbsorbance);

                mChart.setAutoScaleMinMaxEnabled(true);

                leftAxis.setStartAtZero(false);
                leftAxis.enableGridDashedLine(10f, 10f, 0f);

                // limit lines are drawn behind data (and not on top)
                leftAxis.setDrawLimitLinesBehindData(true);

                mChart.getAxisRight().setEnabled(false);


                // add data
                int numSections = ISCNIRScanSDK.ScanConfigInfo.numSections[0];
                if (numSections >= 2 && (!Float.isNaN(minAbsorbance) && !Float.isNaN(maxAbsorbance)) && Current_Scan_Method != ScanMethod.QuickSet)////Scan method : quickset only one section
                {
                    setDataSlew(mChart, mAbsorbanceFloat, numSections);
                } else if (!Float.isNaN(minAbsorbance) && !Float.isNaN(maxAbsorbance)) {
                    setData(mChart, mXValues, mAbsorbanceFloat, ChartType.ABSORBANCE);
                }


                mChart.animateX(2500, Easing.EasingOption.EaseInOutQuart);

                // get the legend (only possible after setting data)
                Legend l = mChart.getLegend();

                // modify the legend ...
                l.setForm(Legend.LegendForm.LINE);
                mChart.getLegend().setEnabled(false);

                return layout;
            } else if (customPagerEnum.getLayoutResId() == R.layout.page_graph_reflectance) {

                LineChart mChart = layout.findViewById(R.id.lineChartRef);
                mChart.setDrawGridBackground(false);


                // enable touch gestures
                mChart.setTouchEnabled(true);

                // enable scaling and dragging
                mChart.setDragEnabled(true);
                mChart.setScaleEnabled(true);

                // if disabled, scaling can be done on x- and y-axis separately
                mChart.setPinchZoom(true);

                // x-axis limit line
                LimitLine llXAxis = new LimitLine(10f, "Index 10");
                llXAxis.setLineWidth(4f);
                llXAxis.enableDashedLine(10f, 10f, 0f);
                llXAxis.setLabelPosition(LimitLine.LimitLabelPosition.RIGHT_BOTTOM);
                llXAxis.setTextSize(10f);

                XAxis xAxis = mChart.getXAxis();
                xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
                xAxis.setAxisMaximum(maxWavelength);
                xAxis.setAxisMinimum(minWavelength);

                YAxis leftAxis = mChart.getAxisLeft();
                leftAxis.removeAllLimitLines(); // reset all limit lines to avoid overlapping lines
                leftAxis.setAxisMaximum(maxReflectance);
                leftAxis.setAxisMinimum(minReflectance);

                mChart.setAutoScaleMinMaxEnabled(true);

                leftAxis.setStartAtZero(false);
                leftAxis.enableGridDashedLine(10f, 10f, 0f);

                // limit lines are drawn behind data (and not on top)
                leftAxis.setDrawLimitLinesBehindData(true);

                mChart.getAxisRight().setEnabled(false);


                // add data
                int numSections = ISCNIRScanSDK.ScanConfigInfo.numSections[0];

                if (numSections >= 2 && (!Float.isNaN(minReflectance) && !Float.isNaN(maxReflectance)) && Current_Scan_Method != ScanMethod.QuickSet)//Scan method : quickset only one section
                {
                    setDataSlew(mChart, mReflectanceFloat, numSections);
                } else if (!Float.isNaN(minReflectance) && !Float.isNaN(maxReflectance)) {
                    setData(mChart, mXValues, mReflectanceFloat, ChartType.REFLECTANCE);
                }


                mChart.animateX(2500, Easing.EasingOption.EaseInOutQuart);

                // get the legend (only possible after setting data)
                Legend l = mChart.getLegend();

                // modify the legend ...
                l.setForm(Legend.LegendForm.LINE);
                mChart.getLegend().setEnabled(false);
                return layout;
            } else if (customPagerEnum.getLayoutResId() == R.layout.page_graph_reference) {

                LineChart mChart = layout.findViewById(R.id.lineChartReference);
                mChart.setDrawGridBackground(false);

                // enable touch gestures
                mChart.setTouchEnabled(true);

                // enable scaling and dragging
                mChart.setDragEnabled(true);
                mChart.setScaleEnabled(true);

                // if disabled, scaling can be done on x- and y-axis separately
                mChart.setPinchZoom(true);

                // x-axis limit line
                LimitLine llXAxis = new LimitLine(10f, "Index 10");
                llXAxis.setLineWidth(4f);
                llXAxis.enableDashedLine(10f, 10f, 0f);
                llXAxis.setLabelPosition(LimitLine.LimitLabelPosition.RIGHT_BOTTOM);
                llXAxis.setTextSize(10f);

                XAxis xAxis = mChart.getXAxis();
                xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
                xAxis.setAxisMaximum(maxWavelength);
                xAxis.setAxisMinimum(minWavelength);

                YAxis leftAxis = mChart.getAxisLeft();
                leftAxis.removeAllLimitLines(); // reset all limit lines to avoid overlapping lines
                leftAxis.setAxisMaximum(maxReference);
                leftAxis.setAxisMinimum(minReference);

                mChart.setAutoScaleMinMaxEnabled(true);

                leftAxis.setStartAtZero(false);
                leftAxis.enableGridDashedLine(10f, 10f, 0f);

                // limit lines are drawn behind data (and not on top)
                leftAxis.setDrawLimitLinesBehindData(true);

                mChart.getAxisRight().setEnabled(false);


                // add data
                int numSections = ISCNIRScanSDK.ScanConfigInfo.numSections[0];

                if (numSections >= 2 && (!Float.isNaN(minReference) && !Float.isNaN(maxReference)) && Current_Scan_Method != ScanMethod.QuickSet)//Scan method : quickset only one section
                {
                    setDataSlew(mChart, mReferenceFloat, numSections);
                } else if (!Float.isNaN(minReference) && !Float.isNaN(maxReference)) {
                    setData(mChart, mXValues, mReferenceFloat, ChartType.INTENSITY);
                }


                mChart.animateX(2500, Easing.EasingOption.EaseInOutQuart);

                // get the legend (only possible after setting data)
                Legend l = mChart.getLegend();

                // modify the legend ...
                l.setForm(Legend.LegendForm.LINE);
                mChart.getLegend().setEnabled(false);

                return layout;
            } else {
                return layout;
            }
        }

        @Override
        public void destroyItem(ViewGroup collection, int position, Object view) {
            collection.removeView((View) view);
        }

        @Override
        public int getCount() {
            return CustomPagerEnum.values().length;
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return getString(R.string.reflectance);
                case 1:
                    return getString(R.string.absorbance);
                case 2:
                    return getString(R.string.intensity);
            }
            return null;
        }

    }

    private class BackGroundReciver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            finish();
        }
    }


    /**
     * Broadcast Receiver handling the disconnect event. If the Nano disconnects,
     * this activity should finish so that the user is taken back to the {@link HomeViewActivity}
     * and display a toast message
     */
    public class DisconnReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            Toast.makeText(mContext, R.string.nano_disconnected, Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    /**
     * Success set Lamp state(ISCNIRScanSDK.LampState should be called)
     */
    public class ReturnSetLampReceiver extends BroadcastReceiver {
        public void onReceive(Context context, Intent intent) {
            //Complete set lamp on,off,auto
            switch (Lamp_Info) {
                case ManualLamp:
                    break;
                case WarmDevice:
                    Lamp_Info = LampInfo.ManualLamp;
                    Boolean reference = false;
                    if (getStringPref(mContext, ISCNIRScanSDK.SharedPreferencesKeys.ReferenceScan, "Not").equals("ReferenceScan"))
                        reference = true;
                    if (reference)
                        storeStringPref(mContext, ISCNIRScanSDK.SharedPreferencesKeys.ReferenceScan, "Not");
                    //Synchronize time and download calibration coefficient and calibration matrix
                    ISCNIRScanSDK.SetCurrentTime();
                    break;
                case CloseWarmUpLampInScan:
                    WarmUp = false;
                    ISCNIRScanSDK.GetDeviceStatus();
                    break;
            }
        }
    }

    /**
     * Success set PGA( ISCNIRScanSDK.SetPGA should be called)
     */
    public class ReturnSetPGAReceiver extends BroadcastReceiver {
        public void onReceive(Context context, Intent intent) {
            //Complete set pga
        }
    }

    /**
     * Success set Scan Repeats( ISCNIRScanSDK.setScanAverage should be called)
     */
    public class ReturnSetScanRepeatsReceiver extends BroadcastReceiver {
        public void onReceive(Context context, Intent intent) {
            //Complete set scan repeats
        }
    }

    //endregion
}
