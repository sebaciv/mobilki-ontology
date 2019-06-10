package pl.edu.agh.ki.mob.onto;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.lang.ref.WeakReference;
import java.util.Optional;

import pl.edu.agh.ki.mob.onto.fall.AccelerometerFallService;

public class MainActivity extends AppCompatActivity implements AccelerometerFallService.FallDetectedCallback {

    private static final int MY_PERMISSIONS_REQUEST_LOCATION = 123;

    private LocationManager locationManager;
    private LocationProvider locationProvider;
    private MyLocationListener locationListener;
    private FallHandler fallHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        fallHandler = new FallHandler(this);

        new Thread(new Runnable() {
            @Override
            public void run() {
                ClassHierarchy.show(getResources());
            }
        }).start();

        Button simulateButton = findViewById(R.id.button_simulate);
        simulateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startReasoning();
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) !=
                        PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    MY_PERMISSIONS_REQUEST_LOCATION);
        } else {
            initLocation();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_LOCATION: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    initLocation();
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                break;
            }
        }
    }

    @SuppressLint("MissingPermission")
    private void initLocation() {
        this.locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        this.locationProvider = this.locationManager.getProvider(LocationManager.GPS_PROVIDER);

        if (locationProvider != null) {
            locationListener = new MyLocationListener();
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 10, locationListener);
        } else {
            Toast.makeText(this,
                    "Location Provider is not available at the moment!",
                    Toast.LENGTH_SHORT
            ).show();
        }

        Intent intent = new Intent(getApplicationContext(), AccelerometerFallService.class);
        startService(intent);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            Toast.makeText(MainActivity.this, "Detection service connected", Toast.LENGTH_SHORT).show();

            AccelerometerFallService.LocalBinder binder = (AccelerometerFallService.LocalBinder) service;
            AccelerometerFallService fallService = binder.getServiceInstance();
            fallService.registerClient(MainActivity.this);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            Toast.makeText(MainActivity.this, "Detection service disconnected", Toast.LENGTH_SHORT).show();
        }
    };


    public class MyLocationListener implements LocationListener {
        private double lat;
        private double lon;
        private float speed; // km/h

        @Override
        public void onLocationChanged(Location location) {
            this.lat = location.getLatitude();
            this.lon = location.getLongitude();
            this.speed = location.getSpeed() * 1000 / 3600;
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }

        @Override
        public void onProviderEnabled(String provider) {

        }

        @Override
        public void onProviderDisabled(String provider) {

        }

        public double getLat() {
            return lat;
        }

        public double getLon() {
            return lon;
        }

        public float getSpeed() {
            return speed;
        }
    }

    private Optional<String> getCurrentSsid() {
        Optional<String> ssid = Optional.empty();
        ConnectivityManager connManager = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        if (networkInfo.isConnected()) {
            final WifiManager wifiManager = (WifiManager) this.getSystemService(Context.WIFI_SERVICE);
            final WifiInfo connectionInfo = wifiManager.getConnectionInfo();
            if (connectionInfo != null) {
                ssid = Optional.ofNullable(connectionInfo.getSSID());
            }
        }
        return ssid;
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void fallDetected() {
        startReasoning();
    }


    private static class FallHandler extends Handler {
        private final WeakReference<MainActivity> mActivity;
        FallHandler(MainActivity activity) {
            mActivity = new WeakReference<MainActivity>(activity);
        }
        @Override
        public void handleMessage(Message msg) {
            MainActivity activity = mActivity.get();


        }
    }

    private void startReasoning() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Optional<OntologyUtils.HumanStatus> result = Optional.empty();

                if (locationListener != null) {
                    double lat = locationListener.getLat();
                    double lon = locationListener.getLon();
                    double speed = locationListener.getSpeed();
                    Optional<Double> temperature = Optional.empty();

                    try {
                        temperature = WeatherUtils.getTemperature(lat, lon);
                    } catch (Exception e) {
                        Log.e("Weather", "", e);
                    }

                    result = OntologyUtils.classify(
                            getResources(),
                            guessLocation(),
                            OntologyUtils.MovementType.NO_MOVEMENT,
                            temperature,
                            Optional.of(speed)
                    );
                } else {
                    result = OntologyUtils.classify(
                            getResources(),
                            guessLocation(),
                            OntologyUtils.MovementType.NO_MOVEMENT,
                            Optional.empty(),
                            Optional.empty()
                    );
                }

                Message m = fallHandler.obtainMessage();
                Bundle b = new Bundle();
                result.ifPresent(value -> b.putString("result", value.name()));
                m.setData(b);
                fallHandler.sendMessage(m);
            }
        }).start();
    }

    private OntologyUtils.LocationType guessLocation() {
        SharedPreferences sharedPref = this.getPreferences(Context.MODE_PRIVATE);
        Optional<String> currentSsid = getCurrentSsid();
        if (currentSsid.map(ssid -> ssid.equals(sharedPref.getString(getString(R.string.my_home_wifi_ssid), ""))).orElse(false)) {
            return OntologyUtils.LocationType.HOME;
        } else {
            return OntologyUtils.LocationType.OUTSIDE;
        }
    }
}
