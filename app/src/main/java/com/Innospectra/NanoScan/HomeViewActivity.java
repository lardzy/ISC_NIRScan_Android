package com.Innospectra.NanoScan;

import static com.ISCSDK.ISCNIRScanSDK.getStringPref;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Switch;
import android.widget.TextView;

import com.ISCSDK.ISCNIRScanSDK;

/**
 * Created by iris.lin on 2018/2/2.
 */

public class HomeViewActivity extends Activity {
    private static Context mContext;
    private ImageButton main_connect;
    private ImageButton main_info;
    private ImageButton main_setting;
    private ImageButton not_warmup;
    private ImageButton warmup;
    private View rootView;
    private boolean isWarmup = false;
    private boolean isAdministrator = false;
    private TextView isAdminMode;
//    private Switch switch_Warmup;
    private static final int REQUEST_WRITE_STORAGE = 112;
    AlertDialog alertDialog;
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_page);
        mContext = this;
        initComponent();

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            boolean hasPermission = (ContextCompat.checkSelfPermission(getBaseContext(),
                    Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED);
            boolean hasPermission1 = (ContextCompat.checkSelfPermission(getBaseContext(),
                    Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED);
            if(!hasPermission || !hasPermission1)
                DialogPane_LocationPermission();
            else
            {
                boolean hasPermission2 = (ContextCompat.checkSelfPermission(getBaseContext(),
                        Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
                if (!hasPermission2) {
                    ActivityCompat.requestPermissions(HomeViewActivity.this,
                            new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                    Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN, Manifest.permission.INTERNET
                            },
                            REQUEST_WRITE_STORAGE);
                }
            }
        }
        else if(android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
        {
            boolean hasPermission = (ContextCompat.checkSelfPermission(getBaseContext(),
                    Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
            if (!hasPermission) {
                ActivityCompat.requestPermissions(HomeViewActivity.this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.INTERNET,
                        },
                        REQUEST_WRITE_STORAGE);
            }
        }
    }
    private void CheckPermission()
    {
        boolean hasPermission1;
        boolean hasPermission2;
        boolean hasPermission3;
        boolean hasPermission4;
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            hasPermission1 = (ContextCompat.checkSelfPermission(getBaseContext(),
                    Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
            hasPermission2 = (ContextCompat.checkSelfPermission(getBaseContext(),
                    Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
            hasPermission3 = (ContextCompat.checkSelfPermission(getBaseContext(),
                    Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED);
            hasPermission4 = (ContextCompat.checkSelfPermission(getBaseContext(),
                    Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED);

            if(!hasPermission1 || !hasPermission2 || !hasPermission3 || !hasPermission4)
            {
               Dialog_Pane("警告","将转到申请信息页面。\n需要位置和存储权限。");
            }
            else
            {
                String DeviceName = getStringPref(mContext, ISCNIRScanSDK.SharedPreferencesKeys.preferredDevice, null);
                if(DeviceName == null || TextUtils.isEmpty(DeviceName))
                    Dialog_Pane_GoToSettingPage("警告","尚未选择设备，将自动转到设置页面。");
                else
                {
                    Intent newscanhIntent = new Intent(mContext, ScanViewActivity.class);
                    Intent newScanUserIntent = new Intent(mContext, ScanViewActivityForUsers.class);
                    newScanUserIntent.putExtra("main","main");
                    newScanUserIntent.putExtra("warmup",isWarmup);
                    newscanhIntent.putExtra("main","main");
                    newscanhIntent.putExtra("warmup",isWarmup);
                    animation();
                    if (isAdministrator){
                        startActivity(newscanhIntent);
                    }else {
                        startActivity(newScanUserIntent);
                    }
                }
            }
        }
        else if( android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
        {
            hasPermission1 = (ContextCompat.checkSelfPermission(getBaseContext(),
                    Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED);
            hasPermission2 = (ContextCompat.checkSelfPermission(getBaseContext(),
                    Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED);
            if(!hasPermission1 || !hasPermission2)
            {
                Dialog_Pane("警告","将转到申请信息页面。\n需要查看附近设备的权限.");
            }
            else
            {
                String DeviceName = getStringPref(mContext, ISCNIRScanSDK.SharedPreferencesKeys.preferredDevice, null);
                if(DeviceName == null || TextUtils.isEmpty(DeviceName))
                    Dialog_Pane_GoToSettingPage("警告","尚未选择设备，将自动转到设置页面。");
                else
                {
                    Intent newscanhIntent = new Intent(mContext, ScanViewActivity.class);
                    Intent newScanUserIntent = new Intent(mContext, ScanViewActivityForUsers.class);
                    newScanUserIntent.putExtra("main","main");
                    newScanUserIntent.putExtra("warmup",isWarmup);
                    newscanhIntent.putExtra("main","main");
                    newscanhIntent.putExtra("warmup",isWarmup);
                    animation();
                    if (isAdministrator){
                        startActivity(newscanhIntent);
                    }else {
                        startActivity(newScanUserIntent);
                    }
                }
            }
        }
    }
    private void animation(){
        int centerX = (rootView.getLeft() + rootView.getRight()) / 2;

        int centerY = (rootView.getTop() + rootView.getBottom()) / 3;

        float finalRadius = (float) Math.hypot((double) centerX, (double) (rootView.getTop() + rootView.getBottom())/1.5);

        Animator mCircularReveal = ViewAnimationUtils.createCircularReveal(

                rootView, centerX, centerY, 0, finalRadius);
        mCircularReveal.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                // 设置开启动画时的背景色
                rootView.setBackgroundColor(Color.argb(100,255,242,239));
//                rootView.setBackgroundColor(Color.RED);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                // 设置结束动画时的背景色
                rootView.setBackgroundColor(Color.TRANSPARENT);
            }
        });

        mCircularReveal.setDuration(400).start();
    }
    private void initComponent()
    {
        main_connect = (ImageButton)findViewById(R.id.main_connect);
        main_info = (ImageButton)findViewById(R.id.main_info);
        main_setting = (ImageButton)findViewById(R.id.main_setting);
        not_warmup = (ImageButton)findViewById(R.id.not_warmup);
        warmup = (ImageButton)findViewById(R.id.warmup);
        isAdminMode = (TextView)findViewById(R.id.isAdminMode);
        rootView = findViewById(R.id.rootView);

        main_connect.setOnClickListener(main_connect_listenser);
        main_info.setOnClickListener(main_info_listenser);
        main_setting.setOnClickListener(main_setting_listenser);
        not_warmup.setOnClickListener(not_warmup_listenser);
        warmup.setOnClickListener(warmup_listenser);

    }

    @Override
    protected void onStart() {
        super.onStart();
        loadAdministratorStatus();
    }

    private void loadAdministratorStatus(){
        SharedPreferences sharedPreferences = getSharedPreferences("settingsViewStatus", Context.MODE_PRIVATE);
        isAdministrator = sharedPreferences.getBoolean("administratorStatus", true);
        if (isAdministrator){
            isAdminMode.setText("管理员模式");
        }else {
            isAdminMode.setText("");
        }
    }

    private ImageButton.OnClickListener not_warmup_listenser = new Button.OnClickListener()
    {
        @Override
        public void onClick(View view) {
            if (isWarmup){
                not_warmup.setImageResource(R.drawable.not_warmup_checked);
                warmup.setImageResource(R.drawable.warmup_unchecked);
                isWarmup = false;
            }
        }
    };
    private ImageButton.OnClickListener warmup_listenser = new Button.OnClickListener()
    {
        @Override
        public void onClick(View view) {
            if (!isWarmup){
                warmup.setImageResource(R.drawable.warmup_checked);
                not_warmup.setImageResource(R.drawable.not_warmup_unchecked);
                isWarmup = true;
            }
        }
    };
    // 扫描按钮监听器
    private Button.OnClickListener main_connect_listenser = new Button.OnClickListener()
    {

        @Override
        public void onClick(View view) {
            CheckPermission();
        }
    };
    private Button.OnClickListener main_info_listenser = new Button.OnClickListener()
    {

        @Override
        public void onClick(View view) {
            Intent infoIntent = new Intent(mContext, InformationViewActivity.class);
            startActivity(infoIntent);
        }
    };

    private Button.OnClickListener main_setting_listenser = new Button.OnClickListener()
    {

        @Override
        public void onClick(View view) {
            Intent settingsIntent = new Intent(mContext, SettingsViewActivity.class);
            startActivity(settingsIntent);
        }
    };

    public static StoreCalibration storeCalibration = new StoreCalibration();
    public static class StoreCalibration
    {
        String device;
        byte[] storrefCoeff;
        byte[] storerefMatrix;
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
                    localIntent.setData(Uri.fromParts("package", HomeViewActivity.this.getPackageName(), null));
                } else if (Build.VERSION.SDK_INT <= 8) {
                    localIntent.setAction(Intent.ACTION_VIEW);
                    localIntent.setClassName("com.android.settings", "com.android.settings.InstalledAppDetails");
                    localIntent.putExtra("com.android.settings.ApplicationPkgName", HomeViewActivity.this.getPackageName());
                }
                startActivity(localIntent);
                alertDialog.dismiss();
            }
        });
        alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }
    private void Dialog_Pane_GoToSettingPage(String title,String content)
    {
        final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(mContext);
        alertDialogBuilder.setTitle(title);
        alertDialogBuilder.setCancelable(false);
        alertDialogBuilder.setMessage(content);
        alertDialogBuilder.setPositiveButton(getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface arg0, int arg1) {
                alertDialog.dismiss();
                Intent settingsIntent = new Intent(mContext, SettingsViewActivity.class);
                startActivity(settingsIntent);
            }
        });
        alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }
    private void DialogPane_AllowPermission(String Title, String Content) {
        final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(mContext);
        alertDialogBuilder.setTitle(mContext.getResources().getString(R.string.not_connected_title));
        alertDialogBuilder.setCancelable(false);
        alertDialogBuilder.setTitle(Title);
        alertDialogBuilder.setMessage(Content);
        alertDialogBuilder.setPositiveButton(getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface arg0, int arg1) {
                alertDialog.dismiss();
                Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                startActivity(intent);
            }
        });

        alertDialog = alertDialogBuilder.create();
        alertDialog.show();

    }
    private void DialogPane_LocationPermission()
    {
        final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(mContext);
        alertDialogBuilder.setTitle("权限许可");
        alertDialogBuilder.setCancelable(false);
        alertDialogBuilder.setMessage("将转到申请信息页面，需要查看附近设备的权限。");

        alertDialogBuilder.setPositiveButton("接受", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface arg0, int arg1) {
                boolean hasPermission = (ContextCompat.checkSelfPermission(getBaseContext(),
                        Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED);
                boolean hasPermission1 = (ContextCompat.checkSelfPermission(getBaseContext(),
                        Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED);
                if (!hasPermission || !hasPermission1) {
                    ActivityCompat.requestPermissions(HomeViewActivity.this,
                            new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                    Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN, Manifest.permission.INTERNET, Manifest.permission.ACCESS_COARSE_LOCATION
                                    , Manifest.permission.ACCESS_FINE_LOCATION
                            },
                            REQUEST_WRITE_STORAGE);
                }
                alertDialog.dismiss();
            }
        });
        alertDialogBuilder.setNegativeButton("拒绝", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface arg0, int arg1) {
                boolean hasPermission = (ContextCompat.checkSelfPermission(getBaseContext(),
                        Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
                if (!hasPermission) {
                    ActivityCompat.requestPermissions(HomeViewActivity.this,
                            new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                    Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN, Manifest.permission.INTERNET
                            },
                            REQUEST_WRITE_STORAGE);
                }
                alertDialog.dismiss();
            }
        });
        alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }
}
