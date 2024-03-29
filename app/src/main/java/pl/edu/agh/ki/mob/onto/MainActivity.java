package pl.edu.agh.ki.mob.onto;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
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
import android.support.annotation.StringRes;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import pl.edu.agh.ki.mob.onto.fall.AccelerometerFallService;

public class MainActivity extends AppCompatActivity implements
        AccelerometerFallService.FallDetectedCallback,
        FallDialog.FallDialogListener,
        SharedPreferences.OnSharedPreferenceChangeListener
{

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
        setupSharedPreferences();

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

    private void setupSharedPreferences() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
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

        Intent intent = new Intent(getApplicationContext(), AccelerometerFallService.class);
        startService(intent);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
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

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

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

    @Override
    protected void onStop() {
        unbindService(mConnection);
        super.onStop();
    }

    public void showFallDialog(OntologyUtils.HumanStatus result) {
        // Create an instance of the dialog fragment and show it
        FallDialog dialog = new FallDialog();
        dialog.setHumanStatus(result);
        dialog.show(getSupportFragmentManager(), "FallDialogFragment");
    }

    @Override
    public void onDialogPositiveClick(DialogFragment dialog) {
        // future work: call someone, send sms
        System.out.println("Boo!");
    }

    @Override
    public void onDialogNegativeClick(DialogFragment dialog) {
        System.out.println("Nothing happened");
    }

    public class MyLocationListener implements LocationListener {
        private double lat;
        private double lon;
        private float speed; // km/h
        private boolean hasValue = false;

        @Override
        public void onLocationChanged(Location location) {
            this.hasValue = true;
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

        public SimpleLocation getLocation() {
            return new SimpleLocation(lat, lon);
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

        public boolean isHasValue() {
            return hasValue;
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
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void fallDetected() {
        startReasoning();
    }

    private static class GeoUtils {
        public static double distance(double lat1, double lon1, double lat2, double lon2) {
            double theta = lon1 - lon2;
            double dist = Math.sin(deg2rad(lat1)) * Math.sin(deg2rad(lat2)) + Math.cos(deg2rad(lat1)) * Math.cos(deg2rad(lat2)) * Math.cos(deg2rad(theta));
            dist = Math.acos(dist);
            dist = rad2deg(dist);
            dist = dist * 60 * 1.1515;
            dist = dist * 1.609344;
            return (dist);
        }

        private static double deg2rad(double deg) {
            return (deg * Math.PI / 180.0);
        }

        private static double rad2deg(double rad) {
            return (rad * 180.0 / Math.PI);
        }
    }

    private boolean isCloseToHome(String address, SimpleLocation location) {
        Geocoder geoCoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geoCoder.getFromLocationName(address, 5);
            if (addresses.size() > 0) {
                double lat = (addresses.get(0).getLatitude());
                double lon = (addresses.get(0).getLongitude());

                Log.d("lat-long", "" + lat + "......." + lon);
                double distance = GeoUtils.distance(lat, lon, location.getLat(), location.getLon());
                return distance < 0.05;
            } else {
                return false;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    private static class FallHandler extends Handler {
        private final WeakReference<MainActivity> mActivity;
        FallHandler(MainActivity activity) {
            mActivity = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            MainActivity activity = mActivity.get();
            String result = (String) msg.getData().get("result");
            if (result == null) {
                Toast.makeText(activity, "Cannot classify fall data!", Toast.LENGTH_SHORT).show();
            } else {
                OntologyUtils.HumanStatus status = OntologyUtils.HumanStatus.valueOf(result);
                switch (status) {
                    case STANDING:
                        Toast.makeText(activity, "Sudden phone movement detected", Toast.LENGTH_SHORT).show();
                        break;
                    case ACCIDENT:
                        Toast.makeText(activity, "Sudden phone movement detected while traveling by vehicle", Toast.LENGTH_SHORT).show();
                        break;
                    default:
                        activity.showFallDialog(status);
                }
            }
        }
    }

    private void startReasoning() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Optional<OntologyUtils.HumanStatus> result = Optional.empty();

                    if (locationListener != null && locationListener.isHasValue()) {
                        SimpleLocation location = locationListener.getLocation();
                        double speed = locationListener.getSpeed();
                        Optional<Double> temperature = Optional.empty();

                        try {
                            temperature = WeatherUtils.getTemperature(location);
                        } catch (Exception e) {
                            Log.e("Weather", "", e);
                        }

                        Double maxVelo = doGetPreference(R.string.max_velocity).map(s -> Double.parseDouble(s)).orElse(20.0);
                        Double maxTemp = doGetPreference(R.string.max_temperature).map(s -> Double.parseDouble(s)).orElse(30.0);

                        result = OntologyUtils.classify(
                                getResources(),
                                guessLocation(Optional.of(location)),
                                OntologyUtils.MovementType.NO_MOVEMENT,
                                temperature.map(value -> (value > maxTemp) ? OntologyUtils.TemperatureType.HIGH : OntologyUtils.TemperatureType.LOW),
                                Optional.of(speed).map(value -> (value > maxVelo) ? OntologyUtils.VelocityType.HIGH : OntologyUtils.VelocityType.LOW)
                        );
                    } else {
                        result = OntologyUtils.classify(
                                getResources(),
                                guessLocation(Optional.empty()),
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
                } catch (Exception e) {
                    e.printStackTrace();
                    System.out.println("Reasoning failed");
                }
            }
        }).start();
    }

    private OntologyUtils.LocationType guessLocation(Optional<SimpleLocation> location) {
        Optional<String> currentSsid = getCurrentSsid();
        if (currentSsid.map(ssid -> ssid.equals(doGetPreference(R.string.my_home_wifi_ssid).orElse(""))).orElse(false)) {
            return OntologyUtils.LocationType.HOME;
        } else {
            return location.map(loc -> {
                Optional<String> address = doGetPreference(R.string.my_home_address);
                if (address.isPresent()) {
                    boolean isClose = isCloseToHome(address.get(), loc);
                    return isClose ? OntologyUtils.LocationType.HOME : OntologyUtils.LocationType.OUTSIDE;
                } else {
                    return OntologyUtils.LocationType.OUTSIDE;
                }
            }).orElse(OntologyUtils.LocationType.OUTSIDE);
        }
    }

    private Optional<String> doGetPreference(@StringRes int resId) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        return Optional.ofNullable(sharedPref.getString(getString(resId), "")).filter(s -> !s.isEmpty());
    }
}
