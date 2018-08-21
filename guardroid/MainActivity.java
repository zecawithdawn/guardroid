package me.rescue.hanyang.guardroid;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;

import java.util.Timer;
import java.util.TimerTask;


public class MainActivity extends Activity implements SensorEventListener, View.OnClickListener {
    int accelXValue;
    int accelYValue;
    int accelZValue;

    int gyroX;
    int gyroY;
    int gyroZ;

    private SensorManager mSensorManager;
    private Sensor mGyroscope;
    private Sensor accSensor;

    Context mContext;
    EditText smsNumber;
    String smsTextContext;
    Button change_button, start_button;
    double lat, lng;
    int flag = 1;

    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //센서 매니저 얻기
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        //자이로스코프 센서(회전)
        mGyroscope = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        //엑셀러로미터 센서(가속)
        accSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        mContext = this;

        //초기화
        initialize();

        // gps정보 받아오기
        get_gps_info();


        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
    }

    //정확도에 대한 메소드 호출 (사용안함)
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    //센서값 얻어오기
    public void onSensorChanged(SensorEvent event) {
        Sensor sensor = event.sensor;

        if (sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            gyroX = Math.round(event.values[0] * 1000);
            gyroY = Math.round(event.values[1] * 1000);
            gyroZ = Math.round(event.values[2] * 1000);

        }
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            accelXValue = (int) event.values[0];
            accelYValue = (int) event.values[1];
            accelZValue = (int) event.values[2];

        }

        if (flag == 1) {
            if (Math.abs((gyroX + gyroY + gyroZ) * (accelXValue + accelYValue + accelZValue)) / 200000 > 1) {
                flag = 0;
                message_dialog();
            }
        }

    }

    // 주기 설명
    // SENSOR_DELAY_UI 갱신에 필요한 정도 주기
    // SENSOR_DELAY_NORMAL 화면 방향 전환 등의 일상적인  주기
    // SENSOR_DELAY_GAME 게임에 적합한 주기
    // SENSOR_DELAY_FASTEST 최대한의 빠른 주기

    //리스너 등록
    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, mGyroscope, SensorManager.SENSOR_DELAY_FASTEST);
        mSensorManager.registerListener(this, accSensor, SensorManager.SENSOR_DELAY_FASTEST);
    }

    //리스너 해제
    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
    }


    private void message_dialog() {


        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("긴급상황 확인창");
        builder.setMessage("긴급상황이 아닌경우 종료 버튼을 눌러주세요");
        builder.setCancelable(true);
        builder.setPositiveButton("종료", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                flag = 1;
            }
        });

        final AlertDialog dlg = builder.create();

        dlg.show();

        final Timer t = new Timer();
        t.schedule(new TimerTask() {
            public void run() {
                dlg.dismiss(); // when the task active then close the dialog
                sendSMS(String.valueOf(smsNumber.getText()), smsTextContext);
                flag = 1;
            }
        }, 30000);


    }


    private void initialize() {
        smsTextContext = "핸드폰사용자에게 위험상황이 인지 되었습니다. 현재의 위치는 ";
        smsNumber = (EditText) findViewById(R.id.emergency_number);
        change_button = (Button) findViewById(R.id.change_button);
        start_button = (Button) findViewById(R.id.start_button);
        change_button.setOnClickListener(this);
        start_button.setOnClickListener(this);

        SharedPreferences phone_number = getSharedPreferences("phone", MODE_PRIVATE);
        if (phone_number.getBoolean("save", false)) {
            smsNumber.setText(phone_number.getString("phone", ""));
        }
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.change_button) {
            if (smsNumber.getText().equals("")) {
                Toast.makeText(MainActivity.this, "전화번호를 입력해 주세요", Toast.LENGTH_SHORT).show();
            } else {
                if (isnum(String.valueOf(smsNumber.getText()))) {
                    SharedPreferences phone_number = getSharedPreferences("phone", MODE_PRIVATE);
                    SharedPreferences.Editor edit = phone_number.edit();

                    edit.putBoolean("save", true);
                    edit.putString("phone", String.valueOf(smsNumber.getText()));
                    edit.apply();

                } else {
                    Toast.makeText(MainActivity.this, "잘못된형식입니다. - 나 빈칸없이 숫자만 써주세요 ", Toast.LENGTH_SHORT).show();
                }
            }
        } else if (v.getId() == R.id.start_button) {
            if (!smsTextContext.equals("")) {
                //백그라운드로 전환
                Toast.makeText(MainActivity.this, "앱이 실행됩니다 ", Toast.LENGTH_SHORT).show();

            } else {
                Toast.makeText(MainActivity.this, "긴급연락처를 입력해주세요 ", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private boolean isnum(String phone_number) {
        char[] chars = phone_number.toCharArray();

        for (char c : chars) {
            if (!Character.isDigit(c)) {
                return false;
            }
        }
        return true;
    }

    public void get_gps_info() {

        // Acquire a reference to the system Location Manager
        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        // GPS 프로바이더 사용가능여부
        boolean isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);

        // 네트워크 프로바이더 사용가능여부
        boolean isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

        //위에꺼 확인용
        Log.d("Main", "isGPSEnabled=" + isGPSEnabled);

        if (!isGPSEnabled) {
            Toast.makeText(MainActivity.this, "GPS를 켜주세요", Toast.LENGTH_LONG).show();
        }

        Log.d("Main", "isNetworkEnabled=" + isNetworkEnabled);

        LocationListener locationListener = new LocationListener() {
            //성공
            public void onLocationChanged(Location location) {
                lat = location.getLatitude();
                lng = location.getLongitude();
            }

            //예외처리들
            public void onStatusChanged(String provider, int status, Bundle extras) {
                Log.d("error", "onStatusChanged");
            }

            //gps켜진경우
            public void onProviderEnabled(String provider) {
                Toast.makeText(MainActivity.this, "gps가 켜져있습니다.", Toast.LENGTH_SHORT).show();
            }

            //gps꺼진경우
            public void onProviderDisabled(String provider) {
                Toast.makeText(MainActivity.this, "Gps를 켜주세요", Toast.LENGTH_SHORT).show();
            }
        };

        // Register the listener with the Location Manager to receive location updates
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);

        // 수동으로 위치 구하기
        String locationProvider = LocationManager.GPS_PROVIDER;
        Location lastKnownLocation = locationManager.getLastKnownLocation(locationProvider);
        if (lastKnownLocation != null) {
            lng = lastKnownLocation.getLatitude();
            lat = lastKnownLocation.getLatitude();
        }
    }


    public void sendSMS(String smsNumber, String smsText) {
        PendingIntent sentIntent = PendingIntent.getBroadcast(this, 0, new Intent("SMS_SENT_ACTION"), 0);
        PendingIntent deliveredIntent = PendingIntent.getBroadcast(this, 0, new Intent("SMS_DELIVERED_ACTION"), 0);

        registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                switch (getResultCode()) {
                    case Activity.RESULT_OK:
                        // 전송성공
                        Toast.makeText(mContext, "긴급상황 전송 완료", Toast.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                        // 전송 실패
                        Toast.makeText(mContext, "전송 실패", Toast.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_NO_SERVICE:
                        // 서비스 지역 아님
                        Toast.makeText(mContext, "서비스 지역이 아닙니다", Toast.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_RADIO_OFF:
                        // 무선꺼짐¸
                        Toast.makeText(mContext, "무선(Radio) 꺼져있습니다", Toast.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_NULL_PDU:
                        // PDU 실패
                        Toast.makeText(mContext, "PDU Null", Toast.LENGTH_SHORT).show();
                        break;
                }
            }
        }, new IntentFilter("SMS_SENT_ACTION"));

        registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                switch (getResultCode()) {
                    case Activity.RESULT_OK:
                        // 도착 완료
                        Toast.makeText(mContext, "SMS 도착 완료", Toast.LENGTH_SHORT).show();
                        break;
                    case Activity.RESULT_CANCELED:
                        // 도착 안됨
                        Toast.makeText(mContext, "SMS 도착 실패", Toast.LENGTH_SHORT).show();
                        break;
                }
            }
        }, new IntentFilter("SMS_DELIVERED_ACTION"));

        //긴급상황 문장에 현재위치를 붙여서 전송
        String temp;
        temp = smsText + "현재 위치는 경도" + lng + " 위도" + lat + "입니다.";

        //이거 두줄이 실제로 메세지 보낸는 곳 위에는 예외처리용 받고 싶지않으면 둘다 null 로 바꿔주면 된다
        SmsManager mSmsManager = SmsManager.getDefault();
        mSmsManager.sendTextMessage(smsNumber, null, temp, sentIntent, deliveredIntent);
    }

    @Override
    public void onStart() {
        super.onStart();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client.connect();
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "Main Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app deep link URI is correct.
                Uri.parse("android-app://me.rescue.hanyang.guardroid/http/host/path")
        );
        AppIndex.AppIndexApi.start(client, viewAction);
    }

    @Override
    public void onStop() {
        super.onStop();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "Main Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app deep link URI is correct.
                Uri.parse("android-app://me.rescue.hanyang.guardroid/http/host/path")
        );
        AppIndex.AppIndexApi.end(client, viewAction);
        client.disconnect();
    }
}