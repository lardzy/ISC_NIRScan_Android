package com.Innospectra.NanoScan;


import android.Manifest;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.text.InputType;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.ISCSDK.ISCNIRScanSDK;

import static com.ISCSDK.ISCNIRScanSDK.getStringPref;
import static com.ISCSDK.ISCNIRScanSDK.storeStringPref;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * This activity controls the view for global settings. These settings do not require a Nano
 * to be connected.
 *
 * The user can change temperature and spatial frequency units, as well as set and clear a
 * preferred Nano device
 *
 * @author collinmast
 */
public class SettingsViewActivity extends Activity {

    private TextView tv_version;
    private Button btn_set;
    private Button btn_forget;
    private Button btn_management;
    private Button btn_set_api_url, btn_set_api_key;
    private AlertDialog alertDialog;
    private TextView tv_pref_nano;
    private EditText et_devicefilter;
    private String preferredNano;
    private static Context mContext;
    private Switch switchForAdministrator;
    private String API_URL, API_KEY;
    private boolean isPasswordDialogShown = false;
    private Button btn_packed_file;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        mContext = this;
        InitComponent();
        //Set up action bar up indicator
        ActionBar ab = getActionBar();
        if (ab != null) {
            ab.setDisplayHomeAsUpEnabled(true);
        }
    }
    private void InitComponent()
    {
        tv_version = (TextView) findViewById(R.id.tv_version);
        btn_set = (Button) findViewById(R.id.btn_set);
        btn_forget = (Button) findViewById(R.id.btn_forget);
        btn_management = (Button) findViewById(R.id.btn_management);
        btn_set_api_url = (Button) findViewById(R.id.btn_set_api_url);
        btn_set_api_key = (Button) findViewById(R.id.btn_set_api_key);
        tv_pref_nano = (TextView) findViewById(R.id.tv_pref_nano);
        et_devicefilter = (EditText)findViewById(R.id.et_devicefilter);
        String devicename = ISCNIRScanSDK.getStringPref(mContext,ISCNIRScanSDK.SharedPreferencesKeys.DeviceFilter,"NIR");
        et_devicefilter.setText(devicename);
        et_devicefilter.setOnEditorActionListener(Device_Filter_OnEditor);
        btn_set_api_url.setOnClickListener(btn_set_api_url_OnClickListener);
        btn_set_api_key.setOnClickListener(btn_set_key_OnClickListener);
        switchForAdministrator = (Switch)findViewById(R.id.switchForAdministrator);
        btn_packed_file = (Button)findViewById(R.id.btn_packed_file);
        btn_packed_file.setOnClickListener(btn_packed_file_OnClickListener);
        // 载入管理员状态
        loadAdministratorStatus();

        switchForAdministrator.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // If the switch is being turned ON and password dialog hasn't been shown yet
                if (isChecked && !isPasswordDialogShown) {
                    showPasswordDialog();
                }
            }
        });
    }
    private View.OnClickListener btn_packed_file_OnClickListener = new View.OnClickListener()
    {
        @Override
        public void onClick(View view) {
            new AlertDialog.Builder(SettingsViewActivity.this)
                    .setTitle("提示")
                    .setMessage("是否导出？")
                    .setPositiveButton("是", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            exportFiles();
                        }
                    })
                    .setNegativeButton("否", null).show();
        }
    };
    private void exportFiles(){
        // 步骤1：创建一个位于Documents/ISC_Report/日期的文件夹
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String dateFolderName = sdf.format(new Date());
        File mainDirectory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "ISC_Report");
        File dateDirectory = new File(mainDirectory, dateFolderName);
        if (!dateDirectory.exists()) {
            dateDirectory.mkdirs();
        }

        // 步骤2：将Documents/ISC_Report/下的所有csv文件移动到Documents/ISC_Report/日期下
        File[] files = mainDirectory.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".csv");
            }
        });

        if (files != null) {
            for (File file : files) {
                file.renameTo(new File(dateDirectory, file.getName()));
            }
        }
        Toast.makeText(SettingsViewActivity.this, "成功导出文件数量：" + files.length, Toast.LENGTH_SHORT).show();
        // 步骤3：将Documents/ISC_Report/日期下的所有文件压缩成zip文件
        File zipFile = new File(mainDirectory, dateFolderName + ".zip");
        try {
            zipDirectory(dateDirectory, zipFile);
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(SettingsViewActivity.this, "导出失败", Toast.LENGTH_SHORT).show();
            return;
        }

        // 步骤4：分享Documents/ISC_Report/日期.zip
        shareFile(zipFile);
//        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
//        intent.setDataAndType(Uri.parse(mainDirectory.getAbsolutePath()), "*/*");
//        startActivity(Intent.createChooser(intent, "Open folder"));
    }
    private void shareFile(File file) {
        Uri fileUri = android.support.v4.content.FileProvider.getUriForFile(SettingsViewActivity.this, "com.Innospectra.NanoScan.file-provider", file);
        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
        shareIntent.setType("application/zip");
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(shareIntent, "分享文件"));
    }
    private void zipDirectory(File directory, File zipFile) throws IOException {
        FileOutputStream fos = new FileOutputStream(zipFile);
        ZipOutputStream zos = new ZipOutputStream(fos);
        File[] files = directory.listFiles();
        for (File file : files) {
            FileInputStream fis = new FileInputStream(file);
            ZipEntry zipEntry = new ZipEntry(file.getName());
            zos.putNextEntry(zipEntry);
            byte[] bytes = new byte[1024];
            int length;
            while ((length = fis.read(bytes)) >= 0) {
                zos.write(bytes, 0, length);
            }
            fis.close();
        }
        zos.closeEntry();
        zos.close();
        fos.close();
    }
    private void showPasswordDialog() {
        isPasswordDialogShown = true;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("管理员密码");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        builder.setView(input);

        builder.setPositiveButton("确认", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (input.getText().toString().equals("admin")) {
                    // Password is correct, do nothing
                } else {
                    switchForAdministrator.setChecked(false);  // Incorrect password, revert switch state
                    Toast.makeText(SettingsViewActivity.this, "密码错误!", Toast.LENGTH_SHORT).show();
                }
                isPasswordDialogShown = false;
            }
        });
        builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switchForAdministrator.setChecked(false);  // Revert switch state on cancel
                isPasswordDialogShown = false;
                dialog.cancel();
            }
        });

        builder.show();
    }
    private EditText.OnEditorActionListener Device_Filter_OnEditor = new EditText.OnEditorActionListener()
    {
        @Override
        public boolean onEditorAction(TextView textView, int actionId, KeyEvent event) {
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                    actionId == EditorInfo.IME_ACTION_DONE ||
                    event.getAction() == KeyEvent.ACTION_DOWN &&
                            event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                ISCNIRScanSDK.storeStringPref(mContext,ISCNIRScanSDK.SharedPreferencesKeys.DeviceFilter,et_devicefilter.getText().toString());
            }
            return false;
        }
    };
    private View.OnClickListener btn_set_api_url_OnClickListener = new View.OnClickListener()
    {
        @Override
        public void onClick(View view) {
            AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
            builder.setTitle("API URL");
            EditText input = new EditText(mContext);
            input.setText(API_URL);
            builder.setView(input);
            builder.setPositiveButton("确定", (dialog, which) -> {
                String component = input.getText().toString();
                if (component.equals("")) {
                    return;
                }
                API_URL = component;
            });
            builder.setNegativeButton("取消", (dialog, which) -> dialog.cancel());
            builder.show();
        }
    };
    private View.OnClickListener btn_set_key_OnClickListener = new View.OnClickListener()
    {
        @Override
        public void onClick(View view) {
            AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
            builder.setTitle("API KEY");
            EditText input = new EditText(mContext);
            input.setText(API_KEY);
            builder.setView(input);
            builder.setPositiveButton("确定", (dialog, which) -> {
                String component = input.getText().toString();
                if (component.equals("")) {
                    return;
                }
                API_KEY = component;
            });
            builder.setNegativeButton("取消", (dialog, which) -> dialog.cancel());
            builder.show();
        }
    };

    @Override
    protected void onStart() {
        super.onStart();
        loadAdministratorStatus();
    }

    @Override
    public void onResume() {
        super.onResume();
        //Initialize preferred device
        preferredNano = getStringPref(mContext, ISCNIRScanSDK.SharedPreferencesKeys.preferredDevice, null);
        //Retrieve package information for displaying version info
        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            String version = pInfo.versionName;
            int versionCode = pInfo.versionCode;
            tv_version.setText(getString(R.string.version, version, versionCode));
        } catch (PackageManager.NameNotFoundException e) {
            tv_version.setText("");
        }

        btn_set.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(CheckPermission())
                    startActivity(new Intent(mContext, SelectDeviceViewActivity.class));
            }
        });
        tv_pref_nano.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(CheckPermission())
                    startActivity(new Intent(mContext, SelectDeviceViewActivity.class));
            }
        });
        btn_management.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(CheckPermission())
                    startActivity(new Intent(mContext, fabricManagementActivity.class));
            }
        });

        if(preferredNano == null){
            btn_forget.setEnabled(false);
        }else{
            btn_forget.setEnabled(true);
        }
        btn_forget.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (preferredNano != null) {
                    confirmationDialog(preferredNano);
                }
            }
        });
        //Update set button and field based on whether a preferred nano has been set or not
        if (preferredNano != null) {
            btn_set.setVisibility(View.INVISIBLE);
            tv_pref_nano.setText(getStringPref(mContext, ISCNIRScanSDK.SharedPreferencesKeys.preferredDeviceModel, null));
            tv_pref_nano.setVisibility(View.VISIBLE);
        } else {
            btn_set.setVisibility(View.VISIBLE);
            tv_pref_nano.setVisibility(View.INVISIBLE);
        }
    }
//    private void showEditDialog(int position) {
//        AlertDialog.Builder builder = new AlertDialog.Builder(this);
//        builder.setTitle("编辑成分");
//        EditText input = new EditText(this);
//        input.setText(data.get(position));
//        builder.setView(input);
//        builder.setPositiveButton("更新", (dialog, which) -> {
//            String component = input.getText().toString();
//            if (component.equals("")) {
//                return;
//            }
//        });
//        builder.setNeutralButton("取消", (dialog, which) -> dialog.cancel());
//        builder.show();
//    }
    @Override
    protected void onPause() {
        super.onPause();
        saveSettingsViewStatus();
    }
    private void saveSettingsViewStatus()
    {
        SharedPreferences sharedPreferences = getSharedPreferences("settingsViewStatus", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("administratorStatus", switchForAdministrator.isChecked());
        editor.putString("API_URL", API_URL);
        editor.putString("API_KEY", API_KEY);
        editor.apply();
    }
    private void loadAdministratorStatus()
    {
        SharedPreferences sharedPreferences = getSharedPreferences("settingsViewStatus", Context.MODE_PRIVATE);
        switchForAdministrator.setChecked(sharedPreferences.getBoolean("administratorStatus", true));
        API_URL = sharedPreferences.getString("API_URL", "http://192.168.115.230:8000/predict/");
        API_KEY = sharedPreferences.getString("API_KEY", "your-secret-api-key");
    }
    /**
     * When the activity is destroyed, make a call to super class
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
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
     * In this case, there is are two items, the up indicator, and the settings button
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        if (id == android.R.id.home) {
            this.finish();
        }

        else if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Function for displaying the dialog to confirm clearing the stored Nano
     * @param mac the mac address of the stored Nano
     */
    public void confirmationDialog(String mac) {

        final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(mContext);
        alertDialogBuilder.setTitle(mContext.getResources().getString(R.string.nano_confirmation_title));
        alertDialogBuilder.setMessage(mContext.getResources().getString(R.string.nano_forget_msg, mac));

        alertDialogBuilder.setPositiveButton(getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface arg0, int arg1) {
                alertDialog.dismiss();
                storeStringPref(mContext, ISCNIRScanSDK.SharedPreferencesKeys.preferredDevice, null);
                storeStringPref(mContext, ISCNIRScanSDK.SharedPreferencesKeys.preferredDeviceModel, null);
                btn_set.setVisibility(View.VISIBLE);
                tv_pref_nano.setVisibility(View.INVISIBLE);
                btn_forget.setEnabled(false);
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
    private Boolean CheckPermission()
    {
        if( android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
        {
            Boolean hasPermission1 = (ContextCompat.checkSelfPermission(getBaseContext(),
                    Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED);
            Boolean hasPermission2 = (ContextCompat.checkSelfPermission(getBaseContext(),
                    Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED);
            if(!hasPermission1 || !hasPermission2)
            {
                Dialog_Pane("Warning","Will go to the application information page.\nShould allow nearby devices permission.");
                return false;
            }
        }
        return true;
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
                Intent localIntent = new Intent();
                localIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                if (Build.VERSION.SDK_INT >= 9) {
                    localIntent.setAction("android.settings.APPLICATION_DETAILS_SETTINGS");
                    localIntent.setData(Uri.fromParts("package", SettingsViewActivity.this.getPackageName(), null));
                } else if (Build.VERSION.SDK_INT <= 8) {
                    localIntent.setAction(Intent.ACTION_VIEW);
                    localIntent.setClassName("com.android.settings", "com.android.settings.InstalledAppDetails");
                    localIntent.putExtra("com.android.settings.ApplicationPkgName", SettingsViewActivity.this.getPackageName());
                }
                startActivity(localIntent);
                alertDialog.dismiss();
            }
        });
        alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }
}
