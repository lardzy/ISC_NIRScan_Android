package com.Innospectra.NanoScan;

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
import android.graphics.drawable.Drawable;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
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
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import static com.ISCSDK.ISCNIRScanSDK.Interpret_intensity;
import static com.ISCSDK.ISCNIRScanSDK.Interpret_length;
import static com.ISCSDK.ISCNIRScanSDK.Interpret_uncalibratedIntensity;
import static com.ISCSDK.ISCNIRScanSDK.Interpret_wavelength;
import static com.ISCSDK.ISCNIRScanSDK.Reference_Info;
import static com.ISCSDK.ISCNIRScanSDK.Scan_Config_Info;
import static com.ISCSDK.ISCNIRScanSDK.getBooleanPref;
import static com.ISCSDK.ISCNIRScanSDK.getStringPref;
import static com.ISCSDK.ISCNIRScanSDK.storeBooleanPref;
import static com.ISCSDK.ISCNIRScanSDK.storeStringPref;
import static com.Innospectra.NanoScan.DeviceStatusViewActivity.GetLampTimeString;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


/**
 * Activity controlling the Nano once it is connected
 * This activity allows a user to initiate a scan, as well as access other "connection-only"
 * settings. When first launched, the app will scan for a preferred device
 * for {@link com.ISCSDK.ISCNIRScanSDK#SCAN_PERIOD}, if it is not found, then it will start another "open"
 * scan for any Nano.
 *
 * If a preferred Nano has not been set, it will start a single scan. If at the end of scanning, a
 * Nano has not been found, a message will be presented to the user indicating and error, and the
 * activity will finish
 *
 * WARNING: This activity uses JNI function calls for communicating with the Spectrum C library, It
 * is important that the name and file structure of this activity remain unchanged, or the functions
 * will NOT work
 *
 * @author collinmast
 */
public class ScanViewActivity extends Activity {


    //region parameter
    private static Context mContext;
    private ProgressDialog barProgressDialog;
    private ProgressBar calProgress;
    private TextView progressBarinsideText;
    private AlertDialog alertDialog;
    private Menu mMenu;
    private ArrayList<Spinner> textileCompositions;
    private Map<String, Integer> textileCompositionsMap = new HashMap<>();
    private Spinner textileComposition, textileComposition_1, textileComposition_2, textileComposition_3, textileComposition_4;
    private ArrayList<ImageButton> imageButtons;
    private ImageButton imageButton, imageButton_1, imageButton_2, imageButton_3, imageButton_4;
    private ArrayList<EditText> editTexts;
    private EditText editTextNumber, editTextNumber_1, editTextNumber_2, editTextNumber_3, editTextNumber_4, fileNumber;
    private EditText et_simple_number;
    private ViewPager mViewPager;
    private String GraphLabel = "ISC Scan";
    private ArrayList<String> mXValues;
    private ArrayList<Entry> mIntensityFloat;
    private ArrayList<Entry> mAbsorbanceFloat;
    private ArrayList<Entry> mReflectanceFloat;
    private ArrayList<Entry> mReferenceFloat;
    private ArrayList<Float> mWavelengthFloat;
    private ArrayList<String> components;

    //! Tiva version is extend wavelength version or not
    public static Boolean isExtendVer = false;
    public static Boolean isExtendVer_PLUS = false;
    //! Control FW level to implement function
    public static ISCNIRScanSDK.FW_LEVEL_STANDARD fw_level_standard  = ISCNIRScanSDK.FW_LEVEL_STANDARD.LEVEL_0;
    public static ISCNIRScanSDK.FW_LEVEL_EXT fw_level_ext  = ISCNIRScanSDK.FW_LEVEL_EXT.LEVEL_EXT_1;
    public static ISCNIRScanSDK.FW_LEVEL_EXT_PLUS fw_level_ext_plus  = ISCNIRScanSDK.FW_LEVEL_EXT_PLUS.LEVEL_EXT_PLUS_1;

    public enum ScanMethod {
        Normal, QuickSet, Manual,Maintain
    }
    LampInfo Lamp_Info = LampInfo.ManualLamp;
    public enum LampInfo{
        WarmDevice,ManualLamp,CloseWarmUpLampInScan
    }
    //endregion

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
    private final BroadcastReceiver ReturnCurrentScanConfigurationDataReceiver = new ReturnCurrentScanConfigurationDataReceiver();
    private final BroadcastReceiver DeviceInfoReceiver = new DeviceInfoReceiver();
    private final BroadcastReceiver GetUUIDReceiver = new GetUUIDReceiver();
    private final BroadcastReceiver GetDeviceStatusReceiver = new GetDeviceStatusReceiver();
    private final BroadcastReceiver ScanConfReceiver = new ScanConfReceiver();
    private final BroadcastReceiver WriteScanConfigStatusReceiver = new WriteScanConfigStatusReceiver();
    private final BroadcastReceiver ScanConfSizeReceiver=  new ScanConfSizeReceiver();
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
    private final BroadcastReceiver GetPGAReceiver = new GetPGAReceiver();

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
    private final IntentFilter RetrunActivateStatusFilter = new IntentFilter(ISCNIRScanSDK.ACTION_RETURN_ACTIVATE);
    private final IntentFilter ReturnCurrentScanConfigurationDataFilter = new IntentFilter(ISCNIRScanSDK.RETURN_CURRENT_CONFIG_DATA);
    private final IntentFilter WriteScanConfigStatusFilter = new IntentFilter(ISCNIRScanSDK.ACTION_RETURN_WRITE_SCAN_CONFIG_STATUS);
    private final IntentFilter ReturnLampRampUpFilter = new IntentFilter(ISCNIRScanSDK.ACTION_RETURN_LAMP_RAMPUP_ADC);
    private final IntentFilter ReturnLampADCAverageFilter = new IntentFilter(ISCNIRScanSDK.ACTION_RETURN_LAMP_AVERAGE_ADC);
    private final IntentFilter ReturnLampRampUpADCTimeStampFilter = new IntentFilter(ISCNIRScanSDK.ACTION_RETURN_LAMP_ADC_TIMESTAMP);
    private final IntentFilter ReturnMFGNumFilter = new IntentFilter(ISCNIRScanSDK.ACTION_RETURN_MFGNUM);
    private final IntentFilter ReturnHWModelFilter = new IntentFilter(ISCNIRScanSDK.ACTION_RETURN_HWMODEL);
    public static final String NOTIFY_BACKGROUND = "com.Innospectra.NanoScan.ScanViewActivity.notifybackground";
    private String  NOTIFY_ISEXTVER = "com.Innospectra.NanoScan.ISEXTVER";
    //endregion
    //region parameter
    private ISCNIRScanSDK.ScanResults Scan_Spectrum_Data;
    private ISCNIRScanSDK mNanoBLEService;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeScanner mBluetoothLeScanner;
    private Handler mHandler;
    private fabricManagementActivity fabricManagementActivity;

    //Form HomeViewActivity->ScanViewActivity check chart view page parameter is initial or not
    Boolean init_viewpage_valuearray = false;
    //Record chart view page select
    int tabPosition = 0;

    //The name filter for BLE
    private static String DEVICE_NAME = "NIR";
    //Check the device is connected or not
    private boolean connected;
    //Get the device name you want to connect from the Settings page
    private String preferredDevice;
    //The active config of the device
    private ISCNIRScanSDK.ScanConfiguration activeConf;
    //Several configs in device were received
    private int receivedConfSize=-1;
    //Record the nnumber of the configs in the device
    private int storedConfSize;
    //Record the scan config list detail
    private ArrayList<ISCNIRScanSDK.ScanConfiguration> ScanConfigList = new ArrayList<ISCNIRScanSDK.ScanConfiguration>();
    //Record the scan config list detail from scan configuration page
    private ArrayList<ISCNIRScanSDK.ScanConfiguration> ScanConfigList_from_ScanConfiguration = new ArrayList<ISCNIRScanSDK.ScanConfiguration>();
    //Record the active config byte
    private byte ActiveConfigByte[];
    //Record the scan config list byte
    private ArrayList <byte []> ScanConfig_Byte_List = new ArrayList<>();
    //Record the scan config list byte from scan configuration page
    private ArrayList <byte []> ScanConfig_Byte_List_from_ScanConfiuration = new ArrayList<>();
    //Record the active config index
    int ActiveConfigindex;

    private float minWavelength=900;
    private float maxWavelength=1700;
    private int MINWAV=900;
    private int MAXWAV=1700;
    private float minAbsorbance=0;
    private float maxAbsorbance=2;
    private float minReflectance=-2;
    private float maxReflectance=2;
    private float minIntensity=-7000;
    private float maxIntensity=7000;
    private float minReference=-7000;
    private float maxReference=7000;
    private int numSections=0;

    private Button btn_normal;
    private Button btn_quickset;
    private Button btn_manual;
    private Button btn_maintain;
    private Button btn_scan;
    //Normal scan setting
    private LinearLayout ly_normal_config;
    private EditText filePrefix;
    private TextView tv_normal_scan_conf;
    private ToggleButton toggle_btn_continuous_scan;
    private TextView tv_normal_interval_time;
    private EditText et_normal_interval_time;
    private TextView tv_normal_repeat;
    private EditText et_normal_scan_repeat;
    private Button btn_normal_continuous_stop;
    //Manual sacn setting
    private LinearLayout ly_lamp;
    private View view_lamp_onoff;
    private TextView tv_manual_scan_conf;
    private LinearLayout ly_manual_conf;
    private ToggleButton toggle_button_manual_scan_mode;
    private ToggleButton toggle_button_manual_lamp;
    private EditText et_manual_lamptime;
    private EditText et_manual_pga;
    private EditText et_manual_repead;
    private ScanMethod Current_Scan_Method = ScanMethod.Normal;
    //Quick set scan setting
    private EditText et_quickset_lamptime;
    private Spinner spin_quickset_scan_method;
    private EditText et_quickset_spec_start;
    private EditText et_quickset_spec_end;
    private Spinner spin_quickset_scan_width;
    ArrayAdapter<CharSequence> adapter_width;
    private TextView tv_quickset_res;
    private EditText et_quickset_res;
    private EditText et_quickset_average_scan;
    private Spinner spin_quickset_exposure_time;
    private ToggleButton toggle_btn_quickset_continuous_scan_mode;
    private TextView tv_quickset_scan_interval_time;
    private EditText et_quickset_scan_interval_time;
    private TextView tv_quickset_continuous_repeat;
    private EditText et_quickset_continuous_scan_repeat;
    private Button btn_quickset_continuous_scan_stop;
    private Button btn_quickset_set_config;
    private Button query_fabric_components;

    int quickset_scan_method_index =0;
    int quickset_exposure_time_index =0;
    int quickset_scan_width_index = 2;
    private int continuous_count=0;
    Boolean show_finish_continous_dialog = false;
    public static boolean showActiveconfigpage = false;
    private int quickset_init_start_nm;
    private int quickset_init_end_nm;
    private int quickset_init_res;
    //Maintain (reference) scan setting
    private ToggleButton Toggle_Button_maintain_reference;

    //When read the activate status of the device, check this from HomeViewActivity->ScanViewActivity trigger or not
    private  String mainflag = "";
    //Check spectrum calibration coefficient is received or not
    Boolean downloadspecFlag = false;
    //Record spectrum calibration coefficient
    byte[] SpectrumCalCoefficients = new byte[144];
    //Allow AddScanConfigViewActivity to get the spectrum calibration coefficient to calculate max pattern
    public static byte []passSpectrumCalCoefficients = new byte[144];

    private byte[] refCoeff;
    private byte[] refMatrix;
    boolean stop_continuous = false;
    int MaxPattern = 0;
    Boolean isScan = false;
    //Record is set config for reference scan or not
    boolean reference_set_config = false;

    //Is go to Scan Configuration page or not
    public static boolean GotoScanConfigFlag = false;
    //On pause event trigger is go to other page or not
    private static Boolean GotoOtherPage = false;
    public  static Boolean isOldTiva = false;
    private Boolean WarmUp = false;
    private Boolean doubleSidedScanning = false;
    private Boolean IsScanPhase_1 = true;
    private TextView tv_reference_results;
    private final List<String[]> post_data = new ArrayList<>();
    private String[] row_data;
    private static String API_URL = null;
    private static String API_KEY = null;
    private final OkHttpClient client = new OkHttpClient();
    private Boolean isPostSampleDataRunning = false;
    private ArrayList<FabricComponent> fabricComponents = new ArrayList<>();


    //endregion
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_scan);
        mContext = this;
        DEVICE_NAME = ISCNIRScanSDK.getStringPref(mContext,ISCNIRScanSDK.SharedPreferencesKeys.DeviceFilter,"NIR");
        Bundle bundle = getIntent().getExtras();
        mainflag = bundle.getString("main" );
        WarmUp = bundle.getBoolean("warmup");
        storeBooleanPref(mContext, ISCNIRScanSDK.SharedPreferencesKeys.LockButton,false);
        // Set up the action bar.
        loadData();
        findViewById(R.id.layout_manual).setVisibility(View.GONE);
        findViewById(R.id.layout_quickset).setVisibility(View.GONE);
        findViewById(R.id.layout_maintain).setVisibility(View.GONE);

        calProgress = (ProgressBar) findViewById(R.id.calProgress);
        calProgress.setVisibility(View.VISIBLE);
        progressBarinsideText = (TextView)findViewById(R.id.progressBarinsideText);
        connected = false;
        Disable_Stop_Continous_button();

        filePrefix = (EditText) findViewById(R.id.et_prefix);

        btn_scan = (Button) findViewById(R.id.btn_scanAndPredict);

        btn_scan.setClickable(false);
        // 将按钮设置为不可用状态
        btn_scan.setBackgroundColor(ContextCompat.getColor(mContext, R.color.btn_unavailable));
        btn_scan.setOnClickListener(Button_Scan_Click);
        // 将界面设置为不可用状态
        setActivityTouchDisable(true);

        textileCompositions = new ArrayList<>();
        imageButtons = new ArrayList<>();
        editTexts = new ArrayList<>();
        row_data = new String[229];

        InitialNormalComponent();
        InitialQuicksetComponent();
        InitialManualComponent();
        InitialMaintainComponent();
        InitialScanMethodButtonComponent();
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
        LocalBroadcastManager.getInstance(mContext).registerReceiver(ReturnCurrentScanConfigurationDataReceiver, ReturnCurrentScanConfigurationDataFilter);
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
        LocalBroadcastManager.getInstance(mContext).registerReceiver(GetPGAReceiver, new IntentFilter(ISCNIRScanSDK.SEND_PGA));
        //endregion
    }
    private void loadData() {
        SharedPreferences sharedPreferences = getSharedPreferences("settingsViewStatus", Context.MODE_PRIVATE);
        API_URL = sharedPreferences.getString("API_URL", "http://192.168.115.230:8000/predict/");
        API_KEY = sharedPreferences.getString("API_KEY", "your-secret-api-key");
        }
    private Button.OnClickListener Continuous_Scan_Stop_Click = new Button.OnClickListener()
    {
        @Override
        public void onClick(View view) {
            stop_continuous = true;
        }
    };
    //region Scan device and connect
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
            final BluetoothManager bluetoothManager =
                    (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            mBluetoothAdapter = bluetoothManager.getAdapter();
            mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
            if(mBluetoothLeScanner == null){
                finish();
                Toast.makeText(ScanViewActivity.this, "Please ensure Bluetooth is enabled and try again", Toast.LENGTH_SHORT).show();
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

    /**
     * Callback function for Bluetooth scanning. This function provides the instance of the
     * Bluetooth device {@link BluetoothDevice} that was found, it's rssi, and advertisement
     * data (scanRecord).
     * When a Bluetooth device with the advertised name matching the
     * string DEVICE_NAME {@link ScanViewActivity#DEVICE_NAME} is found, a call is made to connect
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

    /**
     * Callback function for preferred Nano scanning. This function provides the instance of the
     * Bluetooth device {@link BluetoothDevice} that was found, it's rssi, and advertisement
     * data (scanRecord).
     * When a Bluetooth device with the advertised name matching the
     * string DEVICE_NAME {@link ScanViewActivity#DEVICE_NAME} is found, a call is made to connect
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

    /**
     * Scans for Bluetooth devices on the specified interval {@link ISCNIRScanSDK#SCAN_PERIOD}.
     * This function uses the handler {@link ScanViewActivity#mHandler} to delay call to stop
     * scanning until after the interval has expired. The start and stop functions take an
     * LeScanCallback parameter that specifies the callback function when a Bluetooth device
     * has been found {@link ScanViewActivity#mLeScanCallback}
     * @param enable Tells the Bluetooth adapter {@link ISCNIRScanSDK#mBluetoothAdapter} if
     *  it should start or stop scanning
     */
    @SuppressLint("MissingPermission")
    private void scanLeDevice(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if(mBluetoothLeScanner != null) {
                        mBluetoothLeScanner.stopScan(mLeScanCallback);
                        if (!connected) {
                            notConnectedDialog();
                        }
                    }
                }
            }, ISCNIRScanSDK.SCAN_PERIOD);
            if(mBluetoothLeScanner != null) {
                mBluetoothLeScanner.startScan(mLeScanCallback);
            }else{
                finish();
                Toast.makeText(ScanViewActivity.this, "Please ensure Bluetooth is enabled and try again", Toast.LENGTH_SHORT).show();
            }
        } else {
            mBluetoothLeScanner.stopScan(mLeScanCallback);
        }
    }

    /**
     * Scans for preferred Nano devices on the specified interval {@link ISCNIRScanSDK#SCAN_PERIOD}.
     * This function uses the handler {@link ScanViewActivity#mHandler} to delay call to stop
     * scanning until after the interval has expired. The start and stop functions take an
     * LeScanCallback parameter that specifies the callback function when a Bluetooth device
     * has been found {@link ScanViewActivity#mPreferredLeScanCallback}
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
            if(mBluetoothLeScanner == null)
            {
                notConnectedDialog();
            }
            else
            {
                mBluetoothLeScanner.startScan(mPreferredLeScanCallback);
            }

        } else {
            mBluetoothLeScanner.stopScan(mPreferredLeScanCallback);
        }
    }
    //endregion
    //region After connect to device
    /**
     * Custom receiver that will request the time once all of the GATT notifications have been subscribed to
     * If the connected device has saved the last setting, skip request the time. Read the device's  active config directly.
     */
    public class NotifyCompleteReceiver extends BroadcastReceiver {
        public void onReceive(Context context, Intent intent) {
            if(WarmUp)
            {
                ISCNIRScanSDK.ControlLamp(ISCNIRScanSDK.LampState.ON);
                Lamp_Info = LampInfo.WarmDevice;
            }
            else
            {
                Boolean reference = false;
                if(getStringPref(mContext, ISCNIRScanSDK.SharedPreferencesKeys.ReferenceScan, "Not").equals("ReferenceScan"))
                {
                    reference = true;
                }
                if(preferredDevice.equals(HomeViewActivity.storeCalibration.device) && reference == false)
                {
                    refCoeff = HomeViewActivity.storeCalibration.storrefCoeff;
                    refMatrix = HomeViewActivity.storeCalibration.storerefMatrix;
                    ArrayList<ISCNIRScanSDK.ReferenceCalibration> refCal = new ArrayList<>();
                    refCal.add(new ISCNIRScanSDK.ReferenceCalibration(refCoeff, refMatrix));
                    ISCNIRScanSDK.ReferenceCalibration.writeRefCalFile(mContext, refCal);
                    calProgress.setVisibility(View.INVISIBLE);
                    barProgressDialog = new ProgressDialog(ScanViewActivity.this);
                     //Get active config
		            ISCNIRScanSDK.ShouldDownloadCoefficient = false;
		            ISCNIRScanSDK.SetCurrentTime();
                }
                else
                {
                    if(reference == true)
                    {
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
                barProgressDialog = new ProgressDialog(ScanViewActivity.this);
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
                barProgressDialog = new ProgressDialog(ScanViewActivity.this);
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
    private class  GetActiveScanConfReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            ActiveConfigindex = intent.getByteArrayExtra(ISCNIRScanSDK.EXTRA_ACTIVE_CONF)[0];
            if(ScanConfigList.size()!=0)
            {
                GetActiveConfigOnResume();
            }
            else
            {
                //Get the number of scan config and scan config data
                ISCNIRScanSDK.GetScanConfig();
            }
        }
    }
    /**
     * Get the number of scan config(ISCNIRScanSDK.GetScanConfig() should be called)
     */
    private class  ScanConfSizeReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            storedConfSize = intent.getIntExtra(ISCNIRScanSDK.EXTRA_CONF_SIZE, 0);
        }
    }
    /**
     *Get the scan config data(ISCNIRScanSDK.GetScanConfig() should be called)
     */
    private class ScanConfReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            receivedConfSize++;
            ScanConfig_Byte_List.add(intent.getByteArrayExtra(ISCNIRScanSDK.EXTRA_DATA));
            ScanConfigList.add(ISCNIRScanSDK.scanConf);

            if (storedConfSize>0 && receivedConfSize==0) {
                barProgressDialog.dismiss();
                barProgressDialog = new ProgressDialog(ScanViewActivity.this);
                barProgressDialog.setTitle(getString(R.string.reading_configurations));
                barProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                barProgressDialog.setProgress(0);
                barProgressDialog.setMax(storedConfSize);
                barProgressDialog.setCancelable(false);
                barProgressDialog.show();
            } else {
                barProgressDialog.setProgress(receivedConfSize+1);
            }
            if (barProgressDialog.getProgress() == barProgressDialog.getMax() || barProgressDialog.getMax()==1)
            {
                for(int i=0;i<ScanConfigList.size();i++)
                {
                    int ScanConfigIndextoByte = (byte)ScanConfigList.get(i).getScanConfigIndex();
                    if(ActiveConfigindex == ScanConfigIndextoByte )
                    {
                        activeConf = ScanConfigList.get(i);
                        ActiveConfigByte = ScanConfig_Byte_List.get(i);
                    }
                }
                barProgressDialog.dismiss();
                mMenu.findItem(R.id.action_settings).setEnabled(true);
                storeStringPref(mContext, ISCNIRScanSDK.SharedPreferencesKeys.scanConfiguration, ISCNIRScanSDK.scanConf.getConfigName());
                tv_normal_scan_conf.setText(activeConf.getConfigName());
                tv_manual_scan_conf.setText(activeConf.getConfigName());
                if(downloadspecFlag ==false)
                {
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

    String model_name="";
    String serial_num = "";
    String HWrev = "";
    String Tivarev ="";
    String Specrev = "";
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
            if(Tivarev.substring(0,1) .equals("5"))
            {
                isExtendVer_PLUS = true;
                isExtendVer = false;
            }
            else if(Tivarev.substring(0,1) .equals("3") && (HWrev.substring(0,1).equals("E")|| HWrev.substring(0,1).equals("O")))
            {
                isExtendVer_PLUS = false;
                isExtendVer = true;
            }
            else
            {
                isExtendVer_PLUS = false;
                isExtendVer = false;
            }
            if((isExtendVer||isExtendVer_PLUS) && serial_num.length()>8)
                serial_num = serial_num.substring(0,8);
            else if(!isExtendVer_PLUS&&!isExtendVer && serial_num.length()>7)
                serial_num = serial_num.substring(0,7);
            if(HWrev.substring(0,1).equals("N"))
                Dialog_Pane_Finish("Not support","Not to support the N version of the main board.\nWill go to the home page.");
            else
            {
                if(isExtendVer_PLUS)
                {
                    adapter_width = ArrayAdapter.createFromResource(mContext,
                            R.array.scan_width_plus, android.R.layout.simple_spinner_item);
                    adapter_width.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spin_quickset_scan_width.setAdapter(adapter_width);
                }
                else
                {
                    adapter_width = ArrayAdapter.createFromResource(mContext,
                            R.array.scan_width, android.R.layout.simple_spinner_item);
                    adapter_width.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spin_quickset_scan_width.setAdapter(adapter_width);
                }
                if(isExtendVer)
                    ISCNIRScanSDK.TIVAFW_EXT =  GetFWLevelEXT(Tivarev);
                else if(isExtendVer_PLUS)
                    ISCNIRScanSDK.TIVAFW_EXT_PLUS = GetFWLevelEXTPLUS(Tivarev);
                else
                    ISCNIRScanSDK.TIVAFW_STANDARD = GetFWLevelStandard(Tivarev);
                InitParameter();
                if(!isExtendVer_PLUS && !isExtendVer && fw_level_standard.compareTo(ISCNIRScanSDK.FW_LEVEL_STANDARD.LEVEL_0)==0)
                    Dialog_Pane_Finish("Firmware Out of Date","You must update the firmware on your NIRScan Nano to make this App working correctly!\n" +
                            "FW required version at least V2.4.4.\nDetected version is V" + Tivarev +".");
                else
                    ISCNIRScanSDK.GetMFGNumber();
            }
        }
    }
    /**
     *Get MFG Num (ISCNIRScanSDK.GetMFGNumber() should be called)
     */
    private byte MFG_NUM[];
    public class ReturnMFGNumReceiver extends BroadcastReceiver {

        public void onReceive(Context context, Intent intent) {
            MFG_NUM = intent.getByteArrayExtra(ISCNIRScanSDK.MFGNUM_DATA);
            //Tiva 2.5.x
            if((!isExtendVer_PLUS && !isExtendVer && fw_level_standard.compareTo(ISCNIRScanSDK.FW_LEVEL_STANDARD.LEVEL_4)>=0) || (isExtendVer && fw_level_ext.compareTo(ISCNIRScanSDK.FW_LEVEL_EXT.LEVEL_EXT_3)>=0))
                ISCNIRScanSDK.GetHWModel();
            else
                ISCNIRScanSDK.GetUUID();
        }
    }
    /**
     *Get HW Model (ISCNIRScanSDK.GetHWModel() should be called)
     */
    private String HW_Model="";
    public class ReturnHWModelReceiver extends BroadcastReceiver {

        public void onReceive(Context context, Intent intent) {
            byte[]byteHWMDEL = intent.getByteArrayExtra(ISCNIRScanSDK.HWMODEL_DATA);
            int len = 0;
            for(int i=0;i<byteHWMDEL.length;i++)
            {
                if(byteHWMDEL[i]==0)
                    break;
                else
                    len ++;
            }
            byte[] HWModel = new byte[len];
            for(int i=0;i<len;i++)
                HWModel[i] = byteHWMDEL[i];
            HW_Model = new String(HWModel, StandardCharsets.UTF_8);
            //Get the uuid of the device
            ISCNIRScanSDK.GetUUID();
        }
    }
    /**
     *  Define FW LEVEL according to tiva version
     * @param Tivarev  Tiva version of the device
     */
    private ISCNIRScanSDK.FW_LEVEL_STANDARD GetFWLevelStandard(String Tivarev)
    {
        String[] TivaArray= Tivarev.split(Pattern.quote("."));
        String split_hw[] = HWrev.split("\\.");
        fw_level_standard = ISCNIRScanSDK.FW_LEVEL_STANDARD.LEVEL_0;
        if(Integer.parseInt(TivaArray[1])>=5 && split_hw[0].equals("F"))
        {
                /*New Applications:
                  1. Use new command to read ADC value and timestamp
                 */
            fw_level_standard = ISCNIRScanSDK.FW_LEVEL_STANDARD.LEVEL_5;//>=2.5.X and main board ="F"
        }
        else if(Integer.parseInt(TivaArray[1])>=5)
        {
                /*New Applications:
                  1. Support get pga
                 */
            fw_level_standard = ISCNIRScanSDK.FW_LEVEL_STANDARD.LEVEL_4;//>=2.5.X
        }
        else if(Integer.parseInt(TivaArray[1])>=4 && Integer.parseInt(TivaArray[2])>=3 &&split_hw[0].equals("F"))
        {
                /*New Applications:
                  1. Support read ADC value
                 */
            fw_level_standard = ISCNIRScanSDK.FW_LEVEL_STANDARD.LEVEL_3;//>=2.4.4 and main board ="F"
        }
        else if((Integer.parseInt(TivaArray[1])>=4 && Integer.parseInt(TivaArray[2])>=3) || Integer.parseInt(TivaArray[1])>=5)
        {
                /*New Applications:
                  1. Add Lock Button
                 */
            fw_level_standard = ISCNIRScanSDK.FW_LEVEL_STANDARD.LEVEL_2;//>=2.4.4
        }
        else if((TivaArray.length==3 && Integer.parseInt(TivaArray[1])>=1)|| (TivaArray.length==4 &&  Integer.parseInt(TivaArray[3])>=67))//>=2.1.0.67
        {
            //New Applications:
            // 1. Support activate state

            fw_level_standard = ISCNIRScanSDK.FW_LEVEL_STANDARD.LEVEL_1;
        }
        else
        {
            fw_level_standard = ISCNIRScanSDK.FW_LEVEL_STANDARD.LEVEL_0;
        }
        return fw_level_standard;
    }
    private ISCNIRScanSDK.FW_LEVEL_EXT GetFWLevelEXT(String Tivarev)
    {
        String[] TivaArray= Tivarev.split(Pattern.quote("."));
        String split_hw[] = HWrev.split("\\.");
        fw_level_ext = ISCNIRScanSDK.FW_LEVEL_EXT.LEVEL_EXT_1;
        if(Integer.parseInt(TivaArray[1])>=5 && split_hw[0].equals("O"))
        {
                 /*New Applications:
                  1. Use new command to read ADC value and timestamp
                 */
            fw_level_ext = ISCNIRScanSDK.FW_LEVEL_EXT.LEVEL_EXT_4;//>=3.5.X and main board = "O"
        }
        else if(Integer.parseInt(TivaArray[1])>=5)
        {
                 /*New Applications:
                  1. Support get pga
                 */
            fw_level_ext = ISCNIRScanSDK.FW_LEVEL_EXT.LEVEL_EXT_3;//>=3.5.X
        }
        else if(Integer.parseInt(TivaArray[1])>=3 && split_hw[0].equals("O"))
        {
                 /*New Applications:
                  1. Support read ADC value
                 */
            fw_level_ext = ISCNIRScanSDK.FW_LEVEL_EXT.LEVEL_EXT_2;//>=3.3.0 and main board = "O"
        }
        else if(Integer.parseInt(TivaArray[1])>=3)
        {
                /*New Applications:
                  1. Add Lock Button
                 */
            fw_level_ext = ISCNIRScanSDK.FW_LEVEL_EXT.LEVEL_EXT_1;//>=3.3.0
        }
        else if(Integer.parseInt(TivaArray[1])==2 && Integer.parseInt(TivaArray[2])==1 )
            fw_level_ext = ISCNIRScanSDK.FW_LEVEL_EXT.LEVEL_EXT_1;//==3.2.1

        return fw_level_ext;
    }
    private ISCNIRScanSDK.FW_LEVEL_EXT_PLUS GetFWLevelEXTPLUS(String Tivarev)
    {
        String[] TivaArray= Tivarev.split(Pattern.quote("."));
        String split_hw[] = HWrev.split("\\.");
        fw_level_ext_plus = ISCNIRScanSDK.FW_LEVEL_EXT_PLUS.LEVEL_EXT_PLUS_1;
        return fw_level_ext_plus;
    }
    /**
     *  Determine the wavelength range of the device and parameter initialization
     */
    private void InitParameter()
    {
        if(isExtendVer)
        {
            minWavelength = 1350;
            maxWavelength = 2150;
            MINWAV = 1350;
            MAXWAV = 2150;
        }
        else if(isExtendVer_PLUS)
        {
            minWavelength = 1600;
            maxWavelength = 2400;
            MINWAV = 1600;
            MAXWAV = 2400;
        }
        else
        {
            minWavelength = 900;
            maxWavelength = 1700;
            MINWAV = 900;
            MAXWAV = 1700;
        }
        et_quickset_spec_start.setText(Integer.toString(MINWAV));
        et_quickset_spec_end.setText(Integer.toString(MAXWAV));
        quickset_init_start_nm = (Integer.parseInt(et_quickset_spec_start.getText().toString()));
        quickset_init_end_nm = (Integer.parseInt(et_quickset_spec_end.getText().toString()));
        //not support lock button
        if(!isExtendVer_PLUS && !isExtendVer && fw_level_standard.compareTo(ISCNIRScanSDK.FW_LEVEL_STANDARD.LEVEL_1) <=0)
            storeBooleanPref(mContext, ISCNIRScanSDK.SharedPreferencesKeys.LockButton,false);
    }
    /**
     * Get the device uuid(ISCNIRScanSDK.GetUUID()should be called)
     */
    String uuid="";
    public class GetUUIDReceiver extends BroadcastReceiver {
        public void onReceive(Context context, Intent intent) {

            byte buf[] = intent.getByteArrayExtra(ISCNIRScanSDK.EXTRA_DEVICE_UUID);
            for(int i=0;i<buf.length;i++)
            {
                uuid += Integer.toHexString( 0xff & buf[i] );
                if(i!= buf.length-1)
                {
                    uuid +=":";
                }
            }
            CheckIsOldTIVA();
            if(!isOldTiva)
            {
                //Get the device is activate or not
                ISCNIRScanSDK.ReadActivateState();
            }
            else
            {
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
            if(mainflag!="")//Only from HomeViewActivity->ScanViewActivity should do this
            {
                //set active scan config avoid the device use wpf or winform local config to set config in device
                ISCNIRScanSDK.SetActiveConfig();
                mainflag = "";
            }
            byte state[] = intent.getByteArrayExtra(ISCNIRScanSDK.RETURN_READ_ACTIVATE_STATE);
            if(state[0] == 1)
            {
                Handler handler = new Handler();
                handler.postDelayed(new Runnable(){

                    @Override
                    public void run() {
                        SetDeviceButtonStatus();
                        Dialog_Pane_OpenFunction("设备已激活","设备高级功能全部解锁。");
                    }}, 200);
                mMenu.findItem(R.id.action_settings).setEnabled(true);
                mMenu.findItem(R.id.action_key).setEnabled(true);
                storeStringPref(mContext, ISCNIRScanSDK.SharedPreferencesKeys.Activacatestatus, "Activated.");
            }
            else
            {
                String licensekey = getStringPref(mContext, ISCNIRScanSDK.SharedPreferencesKeys.licensekey, null);
                //The device is locked but saved license key
                if(licensekey!=null && licensekey!="")
                {
                    calProgress.setVisibility(View.VISIBLE);
                    String filterdata = filterDate(licensekey);
                    final byte data[] = hexToBytes(filterdata);
                    Handler handler = new Handler();
                    handler.postDelayed(new Runnable(){

                        @Override
                        public void run() {
                            ISCNIRScanSDK.SetLicenseKey(data);
                        }}, 200);
                }
                else
                {
                    Handler handler = new Handler();
                    handler.postDelayed(new Runnable(){

                        @Override
                        public void run() {
                            SetDeviceButtonStatus();
                            Dialog_Pane("Unlock device","Some functions are locked.");
                        }}, 200);
                    mMenu.findItem(R.id.action_settings).setEnabled(true);
                    mMenu.findItem(R.id.action_key).setEnabled(true);
                    storeStringPref(mContext, ISCNIRScanSDK.SharedPreferencesKeys.Activacatestatus, "Function is locked.");
                    closeFunction();
                }
            }
        }
    }
    /**
     *  Get the activate state of the device(ISCNIRScanSDK.SetLicenseKey(data) should be called)
     */
    public class RetrunActivateStatusReceiver extends BroadcastReceiver {
        public void onReceive(Context context, Intent intent) {
            mMenu.findItem(R.id.action_settings).setEnabled(true);
            mMenu.findItem(R.id.action_key).setEnabled(true);
            calProgress.setVisibility(View.GONE);
            byte state[] = intent.getByteArrayExtra(ISCNIRScanSDK.RETURN_ACTIVATE_STATUS);
            if(state[0] == 1)
            {
                SetDeviceButtonStatus();
                Dialog_Pane_OpenFunction("设备已激活","设备高级功能全部解锁。");
                storeStringPref(mContext, ISCNIRScanSDK.SharedPreferencesKeys.Activacatestatus, "Activated.");
            }
            else
            {
                SetDeviceButtonStatus();
                Dialog_Pane("Unlock device","Some functions are locked.");
                storeStringPref(mContext, ISCNIRScanSDK.SharedPreferencesKeys.Activacatestatus, "Function is locked.");
                closeFunction();
            }
        }
    }
    private void CheckIsOldTIVA()
    {
        String[] TivaArray= Tivarev.split(Pattern.quote("."));
        try {
            if(!isExtendVer_PLUS && !isExtendVer && (Integer.parseInt(TivaArray[1])<4 || Integer.parseInt(TivaArray[1])<4))//Tiva <2.4.4(the newest version)
            {
                isOldTiva = true;
                Dialog_Pane_OldTIVA("Firmware Out of Date", "You must update the firmware on your NIRScan Nano to make this App working correctly!\n" +
                        "FW required version at least V2.4.4\nDetected version is V" + Tivarev + "\nDo you still want to continue?");
            }
            else
                isOldTiva = false;
        }catch (Exception e)
        {

        };
    }
    //endregion
    //region title bar
    /**
     * Initial chart view pager and title bar event
     */
    private void TitleBarEvent()
    {
        //Set up title bar and  enable tab navigation
        ActionBar ab = getActionBar();
        if (ab != null) {
            ab.setDisplayHomeAsUpEnabled(true);
            ab.setTitle(getString(R.string.new_scan));
            ab.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

            mViewPager = (ViewPager) findViewById(R.id.viewpager);
            mViewPager.setOffscreenPageLimit(2);

            // Create a tab listener that is called when the user changes tabs.
            ActionBar.TabListener tl = new ActionBar.TabListener() {
                @Override
                public void onTabSelected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
                    //1.if select tab0 then scan, onTabSelected can't invoke. But select other tab can invoke.
                    if(isScan)
                    {
                        if(tabPosition == 0) //2. select tab0 then scan. Choose tab1.at this time isscan =true but tabPosition will be equal to 0 will cause page error.
                        //So if tabPosition is 0, it will choose to do mViewPager.setCurrentItem (tab.getPosition ()); to see the current state
                        {
                            mViewPager.setCurrentItem(tab.getPosition());
                        }
                        else//The tabPosition will record the current tab and then update after the scan
                        {
                            mViewPager.setCurrentItem(tabPosition);
                        }
                        isScan = false;
                    }
                    else
                    {
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
                ab.addTab(
                        ab.newTab()
                                .setText(getResources().getStringArray(R.array.graph_tab_index)[i])
                                .setTabListener(tl));
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
    private void InitialFabricComposition(){
        // 获得下拉框内容
        SharedPreferences sharedPreferences = getSharedPreferences("my_preferences", MODE_PRIVATE);
        String componentsString = sharedPreferences.getString("components", "");
        if (!componentsString.equals("")){
            components = new ArrayList<>(Arrays.asList(componentsString.split(",")));
            components.add(0, "");
        }else {
            components = new ArrayList<>();
        }
        // 获取双面扫描状态
        SharedPreferences sharedPreferences1 = getSharedPreferences("switch_status", MODE_PRIVATE);
        doubleSidedScanning = sharedPreferences1.getBoolean("switch_status",true);

//        System.out.println("读取到纤维长度：" +components.size());
        if (components != null){
            for (int i = 0; i < components.size(); i++) {
                System.out.println("读取到纤维列表：" + components.get(i));
            }
        }
        // 绑定下拉框
        ArrayAdapter adapter = new ArrayAdapter(this, android.R.layout.simple_spinner_item, components);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        textileComposition = (Spinner) findViewById(R.id.textileComposition);
        textileComposition.setAdapter(adapter);
        textileComposition_1 = (Spinner) findViewById(R.id.textileComposition_1);
        textileComposition_1.setAdapter(adapter);
        textileComposition_2 = (Spinner) findViewById(R.id.textileComposition_2);
        textileComposition_2.setAdapter(adapter);
        textileComposition_3 = (Spinner) findViewById(R.id.textileComposition_3);
        textileComposition_3.setAdapter(adapter);
        textileComposition_4 = (Spinner) findViewById(R.id.textileComposition_4);
        textileComposition_4.setAdapter(adapter);
        tv_reference_results = findViewById(R.id.tv_reference_results);
        tv_reference_results.setText("参考结果：");
        imageButton = (ImageButton) findViewById(R.id.imageButton);
        imageButton_1 = (ImageButton) findViewById(R.id.imageButton_1);
        imageButton_2 = (ImageButton) findViewById(R.id.imageButton_2);
        imageButton_3 = (ImageButton) findViewById(R.id.imageButton_3);
        imageButton_4 = (ImageButton) findViewById(R.id.imageButton_4);
        query_fabric_components = (Button) findViewById(R.id.query_fabric_components);
        query_fabric_components.setOnClickListener(v -> {
            // 这里放置查询纤维成分的代码
            getFabricComponents(et_simple_number);
            query_fabric_components.setEnabled(false);
        });
        fileNumber = (EditText) findViewById(R.id.fileNumber);

        editTextNumber = (EditText) findViewById(R.id.editTextNumber);
        editTextNumber.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Do nothing
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Do nothing
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.toString().equals("")) {
                    return;
                }
                validateInput(s.toString(), editTextNumber);
            }
        });
        editTextNumber_1 = (EditText) findViewById(R.id.editTextNumber_1);
        editTextNumber_1.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Do nothing
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Do nothing
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.toString().equals("")) {
                    return;
                }
                validateInput(s.toString(), editTextNumber_1);
            }
        });
        editTextNumber_2 = (EditText) findViewById(R.id.editTextNumber_2);
        editTextNumber_2.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Do nothing
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Do nothing
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.toString().equals("")) {
                    return;
                }
                validateInput(s.toString(), editTextNumber_2);
            }
        });
        editTextNumber_3 = (EditText) findViewById(R.id.editTextNumber_3);
        editTextNumber_3.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Do nothing
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Do nothing
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.toString().equals("")) {
                    return;
                }
                validateInput(s.toString(), editTextNumber_3);
            }
        });
        editTextNumber_4 = (EditText) findViewById(R.id.editTextNumber_4);
        editTextNumber_4.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Do nothing
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Do nothing
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.toString().equals("")) {
                    return;
                }
                validateInput(s.toString(), editTextNumber_4);
            }
        });

        imageButton.setOnClickListener(v -> editTextNumber.setText(""));
        imageButton_1.setOnClickListener(v -> editTextNumber_1.setText(""));
        imageButton_2.setOnClickListener(v -> editTextNumber_2.setText(""));
        imageButton_3.setOnClickListener(v -> editTextNumber_3.setText(""));
        imageButton_4.setOnClickListener(v -> editTextNumber_4.setText(""));

        textileCompositions.add(textileComposition);
        textileCompositions.add(textileComposition_1);
        textileCompositions.add(textileComposition_2);
        textileCompositions.add(textileComposition_3);
        textileCompositions.add(textileComposition_4);
        imageButtons.add(imageButton);
        imageButtons.add(imageButton_1);
        imageButtons.add(imageButton_2);
        imageButtons.add(imageButton_3);
        imageButtons.add(imageButton_4);
        editTexts.add(editTextNumber);
        editTexts.add(editTextNumber_1);
        editTexts.add(editTextNumber_2);
        editTexts.add(editTextNumber_3);
        editTexts.add(editTextNumber_4);
    }
    public int getMaxNumberFromFilenames() {
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
     * Initial the button event of the scan mode
     */
    private boolean validateInput(String input, EditText editText) {
        try {
            // 检查是否为无效数字
            float value = Float.parseFloat(input);
            if (value > 100.0f) {
                // Value is greater than 100, show error message
                editText.setError("数字必须小于等于 100！");
                return false;
            }

            // 检查精度是否超过1位小数
            int decimalIndex = input.indexOf(".");
            if (decimalIndex != -1 && input.length() - decimalIndex - 1 > 1) {
                // Input has more than 1 decimal place, show error message
                editText.setError("至多精确到一位小数！");
                return false;
            }
            return true;
        } catch (NumberFormatException e) {
            // 输入了无效内容
            editText.setError("无效内容！");
            return false;
        }
    }
    private void InitialScanMethodButtonComponent()
    {
        btn_normal = (Button) findViewById(R.id.btn_normal);
        btn_quickset = (Button) findViewById(R.id.btn_quickset);
        btn_manual = (Button) findViewById(R.id.btn_manual);
        btn_maintain = (Button) findViewById(R.id.btn_maintain);

        btn_normal.setOnClickListener(Button_Normal_Click);
        btn_normal.setClickable(false);
        btn_normal.setBackgroundColor(ContextCompat.getColor(mContext, R.color.btn_unavailable));

        btn_quickset.setOnClickListener(Button_Quicket_Click);
        btn_quickset.setClickable(false);
        btn_quickset.setBackgroundColor(ContextCompat.getColor(mContext, R.color.btn_unavailable));

        btn_manual.setOnClickListener(Button_Manual_Click);
        btn_manual.setClickable(false);
        btn_manual.setBackgroundColor(ContextCompat.getColor(mContext, R.color.btn_unavailable));

        btn_maintain.setOnClickListener(Button_Maintain_Click);
        btn_maintain.setClickable(false);
        btn_maintain.setBackgroundColor(ContextCompat.getColor(mContext, R.color.btn_unavailable));
    }
    /**
     * Initial the component of the normal scan mode
     */
    private void InitialNormalComponent()
    {
        tv_normal_scan_conf = (TextView) findViewById(R.id.tv_scan_conf);
        ly_normal_config = (LinearLayout) findViewById(R.id.ll_conf);
        tv_normal_repeat = (TextView) findViewById(R.id.tv_normal_repeat);
        toggle_btn_continuous_scan = (ToggleButton) findViewById(R.id.btn_doubleside);
        tv_normal_interval_time = (TextView) findViewById(R.id.tv_normal_interval_time);
        et_normal_interval_time = (EditText) findViewById(R.id.et_normal_interval_time);
        et_normal_scan_repeat = (EditText) findViewById(R.id.et_normal_repeat);
        btn_normal_continuous_stop = (Button)findViewById(R.id.btn_continuous_stop);
        et_simple_number = (EditText) findViewById(R.id.et_simple_number);

        et_simple_number.setOnTouchListener(et_simple_number_OnTouch);
        et_simple_number.addTextChangedListener(et_simple_number_OnTextChange);
        et_simple_number.setOnEditorActionListener(et_simple_number_OnEdit);
        ly_normal_config.setClickable(false);
        ly_normal_config.setOnClickListener(Normal_Config_Click);
        toggle_btn_continuous_scan.setChecked(getBooleanPref(mContext, ISCNIRScanSDK.SharedPreferencesKeys.continuousScan, false));
        toggle_btn_continuous_scan.setOnClickListener(Normal_Continuous_Scan_Click);
        btn_normal_continuous_stop.setOnClickListener(Continuous_Scan_Stop_Click);
        et_normal_scan_repeat.setEnabled(toggle_btn_continuous_scan.isChecked());
        et_normal_scan_repeat.setOnEditorActionListener(Normal_Continuous_Scan_Repeat_OnEdit);
        tv_normal_repeat.setEnabled(toggle_btn_continuous_scan.isChecked());
        tv_normal_interval_time.setEnabled(toggle_btn_continuous_scan.isChecked());
        et_normal_interval_time.setEnabled(toggle_btn_continuous_scan.isChecked());
    }
    /**
     * Initial the component of the quick set  scan mode
     */
    private void InitialQuicksetComponent()
    {
        et_quickset_lamptime = (EditText)findViewById(R.id.et_prefix_lamp_quickset);
        spin_quickset_scan_method = (Spinner)findViewById(R.id.spin_scan_method);
        et_quickset_spec_start = (EditText)findViewById(R.id.et_spec_start);
        et_quickset_spec_end = (EditText)findViewById(R.id.et_spec_end);
        spin_quickset_scan_width = (Spinner)findViewById(R.id.spin_scan_width);
        et_quickset_res = (EditText)findViewById(R.id.et_res);
        et_quickset_average_scan = (EditText)findViewById(R.id.et_aver_scan);
        spin_quickset_exposure_time = (Spinner)findViewById(R.id.spin_time);
        toggle_btn_quickset_continuous_scan_mode = (ToggleButton)findViewById(R.id.btn_continuous_scan_mode);
        tv_quickset_scan_interval_time = (TextView)findViewById(R.id.tv_quickset_scan_interval_time);
        et_quickset_scan_interval_time = (EditText)findViewById(R.id.scan_interval_time);
        tv_quickset_continuous_repeat = (TextView)findViewById(R.id.tv_quickset_continuous_repeat);
        et_quickset_continuous_scan_repeat = (EditText)findViewById(R.id.et_repeat_quick);
        btn_quickset_continuous_scan_stop = (Button)findViewById(R.id.btn_continuous_stop_quick);
        btn_quickset_set_config = (Button)findViewById(R.id.btn_set_value);
        tv_quickset_res = (TextView)findViewById(R.id.tv_res);

        et_quickset_lamptime.setOnEditorActionListener(Quickset_Lamp_Time_OnEditor);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.scan_method_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spin_quickset_scan_method.setAdapter(adapter);
        spin_quickset_scan_method.setOnItemSelectedListener(Quickset_Scan_Method_ItemSelect);
        et_quickset_spec_start.setOnEditorActionListener(Quickset_Spec_Start_OnEditor);
        et_quickset_spec_end.setOnEditorActionListener(Quickset_Spec_End_OnEditor);
        adapter_width= ArrayAdapter.createFromResource(this,
                R.array.scan_width, android.R.layout.simple_spinner_item);
        adapter_width.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spin_quickset_scan_width.setAdapter(adapter_width);
        spin_quickset_scan_width.setOnItemSelectedListener(Quickset_Scan_Width_ItemSelect);
        et_quickset_res.setOnEditorActionListener(Quickset_Res_OnEditor);
        ArrayAdapter<CharSequence> adapter_time = ArrayAdapter.createFromResource(this,
                R.array.exposure_time, android.R.layout.simple_spinner_item);
        adapter_time.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spin_quickset_exposure_time.setAdapter(adapter_time);
        spin_quickset_exposure_time.setOnItemSelectedListener(Quickset_Exposure_Time_ItemSelect);
        toggle_btn_quickset_continuous_scan_mode.setOnClickListener(QuickSet_Continuous_Scan_Click);
        et_quickset_continuous_scan_repeat.setOnEditorActionListener(QuickSet_Continuous_Scan_Repeat_OnEdit);
        btn_quickset_continuous_scan_stop.setOnClickListener(Continuous_Scan_Stop_Click);
        btn_quickset_set_config.setOnClickListener(Quickset_Set_Config_Click);

        quickset_init_start_nm = (Integer.parseInt(et_quickset_spec_start.getText().toString()));
        quickset_init_end_nm = (Integer.parseInt(et_quickset_spec_end.getText().toString()));
        quickset_init_res = (Integer.parseInt(et_quickset_res.getText().toString()));
        tv_quickset_continuous_repeat.setEnabled(toggle_btn_quickset_continuous_scan_mode.isChecked());
        et_quickset_continuous_scan_repeat.setEnabled(toggle_btn_quickset_continuous_scan_mode.isChecked());
        tv_quickset_scan_interval_time.setEnabled(toggle_btn_quickset_continuous_scan_mode.isChecked());
        et_quickset_scan_interval_time.setEnabled(toggle_btn_quickset_continuous_scan_mode.isChecked());
    }
    /**
     * Initial the component of the manual scan mode
     */
    private void InitialManualComponent()
    {
        ly_lamp = (LinearLayout)findViewById(R.id.ly_lamp);
        view_lamp_onoff = (View) findViewById(R.id.view_lamp_onoff);
        tv_manual_scan_conf = (TextView)findViewById(R.id.tv_scan_conf_manual) ;
        toggle_button_manual_scan_mode = (ToggleButton) findViewById(R.id.btn_scan_mode);
        toggle_button_manual_lamp = (ToggleButton) findViewById(R.id.btn_lamp);
        et_manual_lamptime = (EditText) findViewById(R.id.et_prefix_lamp);
        et_manual_pga = (EditText) findViewById(R.id.et_pga);
        et_manual_repead = (EditText) findViewById(R.id.et_repeat);
        ly_manual_conf = (LinearLayout)findViewById(R.id.ly_conf_manual);

        toggle_button_manual_scan_mode.setOnClickListener(Toggle_Button_Manual_ScanMode_Click);
        toggle_button_manual_lamp.setOnCheckedChangeListener(Toggle_Button_Manual_Lamp_Changed);
        et_manual_pga.setOnEditorActionListener(Manual_PGA_OnEditor);
        et_manual_repead.setOnEditorActionListener(Manual_Repeat_OnEditor);
        et_manual_lamptime.setOnEditorActionListener(Manual_Lamptime_OnEditor);
        ly_manual_conf.setOnClickListener(Manual_Config_Click);
        toggle_button_manual_lamp.setEnabled(false);
        et_manual_repead.setEnabled(false);
        et_manual_pga.setEnabled(false);
        et_manual_lamptime.setEnabled(false);
    }
    /**
     * Initial the component of the matain(reference) scan mode
     */
    private void InitialMaintainComponent()
    {
        Toggle_Button_maintain_reference = (ToggleButton)findViewById(R.id.btn_reference);
    }
    private void DisableLinearComponet(LinearLayout layout)
    {

        for (int i = 0; i < layout.getChildCount(); i++) {
            View child = layout.getChildAt(i);
            child.setEnabled(false);
        }
    }
    private void DisableAllComponent()
    {
        //normal------------------------------------------------
        LinearLayout layout = (LinearLayout) findViewById(R.id.ll_user);
        DisableLinearComponet(layout);
        layout = (LinearLayout) findViewById(R.id.ll_fileNumberLayout);
        DisableLinearComponet(layout);
        layout = (LinearLayout) findViewById(R.id.ll_simple_number);
        DisableLinearComponet(layout);

        textileComposition.setEnabled(false);
        textileComposition_1.setEnabled(false);
        textileComposition_2.setEnabled(false);
        textileComposition_3.setEnabled(false);
        textileComposition_4.setEnabled(false);
        editTextNumber.setEnabled(false);
        editTextNumber_1.setEnabled(false);
        editTextNumber_2.setEnabled(false);
        editTextNumber_3.setEnabled(false);
        editTextNumber_4.setEnabled(false);
        imageButton.setEnabled(false);
        imageButton_1.setEnabled(false);
        imageButton_2.setEnabled(false);
        imageButton_3.setEnabled(false);
        imageButton_4.setEnabled(false);

        // layout = (LinearLayout) findViewById(R.id.ll_os);
        // DisableLinearComponet(layout);
        layout = (LinearLayout) findViewById(R.id.ll_doubleSideScan);
        DisableLinearComponet(layout);
        layout = (LinearLayout) findViewById(R.id.ly_normal_interval_time);
        DisableLinearComponet(layout);
        layout = (LinearLayout) findViewById(R.id.ly_normal_repeat);
        DisableLinearComponet(layout);
        layout = (LinearLayout) findViewById(R.id.ll_continuous_stop);
        DisableLinearComponet(layout);
        //manual-----------------------------------------------------------------------
        layout = (LinearLayout) findViewById(R.id.ll_conf);
        DisableLinearComponet(layout);
        layout = (LinearLayout) findViewById(R.id.ll_prefix_manual);
        DisableLinearComponet(layout);
        layout = (LinearLayout) findViewById(R.id.ly_scan_mode);
        DisableLinearComponet(layout);
        layout = (LinearLayout) findViewById(R.id.ly_lamp);
        DisableLinearComponet(layout);
        layout = (LinearLayout) findViewById(R.id.ly_pga);
        DisableLinearComponet(layout);
        layout = (LinearLayout) findViewById(R.id.ly_repeat);
        DisableLinearComponet(layout);
        layout = (LinearLayout) findViewById(R.id.ly_conf_manual);
        DisableLinearComponet(layout);
        //quick set ----------------------------------
        layout = (LinearLayout) findViewById(R.id.ll_prefix_quickset);
        DisableLinearComponet(layout);
        layout = (LinearLayout) findViewById(R.id.ly_scan_method);
        DisableLinearComponet(layout);
        layout = (LinearLayout) findViewById(R.id.ly_spec_start);
        DisableLinearComponet(layout);
        layout = (LinearLayout) findViewById(R.id.ly_spec_end);
        DisableLinearComponet(layout);
        layout = (LinearLayout) findViewById(R.id.ly_scan_width);
        DisableLinearComponet(layout);
        layout = (LinearLayout) findViewById(R.id.ly_res);
        DisableLinearComponet(layout);
        layout = (LinearLayout) findViewById(R.id.ly_aver_scan);
        DisableLinearComponet(layout);
        layout = (LinearLayout) findViewById(R.id.ly_ex_time);
        DisableLinearComponet(layout);
        layout = (LinearLayout) findViewById(R.id.ly_continus_scan_mode);
        DisableLinearComponet(layout);
        layout = (LinearLayout) findViewById(R.id.ly_scan_interval_time);
        DisableLinearComponet(layout);
        layout = (LinearLayout) findViewById(R.id.ly_repeat_quick);
        DisableLinearComponet(layout);
        layout = (LinearLayout) findViewById(R.id.ly_set_value);
        DisableLinearComponet(layout);
        layout = (LinearLayout) findViewById(R.id.ll_continuous_stop_quick);
        DisableLinearComponet(layout);
        //maintain------------------------------------------
        layout = (LinearLayout) findViewById(R.id.ly_reference);
        DisableLinearComponet(layout);
        //------------------------------------------
        btn_scan.setClickable(false);
        btn_normal.setClickable(false);
        btn_quickset.setClickable(false);
        btn_manual.setClickable(false);
        btn_maintain.setClickable(false);
        mMenu.findItem(R.id.action_settings).setEnabled(false);
        mMenu.findItem(R.id.action_key).setEnabled(false);
    }

    private void Disable_Stop_Continous_button()
    {
        LinearLayout layout = (LinearLayout) findViewById(R.id.ll_continuous_stop);
        DisableLinearComponet(layout);
        layout = (LinearLayout) findViewById(R.id.ll_continuous_stop_quick);
        DisableLinearComponet(layout);
    }

    private void Enable_Stop_Continous_button()
    {
        LinearLayout layout = (LinearLayout) findViewById(R.id.ll_continuous_stop);
        EnableLinearComponet(layout);
        layout = (LinearLayout) findViewById(R.id.ll_continuous_stop_quick);
        EnableLinearComponet(layout);
    }

    private void EnableLinearComponet(LinearLayout layout)
    {

        for (int i = 0; i < layout.getChildCount(); i++) {
            View child = layout.getChildAt(i);
            child.setEnabled(true);
        }
    }
    private void EnableAllComponent()
    {
        //normal------------------------------------------
        LinearLayout layout = (LinearLayout) findViewById(R.id.ll_user);
        EnableLinearComponet(layout);
        layout = (LinearLayout) findViewById(R.id.ll_fileNumberLayout);
        EnableLinearComponet(layout);
        layout = (LinearLayout) findViewById(R.id.ll_simple_number);
        EnableLinearComponet(layout);

        textileComposition.setEnabled(true);
        textileComposition_1.setEnabled(true);
        textileComposition_2.setEnabled(true);
        textileComposition_3.setEnabled(true);
        textileComposition_4.setEnabled(true);
        editTextNumber.setEnabled(true);
        editTextNumber_1.setEnabled(true);
        editTextNumber_2.setEnabled(true);
        editTextNumber_3.setEnabled(true);
        editTextNumber_4.setEnabled(true);
        imageButton.setEnabled(true);
        imageButton_1.setEnabled(true);
        imageButton_2.setEnabled(true);
        imageButton_3.setEnabled(true);
        imageButton_4.setEnabled(true);
        // layout = (LinearLayout) findViewById(R.id.ll_os);
        // EnableLinearComponet(layout);
        layout = (LinearLayout) findViewById(R.id.ll_doubleSideScan);
        EnableLinearComponet(layout);
        layout = (LinearLayout) findViewById(R.id.ly_normal_interval_time);
        EnableLinearComponet(layout);
        layout = (LinearLayout) findViewById(R.id.ly_normal_repeat);
        EnableLinearComponet(layout);
        layout = (LinearLayout) findViewById(R.id.ll_continuous_stop);
        EnableLinearComponet(layout);
        tv_normal_repeat.setEnabled(toggle_btn_continuous_scan.isChecked());
        et_normal_scan_repeat.setEnabled(toggle_btn_continuous_scan.isChecked());
        tv_normal_interval_time.setEnabled(toggle_btn_continuous_scan.isChecked());
        et_normal_interval_time.setEnabled(toggle_btn_continuous_scan.isChecked());
        //manual-------------------------------------------------------------
        layout = (LinearLayout) findViewById(R.id.ll_conf);
        EnableLinearComponet(layout);
        layout = (LinearLayout) findViewById(R.id.ll_prefix_manual);
        EnableLinearComponet(layout);
        layout = (LinearLayout) findViewById(R.id.ly_scan_mode);
        EnableLinearComponet(layout);
        layout = (LinearLayout) findViewById(R.id.ly_lamp);
        EnableLinearComponet(layout);
        layout = (LinearLayout) findViewById(R.id.ly_pga);
        EnableLinearComponet(layout);
        layout = (LinearLayout) findViewById(R.id.ly_repeat);
        EnableLinearComponet(layout);
        layout = (LinearLayout) findViewById(R.id.ly_conf_manual);
        EnableLinearComponet(layout);
        if(toggle_button_manual_scan_mode.isChecked() == false)
        {
            toggle_button_manual_lamp.setEnabled(false);
            et_manual_repead.setEnabled(false);
            et_manual_pga.setEnabled(false);
            et_manual_lamptime.setEnabled(true);
        }
        //quick set ----------------------------------
        layout = (LinearLayout) findViewById(R.id.ll_prefix_quickset);
        EnableLinearComponet(layout);
        layout = (LinearLayout) findViewById(R.id.ly_scan_method);
        EnableLinearComponet(layout);
        layout = (LinearLayout) findViewById(R.id.ly_spec_start);
        EnableLinearComponet(layout);
        layout = (LinearLayout) findViewById(R.id.ly_spec_end);
        EnableLinearComponet(layout);
        layout = (LinearLayout) findViewById(R.id.ly_scan_width);
        EnableLinearComponet(layout);
        layout = (LinearLayout) findViewById(R.id.ly_res);
        EnableLinearComponet(layout);
        layout = (LinearLayout) findViewById(R.id.ly_aver_scan);
        EnableLinearComponet(layout);
        layout = (LinearLayout) findViewById(R.id.ly_ex_time);
        EnableLinearComponet(layout);
        layout = (LinearLayout) findViewById(R.id.ly_continus_scan_mode);
        EnableLinearComponet(layout);
        layout = (LinearLayout) findViewById(R.id.ly_scan_interval_time);
        EnableLinearComponet(layout);
        layout = (LinearLayout) findViewById(R.id.ly_repeat_quick);
        EnableLinearComponet(layout);
        layout = (LinearLayout) findViewById(R.id.ly_set_value);
        EnableLinearComponet(layout);
        layout = (LinearLayout) findViewById(R.id.ll_continuous_stop_quick);
        EnableLinearComponet(layout);
        tv_quickset_continuous_repeat.setEnabled(toggle_btn_quickset_continuous_scan_mode.isChecked());
        et_quickset_continuous_scan_repeat.setEnabled(toggle_btn_quickset_continuous_scan_mode.isChecked());
        tv_quickset_scan_interval_time.setEnabled(toggle_btn_quickset_continuous_scan_mode.isChecked());
        et_quickset_scan_interval_time.setEnabled(toggle_btn_quickset_continuous_scan_mode.isChecked());
        //maintain------------------------------------------
        layout = (LinearLayout) findViewById(R.id.ly_reference);
        EnableLinearComponet(layout);
        //------------------------------------------
        btn_scan.setClickable(true);
        btn_normal.setClickable(true);
        btn_quickset.setClickable(true);
        btn_manual.setClickable(true);
        btn_maintain.setClickable(true);
        setActivityTouchDisable(false);
        mMenu.findItem(R.id.action_settings).setEnabled(true);
        mMenu.findItem(R.id.action_key).setEnabled(true);
    }
    /**
     * Unlock device will open all scan mode
     */
    private void openFunction()
    {
        btn_normal.setClickable(true);
        btn_quickset.setClickable(true);
        btn_manual.setClickable(true);
        btn_maintain.setClickable(true);
        btn_manual.setBackgroundColor(0xFF0099CC);
        btn_quickset.setBackgroundColor(0xFF0099CC);
        btn_maintain.setBackgroundColor(0xFF0099CC);
        btn_normal.setBackgroundColor(ContextCompat.getColor(mContext, R.color.red));

        findViewById(R.id.layout_normal).setVisibility(View.VISIBLE);
        findViewById(R.id.layout_manual).setVisibility(View.GONE);
        findViewById(R.id.layout_quickset).setVisibility(View.GONE);
        findViewById(R.id.layout_maintain).setVisibility(View.GONE);

        Current_Scan_Method = ScanMethod.Normal;
        btn_scan.setClickable(true);
        btn_scan.setBackgroundColor(ContextCompat.getColor(mContext, R.color.red));
        setActivityTouchDisable(false);

        if((HW_Model.equals("R") && isExtendVer) || HW_Model.equals("R3")||HW_Model.equals("R11"))
        {
            ly_lamp.setVisibility(View.GONE);
            view_lamp_onoff.setVisibility(View.GONE);
        }
    }
    /**
     * Lock device can only use normal scan
     */
    private void closeFunction()
    {
        btn_quickset.setClickable(false);
        btn_manual.setClickable(false);
        btn_maintain.setClickable(false);
        btn_manual.setBackgroundColor(ContextCompat.getColor(mContext, R.color.gray));
        btn_quickset.setBackgroundColor(ContextCompat.getColor(mContext, R.color.gray));
        btn_maintain.setBackgroundColor(ContextCompat.getColor(mContext, R.color.gray));
        findViewById(R.id.layout_normal).setVisibility(View.VISIBLE);
        findViewById(R.id.layout_manual).setVisibility(View.GONE);
        findViewById(R.id.layout_quickset).setVisibility(View.GONE);
        findViewById(R.id.layout_maintain).setVisibility(View.GONE);
        btn_normal.setBackgroundColor(ContextCompat.getColor(mContext, R.color.red));
        Current_Scan_Method = ScanMethod.Normal;
        btn_scan.setClickable(true);
        btn_scan.setBackgroundColor(ContextCompat.getColor(mContext, R.color.red));
        setActivityTouchDisable(false);
    }
    // 监听EditText的触摸事件，点击右侧的清除按钮时清空EditText
    private View.OnTouchListener et_simple_number_OnTouch = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                Drawable rightDrawable = et_simple_number.getCompoundDrawables()[2];
                if (rightDrawable != null && event.getRawX() >= (et_simple_number.getRight() - rightDrawable.getBounds().width())) {
                    // 清空 EditText
                    et_simple_number.setText("");
                    return true;
                }
            }
            return false;
        }
    };
    private TextView.OnEditorActionListener et_simple_number_OnEdit = new TextView.OnEditorActionListener() {
        @Override
        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                // 回车键触发后隐藏键盘并查询纤维成分
                InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(et_simple_number.getWindowToken(), 0);
                if (!isPostSampleDataRunning)
                    getFabricComponents(et_simple_number);
                return true;
            }
            return false;
        }
    };
    private void getFabricComponents(EditText sampleNumber){
        // 清空纤维成分列表
        fabricComponents.clear();
        // 获取样品编号
        String s = sampleNumber.getText().toString();
        if (s.isEmpty()) {
            Toast.makeText(mContext, "请输入样品编号", Toast.LENGTH_SHORT).show();
            return;
        }
        // 检查样品编号是否合理
        if (!s.matches("^\\d{2}.\\d{6}.*")){
            Toast.makeText(mContext, "请输入正确的样品编号", Toast.LENGTH_SHORT).show();
            sampleNumber.setError("请填写正确的样品编号！");
        }else {
            s = s.substring(0, 9);
            // 只保留样品编号的前9位
            et_simple_number.setText(s);
            postSampleData(s);
        }
    }
    private void postSampleData(String number){
        String reportNo = number;
        isPostSampleDataRunning = true;
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String url = "http://192.168.106.110/OtherDeal/FibreQuery" +
                            "?AnthoirCode=D90D6D76D5154C6CA01F8E8C4B5ADB01" +
                            "&ReportNo=" + reportNo;

                    Request request = new Request.Builder()
                            .url(url)
                            .post(RequestBody.create(null, new byte[0]))
                            .build();

//                    System.out.println(request);

                    Response response = client.newCall(request).execute();
                    if (response.isSuccessful()) {
                        JSONObject jsonResponse = new JSONObject(response.body().string());
                        int code = jsonResponse.getInt("code");
                        if (code == 0) {
                            JSONArray data = jsonResponse.getJSONArray("data");
                            for (int i = 0; i < data.length(); i++) {
                                JSONObject item = data.getJSONObject(i);
                                String checkItemName = item.getString("CheckItemName");
                                String checkResult = item.getString("CheckResult");
                                if (checkItemName.equals("纤维含量")){
                                    fabricComponents.add(new FabricComponent(checkResult));
                                }
                            }
                            // 在主线程中更新UI
                            runOnUiThread(() -> applyFabricComponents());
                        } else {
                            // TODO: 处理错误信息
                            String message = jsonResponse.getString("message");
                            Toast.makeText(mContext, message, Toast.LENGTH_SHORT).show();
                        }
                    }else {
                        Toast.makeText(mContext, "网络错误：" + response, Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                runOnUiThread(() -> query_fabric_components.setEnabled(true));
                isPostSampleDataRunning = false;
            }
        }).start();
    }
    // 显示纤维成分列表
    private void applyFabricComponents(){
        if (fabricComponents.size() == 1){
            Iterator<Map.Entry<String, Double>> iterator = fabricComponents.get(0).getFiberComposition().entrySet().iterator();
            for (int i = 0; i < editTexts.size() && iterator.hasNext(); i++) {
                Map.Entry<String, Double> entry = iterator.next();
                editTexts.get(i).setText(entry.getValue().toString());
                //TODO: 将纤维名称归类到下拉框已有纤维种类中
//                textileCompositions.get(i).setText(ClassificationOfFiberComponents(entry.getKey()));

            }
        } else if (fabricComponents.size() > 1) {
            //TODO: 处理多个纤维成分的情况
        }else {
            Toast.makeText(mContext, "未查询到纤维成分", Toast.LENGTH_SHORT).show();
            return;
        }
    }
    private String ClassificationOfFiberComponents(String fiberName){
        if (fiberName.equals("棉")){
            return "棉";
        } else if (fiberName.equals("涤纶") || fiberName.equals("聚酯纤维")){
            return "聚酯纤维";
        } else if (fiberName.equals("氨纶")){
            return "氨纶";
        } else if (fiberName.equals("锦纶")){
            return "锦纶";
        } else if (fiberName.equals("桑蚕丝")){
            return "桑蚕丝";
        } else if (fiberName.equals("乙纶")){
            return "乙纶";
        } else if (fiberName.equals("动物毛纤维") || fiberName.equals("绵羊毛") || fiberName.equals("山羊绒")
                || fiberName.equals("牦牛绒") || fiberName.equals("兔毛") || fiberName.equals("马海毛") ||
                fiberName.equals("骆驼毛") || fiberName.equals("骆驼绒")) {
            return "动物毛纤维";
        } else if (fiberName.equals("再生纤维素纤维") ||fiberName.equals("莫代尔") || fiberName.equals("粘纤")
                || fiberName.equals("莱赛尔")) {
            return "再生纤维素纤维";
        } else if (fiberName.equals("铜氨纤维")){
            return "铜氨纤维";
        } else if (fiberName.equals("醋纤")){
            return "醋纤";
        } else if (fiberName.equals("丙纶")){
            return "丙纶";
        } else if (fiberName.equals("海藻纤维")){
            return "海藻纤维";
        } else if (fiberName.equals("聚酰亚胺纤维")){
            return "聚酰亚胺纤维";
        } else if (fiberName.equals("壳聚糖纤维")){
            return "壳聚糖纤维";
        }else {
            return "其他纤维";
        }
    }
    // 监听EditText的文本变化，显示或隐藏清除按钮
    private TextWatcher et_simple_number_OnTextChange = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void afterTextChanged(Editable s) {
            if (s.length() > 0) {
                et_simple_number.setCompoundDrawablesWithIntrinsicBounds(0, 0, android.R.drawable.ic_menu_close_clear_cancel, 0);
            } else {
                et_simple_number.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
            }
        }
    };
    //endregion
    //region Scan Method Button Event
    private Button.OnClickListener Button_Normal_Click = new Button.OnClickListener() {
        @Override
        public void onClick(View v) {
            ChangeLampState();
            findViewById(R.id.layout_normal).setVisibility(View.VISIBLE);
            findViewById(R.id.layout_manual).setVisibility(View.GONE);
            findViewById(R.id.layout_quickset).setVisibility(View.GONE);
            findViewById(R.id.layout_maintain).setVisibility(View.GONE);
            btn_normal.setBackgroundColor(ContextCompat.getColor(mContext, R.color.red));
            btn_manual.setBackgroundColor(0xFF0099CC);
            btn_quickset.setBackgroundColor(0xFF0099CC);
            btn_maintain.setBackgroundColor(0xFF0099CC);
            Current_Scan_Method = ScanMethod.Normal;
            //----------------------------------------------------
            if(getStringPref(mContext, ISCNIRScanSDK.SharedPreferencesKeys.Activacatestatus, "Function is locked.").contains("Activated"))
            {
                openFunction();
            }
            else
            {
                closeFunction();
            }
        }
    };
    private Button.OnClickListener Button_Quicket_Click = new Button.OnClickListener() {
        @Override
        public void onClick(View v) {
            ChangeLampState();
            findViewById(R.id.layout_quickset).setVisibility(View.VISIBLE);
            findViewById(R.id.layout_manual).setVisibility(View.GONE);
            findViewById(R.id.layout_normal).setVisibility(View.GONE);
            findViewById(R.id.layout_maintain).setVisibility(View.GONE);
            btn_quickset.setBackgroundColor(ContextCompat.getColor(mContext, R.color.red));
            btn_manual.setBackgroundColor(0xFF0099CC);
            btn_normal.setBackgroundColor(0xFF0099CC);
            btn_maintain.setBackgroundColor(0xFF0099CC);
            Current_Scan_Method = ScanMethod.QuickSet;
            //---------------------------------------------------------------
            UI_ShowMaxPattern();

        }
    };
    private Button.OnClickListener Button_Manual_Click = new Button.OnClickListener() {
        @Override
        public void onClick(View v) {
            ChangeLampState();
            findViewById(R.id.layout_manual).setVisibility(View.VISIBLE);
            findViewById(R.id.layout_normal).setVisibility(View.GONE);
            findViewById(R.id.layout_quickset).setVisibility(View.GONE);
            findViewById(R.id.layout_maintain).setVisibility(View.GONE);
            btn_manual.setBackgroundColor(ContextCompat.getColor(mContext, R.color.red));
            btn_normal.setBackgroundColor(0xFF0099CC);
            btn_quickset.setBackgroundColor(0xFF0099CC);
            btn_maintain.setBackgroundColor(0xFF0099CC);
            if(Current_Scan_Method != ScanMethod.Manual)//State Normal,Quickset,Maintan -> Manual
            {
                toggle_button_manual_scan_mode.setChecked(false);
                toggle_button_manual_lamp.setEnabled(false);
                et_manual_repead.setEnabled(false);
                et_manual_pga.setEnabled(false);
                et_manual_lamptime.setEnabled(true);
                et_manual_repead.setText("6");
                et_manual_pga.setText("1");
                et_manual_lamptime.setText("625");
            }
            Current_Scan_Method = ScanMethod.Manual;
        }
    };
    private Button.OnClickListener Button_Maintain_Click = new Button.OnClickListener() {
        @Override
        public void onClick(View v) {
            ChangeLampState();
            findViewById(R.id.layout_maintain).setVisibility(View.VISIBLE);
            findViewById(R.id.layout_manual).setVisibility(View.GONE);
            findViewById(R.id.layout_normal).setVisibility(View.GONE);
            findViewById(R.id.layout_quickset).setVisibility(View.GONE);
            btn_maintain.setBackgroundColor(ContextCompat.getColor(mContext, R.color.red));
            btn_manual.setBackgroundColor(0xFF0099CC);
            btn_normal.setBackgroundColor(0xFF0099CC);
            btn_quickset.setBackgroundColor(0xFF0099CC);
            Current_Scan_Method = ScanMethod.Maintain;
        }
    };
    private void ChangeLampState()
    {
        if(WarmUp)
        {
            ISCNIRScanSDK.ControlLamp(ISCNIRScanSDK.LampState.AUTO);
            WarmUp = false;
        }
        if(Current_Scan_Method == ScanMethod.Manual && toggle_button_manual_scan_mode.isChecked())//Manual->Normal,Quickset,Maintain
        {
            if(toggle_button_manual_lamp.getText().toString().toUpperCase().equals("ON"))
            {
                toggle_button_manual_lamp.setChecked(false);//close lamp
            }
            ISCNIRScanSDK.ControlLamp(ISCNIRScanSDK.LampState.AUTO);
        }
    }
    //endregion
    //region Normal UI component Event
    private LinearLayout.OnClickListener Normal_Config_Click = new LinearLayout.OnClickListener() {
        @Override
        public void onClick(View v) {
            if(activeConf != null) {
                GotoOtherPage = true;
                Intent activeConfIntent = new Intent(mContext, ActiveConfigDetailViewActivity.class);
                activeConfIntent.putExtra("conf",activeConf);
                startActivity(activeConfIntent);
            }
        }
    };
    private Button.OnClickListener Normal_Continuous_Scan_Click = new Button.OnClickListener()
    {
        @Override
        public void onClick(View view) {
            et_normal_scan_repeat.setEnabled(toggle_btn_continuous_scan.isChecked());
            tv_normal_repeat.setEnabled(toggle_btn_continuous_scan.isChecked());
            tv_normal_interval_time.setEnabled(toggle_btn_continuous_scan.isChecked());
            et_normal_interval_time.setEnabled(toggle_btn_continuous_scan.isChecked());
        }
    };
    private EditText.OnEditorActionListener Normal_Continuous_Scan_Repeat_OnEdit = new EditText.OnEditorActionListener()
    {
        @Override
        public boolean onEditorAction(TextView textView, int actionId, KeyEvent event) {
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                    actionId == EditorInfo.IME_ACTION_DONE ||
                    event.getAction() == KeyEvent.ACTION_DOWN &&
                            event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                int value = Integer.parseInt(et_normal_scan_repeat.getText().toString());
                if(value<=1)
                {
                    NotValidValueDialog("Warning","The number of continuous scan repeats should be larger than 1.");
                    et_normal_scan_repeat.setText("2");
                }
            }
            return false;
        }
    };
    //endregion
    //region QuickSet UI component Event
    private EditText.OnEditorActionListener Quickset_Lamp_Time_OnEditor = new EditText.OnEditorActionListener()
    {
        @Override
        public boolean onEditorAction(TextView textView, int actionId, KeyEvent event) {
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                    actionId == EditorInfo.IME_ACTION_DONE ||
                    event.getAction() == KeyEvent.ACTION_DOWN &&
                            event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                if(Integer.parseInt(et_quickset_lamptime.getText().toString())!=625)
                {
                    int lamptime = Integer.parseInt(et_quickset_lamptime.getText().toString());
                    ISCNIRScanSDK.SetLampStableTime(lamptime);
                }
                return false; // consume.
            }
            return false;
        }
    };

    private Spinner.OnItemSelectedListener Quickset_Scan_Method_ItemSelect = new Spinner.OnItemSelectedListener()
    {
        @Override
        public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
            quickset_scan_method_index = i;
            UI_ShowMaxPattern();
        }
        @Override
        public void onNothingSelected(AdapterView<?> adapterView) {
        }
    };

    private EditText.OnEditorActionListener Quickset_Spec_Start_OnEditor = new EditText.OnEditorActionListener()
    {
        @Override
        public boolean onEditorAction(TextView textView, int actionId, KeyEvent event) {
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                    actionId == EditorInfo.IME_ACTION_DONE ||
                    event.getAction() == KeyEvent.ACTION_DOWN &&
                            event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                if(et_quickset_spec_start.getText().toString().matches("")|| Integer.parseInt(et_quickset_spec_start.getText().toString())> Integer.parseInt(et_quickset_spec_end.getText().toString()) || Integer.parseInt(et_quickset_spec_start.getText().toString())<MINWAV)
                {
                    et_quickset_spec_start.setText(Integer.toString(quickset_init_start_nm));
                    Dialog_Pane("Error","Start wavelength should be between " + MINWAV + "nm and end wavelength!");
                    return false; // consume.
                }
            }
            quickset_init_start_nm = Integer.parseInt(et_quickset_spec_start.getText().toString());
            UI_ShowMaxPattern();
            return false;
        }
    };
    private EditText.OnEditorActionListener Quickset_Spec_End_OnEditor = new EditText.OnEditorActionListener()
    {
        @Override
        public boolean onEditorAction(TextView textView, int actionId, KeyEvent event) {
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                    actionId == EditorInfo.IME_ACTION_DONE ||
                    event.getAction() == KeyEvent.ACTION_DOWN &&
                            event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                if(et_quickset_spec_end.getText().toString().matches("")|| Integer.parseInt(et_quickset_spec_end.getText().toString())< Integer.parseInt(et_quickset_spec_start.getText().toString()) || Integer.parseInt(et_quickset_spec_end.getText().toString())>MAXWAV)
                {
                    et_quickset_spec_end.setText(Integer.toString(quickset_init_end_nm));
                    Dialog_Pane("Error","End wavelength should be between start wavelength and " + MAXWAV + "nm!");
                    return false; // consume.
                }
            }
            quickset_init_end_nm = Integer.parseInt(et_quickset_spec_end.getText().toString());
            UI_ShowMaxPattern();
            return false;
        }
    };
    private Spinner.OnItemSelectedListener Quickset_Scan_Width_ItemSelect = new Spinner.OnItemSelectedListener()
    {
        @Override
        public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
            quickset_scan_width_index = i+2;
            UI_ShowMaxPattern();
        }

        @Override
        public void onNothingSelected(AdapterView<?> adapterView) {

        }
    };
    private EditText.OnEditorActionListener Quickset_Res_OnEditor = new EditText.OnEditorActionListener()
    {
        @Override
        public boolean onEditorAction(TextView textView, int actionId, KeyEvent event) {
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                    actionId == EditorInfo.IME_ACTION_DONE ||
                    event.getAction() == KeyEvent.ACTION_DOWN &&
                            event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                if(et_quickset_res.getText().toString().matches("")|| Integer.parseInt(et_quickset_res.getText().toString())< 2 || Integer.parseInt(et_quickset_res.getText().toString())>MaxPattern)
                {
                    et_quickset_res.setText(Integer.toString(quickset_init_res));
                    Dialog_Pane("Error","D-Res. range is 2~" + MaxPattern + ".");
                    return false; // consume.

                }
            }
            quickset_init_res = Integer.parseInt(et_quickset_res.getText().toString());
            return false;
        }
    };
    private Spinner.OnItemSelectedListener Quickset_Exposure_Time_ItemSelect = new Spinner.OnItemSelectedListener()
    {
        @Override
        public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
            quickset_exposure_time_index = i;
        }
        @Override
        public void onNothingSelected(AdapterView<?> adapterView) {
        }
    };
    private Button.OnClickListener Quickset_Set_Config_Click = new Button.OnClickListener()
    {
        @Override
        public void onClick(View view) {
            if(checkQuicksetValue())
            {
                btn_quickset_set_config.setClickable(false);
                btn_scan.setClickable(false);
                calProgress.setVisibility(view.VISIBLE);
                byte[] EXTRA_DATA = ChangeScanConfigToByte();
                ISCNIRScanSDK.ScanConfig(EXTRA_DATA,ISCNIRScanSDK.ScanConfig.SET);
            }
        }
    };
    private EditText.OnEditorActionListener QuickSet_Continuous_Scan_Repeat_OnEdit = new EditText.OnEditorActionListener()
    {
        @Override
        public boolean onEditorAction(TextView textView, int actionId, KeyEvent event) {
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                    actionId == EditorInfo.IME_ACTION_DONE ||
                    event.getAction() == KeyEvent.ACTION_DOWN &&
                            event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                int value = Integer.parseInt(et_quickset_continuous_scan_repeat.getText().toString());
                if(value<=1)
                {
                    NotValidValueDialog("Warning","The number of continuous scan repeats should be larger than 1.");
                    et_quickset_continuous_scan_repeat.setText("2");
                }
            }
            return false;
        }
    };


    /**
     * Send broadcast  ACTION_WRITE_SCAN_CONFIG will  through WriteScanConfigStatusReceiver  to get  the status of set config to the device(SetConfig should be called)
     */
    public class WriteScanConfigStatusReceiver extends BroadcastReceiver {

        public void onReceive(Context context, Intent intent) {
            calProgress.setVisibility(View.GONE);
            byte status[] = intent.getByteArrayExtra(ISCNIRScanSDK.RETURN_WRITE_SCAN_CONFIG_STATUS);
            btn_scan.setClickable(true);
            if((int)status[0] == 1)
            {
                if((int)status[2] == -1 && (int)status[3]==-1)
                {
                    Dialog_Pane("Fail","Set configuration fail!");
                }
                else
                {
                    //Get the scan config of the device
                    ISCNIRScanSDK.ReadCurrentScanConfig();
                }
            }
            else if((int)status[0] == -1)
            {
                Dialog_Pane("Fail","Set configuration fail!");
            }
            else if((int)status[0] == -2)
            {
                Dialog_Pane("Fail","Set configuration fail! Hardware not compatible!");
            }
            else if((int)status[0] == -3)
            {
                Dialog_Pane("Fail","Set configuration fail! Function is currently locked!" );
            }
        }
    }

    /**
     * Get  the  current scan config  in the device(ISCNIRScanSDK.ReadCurrentScanConfig(data) should be called)
     */
    public class ReturnCurrentScanConfigurationDataReceiver extends BroadcastReceiver {
        public void onReceive(Context context, Intent intent) {
            Boolean flag = Compareconfig(intent.getByteArrayExtra(ISCNIRScanSDK.EXTRA_CURRENT_CONFIG_DATA));
            calProgress.setVisibility(View.GONE);
            if(flag)
            {
                if(saveReference == true)
                {
                    saveReference = false;
                    ISCNIRScanSDK.ClearDeviceError();
                    Handler handler = new Handler();
                    handler.postDelayed(new Runnable(){

                        @Override
                        public void run() {
                            finish();
                        }}, 100);
                }
                else if(Current_Scan_Method == ScanMethod.Maintain) //reference
                {
                    String model = "";
                    if(!model_name.isEmpty() && model_name != null)
                    {
                        String[] SplitModel = model_name.split("-");
                        if(SplitModel.length > 0)
                            model = SplitModel[SplitModel.length - 1];
                    }
                    if(model.equals("R11") || model.equals("F11"))
                        ISCNIRScanSDK.SetPGA(4);
                    else if(model.equals("R13") || model.equals("F1") || model.equals("T1"))
                        ISCNIRScanSDK.SetPGA(8);
                    else if(model.equals("F13") || model.equals("T11") || model.equals("T13"))
                        ISCNIRScanSDK.SetPGA(16);
                    else
                        ISCNIRScanSDK.SetPGA(64);
                    ReferenceConfigSaveSuccess();
                }
                else
                {
                    Dialog_Pane("Success","Complete to set configuration.");
                }
            }
            else
            {
                if(saveReference == true)
                {
                    Dialog_Pane("Fail","Restore config fail, should re-open device.");
                    saveReference = false;
                    ISCNIRScanSDK.ClearDeviceError();
                    Handler handler = new Handler();
                    handler.postDelayed(new Runnable(){

                        @Override
                        public void run() {
                            finish();
                        }}, 100);
                }
                else
                {
                    Dialog_Pane("Fail","Set configuration fail.");
                }
            }
        }
    }
    //endregion
    //region Manual UI componet Event
    private Button.OnClickListener Toggle_Button_Manual_ScanMode_Click = new Button.OnClickListener() {
        @Override
        public void onClick(View v) {
            if(toggle_button_manual_scan_mode.getText().toString().toUpperCase().equals("ON"))
            {
                toggle_button_manual_lamp.setEnabled(true);
                et_manual_repead.setEnabled(true);
                et_manual_pga.setEnabled(true);
                et_manual_lamptime.setEnabled(false);
                if((HW_Model.equals("R")&&isExtendVer)|| HW_Model.equals("R3")||HW_Model.equals("R11"))
                    toggle_button_manual_lamp.setVisibility(View.GONE);
                else
                    toggle_button_manual_lamp.setChecked(true);
            }
            else
            {
                toggle_button_manual_lamp.setEnabled(false);
                et_manual_repead.setEnabled(false);
                et_manual_pga.setEnabled(false);
                et_manual_lamptime.setEnabled(true);
                toggle_button_manual_lamp.setChecked(false);
                ISCNIRScanSDK.ControlLamp(ISCNIRScanSDK.LampState.AUTO);
            }
        }
    };
    private ToggleButton.OnCheckedChangeListener Toggle_Button_Manual_Lamp_Changed = new ToggleButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton compoundButton, boolean b) {

            if(toggle_button_manual_lamp.getText().toString().toUpperCase().equals("OFF"))//OFF->ON
            {
                ISCNIRScanSDK.ControlLamp(ISCNIRScanSDK.LampState.ON);
            }
            else
            {
                ISCNIRScanSDK.ControlLamp(ISCNIRScanSDK.LampState.OFF);
            }
        }
    };
    private EditText.OnEditorActionListener Manual_Lamptime_OnEditor = new EditText.OnEditorActionListener()
    {
        @Override
        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                if(Integer.parseInt(et_manual_lamptime.getText().toString())!=625)
                {
                    int lamptime = Integer.parseInt(et_manual_lamptime.getText().toString());
                    ISCNIRScanSDK.SetLampStableTime(lamptime);
                }
                return false;
            }
            return false;
        }
    };

    private EditText.OnEditorActionListener Manual_PGA_OnEditor = new EditText.OnEditorActionListener()
    {
        @Override
        public boolean onEditorAction(TextView textView, int actionId, KeyEvent event) {
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                    actionId == EditorInfo.IME_ACTION_DONE ||
                    event.getAction() == KeyEvent.ACTION_DOWN &&
                            event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                if( checkValidPga()==true)
                {
                    int pga = Integer.parseInt(et_manual_pga.getText().toString());
                    ISCNIRScanSDK.SetPGA(pga);
                    if( (!isExtendVer_PLUS && !isExtendVer && fw_level_standard.compareTo(ISCNIRScanSDK.FW_LEVEL_STANDARD.LEVEL_4)>=0)
                        || (isExtendVer && fw_level_ext.compareTo(ISCNIRScanSDK.FW_LEVEL_EXT.LEVEL_EXT_3)>=0))
                        ISCNIRScanSDK.GetPGA();
                    return false;
                }
            }
            return false;
        }
    };
    /**
     * Get PGA(ISCNIRScanSDK.GetPGA()should be called and TIVA should � 2.5.x)
     */
    public class GetPGAReceiver extends BroadcastReceiver {
        public void onReceive(Context context, Intent intent) {

            byte buf[] = intent.getByteArrayExtra(ISCNIRScanSDK.EXTRA_PGA);
            int pga = Integer.parseInt(et_manual_pga.getText().toString());
            int getpga = (int) buf[0];
            if(pga == getpga)
                Dialog_Pane("Success", "Set PGA : " + pga);
            else
                Dialog_Pane("Fail", "Set PGA : " + pga);
        }
    }

    private EditText.OnEditorActionListener Manual_Repeat_OnEditor = new EditText.OnEditorActionListener()
    {
        @Override
        public boolean onEditorAction(TextView textView, int actionId, KeyEvent event) {
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                    actionId == EditorInfo.IME_ACTION_DONE ||
                    event.getAction() == KeyEvent.ACTION_DOWN &&
                            event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                if(checkValidRepeat())
                {
                    int scan_repeat = Integer.parseInt(et_manual_repead.getText().toString());
                    ISCNIRScanSDK.SetScanRepeat(scan_repeat);
                    return false;
                }

            }
            return false;
        }
    };
    private LinearLayout.OnClickListener Manual_Config_Click = new LinearLayout.OnClickListener()
    {
        @Override
        public void onClick(View view) {
            if(activeConf != null) {
                GotoOtherPage = true;
                Intent activeConfIntent = new Intent(mContext, ActiveConfigDetailViewActivity.class);
                activeConfIntent.putExtra("conf",activeConf);
                startActivity(activeConfIntent);
            }
        }
    };
    private Button.OnClickListener QuickSet_Continuous_Scan_Click = new Button.OnClickListener()
    {
        @Override
        public void onClick(View view) {
            tv_quickset_continuous_repeat.setEnabled(toggle_btn_quickset_continuous_scan_mode.isChecked());
            et_quickset_continuous_scan_repeat.setEnabled(toggle_btn_quickset_continuous_scan_mode.isChecked());
            tv_quickset_scan_interval_time.setEnabled(toggle_btn_quickset_continuous_scan_mode.isChecked());
            et_quickset_scan_interval_time.setEnabled(toggle_btn_quickset_continuous_scan_mode.isChecked());
        }
    };
    //endregion
    //region Scan
    private Button.OnClickListener Button_Scan_Click = new Button.OnClickListener()
    {
        @Override
        public void onClick(View view) {
            storeStringPref(mContext, ISCNIRScanSDK.SharedPreferencesKeys.prefix, filePrefix.getText().toString());
            long delaytime = 300;
            row_data = new String[229];
            post_data.clear();
            tv_reference_results.setText("预测中...");
            // 检查成分是否为空或者内容不规范
            if (!checkEditTextContent(textileComposition, editTextNumber)){
                Dialog_Pane("错误", "请至少填写一个成分和含量，并首先填写在第一行！");
                return;
            }else if (!checkEditTextContent(textileComposition_1, editTextNumber_1)) {
                Dialog_Pane("错误", "请填写<" +
                        textileComposition_1.getSelectedItem().toString() + ">的正确含量！");
                return;
            }else if (!checkEditTextContent(textileComposition_2, editTextNumber_2)) {
                Dialog_Pane("错误", "请填写<" +
                        textileComposition_2.getSelectedItem().toString() + ">的正确含量！");
                return;
            }else if (!checkEditTextContent(textileComposition_3, editTextNumber_3)) {
                Dialog_Pane("错误", "请填写<" +
                        textileComposition_3.getSelectedItem().toString() + ">的正确含量！");
                return;
            }else if (!checkEditTextContent(textileComposition_4, editTextNumber_4)) {
                Dialog_Pane("错误", "请填写<" +
                        textileComposition_4.getSelectedItem().toString() + ">的正确含量！");
                return;
            }

            // 检查含量总数是否为100
            double epsilon = 0.0001;
            double total = EditTextToNum(editTextNumber) +
                    EditTextToNum(editTextNumber_1) +
                    EditTextToNum(editTextNumber_2) +
                    EditTextToNum(editTextNumber_3) +
                    EditTextToNum(editTextNumber_4);

            if (Math.abs(total - 100.0) > epsilon) {
                Dialog_Pane("错误", "化学值总数必须为100！");
                return;
            }

            // 检查文件编号是否为空
            if (fileNumber.getText().toString().isEmpty()) {
                Dialog_Pane("错误", "请填写文件编号！");
                return;
            }

            // 检查样品编号是否合理
            if (!et_simple_number.getText().toString().matches("^\\d{2}.\\d{6}.*")){
                Dialog_Pane("错误", "请填写正确的样品编号！");
                et_simple_number.setError("请填写正确的样品编号！");
                return;
            }else {
                // 只保留样品编号的前9位
                et_simple_number.setText(et_simple_number.getText().toString().substring(0, 9));
            }

            // 检查扫描方式
            if(Current_Scan_Method == ScanMethod.Manual)
            {
                if( checkValidPga()==false)
                {
                    NotValidValueDialog("Error","PGA vlaue is 1,2,4,8,16,32,64.");
                    return;
                }
                else if( checkValidRepeat()==false)
                {
                    NotValidValueDialog("Error","Scan repeat range is 1~50.");
                    return;
                }
                else if(toggle_button_manual_scan_mode.getText().toString().equals("On"))
                {
                    DisableAllComponent();
                    btn_scan.setText(getString(R.string.scanning));
                    calProgress.setVisibility(View.VISIBLE);
                    PerformScan(delaytime);
                }
                else
                {
                    PerformScan(delaytime);
                }
            }
            else if(Current_Scan_Method == ScanMethod.QuickSet)
            {
                if(toggle_btn_quickset_continuous_scan_mode.isChecked())
                {
                    progressBarinsideText.setVisibility(View.VISIBLE);
                    continuous_count = 1;
                    progressBarinsideText.setText("Scanning : " + Integer.toString(continuous_count));
                    continuous_count = 1;
                }
                PerformScan(delaytime);
            }
            else if(Current_Scan_Method == ScanMethod.Maintain)
            {
                if(Toggle_Button_maintain_reference.isChecked())
                {
                    Dialog_Pane_maintain("Warning","Replace Factory Reference is ON !!! \n This sacn result will REPLACE the Factory Reference and can NOT be reversed!");
                }
                else
                {
                    PerformScan(delaytime);
                }
            }
            else//Normal
            {
                if(toggle_btn_continuous_scan.isChecked())
                {
                    progressBarinsideText.setVisibility(View.VISIBLE);
                    continuous_count = 1;
                    progressBarinsideText.setText("Scanning : " + Integer.toString(continuous_count));
                }
                PerformScan(delaytime);
            }
            //---------------------------------------------------------------------------------------------------
            if(Current_Scan_Method == ScanMethod.Maintain && Toggle_Button_maintain_reference.isChecked())
            {
            }
            else
            {
                // 开始扫描
                DisableAllComponent();
                calProgress.setVisibility(View.VISIBLE);
                btn_scan.setText(getString(R.string.scanning));
            }
        }
    };
    private boolean checkEditTextContent(Spinner spinner,EditText editText) {
        // 当spinner不为空时，editText不能为空
       if (!spinner.getSelectedItem().toString().equals("")) {
           if (!editText.getText().toString().isEmpty() && validateInput(editText.getText().toString(), editText)) {
               return true;
           }
       }else if (spinner.getSelectedItem().toString().equals("")){ // 当spinner为空时，editText只能为空
           if (editText.getText().toString().isEmpty()) {
               return true;
           }
       }
       return false;
    }

    private double EditTextToNum(EditText editText) {
        if (editText.getText().toString().isEmpty()) {
            return 0;
        } else {
            double value = Double.parseDouble(editText.getText().toString());
            return value;
        }
    }
//    private double roundToOneDecimal(double value) {
//        return Math.round(value * 10.0) / 10.0;
//    }
    /**
     * Send broadcast  START_SCAN will  through ScanStartedReceiver  to notify scanning(PerformScan should be called)
     */
    public class ScanStartedReceiver extends BroadcastReceiver {
        public void onReceive(Context context, Intent intent) {
            calProgress.setVisibility(View.VISIBLE);
            btn_scan.setText(getString(R.string.scanning));
        }
    }
    boolean continuous = false;
    ISCNIRScanSDK.ReferenceCalibration reference_calibration;
    String CurrentTime;
    long MesureScanTime=0;
    /**
     * Custom receiver for handling scan data and setting up the graphs properly(ISCNIRScanSDK.StartScan() should be called)
     */
    public class ScanDataReadyReceiver extends BroadcastReceiver {
        public void onReceive(Context context, Intent intent) {
            long endtime = System.currentTimeMillis();
            MesureScanTime = endtime - ISCNIRScanSDK.startScanTime;
            reference_calibration = ISCNIRScanSDK.ReferenceCalibration.currentCalibration.get(0);
            if(Interpret_length<=0)
            {
                Dialog_Pane_Finish("Error","The scan interpret fail. Please check your device.");
            }
            else
            {
                //Get scan spectrum data
                Scan_Spectrum_Data = new ISCNIRScanSDK.ScanResults(Interpret_wavelength,Interpret_intensity,Interpret_uncalibratedIntensity,Interpret_length);

                mXValues.clear();
                mIntensityFloat.clear();
                mAbsorbanceFloat.clear();
                mReflectanceFloat.clear();
                mWavelengthFloat.clear();
                mReferenceFloat.clear();
                int index;
                for (index = 0; index < Scan_Spectrum_Data.getLength(); index++) {
                    mXValues.add(String.format("%.02f", ISCNIRScanSDK.ScanResults.getSpatialFreq(mContext, Scan_Spectrum_Data.getWavelength()[index])));
                    mIntensityFloat.add(new Entry((float) Scan_Spectrum_Data.getWavelength()[index],(float) Scan_Spectrum_Data.getUncalibratedIntensity()[index]));
                    mAbsorbanceFloat.add(new Entry((float) Scan_Spectrum_Data.getWavelength()[index],(-1) * (float) Math.log10((double) Scan_Spectrum_Data.getUncalibratedIntensity()[index] / (double) Scan_Spectrum_Data.getIntensity()[index])));
                    mReflectanceFloat.add(new Entry((float) Scan_Spectrum_Data.getWavelength()[index],(float) Scan_Spectrum_Data.getUncalibratedIntensity()[index] / Scan_Spectrum_Data.getIntensity()[index]));
                    mWavelengthFloat.add((float) Scan_Spectrum_Data.getWavelength()[index]);
                    mReferenceFloat.add(new Entry((float) Scan_Spectrum_Data.getWavelength()[index],(float) Scan_Spectrum_Data.getIntensity()[index]));
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
                    if (e.getY() < minAbsorbance || Float.isNaN(minAbsorbance)) minAbsorbance = e.getY();
                    if (e.getY() > maxAbsorbance || Float.isNaN(maxAbsorbance)) maxAbsorbance = e.getY();
                }
                if(minAbsorbance==0 && maxAbsorbance==0)
                {
                    maxAbsorbance=2;
                }
                minReflectance = mReflectanceFloat.get(0).getY();
                maxReflectance = mReflectanceFloat.get(0).getY();

                for (Entry e : mReflectanceFloat) {
                    if (e.getY() < minReflectance|| Float.isNaN(minReflectance) ) minReflectance = e.getY();
                    if (e.getY() > maxReflectance|| Float.isNaN(maxReflectance) ) maxReflectance = e.getY();
                }
                if(minReflectance==0 && maxReflectance==0)
                {
                    maxReflectance=2;
                }
                minIntensity = mIntensityFloat.get(0).getY();
                maxIntensity = mIntensityFloat.get(0).getY();

                for (Entry e : mIntensityFloat) {
                    if (e.getY() < minIntensity|| Float.isNaN(minIntensity)) minIntensity = e.getY();
                    if (e.getY() > maxIntensity|| Float.isNaN(maxIntensity)) maxIntensity = e.getY();
                }
                if(minIntensity==0 && maxIntensity==0)
                {
                    maxIntensity=1000;
                }
                minReference = mReferenceFloat.get(0).getY();
                maxReference = mReferenceFloat.get(0).getY();

                for (Entry e : mReferenceFloat) {
                    if (e.getY() < minReference || Float.isNaN(minReference)) minReference = e.getY();
                    if (e.getY() > maxReference || Float.isNaN(maxReference)) maxReference = e.getY();
                }
                if(minReference==0 && maxReference==0)
                {
                    maxReference=1000;
                }
                isScan = true;
                tabPosition = mViewPager.getCurrentItem();
                mViewPager.setAdapter(mViewPager.getAdapter());
                mViewPager.invalidate();
                //number of slew
                String slew="";
                if(activeConf != null && activeConf.getScanType().equals("Slew")){
                    int numSections = activeConf.getSlewNumSections();
                    int i;
                    for(i = 0; i < numSections; i++){
                        slew = slew + activeConf.getSectionNumPatterns()[i]+"%";
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
                if(Current_Scan_Method == ScanMethod.Normal)
                {
                    continuous = toggle_btn_continuous_scan.isChecked();
                }
                else
                {
                    continuous = toggle_btn_quickset_continuous_scan_mode.isChecked();
                }
                storeStringPref(mContext, ISCNIRScanSDK.SharedPreferencesKeys.prefix, filePrefix.getText().toString());
                if(WarmUp)
                {
                    ISCNIRScanSDK.ControlLamp(ISCNIRScanSDK.LampState.AUTO);
                    Lamp_Info = LampInfo.CloseWarmUpLampInScan;
                }
                else
                    //Get Device information from the device
                    ISCNIRScanSDK.GetDeviceStatus();
            }
        }
    }
    /**
     * GetDeviceStatusReceiver to get  the device status(ISCNIRScanSDK.GetDeviceStatus()should be called)
     */
    String battery="";
    String TotalLampTime;
    byte[] devbyte;
    byte[] errbyte;
    float temprature;
    float humidity;
    public class GetDeviceStatusReceiver extends BroadcastReceiver {

        public void onReceive(Context context, Intent intent) {
            battery = Integer.toString( intent.getIntExtra(ISCNIRScanSDK.EXTRA_BATT, 0));
            long lamptime = intent.getLongExtra(ISCNIRScanSDK.EXTRA_LAMPTIME,0);
            TotalLampTime = GetLampTimeString(lamptime);
            devbyte = intent.getByteArrayExtra(ISCNIRScanSDK.EXTRA_DEV_STATUS_BYTE);
            errbyte = intent.getByteArrayExtra(ISCNIRScanSDK.EXTRA_ERR_BYTE);
            if(isExtendVer_PLUS ||(isExtendVer && (fw_level_ext.compareTo(ISCNIRScanSDK.FW_LEVEL_EXT.LEVEL_EXT_2)==0|| fw_level_ext.compareTo(ISCNIRScanSDK.FW_LEVEL_EXT.LEVEL_EXT_4)==0))
                    || (!isExtendVer_PLUS && !isExtendVer && (fw_level_standard.compareTo(ISCNIRScanSDK.FW_LEVEL_STANDARD.LEVEL_3)==0 || fw_level_standard.compareTo(ISCNIRScanSDK.FW_LEVEL_STANDARD.LEVEL_5)==0 ) ))
                ISCNIRScanSDK.GetScanLampRampUpADC();
            else
                DoScanComplete();
        }
    }

    /**
     *Get lamp ramp up adc data (ISCNIRScanSDK.GetScanLampRampUpADC() should be called)
     */
    private byte Lamp_RAMPUP_ADC_DATA[];
    public class ReturnLampRampUpADCReceiver extends BroadcastReceiver {

        public void onReceive(Context context, Intent intent) {
            Lamp_RAMPUP_ADC_DATA = intent.getByteArrayExtra(ISCNIRScanSDK.LAMP_RAMPUP_DATA);
            ISCNIRScanSDK.GetLampADCAverage();
        }
    }
    /**
     *Get lamp average adc data (ISCNIRScanSDK.GetLampADCAverage() should be called)
     */
    private byte Lamp_AVERAGE_ADC_DATA[];
    public class ReturnLampADCAverageReceiver extends BroadcastReceiver {

        public void onReceive(Context context, Intent intent) {
            Lamp_AVERAGE_ADC_DATA = intent.getByteArrayExtra(ISCNIRScanSDK.LAMP_ADC_AVERAGE_DATA);
            if(isExtendVer_PLUS ||(!isExtendVer_PLUS && !isExtendVer && fw_level_standard.compareTo(ISCNIRScanSDK.FW_LEVEL_STANDARD.LEVEL_5)==0)
            || (isExtendVer && fw_level_ext.compareTo(ISCNIRScanSDK.FW_LEVEL_EXT.LEVEL_EXT_4)==0))
                ISCNIRScanSDK.GetLampADCTimeStamp();
            else
                DoScanComplete();
        }
    }
    /**
     *Get lamp ramp up adc timestamp (ISCNIRScanSDK.GetLampADCTimeStamp() should be called)
     */
    private byte Lamp_RAMPUP_ADC_TIMESTAMP[];
    public class ReturnLampRampUpADCTimeStampReceiver extends BroadcastReceiver {

        public void onReceive(Context context, Intent intent) {
            Lamp_RAMPUP_ADC_TIMESTAMP = intent.getByteArrayExtra(ISCNIRScanSDK.LAMP_ADC_TIMESTAMP);
            DoScanComplete();
        }
    }
    /**
     * Finish scan will write scan data to .csv and setting UI
     * Continuous scan will trigger scan event to scan data
     */
    private void DoScanComplete()
    {
        long delaytime =0;
        Boolean isLockButton = getBooleanPref(mContext, ISCNIRScanSDK.SharedPreferencesKeys.LockButton,false);
        if(isLockButton) //User open lock button on scan setting
            ISCNIRScanSDK.ControlPhysicalButton(ISCNIRScanSDK.PhysicalButton.Lock);
        else
            ISCNIRScanSDK.ControlPhysicalButton(ISCNIRScanSDK.PhysicalButton.Unlock);
        delaytime = 300;
        if(getStringPref(mContext, ISCNIRScanSDK.SharedPreferencesKeys.Activacatestatus, "").contains("Activated") ==false)
        {
            closeFunction();
        }
        writeCSV(Scan_Spectrum_Data);
        //------------------------------------------------------------------------------------------------------------
        calProgress.setVisibility(View.GONE);
        progressBarinsideText.setVisibility(View.GONE);
        // 发送POST请求到API接口
        postCsvData(post_data);
        // 当启用了双面扫描，且当前为第一阶段时
        if (doubleSidedScanning && IsScanPhase_1){
            IsScanPhase_1 = false;
            btn_scan.setText("请扫描另一面");
            btn_scan.setClickable(true);
            setActivityTouchDisable(false);
            Toast.makeText(mContext, "请将样品翻面，再次扫描！", Toast.LENGTH_SHORT).show();
        }else {
            // 将当前的文件编号加1
            int fileNum = Integer.parseInt(fileNumber.getText().toString());
            fileNumber.setText(String.valueOf(fileNum + 1));
            // 设置为双面扫描的第一阶段
            IsScanPhase_1 = true;
            btn_scan.setText(getString(R.string.scan));
            EnableAllComponent();
            Disable_Stop_Continous_button();
        }

        //Tiva version <2.1.0.67
        if(!isExtendVer_PLUS && !isExtendVer && fw_level_standard.compareTo(ISCNIRScanSDK.FW_LEVEL_STANDARD.LEVEL_0)==0)
            closeFunction();
        //-------------------------------------------------------------------------------------------------------------
        if(Current_Scan_Method == ScanMethod.Maintain && Toggle_Button_maintain_reference.isChecked())
        {
            Handler handler = new Handler();
            handler.postDelayed(new Runnable(){

                @Override
                public void run() {
                    ISCNIRScanSDK.SaveReference();
                    SaveReferenceDialog();
                }}, 200);
        }
        float interval_time = 0;
        int repeat = 0;
        if(Current_Scan_Method == ScanMethod.Normal)
        {
            interval_time = Float.parseFloat(et_normal_interval_time.getText().toString());
            repeat = Integer.parseInt(et_normal_scan_repeat.getText().toString()) -1;//-1 want to match scan count
        }
        else//Quick set mode
        {
            interval_time = Integer.parseInt(et_quickset_scan_interval_time.getText().toString());
            repeat = Integer.parseInt(et_quickset_continuous_scan_repeat.getText().toString()) -1;//-1 want to match scan count
        }
        if (continuous) {
            progressBarinsideText.setText("Scanning : " + Integer.toString(continuous_count +1));
            if(continuous_count == repeat +1 || stop_continuous == true)
            {
                continuous = false;
                stop_continuous = false;
                toggle_btn_quickset_continuous_scan_mode.setChecked(false);
                toggle_btn_continuous_scan.setChecked(false);
                Disable_Stop_Continous_button();
                String content = "总共进行了 " + continuous_count + " 次扫描！";
                Dialog_Pane("连续扫描完成！",content);
                continuous_count = 0;
                progressBarinsideText.setVisibility(View.GONE);
                return;
            }
            continuous_count ++;
            calProgress.setVisibility(View.VISIBLE);
            progressBarinsideText.setVisibility(View.VISIBLE);
            btn_scan.setText(getString(R.string.scanning));
            DisableAllComponent();
            Enable_Stop_Continous_button();
            try {
                Thread.sleep((long) (interval_time*1000));
            }catch (Exception e)
            {

            }
            Handler handler = new Handler();
            handler.postDelayed(new Runnable(){

                @Override
                public void run() {
                    ISCNIRScanSDK.StartScan();
                }}, delaytime);

        }
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
                // 更新警告文本
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // 更新UI的代码，例如：
                      tv_reference_results.setText("网络异常：" + e.getMessage());
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
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                // 更新UI的代码，例如：
                                tv_reference_results.setText("预测结果：" + parsingJSON(result));
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
                            tv_reference_results.setText("请求失败:" + response.message());
                        }
                    });
                }
            }
        });
    }
    private String parsingJSON(String jsonResponse){
        try {
            JSONArray jsonArray = new JSONArray(jsonResponse);
            String result = "";
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONArray innerArray = jsonArray.getJSONArray(i);
                String fileName = innerArray.getString(0);
                result += (i + 1) +"：" + fileName;
                // 提取成分数据
                for (int j = 1; j < innerArray.length(); j++) {
                    String component = innerArray.getString(j);
                    System.out.println("Component: " + component);
                    result += "\n" + component;
                }
                result += "\n" + "------------------------\n";
            }
            return result;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return "";
    }
    private String ErrorByteTransfer()
    {
        String ErrorMsg = "";
        int ErrorInt = errbyte[0]&0xFF | (errbyte[1] << 8);
        if((ErrorInt & 0x00000001) > 0)//Scan Error
        {
            ErrorMsg += "Scan Error : ";
            int ErrDetailInt = errbyte[4]&0xFF;
            if ((ErrDetailInt & 0x01) > 0)
                ErrorMsg += "DLPC150 Boot Error Detected.    ";
            if ((ErrDetailInt & 0x02) > 0)
                ErrorMsg += "DLPC150 Init Error Detected.    ";
            if ((ErrDetailInt & 0x04) > 0)
                ErrorMsg += "DLPC150 Lamp Driver Error Detected.    ";
            if ((ErrDetailInt & 0x08) > 0)
                ErrorMsg += "DLPC150 Crop Image Failed.    ";
            if ((ErrDetailInt & 0x10) > 0)
                ErrorMsg += "ADC Data Error.    ";
            if ((ErrDetailInt & 0x20) > 0)
                ErrorMsg += "Scan Config Invalid.    ";
            if ((ErrDetailInt & 0x40) > 0)
                ErrorMsg += "Scan Pattern Streaming Error.    ";
            if ((ErrDetailInt & 0x80) > 0)
                ErrorMsg += "DLPC150 Read Error.    ";
            ErrorMsg += ",";
        }
        if ((ErrorInt & 0x00000002) > 0)  // ADC Error
        {
            ErrorMsg += "ADC Error : ";
            int ErrDetailInt = errbyte[5]&0xFF;
            if (ErrDetailInt == 1)
                ErrorMsg += "Timeout Error.    ";
            else if (ErrDetailInt == 2)
                ErrorMsg += "PowerDown Error.    ";
            else if (ErrDetailInt == 3)
                ErrorMsg += "PowerUp Error.    ";
            else if (ErrDetailInt == 4)
                ErrorMsg += "Standby Error.    ";
            else if (ErrDetailInt == 5)
                ErrorMsg += "WakeUp Error.    ";
            else if (ErrDetailInt == 6)
                ErrorMsg += "Read Register Error.    ";
            else if (ErrDetailInt == 7)
                ErrorMsg += "Write Register Error.    ";
            else if (ErrDetailInt == 8)
                ErrorMsg += "Configure Error.    ";
            else if (ErrDetailInt == 9)
                ErrorMsg += "Set Buffer Error.    ";
            else if (ErrDetailInt == 10)
                ErrorMsg += "Command Error.    ";
            else if (ErrDetailInt == 11)
                ErrorMsg += "Set PGA Error.    ";
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
            int ErrDetailInt = errbyte[11]&0xFF;
            if (ErrDetailInt == 1)
                ErrorMsg += "DLPC150 Error.    ";
            else if (ErrDetailInt == 2)
                ErrorMsg += "Read UUID Error.    ";
            else if (ErrDetailInt == 3)
                ErrorMsg += "Flash Initial Error.    ";
            ErrorMsg += ",";
        }
        if ((ErrorInt & 0x00000080) > 0)  // TMP Sensor Error
        {
            ErrorMsg += "TMP Error : ";
            int ErrDetailInt = errbyte[12]&0xFF;
            if (ErrDetailInt == 1)
                ErrorMsg += "Invalid Manufacturing ID.    ";
            else if (ErrDetailInt == 2)
                ErrorMsg += "Invalid Device ID.    ";
            else if (ErrDetailInt == 3)
                ErrorMsg += "Reset Error.    ";
            else if (ErrDetailInt == 4)
                ErrorMsg += "Read Register Error.    ";
            else if (ErrDetailInt == 5)
                ErrorMsg += "Write Register Error.    ";
            else if (ErrDetailInt == 6)
                ErrorMsg += "Timeout Error.    ";
            else if (ErrDetailInt == 7)
                ErrorMsg += "I2C Error.    ";
            ErrorMsg += ",";
        }
        if ((ErrorInt & 0x00000100) > 0)  // HDC Sensor Error
        {
            ErrorMsg += "HDC Error : ";
            int ErrDetailInt = errbyte[13]&0xFF;
            if (ErrDetailInt == 1)
                ErrorMsg += "Invalid Manufacturing ID.    ";
            else if (ErrDetailInt == 2)
                ErrorMsg += "Invalid Device ID.    ";
            else if (ErrDetailInt == 3)
                ErrorMsg += "Reset Error.    ";
            else if (ErrDetailInt == 4)
                ErrorMsg += "Read Register Error.    ";
            else if (ErrDetailInt == 5)
                ErrorMsg += "Write Register Error.    ";
            else if (ErrDetailInt == 6)
                ErrorMsg += "Timeout Error.    ";
            else if (ErrDetailInt == 7)
                ErrorMsg += "I2C Error.    ";
            ErrorMsg += ",";
        }
        if ((ErrorInt & 0x00000200) > 0)  // Battery Error
        {
            ErrorMsg += "Battery Error : ";
            int ErrDetailInt = errbyte[14]&0xFF;
            if (ErrDetailInt == 0x01)
                ErrorMsg += "Battery Low.    ";
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
            int ErrDetailInt = errbyte[17]&0xFF;
            if ((ErrDetailInt & 0x01) > 0)
                ErrorMsg += "Unstable Lamp ADC.    ";
            if ((ErrDetailInt & 0x02) > 0)
                ErrorMsg += "Unstable Peak Intensity.    ";
            if ((ErrDetailInt & 0x04) > 0)
                ErrorMsg += "ADS1255 Error.    ";
            if ((ErrDetailInt & 0x08) > 0)
                ErrorMsg += "Auto PGA Error.    ";

            ErrDetailInt = errbyte[18]&0xFF;
            if ((ErrDetailInt & 0x01) > 0)
                ErrorMsg += "Unstable Scan in Repeated times.    ";
            ErrorMsg += ",";
        }
        if(ErrorMsg.equals(""))
            ErrorMsg = "Not Found";
        return ErrorMsg;
    }
    /**
     * Write scan data to CSV file
     * @param scanResults the {@link ISCNIRScanSDK.ScanResults} structure to save
     */
    private void writeCSV(ISCNIRScanSDK.ScanResults scanResults) {
        Boolean HaveError = false;
        int scanType;
        int numSections;
        String widthnm[] ={"","","2.34","3.51","4.68","5.85","7.03","8.20","9.37","10.54","11.71","12.88","14.05","15.22","16.39","17.56","18.74"
                ,"19.91","21.08","22.25","23.42","24.59","25.76","26.93","28.10","29.27","30.44","31.62","32.79","33.96","35.13","36.30","37.47","38.64","39.81"
                ,"40.98","42.15","43.33","44.50","45.67","46.84","48.01","49.18","50.35","51.52","52.69","53.86","55.04","56.21","57.38","58.55","59.72","60.89"};
        String widthnm_plus[] ={"","","2","3","4","5","6","7","8","9","10","11","12","13","14","15","16"
                ,"17","18","19","20","21","22","23","24","25","26","27","28","29","30","31","32","33","34"
                ,"35","36","37","38","39","40","41","42","43","44","45","46","47","48","49","50","51","52","53","54","55","56","57","58","59","60","61"};
        String exposureTime[] = {"0.635","1.27"," 2.54"," 5.08","15.24","30.48","60.96"};
        int index = 0;
        double temp;
        double humidity;
        //-------------------------------------------------
        String newdate = "";
        String CSV[][] = new String[35][15];
        for (int i = 0; i < 35; i++)
            for (int j = 0; j < 15; j++)
                CSV[i][j] = ",";

        numSections = Scan_Config_Info.numSections[0];
        scanType = Scan_Config_Info.scanType[0];
        //----------------------------------------------------------------
        String configname = getBytetoString(Scan_Config_Info.configName);
        if(Current_Scan_Method == ScanMethod.Maintain && Toggle_Button_maintain_reference.isChecked())
        {
            configname = "Reference";
            Date datetime = new Date();
            SimpleDateFormat format_1 = new SimpleDateFormat("yy/mm/dd");
            SimpleDateFormat format_2 = new SimpleDateFormat("HH:mm:ss");
            newdate = format_1.format(datetime) + "T" + format_2.format(datetime);
            CSV[14][8] = newdate;
        }
        else
        {
            CSV[14][8] =  Reference_Info.refday[0]  + "/" +Reference_Info.refday[1] + "/"+ Reference_Info.refday[2] + "T" + Reference_Info.refday[3] + ":" + Reference_Info.refday[4] + ":" + Reference_Info.refday[5];
        }
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
        if((isExtendVer && fw_level_ext.compareTo(ISCNIRScanSDK.FW_LEVEL_EXT.LEVEL_EXT_2)>=0) || (!isExtendVer &&  fw_level_standard.compareTo(ISCNIRScanSDK.FW_LEVEL_STANDARD.LEVEL_3)>=0))
            CSV[14][0] = "Lamp ADC:,";
        else
            CSV[14][0] = "Lamp Indicator:,";
        CSV[15][0] = "Data Date-Time:,";
        CSV[16][0] = "Total Measurement Time in sec:,";

        CSV[1][1] = configname  + ",";
        CSV[2][1] = "Slew,";
        CSV[2][2] = "Num Section:,";
        CSV[2][3] = Integer.toString(numSections) + ",";

        for(int i=0;i<numSections;i++)
        {
            if(Scan_Config_Info.sectionScanType[i] ==0)
                CSV[3][i+1] = "Column,";
            else
                CSV[3][i+1] = "Hadamard,";
            CSV[4][i+1] = Scan_Config_Info.sectionWavelengthStartNm[i] + ",";
            CSV[5][i+1] = Scan_Config_Info.sectionWavelengthEndNm[i] + ",";
            index = Scan_Config_Info.sectionWidthPx[i];
            if(isExtendVer_PLUS)
                CSV[6][i+1] = widthnm_plus[index] + ",";
            else
                CSV[6][i+1] = widthnm[index] + ",";
            index = Scan_Config_Info.sectionExposureTime[i];
            CSV[7][i+1] = exposureTime[index] + ",";
            CSV[8][i+1] = Scan_Config_Info.sectionNumPatterns[i] + ",";
        }
        CSV[9][1] =Scan_Config_Info. sectionNumRepeats[0] + ",";
        CSV[10][1] = Scan_Config_Info.pga[0] + ",";
        temp = Scan_Config_Info.systemp[0];
        temp = temp/100;
        CSV[11][1] = temp  + ",";
        humidity =  Scan_Config_Info.syshumidity[0];
        humidity =  humidity/100;
        CSV[12][1] = humidity  + ",";
        CSV[13][1] = battery + ",";
        CSV[14][1] = Scan_Config_Info.lampintensity[0] + ",";
        CSV[15][1] = Scan_Config_Info.day[0] + "/" + Scan_Config_Info.day[1] + "/"+ Scan_Config_Info.day[2]  + "T" + Scan_Config_Info.day[3] + ":" + Scan_Config_Info.day[4] + ":" + Scan_Config_Info.day[5] + ",";
        CSV[16][1] = Double.toString((double) MesureScanTime/1000);
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
             mfg_num  = new String(MFG_NUM, "ISO-8859-1");
            if (!mfg_num.contains("70UB1") && !mfg_num.contains("95UB1"))
                mfg_num = "";
            else if(mfg_num.contains("95UB1"))//Extended 19 words, standard 18 words
            {
                if(isExtendVer && mfg_num.length()>=19)
                    mfg_num = mfg_num.substring(0,19);
                else if(!isExtendVer_PLUS && !isExtendVer && mfg_num.length()>=18)
                    mfg_num = mfg_num.substring(0,18);
                else
                    mfg_num = mfg_num.substring(0,mfg_num.length()-2);
            }
        }catch (Exception e)
        {
            mfg_num = "";
        };
        CSV[19][2] = mfg_num + ",";
        String version = "";
        int versionCode = 0;
        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            version = pInfo.versionName;
            versionCode = pInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
        }
        CSV[20][1] = version + "." + Integer.toString(versionCode);
        CSV[21][1] = Tivarev + ",";
        CSV[22][1] = uuid + ",";
        String split_hw[] = HWrev.split("\\.");
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
        if(getBytetoString(Reference_Info.refconfigName).equals("SystemTest"))
            CSV[1][8] = "Built-in Factory Reference";
        else
            CSV[1][8] = "Built-in User Reference";
        CSV[2][8] = "Slew,";
        CSV[2][9] = "Num Section:,";
        CSV[2][10] = "1,";
        if(Reference_Info.refconfigtype[0] == 0)
            CSV[3][8] = "Column,";
        else
            CSV[3][8] = "Hadamard,";
        CSV[4][8] = Double.toString(Reference_Info.refstartwav[0]);
        CSV[5][8] = Double.toString(Reference_Info.refendwav[0]);
        index = Reference_Info.width[0];
        if(isExtendVer_PLUS)
            CSV[6][8] = widthnm_plus[index] + ",";
        else
            CSV[6][8] = widthnm[index] + ",";
        index = Reference_Info.refexposuretime[0];
        CSV[7][8] = exposureTime[index] + ",";
        CSV[8][8] = Integer.toString( Reference_Info.numpattren[0]) + ",";
        CSV[9][8] = Reference_Info.numrepeat[0] + ",";
        CSV[10][8] = Integer.toString(Reference_Info.refpga[0]);
        temp = Reference_Info.refsystemp[0];
        temp = temp/100;
        CSV[11][8] = Double.toString(temp) + ",";
        humidity =  Reference_Info.refsyshumidity[0]/100;
        CSV[12][8] = Double.toString(humidity) ;
        CSV[13][8] = Reference_Info.reflampintensity[0] +",";

        //Calibration Coefficients
        CSV[17][7] = "***Calibration Coefficients***,";
        CSV[18][7] = "Shift Vector Coefficients:,";
        CSV[19][7] = "Pixel to Wavelength Coefficients:,";
        CSV[21][7] = "***Lamp Usage * **,";
        CSV[22][7] ="Total Time(HH:MM:SS):,";
        CSV[23][7] ="***Device/Error Status***,";
        CSV[24][7] ="Device Status:,";
        CSV[25][7] ="Error status:,";

        CSV[18][8] = Scan_Config_Info.shift_vector_coff[0] + ",";
        CSV[18][9] = Scan_Config_Info.shift_vector_coff[1] + ",";
        CSV[18][10] = Scan_Config_Info.shift_vector_coff[2] + ",";

        CSV[19][8] = Scan_Config_Info.pixel_coff[0] + ",";
        CSV[19][9] = Scan_Config_Info.pixel_coff[1] + ",";
        CSV[19][10] = Scan_Config_Info.pixel_coff[2] + ",";
        CSV[22][8] = TotalLampTime + ",";
        final StringBuilder stringBuilder = new StringBuilder(8);
        for(int i= 3;i>= 0;i--)
            stringBuilder.append(String.format("%02X", devbyte[i]));
        CSV[24][8] ="0x" + stringBuilder.toString();
        final StringBuilder stringBuilder_errorstatus = new StringBuilder(8);
        for(int i= 3;i>= 0;i--)
            stringBuilder_errorstatus.append(String.format("%02X", errbyte[i]));
        CSV[25][8] ="0x" + stringBuilder_errorstatus.toString() + ",";
        final StringBuilder stringBuilder_errorcode = new StringBuilder(8);
        for(int i= 0;i<20;i++)
        {
            stringBuilder_errorcode.append(String.format("%02X", errbyte[i]));
            if(errbyte[i] !=0)
                HaveError = true;
        }
        CSV[25][9] = "Error Code:,";
        CSV[25][10] ="0x" + stringBuilder_errorcode.toString() + ",";
        CSV[27][0] = "***Scan Data***,";
        // 开始写入扫描数据到数组。
        CSVWriter writer;
        // 文件名前缀
        String prefix = filePrefix.getText().toString();
        if (prefix.equals("")) {
            prefix = "ISC";
        }
        if(android.os.Environment.getExternalStorageState().equals(android.os. Environment.MEDIA_REMOVED))
        {
            Toast.makeText(ScanViewActivity.this , "没有找到 SD card." , Toast.LENGTH_SHORT ).show();
            return;
        }
        //--------------------------------------
        // 定位到手机文档目录
        File mSDFile  = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
        File mFile = new File(mSDFile.getParent() + "/" + mSDFile.getName() + "/ISC_Report");
        //No file exist
        if(!mFile.exists())
        {
            mFile.mkdirs();
        }
        // 设置路径权限(报错...)
        mFile.setExecutable(true);
        mFile.setReadable(true);
        mFile.setWritable(true);
        // initiate media scan and put the new things into the path array to
        // make the scanner aware of the location and the files you want to see
        MediaScannerConnection.scanFile(this, new String[] {mFile.toString()}, null, null);
        //-------------------------------------------------------------------------
        // 将组分信息加入文件名中
        String compositionNameAndNumber = getCompositionAndNumberString();
        // 将文件序号加入文件名中
        String fileNum = fileNumber.getText().toString();
        // 将样品编号加入文件命名中
        String sampleNum = et_simple_number.getText().toString();
        String csvOS= "";
        CSV[26][9] = "Error Details:,";

        // 根据是否发生错误，设置文件名
        if(HaveError)
        {
            CSV[26][10] = ErrorByteTransfer();
            csvOS = mSDFile.getParent() + "/" + mSDFile.getName() + "/ISC_Report/" + prefix +
                    "_" + configname + "_" + compositionNameAndNumber + "_" + sampleNum + "_" +
                    CurrentTime + "_Error_Detected_" + fileNum + ".csv";
        }
        else
        {
            CSV[26][10] = "Not Found,";
            csvOS = mSDFile.getParent() + "/" + mSDFile.getName() + "/ISC_Report/" + prefix +
                    "_" + configname + "_" + compositionNameAndNumber + "_"+ sampleNum + "_" +
                    CurrentTime + "_" + fileNum + ".csv";
        }
        // 将文件名加入单行数据中
        row_data[0] = "\"" + prefix + "_" + configname + "_" + CurrentTime + "_" + fileNum + ".csv" + "\"";

        try {
            List<String[]> data = new ArrayList<String[]>();
//            System.out.println("准备将CSV数组中的数据写入data中");
            String buf = "";
            for (int i = 0; i < 28; i++)
            {
                for (int j = 0; j < 15; j++)
                {
                    buf += CSV[i][j];
//                    System.out.println("buf: " + buf + " i: " + i + " j: " + j);
                    if (j == 14)
                    {
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
                //float reflect = (float) Scan_Spectrum_Data.getUncalibratedIntensity()[csvIndex] / Scan_Spectrum_Data.getIntensity()[csvIndex];
                row_data[csvIndex + 1] = String.valueOf(absorb);
                float reference = (float) Scan_Spectrum_Data.getIntensity()[csvIndex];
                data.add(new String[]{String.valueOf(waves), String.valueOf(absorb),String.valueOf(reference), String.valueOf(intens)});
            }
            // 结尾添加属性信息
            data.add(new String[]{"***End of Scan Data***"});
            // 添加row数据到post_data
            post_data.add(row_data);
            for (int i = 0; i < textileCompositions.size() && i < editTexts.size(); i++){
                if (!editTexts.get(i).getText().toString().equals("")){
                    data.add(new String[]{textileCompositions.get(i).getSelectedItem().toString(), editTexts.get(i).getText().toString()});
                }
            }

            if(isExtendVer_PLUS)
                data = WriteADCNotTimeStamp_PLUS(data,CSV);
            else if((!isExtendVer_PLUS && !isExtendVer && fw_level_standard.compareTo(ISCNIRScanSDK.FW_LEVEL_STANDARD.LEVEL_5)==0)
                || (isExtendVer && fw_level_ext.compareTo(ISCNIRScanSDK.FW_LEVEL_EXT.LEVEL_EXT_4)==0))
                data = WriteADCTimeStamp(data,CSV);
            else if((isExtendVer && (fw_level_ext.compareTo(ISCNIRScanSDK.FW_LEVEL_EXT.LEVEL_EXT_2)==0 || fw_level_ext.compareTo(ISCNIRScanSDK.FW_LEVEL_EXT.LEVEL_EXT_4)==0))
                    || (!isExtendVer_PLUS && !isExtendVer &&  (fw_level_standard.compareTo(ISCNIRScanSDK.FW_LEVEL_STANDARD.LEVEL_3)==0 || fw_level_standard.compareTo(ISCNIRScanSDK.FW_LEVEL_STANDARD.LEVEL_5)==0) ))
                data = WriteADCNotTimeStamp(data,CSV);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
                MediaStoreWriteCSV(data,configname,prefix);
            else
            {
                // initiate media scan and put the new things into the path array to
                // make the scanner aware of the location and the files you want to see
//                System.out.println("使用CSVWriter输出！");
                MediaScannerConnection.scanFile(this, new String[] {csvOS}, null, null);
                writer = new CSVWriter(new FileWriter(csvOS), ',', CSVWriter.NO_QUOTE_CHARACTER);
                writer.writeAll(data);
                writer.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private void MediaStoreWriteCSV(List<String[]> data,String configname,String prefix)
    {
//        System.out.println("使用MediaStoreWriteCSV输出！");
        try {
            // 将组分信息加入文件名中
            String compositionNameAndNumber = getCompositionAndNumberString();
            // 将文件序号加入文件名中
            String fileNum = fileNumber.getText().toString();
            // 将样品编号加入文件命名中
            String sampleNum = et_simple_number.getText().toString();

            ContentValues values = new ContentValues();
            values.put(MediaStore.MediaColumns.DISPLAY_NAME, prefix+"_" + configname + "_" +
                    compositionNameAndNumber +  "_" + sampleNum + "_" + CurrentTime + "_" + fileNum + ".csv");       //file name
            values.put(MediaStore.MediaColumns.MIME_TYPE, "text/comma-separated-values");
            values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS + "/ISC_Report/");
            Uri uri = getContentResolver().insert(MediaStore.Files.getContentUri("external"), values);
            OutputStream outputStream = getContentResolver().openOutputStream(uri);
            int Len = data.size();
            for(int i=0;i<Len;i++)
            {
                String datacontent = "";
                for(int j=0;j<data.get(i).length;j++)
                {
                    datacontent +=  data.get(i)[j];
                    if(j < data.get(i).length - 1)
                        datacontent += ",";
                }
                datacontent += "\r\n";
                outputStream.write(datacontent.getBytes());
            }
            outputStream.close();
        }catch (Exception e)
        {

        }
    }
    private List<String[]> WriteADCTimeStamp(List<String[]> data,String CSV[][] )
    {
        try {
            data.add(new String[]{""});
            data.add(new String[]{"***Lamp Ramp Up ADC***,"});
            data.add(new String[]{"Timestamp(ms),ADC0,ADC1,ADC2,ADC3"});
            String[] ADC = new String[5];
            int count = 0;
            int timeindex = 0;
            List<Integer> time = new ArrayList<Integer>();
            for(int i=0;i<Lamp_RAMPUP_ADC_TIMESTAMP.length;i+=4)
            {
                Integer buftime = (Lamp_RAMPUP_ADC_TIMESTAMP[i+1]&0xff)<<8|Lamp_RAMPUP_ADC_TIMESTAMP[i]&0xff;
                if(buftime ==0)
                    break;
                else
                    time.add(buftime);
            }
            for(int i=0;i<Lamp_RAMPUP_ADC_DATA.length;i+=2)
            {
                int adc_value = (Lamp_RAMPUP_ADC_DATA[i+1]&0xff)<<8|Lamp_RAMPUP_ADC_DATA[i]&0xff;
                if(adc_value ==0)
                    break;
                ADC[count + 1] = Integer.toString(adc_value) ;
                count ++;
                if(count ==4)
                {
                    ADC[0] = Integer.toString(time.get(timeindex));
                    timeindex ++;
                    data.add(ADC);
                    count = 0;
                    ADC = new String[5];
                }
            }
            data.add(new String[]{""});
            data.add(new String[]{"***Lamp ADC among repeated times***,"});
            data.add(new String[]{"Timestamp(ms),ADC0,ADC1,ADC2,ADC3"});
            ADC = new String[5];
            int Average_ADC[] = new int[4];
            int cal_count =0;
            count = 0;
            for(int i=0;i<Lamp_AVERAGE_ADC_DATA.length;i+=2)
            {
                int adc_value = (Lamp_AVERAGE_ADC_DATA[i+1]&0xff)<<8|Lamp_AVERAGE_ADC_DATA[i]&0xff;
                if(adc_value ==0 )
                    break;
                ADC[count + 1] =Integer.toString(adc_value) ;
                Average_ADC[count] +=adc_value;
                count ++;
                if(count ==4)
                {
                    ADC[0] = Integer.toString(time.get(timeindex));
                    timeindex ++;
                    data.add(ADC);
                    cal_count ++;
                    count = 0;
                    ADC = new String[5];
                }
            }
            String AverageADC = "Lamp ADC:,";

            for(int i=0;i<4;i++)
            {
                double buf_adc = (double)Average_ADC[i];
                AverageADC +=Math.round( buf_adc/cal_count) + ",";
            }
            AverageADC +=",," + CSV[14][7] + CSV[14][8];// add ref data-time data
            data.get(14)[0] = AverageADC;
        }catch (Exception e)
        {

        }
        return  data;
    }
    private List<String[]> WriteADCNotTimeStamp(List<String[]> data,String CSV[][] )
    {
        try {
            data.add(new String[]{""});
            data.add(new String[]{"***Lamp Ramp Up ADC***,"});
            data.add(new String[]{"ADC0,ADC1,ADC2,ADC3"});
            String[] ADC = new String[4];
            int count = 0;
            for(int i=0;i<Lamp_RAMPUP_ADC_DATA.length;i+=2)
            {
                int adc_value = (Lamp_RAMPUP_ADC_DATA[i+1]&0xff)<<8|Lamp_RAMPUP_ADC_DATA[i]&0xff;
                if(adc_value ==0)
                    break;
                ADC[count] = Integer.toString(adc_value) ;
                count ++;
                if(count ==4)
                {
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
            int Average_ADC[] = new int[4];
            int cal_count =0;
            count = 0;
            for(int i=0;i<Lamp_AVERAGE_ADC_DATA.length;i+=2)
            {
                int adc_value = (Lamp_AVERAGE_ADC_DATA[i+1]&0xff)<<8|Lamp_AVERAGE_ADC_DATA[i]&0xff;
                if(adc_value ==0 )
                    break;
                ADC[count] =Integer.toString(adc_value) ;
                Average_ADC[count] +=adc_value;
                count ++;
                if(count ==4)
                {
                    data.add(ADC);
                    cal_count ++;
                    count = 0;
                    ADC = new String[4];
                }
            }
            String AverageADC = "Lamp ADC:,";

            for(int i=0;i<4;i++)
            {
                double buf_adc = (double)Average_ADC[i];
                AverageADC +=Math.round( buf_adc/cal_count) + ",";
            }
            AverageADC +=",," + CSV[14][7] + CSV[14][8];// add ref data-time data
            data.get(14)[0] = AverageADC;
        }catch (Exception e)
        {

        }
        return  data;
    }
    private List<String[]> WriteADCNotTimeStamp_PLUS(List<String[]> data,String CSV[][] )
    {
        try {
            data.add(new String[]{""});
            data.add(new String[]{"***Lamp Ramp Up ADC***,"});
            data.add(new String[]{"ADC0,ADC1,ADC2"});
            String[] ADC = new String[3];
            int count = 0;
            for(int i=0;i<Lamp_RAMPUP_ADC_DATA.length;i+=2)
            {
                int adc_value = (Lamp_RAMPUP_ADC_DATA[i+1]&0xff)<<8|Lamp_RAMPUP_ADC_DATA[i]&0xff;
                if(adc_value ==0)
                    break;
                if(count < 3)
                    ADC[count] = Integer.toString(adc_value) ;
                count ++;
                if(count ==4)
                {
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
            int Average_ADC[] = new int[3];
            int cal_count =0;
            count = 0;
            for(int i=0;i<Lamp_AVERAGE_ADC_DATA.length;i+=2)
            {
                int adc_value = (Lamp_AVERAGE_ADC_DATA[i+1]&0xff)<<8|Lamp_AVERAGE_ADC_DATA[i]&0xff;
                if(adc_value ==0 )
                    break;
                if(count < 3)
                {
                    ADC[count] =Integer.toString(adc_value) ;
                    Average_ADC[count] +=adc_value;
                }
                count ++;
                if(count ==4)
                {
                    data.add(ADC);
                    cal_count ++;
                    count = 0;
                    ADC = new String[3];
                }
            }
            String AverageADC = "Lamp ADC:,";

            for(int i=0;i<3;i++)
            {
                double buf_adc = (double)Average_ADC[i];
                AverageADC +=Math.round( buf_adc/cal_count) + ",";
            }
            AverageADC +=",,," + CSV[14][7] + CSV[14][8];// add ref data-time data
            data.get(14)[0] = AverageADC;
        }catch (Exception e)
        {

        }
        return  data;
    }
    // 返回文件名String
    public String getCompositionAndNumberString() {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < textileCompositions.size() && i < editTexts.size(); i++) {
            Spinner spinner = textileCompositions.get(i);
            EditText editText = editTexts.get(i);

            String composition = spinner.getSelectedItem() != null ? spinner.getSelectedItem().toString().trim() : "";
            String number = editText.getText().toString().trim();

            if (!composition.isEmpty() && !number.isEmpty()) {
                if (sb.length() > 0) {
                    sb.append(",");
                }
                sb.append(composition).append(",").append(number);
            }
        }

        return sb.toString();
    }
    //endregion
    //region Draw spectral plot
    /**
     * Pager enum to control tab tile and layout resource
     */
    public enum CustomPagerEnum {
        REFLECTANCE(R.string.reflectance, R.layout.page_graph_reflectance),
        ABSORBANCE(R.string.absorbance, R.layout.page_graph_absorbance),
        INTENSITY(R.string.intensity, R.layout.page_graph_intensity),
        REFERENCE(R.string.reference_tab,R.layout.page_graph_reference);
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
                LineChart mChart = (LineChart) layout.findViewById(R.id.lineChartInt);
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
                int numSections= Scan_Config_Info.numSections[0];
                /*if(activeConf != null && activeConf.getScanType().equals("Slew")) {
                    numSections = activeConf.getSlewNumSections();
                }*/
                if(numSections>=2 &&(Float.isNaN(minIntensity)==false && Float.isNaN(maxIntensity)==false) && Current_Scan_Method!= ScanMethod.QuickSet)//Scan method : quickset only one section
                {
                    setDataSlew(mChart, mIntensityFloat,numSections); //scan data section > 1
                }
                else if(Float.isNaN(minIntensity)==false && Float.isNaN(maxIntensity)==false)
                {
                    setData(mChart, mXValues, mIntensityFloat,ChartType.INTENSITY);//scan data section = 1
                }


                mChart.animateX(2500, Easing.EasingOption.EaseInOutQuart);

                // get the legend (only possible after setting data)
                Legend l = mChart.getLegend();

                // modify the legend ...
                l.setForm(Legend.LegendForm.LINE);
                mChart.getLegend().setEnabled(false);
                return layout;
            } else if (customPagerEnum.getLayoutResId() == R.layout.page_graph_absorbance) {

                LineChart mChart = (LineChart) layout.findViewById(R.id.lineChartAbs);
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
                int numSections= Scan_Config_Info.numSections[0];
                /*if(activeConf != null && activeConf.getScanType().equals("Slew")) {
                    numSections = activeConf.getSlewNumSections();
                }*/
                if(numSections>=2 &&(Float.isNaN(minAbsorbance)==false && Float.isNaN(maxAbsorbance)==false)&& Current_Scan_Method!=ScanMethod.QuickSet)////Scan method : quickset only one section
                {
                    setDataSlew(mChart, mAbsorbanceFloat,numSections);
                }
                else if( Float.isNaN(minAbsorbance)==false && Float.isNaN(maxAbsorbance)==false)
                {
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

                LineChart mChart = (LineChart) layout.findViewById(R.id.lineChartRef);
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
                int numSections= Scan_Config_Info.numSections[0];
                /*if(activeConf != null && activeConf.getScanType().equals("Slew")) {
                    numSections = activeConf.getSlewNumSections();
                }*/
                if(numSections>=2 &&(Float.isNaN(minReflectance)==false && Float.isNaN(maxReflectance)==false)&& Current_Scan_Method!=ScanMethod.QuickSet)//Scan method : quickset only one section
                {
                    setDataSlew(mChart, mReflectanceFloat,numSections);
                }

                else if(Float.isNaN(minReflectance)==false && Float.isNaN(maxReflectance)==false)
                {
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

                LineChart mChart = (LineChart) layout.findViewById(R.id.lineChartReference);
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
                int numSections= Scan_Config_Info.numSections[0];
                /*if(activeConf != null && activeConf.getScanType().equals("Slew")) {
                    numSections = activeConf.getSlewNumSections();
                }*/
                if(numSections>=2 &&(Float.isNaN(minReference)==false && Float.isNaN(maxReference)==false)&& Current_Scan_Method!=ScanMethod.QuickSet)//Scan method : quickset only one section
                {
                    setDataSlew(mChart, mReferenceFloat,numSections);
                }
                else if( Float.isNaN(minReference)==false && Float.isNaN(maxReference)==false)
                {
                    setData(mChart, mXValues, mReferenceFloat, ChartType.INTENSITY);
                }


                mChart.animateX(2500, Easing.EasingOption.EaseInOutQuart);

                // get the legend (only possible after setting data)
                Legend l = mChart.getLegend();

                // modify the legend ...
                l.setForm(Legend.LegendForm.LINE);
                mChart.getLegend().setEnabled(false);

                return layout;
            }else {
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

    private void setData(LineChart mChart, ArrayList<String> xValues, ArrayList<Entry> yValues, ChartType type) {

        if (type == ChartType.REFLECTANCE) {
            //init yvalues
            int size = yValues.size();
            if(size == 0)
            {
                return;
            }
            //---------------------------------------------------------
            // create a dataset and give it a type
            LineDataSet set1 = new LineDataSet(yValues,GraphLabel);
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
        } else if (type == ChartType.ABSORBANCE) {
            int size = yValues.size();
            if(size == 0)
            {
                return;
            }
            // create a dataset and give it a type
            LineDataSet set1 = new LineDataSet(yValues, GraphLabel);

            // set the line to be drawn like this "- - - - - -"
            set1.enableDashedLine(10f, 5f, 0f);
            set1.enableDashedHighlightLine(10f, 5f, 0f);
            set1.setColor(Color.BLACK);
            set1.setCircleColor(Color.GREEN);
            set1.setLineWidth(1f);
            set1.setCircleSize(2f);
            set1.setDrawCircleHole(false);
            set1.setValueTextSize(9f);
            set1.setFillAlpha(65);
            set1.setFillColor(Color.GREEN);
            set1.setDrawFilled(true);
            set1.setValues(yValues);

            ArrayList<ILineDataSet> dataSets = new ArrayList<ILineDataSet>();
            dataSets.add(set1);
            LineData data = new LineData(dataSets);
            mChart.setData(data);

            mChart.setMaxVisibleValueCount(20);
        } else if (type == ChartType.INTENSITY) {
            int size = yValues.size();
            if(size == 0)
            {
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
            if(size == 0)
            {
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

    private void setDataSlew(LineChart mChart, ArrayList<Entry> yValues,int slewnum)
    {
        if(yValues.size()<=1)
        {
            return;
        }
        ArrayList<Entry> yValues1 = new ArrayList<Entry>();
        ArrayList<Entry> yValues2 = new ArrayList<Entry>();
        ArrayList<Entry> yValues3 = new ArrayList<Entry>();
        ArrayList<Entry> yValues4 = new ArrayList<Entry>();
        ArrayList<Entry> yValues5 = new ArrayList<Entry>();

        for(int i=0;i<activeConf.getSectionNumPatterns()[0];i++)
        {
            if(Float.isInfinite(yValues.get(i).getY()) == false)
            {
                yValues1.add(new Entry(yValues.get(i).getX(),yValues.get(i).getY()));
            }
        }
        int offset = activeConf.getSectionNumPatterns()[0];
        for(int i=0;i<activeConf.getSectionNumPatterns()[1];i++)
        {
            if(Float.isInfinite(yValues.get(offset+ i).getY()) == false)
            {
                yValues2.add(new Entry(yValues.get(offset + i).getX(),yValues.get(offset+ i).getY()));
            }
        }
        if(slewnum>=3)
        {
            offset = activeConf.getSectionNumPatterns()[0] + activeConf.getSectionNumPatterns()[1];
            for(int i=0;i<activeConf.getSectionNumPatterns()[2];i++)
            {
                if(Float.isInfinite(yValues.get(offset+ i).getY()) == false)
                {
                    yValues3.add(new Entry(yValues.get(offset + i).getX(),yValues.get(offset+ i).getY()));
                }

            }
        }
        if(slewnum>=4)
        {
            offset = activeConf.getSectionNumPatterns()[0] + activeConf.getSectionNumPatterns()[1]+ activeConf.getSectionNumPatterns()[2];
            for(int i=0;i<activeConf.getSectionNumPatterns()[3];i++)
            {
                if(Float.isInfinite(yValues.get(offset+ i).getY()) == false)
                {
                    yValues4.add(new Entry(yValues.get(offset + i).getX(),yValues.get(offset+ i).getY()));
                }
            }
        }
        if(slewnum==5)
        {
            offset = activeConf.getSectionNumPatterns()[0] + activeConf.getSectionNumPatterns()[1]+ activeConf.getSectionNumPatterns()[2]+ activeConf.getSectionNumPatterns()[3];
            for(int i=0;i<activeConf.getSectionNumPatterns()[4];i++)
            {
                if(Float.isInfinite(yValues.get(offset+ i).getY()) == false)
                {
                    yValues5.add(new Entry(yValues.get(offset + i).getX(),yValues.get(offset+ i).getY()));
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

        if(slewnum==2)
        {
            LineData data = new LineData(set1, set2);
            mChart.setData(data);
            mChart.setMaxVisibleValueCount(20);
        }
        if(slewnum==3)
        {
            LineData data = new LineData(set1, set2,set3);
            mChart.setData(data);
            mChart.setMaxVisibleValueCount(20);
        }

        if(slewnum==4)
        {
            LineData data = new LineData(set1, set2,set3,set4);
            mChart.setData(data);
            mChart.setMaxVisibleValueCount(20);
        }

        if(slewnum==5)
        {
            LineData data = new LineData(set1, set2,set3,set4,set5);
            mChart.setData(data);
            mChart.setMaxVisibleValueCount(20);
        }
    }
    /**
     * Custom enum for chart type
     */
    public enum ChartType {
        REFLECTANCE,
        ABSORBANCE,
        INTENSITY
    }
    //endregion
    //region Common function
    private Boolean checkValidRepeat()
    {
        try
        {
            int value = Integer.parseInt(et_manual_repead.getText().toString());
            if(value>=1&&value<=50)
            {
                return true;
            }
        }
        catch (NumberFormatException ex)
        {
        }

        return false;
    }
    private Boolean checkValidPga()
    {
        try
        {
            int value = Integer.parseInt(et_manual_pga.getText().toString());
            if(value==1 || value == 2 || value == 4 || value==8 || value==16 || value==32 ||value==64)
            {
                return true;
            }
        }
        catch (NumberFormatException ex)
        {
        }
        return false;
    }

    private Boolean checkQuicksetValue()
    {
        if(Integer.parseInt(et_quickset_spec_start.getText().toString())<MINWAV || Integer.parseInt(et_quickset_spec_start.getText().toString())>MAXWAV)
        {
            Dialog_Pane("Error","Spectral Start (nm) range is " + MINWAV + "~" + MAXWAV + ".");
            return false;
        }
        if(Integer.parseInt(et_quickset_spec_end.getText().toString())<MINWAV || Integer.parseInt(et_quickset_spec_end.getText().toString())>MAXWAV)
        {
            Dialog_Pane("Error","Spectral End (nm) range is "  + MINWAV + "~" + MAXWAV + ".");
            return false;
        }
        if(Integer.parseInt(et_quickset_spec_end.getText().toString())<= Integer.parseInt(et_quickset_spec_start.getText().toString()))
        {
            Dialog_Pane("Error","Spectral End (nm) should larger than  Spectral Start (nm).");
            return false;
        }
        if(Integer.parseInt(et_quickset_average_scan.getText().toString())>65535)
        {
            Dialog_Pane("Error","Average Scans (times) range is 0~65535.");
            return false;
        }
        if(Integer.parseInt(et_quickset_res.getText().toString())>MaxPattern || Integer.parseInt(et_quickset_res.getText().toString())<2)
        {
            Dialog_Pane("Error","D-Res. range is 2~" + MaxPattern + ".");
            return false;
        }
        return true;
    }
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
            //Shift the binary value of the first value by 4 bits to the left,ex: 00001000 => 10000000 (8=>128)
            //Then concatenate with the binary value of the second value ex: 10000000 | 00001100 => 10001100 (137)
            int value = (high << 4) | low;
            //Complementary with FFFFFFFF
            if (value > 127)
                value -= 256;
            //Finally change to byte
            rawData [i] = (byte) value;
        }
        return rawData ;
    }
    public static String getBytetoString(byte configName[]) {
        byte[] byteChars = new byte[40];
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        byte[] var3 = byteChars;
        int i = byteChars.length;
        for(int var5 = 0; var5 < i; ++var5) {
            byte b = var3[var5];
            byteChars[b] = 0;
        }
        String s = null;
        for(i = 0; i < configName.length; ++i) {
            byteChars[i] = configName[i];
            if(configName[i] == 0) {
                break;
            }
            os.write(configName[i]);
        }
        try {
            s = new String(os.toByteArray(), "UTF-8");
        } catch (UnsupportedEncodingException var7) {
            var7.printStackTrace();
        }
        return s;
    }
    //endregion
    //region Dialog Pane
    private void Dialog_Pane_Finish(String title,String content)
    {
        final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(mContext);
        alertDialogBuilder.setTitle(title);
        alertDialogBuilder.setCancelable(false);
        alertDialogBuilder.setMessage(content);
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
    private void Dialog_Pane_maintain(String title,String content)
    {
        final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(mContext);
        alertDialogBuilder.setTitle(title);
        alertDialogBuilder.setMessage(content);

        alertDialogBuilder.setNegativeButton(getResources().getString(R.string.yes_i_know), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface arg0, int arg1) {
                calProgress.setVisibility(View.VISIBLE);
                SetReferenceParameter();
                alertDialog.dismiss();
            }
        });
        alertDialogBuilder.setPositiveButton(getResources().getString(R.string.cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface arg0, int arg1) {
                Toggle_Button_maintain_reference.setChecked(false);
                alertDialog.dismiss();
            }
        });
        alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    private void ReferenceConfigSaveSuccess() {
        final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(mContext);
        alertDialogBuilder.setTitle("Finish");
        alertDialogBuilder.setCancelable(false);
        alertDialogBuilder.setMessage("Complete save reference config, start scan");
        alertDialogBuilder.setPositiveButton(getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface arg0, int arg1) {
                PerformScan(0);
                DisableAllComponent();
                calProgress.setVisibility(View.VISIBLE);
                btn_scan.setText(getString(R.string.scanning));
                alertDialog.dismiss();
            }
        });
        alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }
    private void NotValidValueDialog(String title,String content) {
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
    private void Dialog_Pane(String title,String content)
    {
        final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(mContext);
        alertDialogBuilder.setTitle(title);
        alertDialogBuilder.setCancelable(false);
        alertDialogBuilder.setMessage(content);

        alertDialogBuilder.setPositiveButton(getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface arg0, int arg1) {

                alertDialog.dismiss();
                btn_quickset_set_config.setClickable(true);
            }
        });
        alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }
    private void Dialog_Pane_OpenFunction(String title,String content)
    {
        final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(mContext);
        alertDialogBuilder.setTitle(title);
        alertDialogBuilder.setCancelable(false);
        alertDialogBuilder.setMessage(content);

        alertDialogBuilder.setPositiveButton(getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface arg0, int arg1) {
                alertDialog.dismiss();
                btn_quickset_set_config.setClickable(true);
                openFunction();
            }
        });
        alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }
    private void Dialog_Pane_OldTIVA(String title,String content)
    {
        final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(mContext);
        alertDialogBuilder.setTitle(title);
        alertDialogBuilder.setMessage(content);

        alertDialogBuilder.setNegativeButton(getResources().getString(R.string.yes), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface arg0, int arg1) {
                alertDialog.dismiss();
                NotValidValueDialog("Limited Functions","Running with older Tiva firmware\nis not recommended and functions\nwill be limited!");
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

    Boolean saveReference = false;
    private void SaveReferenceDialog() {
        final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(mContext);
        alertDialogBuilder.setTitle("Finish");
        alertDialogBuilder.setCancelable(false);
        alertDialogBuilder.setMessage("Replace Factory Reference is complete.\nShould reconnect bluetooth to reload reference.");

        alertDialogBuilder.setPositiveButton(getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface arg0, int arg1) {
                alertDialog.dismiss();
                storeStringPref(mContext, ISCNIRScanSDK.SharedPreferencesKeys.ReferenceScan, "ReferenceScan");
                ISCNIRScanSDK.ScanConfig(ActiveConfigByte,ISCNIRScanSDK.ScanConfig.SET);
                saveReference = true;
            }
        });

        alertDialog = alertDialogBuilder.create();
        alertDialog.show();

    }
    //endregion
    // back to this page shoule get active config index
    private void GetActiveConfigOnResume()
    {
        ScanConfigList_from_ScanConfiguration = ScanConfigurationsViewActivity.bufconfigs;//from scan configuration
        ScanConfig_Byte_List_from_ScanConfiuration = ScanConfigurationsViewActivity.bufEXTRADATA_fromScanConfigurationsViewActivity;
        int storenum = ScanConfigList_from_ScanConfiguration.size();
        if(storenum !=0)
        {
            if(storenum!=ScanConfigList.size())
            {
                ScanConfigList.clear();
                ScanConfig_Byte_List.clear();
                for(int i=0;i<ScanConfigList_from_ScanConfiguration.size();i++)
                {
                    ScanConfigList.add(ScanConfigList_from_ScanConfiguration.get(i));
                    ScanConfig_Byte_List.add(ScanConfig_Byte_List_from_ScanConfiuration.get(i));
                }
            }
            for(int i=0;i<ScanConfigList.size();i++)
            {
                int ScanConfigIndextoByte = (byte)ScanConfigList.get(i).getScanConfigIndex();
                if(ActiveConfigindex == ScanConfigIndextoByte )
                {
                    activeConf = ScanConfigList.get(i);
                    ActiveConfigByte = ScanConfig_Byte_List.get(i);
                    tv_normal_scan_conf.setText(activeConf.getConfigName());
                    tv_manual_scan_conf.setText(activeConf.getConfigName());
                }
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        loadData();
        GotoOtherPage = false;
        numSections=0;
        row_data = new String[229];
        post_data.clear();
         // 更新当前文件名编号
        int fileNum = getMaxNumberFromFilenames();
//        System.out.println("当前文件名编号" + fileNum);
        if (fileNum != -1) {
            fileNumber.setText(String.valueOf(fileNum + 1));
        }else {
            fileNumber.setText("0");
        }

        //From HomeViewActivity to ScanViewActivity
        if(!init_viewpage_valuearray)
        {
            init_viewpage_valuearray = true;
            //Initialize view pager
            CustomPagerAdapter pagerAdapter = new CustomPagerAdapter(this);
            mViewPager.setAdapter(pagerAdapter);
            mViewPager.invalidate();
            mViewPager.setOnPageChangeListener(
                    new ViewPager.SimpleOnPageChangeListener() {
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
        }
        else
        {
            if((!isExtendVer_PLUS && !isExtendVer && fw_level_standard.compareTo(ISCNIRScanSDK.FW_LEVEL_STANDARD.LEVEL_0)==0) || isOldTiva)
                closeFunction();
            else if(getStringPref(mContext, ISCNIRScanSDK.SharedPreferencesKeys.Activacatestatus, null).contains("Activated"))
            {
                if(!showActiveconfigpage)
                    openFunction();
            }
            else
                closeFunction();
        }
        if(!showActiveconfigpage)
        {
            LocalBroadcastManager.getInstance(mContext).registerReceiver(ScanConfSizeReceiver, new IntentFilter(ISCNIRScanSDK.SCAN_CONF_SIZE));
            LocalBroadcastManager.getInstance(mContext).registerReceiver(GetActiveScanConfReceiver, new IntentFilter(ISCNIRScanSDK.SEND_ACTIVE_CONF));
            LocalBroadcastManager.getInstance(mContext).registerReceiver(WriteScanConfigStatusReceiver, WriteScanConfigStatusFilter);
            LocalBroadcastManager.getInstance(mContext).registerReceiver(GetDeviceStatusReceiver,new IntentFilter(ISCNIRScanSDK.ACTION_STATUS));

        }
        //-----------------------------------------------------------------------------------------------------------
        //In active page back to this page,do nothing,don't init scan Configuration text
        if(showActiveconfigpage)
        {
            showActiveconfigpage = false;
        }
        //First to connect
        else
        {
            tv_normal_scan_conf.setText(getStringPref(mContext, ISCNIRScanSDK.SharedPreferencesKeys.scanConfiguration, "Column 1"));
            tv_manual_scan_conf.setText(getStringPref(mContext, ISCNIRScanSDK.SharedPreferencesKeys.scanConfiguration, "Column 1"));
        }
        if(!GotoScanConfigFlag && activeConf!=null)
        {
            tv_normal_scan_conf.setText(activeConf.getConfigName());
            tv_manual_scan_conf.setText(activeConf.getConfigName());
        }
        else if(GotoScanConfigFlag)
        {
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
        }
        catch (Exception e)
        {

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
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(ReturnCurrentScanConfigurationDataReceiver);
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
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(GetPGAReceiver);
        mHandler.removeCallbacksAndMessages(null);
        storeBooleanPref(mContext, ISCNIRScanSDK.SharedPreferencesKeys.continuousScan, toggle_btn_continuous_scan.isChecked());
    }
    @Override
    public void onPause() {
        super.onPause();
        //back to desktop,should disconnect to device
//        if(!GotoOtherPage)
//            finish();
        // 取消注册广播接收器，避免重复写入文件
        if(!showActiveconfigpage) {
            LocalBroadcastManager.getInstance(mContext).unregisterReceiver(ScanConfSizeReceiver);
            LocalBroadcastManager.getInstance(mContext).unregisterReceiver(GetActiveScanConfReceiver);
            LocalBroadcastManager.getInstance(mContext).unregisterReceiver(WriteScanConfigStatusReceiver);
            LocalBroadcastManager.getInstance(mContext).unregisterReceiver(GetDeviceStatusReceiver);
        }
    }
    private class  BackGroundReciver extends BroadcastReceiver {
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
    private void setActivityTouchDisable(boolean value) {
        if (value) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
        }
    }
    private void UI_ShowMaxPattern()
    {
        int start_nm = Integer.parseInt(et_quickset_spec_start.getText().toString());
        int end_nm =  Integer.parseInt(et_quickset_spec_end.getText().toString());
        int width_index = quickset_scan_width_index;
        int num_repeat = Integer.parseInt(et_quickset_average_scan.getText().toString());
        int scan_type = quickset_scan_method_index;
        int IsEXTver = 0;
        if(isExtendVer)
        {
            IsEXTver = 1;
        }
        else if(isExtendVer_PLUS)
            IsEXTver = 2;
        MaxPattern = GetMaxPattern(start_nm,end_nm,width_index,num_repeat,scan_type,IsEXTver);
        String text = "D-Res. (pts, max:" + MaxPattern +")";
        tv_quickset_res.setText(text);
    }
    /**
     *  Set device physical button status
     */
    private void SetDeviceButtonStatus()
    {
        if(isExtendVer_PLUS || isExtendVer || (!isExtendVer_PLUS && !isExtendVer && fw_level_standard.compareTo(ISCNIRScanSDK.FW_LEVEL_STANDARD.LEVEL_1)>0))
        {
            Boolean isLockButton = getBooleanPref(mContext, ISCNIRScanSDK.SharedPreferencesKeys.LockButton,false);
            //User open lock button on Configure page
            if(isLockButton)
            {
                ISCNIRScanSDK.ControlPhysicalButton(ISCNIRScanSDK.PhysicalButton.Lock);
            }
            else
            {
                ISCNIRScanSDK.ControlPhysicalButton(ISCNIRScanSDK.PhysicalButton.Unlock);
            }
        }
    }
    /**
     *Get max pattern that user can set
     */
    private int GetMaxPattern(int start_nm,int end_nm,int width_index,int num_repeat,int scan_type,int IsEXTver)
    {
        return ISCNIRScanSDK.GetMaxPatternJNI(scan_type,start_nm,end_nm,width_index,num_repeat,SpectrumCalCoefficients,IsEXTver);
    }
    /**
     * Change scan config to byte array
     */
    public byte[] ChangeScanConfigToByte()
    {
        ISCNIRScanSDK.ScanConfigInfo write_scan_config = new ISCNIRScanSDK.ScanConfigInfo();
        //transfer config name to byte
        String isoString ="QuickSet";
        int name_size = isoString.length();
        byte[] ConfigNamebytes=isoString.getBytes();
        for(int i=0;i<name_size;i++)
        {
            write_scan_config.configName[i] = ConfigNamebytes[i];
        }
        write_scan_config.write_scanType = 2;
        //transfer SerialNumber to byte
        String SerialNumber = "12345678";
        byte[] SerialNumberbytes=SerialNumber.getBytes();
        int SerialNumber_size = SerialNumber.length();
        for(int i=0;i<SerialNumber_size;i++)
        {
            write_scan_config.scanConfigSerialNumber[i] = SerialNumberbytes[i];
        }
        write_scan_config.write_scanConfigIndex = 255;
        write_scan_config.write_numSections =(byte) 1;
        write_scan_config.write_numRepeat = Integer.parseInt(et_quickset_average_scan.getText().toString());
        numSections=1;

        for(int i=0;i<numSections;i++)
        {
            write_scan_config.sectionScanType[i] = (byte)spin_quickset_scan_method.getSelectedItemPosition();
        }
        for(int i=0;i<numSections;i++)
        {
            write_scan_config.sectionWavelengthStartNm[i] =Integer.parseInt(et_quickset_spec_start.getText().toString());
        }
        for(int i=0;i<numSections;i++)
        {
            write_scan_config.sectionWavelengthEndNm[i] =Integer.parseInt(et_quickset_spec_end.getText().toString());
        }
        for(int i=0;i<numSections;i++)
        {
            write_scan_config.sectionNumPatterns[i] =Integer.parseInt(et_quickset_res.getText().toString());
        }
        for(int i=0;i<numSections;i++)
        {
            write_scan_config.sectionWidthPx[i] = (byte)(spin_quickset_scan_width.getSelectedItemPosition()+2);
        }
        for(int i=0;i<numSections;i++)
        {
            write_scan_config.sectionExposureTime[i] = spin_quickset_exposure_time.getSelectedItemPosition();
        }
        return ISCNIRScanSDK.WriteScanConfiguration(write_scan_config);
    }
    /**
     * Compare the device's  config  and user settings are the same or not
     * @param EXTRA_DATA  scan config byte data
     */
    public Boolean Compareconfig(byte EXTRA_DATA[])
    {
        if(EXTRA_DATA.length!=155)
        {
            return false;
        }
        String model = "";
        if(!model_name.isEmpty() && model_name != null)
        {
            String[] SplitModel = model_name.split("-");
            if(SplitModel.length > 0)
                model = SplitModel[SplitModel.length - 1];
        }
        ISCNIRScanSDK.ScanConfiguration config = ISCNIRScanSDK.current_scanConf;
        if(reference_set_config)//check reference setting
        {
            reference_set_config = false;
            if(Integer.parseInt(et_quickset_spec_start.getText().toString())!=MINWAV)
                return false;
            if(Integer.parseInt(et_quickset_spec_end.getText().toString())!=MAXWAV)
                return false;
            if(model.equals("R11"))
            {
                if(config.getSectionScanType()[0]!= (byte)1)
                    return false;
                if(config.getSectionWidthPx()[0]!=(byte)9)
                    return false;
                if(config.getSectionNumPatterns()[0]!=160)
                    return false;
                if(config.getSectionNumRepeats()[0]!=12)
                    return false;
                if( config.getSectionExposureTime()[0]!=0)
                    return false;
            }
            else if(model.equals("R13"))
            {
                if(config.getSectionScanType()[0]!= (byte)1)
                    return false;
                if(config.getSectionWidthPx()[0]!=(byte)9)
                    return false;
                if(config.getSectionNumPatterns()[0]!=228)
                    return false;
                if(config.getSectionNumRepeats()[0]!=12)
                    return false;
                if( config.getSectionExposureTime()[0]!=1)
                    return false;
            }
            else if(model.equals("T11") || model.equals("F13"))
            {
                if(config.getSectionScanType()[0]!= (byte)0)
                    return false;
                if(config.getSectionWidthPx()[0]!=(byte)9)
                    return false;
                if(config.getSectionNumPatterns()[0]!=228)
                    return false;
                if(config.getSectionNumRepeats()[0]!=30)
                    return false;
                if( config.getSectionExposureTime()[0]!=0)
                    return false;
            }
            else
            {
                if(config.getSectionScanType()[0]!= (byte)0)
                    return false;
                if(config.getSectionWidthPx()[0]!=(byte)6)
                    return false;
                if(config.getSectionNumPatterns()[0]!=228)
                    return false;
                if(config.getSectionNumRepeats()[0]!=30)
                    return false;
                if( config.getSectionExposureTime()[0]!=0)
                    return false;
            }
        }
        else if(saveReference)//after save reference, should set active config and compare
        {
            if(config.getSectionScanType()[0]!= activeConf.getSectionScanType()[0])
                return false;
            if(config.getSectionWavelengthStartNm()[0]!=activeConf.getSectionWavelengthStartNm()[0])
                return false;
            if(config.getSectionWavelengthEndNm()[0]!=activeConf.getSectionWavelengthEndNm()[0])
                return false;
            if(activeConf.getSectionWidthPx()[0] != config.getSectionWidthPx()[0])
                return false;
            if(config.getSectionNumPatterns()[0]!=activeConf.getSectionNumPatterns()[0])
                return false;
            if(config.getSectionNumRepeats()[0]!=activeConf.getSectionNumRepeats()[0])
                return false;
            if( config.getSectionExposureTime()[0]!=activeConf.getSectionExposureTime()[0])
                return false;
        }
        else //check quickset setting
        {
            if(config.getSectionScanType()[0]!= spin_quickset_scan_method.getSelectedItemPosition())
                return false;
            if(config.getSectionWavelengthStartNm()[0]!=Integer.parseInt(et_quickset_spec_start.getText().toString()))
                return false;
            if(config.getSectionWavelengthEndNm()[0]!=Integer.parseInt(et_quickset_spec_end.getText().toString()))
                return false;
            if(spin_quickset_scan_width.getSelectedItemPosition()+2 != config.getSectionWidthPx()[0])
                return false;
            if(config.getSectionNumPatterns()[0]!=Integer.parseInt(et_quickset_res.getText().toString()))
                return false;
            if(config.getSectionNumRepeats()[0]!=Integer.parseInt(et_quickset_average_scan.getText().toString()))
                return false;
            if( config.getSectionExposureTime()[0]!=spin_quickset_exposure_time.getSelectedItemPosition())
                return false;
        }
        return true;
    }
    public void SetReferenceParameter()
    {
        reference_set_config = true;
        ISCNIRScanSDK.SetReferenceParameter(MINWAV,MAXWAV);
    }
    /**
     *Perform scan to the device
     * @param delaytime  set delay time tto avoid ble hang
     */
    private void PerformScan(long delaytime)
    {
        Handler handler = new Handler();
        handler.postDelayed(new Runnable(){

            @Override
            public void run() {
                //Send broadcast START_SCAN will trigger to scan data
                ISCNIRScanSDK.StartScan();
            }}, delaytime);
    }
    //region Receiver
    /**
     *  Success set Lamp state(ISCNIRScanSDK.LampState should be called)
     */
    public class ReturnSetLampReceiver extends BroadcastReceiver {
        public void onReceive(Context context, Intent intent) {
            //Complete set lamp on,off,auto
            switch (Lamp_Info)
            {
                case ManualLamp:
                    break;
                case WarmDevice:
                    Lamp_Info = LampInfo.ManualLamp;
                    Boolean reference = false;
                    if(getStringPref(mContext, ISCNIRScanSDK.SharedPreferencesKeys.ReferenceScan, "Not").equals("ReferenceScan"))
                        reference = true;
                    if(reference == true)
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
     *  Success set PGA( ISCNIRScanSDK.SetPGA should be called)
     */
    public class ReturnSetPGAReceiver extends BroadcastReceiver {
        public void onReceive(Context context, Intent intent) {
            //Complete set pga
        }
    }
    /**
     *  Success set Scan Repeats( ISCNIRScanSDK.setScanAverage should be called)
     */
    public class ReturnSetScanRepeatsReceiver extends BroadcastReceiver {
        public void onReceive(Context context, Intent intent) {
            //Complete set scan repeats
        }
    }
    //endregion
}
