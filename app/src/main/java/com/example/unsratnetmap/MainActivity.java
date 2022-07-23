package com.example.unsratnetmap;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.google.gson.JsonObject;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity
        implements LocationListener {

    private TextView txtKoodinatNow, txtRssi, txtSsid;
    private Button btnCariLokasiNow, btnUpload, btnScanWifi;
    private String provider, latitude, longitude, gSsid, gRssi;
    private LocationManager locationManager;
    private WifiManager wifiManager;
    private WifiInfo info;
    ProgressDialog progressDialog;

    private ListView listView;
    private List<ScanResult> results;
    private ArrayList<String> arrayList = new ArrayList<>();
    private ArrayList<String[]> arrayList2 = new ArrayList<String[]>();
    private ArrayAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //definisi dari inisialisasi
        txtKoodinatNow = (TextView) findViewById(R.id.txtKoordinatNow);
        txtRssi = (TextView) findViewById(R.id.txtRSSI);
        txtSsid = (TextView) findViewById(R.id.txtSSID);
        progressDialog = new ProgressDialog(MainActivity.this);


        btnCariLokasiNow = (Button) findViewById(R.id.btnCariLokasiNow);
        btnUpload = (Button) findViewById(R.id.btnUpload);
        btnScanWifi = (Button) findViewById(R.id.btnScanWifi);
        //pendefinisian location manager
        locationManager = (LocationManager)
                getSystemService(Context.LOCATION_SERVICE);
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        //Criteria baru
        Criteria criteria = new Criteria();
        //set value string provider
        provider = locationManager.getBestProvider(criteria,false);
        listView = findViewById(R.id.wifiList);

        if (!wifiManager.isWifiEnabled()) {
            Toast.makeText(this, "WiFI is Disabled.. We need to enable it", Toast.LENGTH_LONG).show();
            wifiManager.setWifiEnabled(true);

        }

        btnCariLokasiNow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //cek permission
                if (ActivityCompat.checkSelfPermission(MainActivity.this,
                        Manifest.permission.ACCESS_FINE_LOCATION) !=
                        PackageManager.PERMISSION_GRANTED
                        &&
                        ActivityCompat.checkSelfPermission(MainActivity.this,
                                Manifest.permission.ACCESS_COARSE_LOCATION) !=
                                PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                //membuat lokas baru dengan isi lokasi terakhir device
                Location location =
                        locationManager.getLastKnownLocation(provider);
                if (location != null) {
                    System.out.println("Provider" + provider +
                            "has been selected. ");
                    //jalankan method onLocationChange dengan value location
                    onLocationChanged(location);
                }
                else {
                    txtKoodinatNow.setText("Harap menunggu hingga lokasi" +
                            "didapatkan atau cek pengaturan lokasi anda");
                }

            }
        });



        btnScanWifi.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                scanWifi();
            }
        });

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, arrayList);
        listView.setAdapter(adapter);
        scanWifi();

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                String arrayData[];

                arrayData = arrayList2.get(i);
                gSsid = arrayData[0];
                gRssi = arrayData[1];
                txtSsid.setText(gSsid);
                txtRssi.setText(gRssi);
            }
        });



        btnUpload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if ( gSsid.isEmpty() && gRssi.isEmpty() && latitude.isEmpty() && longitude.isEmpty()){
                    Toast.makeText(getApplicationContext(),"Data Tidak Lengkap", Toast.LENGTH_SHORT).show();
                    return;
                } else {
                    String sSsid = gSsid.toString();
                    String sRssi = gRssi.toString();
                    String sLati = latitude.toString();
                    String sLong = longitude.toString();
                    insertToDatabase(sSsid, sRssi, sLati, sLong);
                }
            }
        });



    }

    public void insertToDatabase(final String ssid, final String rssi, final String lat, final String longitude){
        if (checkNetworkCon()) {

            StringRequest stringRequest = new StringRequest(Request.Method.POST,
                    DbContract.SERVER_ADD_DATA_URL,
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            try {
                                String resp;
                                resp = response;
                                Toast.makeText(getApplicationContext(),resp, Toast.LENGTH_LONG).show();
                                JSONObject jsonObject = new JSONObject(response);
                                resp = jsonObject.getString("server");
                            } catch (JSONException e) {
                                e.printStackTrace();

                            }

                        }
                    }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {

                }
            }){
                @Override
                protected Map<String, String> getParams() throws AuthFailureError {
                    Map<String, String> params = new HashMap<>();
                    params.put("ssid", ssid);
                    params.put("rssi", rssi);
                    params.put("lat", lat);
                    params.put("longitude", longitude);
                    return params;

                }
            };

            VolleyConnection.getInstance(MainActivity.this).addToRequestQueue(stringRequest);
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    progressDialog.cancel();
                }
            }, 2000);



        } else {
            Toast.makeText(getApplicationContext(),"Tidak Ada Internet", Toast.LENGTH_SHORT).show();
        }

    }
    public boolean checkNetworkCon() {
        ConnectivityManager connectivityManager = (ConnectivityManager)
                this.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo nInfo = connectivityManager.getActiveNetworkInfo();
        return (nInfo != null && nInfo.isConnected());
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        locationManager.requestLocationUpdates(provider,400,1,this);
    }
    @Override
    protected void onPause() {
        super.onPause();
        //stop update
        locationManager.removeUpdates(this);
    }

    @Override
    public void onLocationChanged(Location location) {
        double lat = (double) (location.getLatitude());
        double lang = (double) (location.getLongitude());
        latitude = String.valueOf(lat);
        longitude = String.valueOf(lang);
        txtKoodinatNow.setText(String.valueOf(lat) + " , " +
                String.valueOf(lang));
    }
    @Override
    public void onStatusChanged(String s, int i, Bundle bundle){

    }
    @Override
    public void onProviderEnabled(String s) {
        Toast.makeText(getApplicationContext(),"Provider"
                + provider + "diaktifkan" , Toast.LENGTH_SHORT).show();
    }
    @Override
    public void onProviderDisabled(String s) {
        Toast.makeText(getApplicationContext(),"Provider"
                + provider + "dinonaktifkan" , Toast.LENGTH_SHORT).show();
    }

    private void scanWifi() {
        arrayList.clear();
        arrayList2.clear()  ;
        registerReceiver(wifiReciever, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        wifiManager.startScan();
        Toast.makeText(this, "Scanning Wifi . . .", Toast.LENGTH_SHORT).show();
    };



    BroadcastReceiver wifiReciever = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            results = wifiManager.getScanResults();
            unregisterReceiver(this);

            for (ScanResult scanResult : results){
                String ssid, rssi;
                ssid = scanResult.SSID;
                rssi = String.valueOf(scanResult.level);
                String hasil[] = {ssid,rssi};
                arrayList2.add(hasil);
                arrayList.add(scanResult.SSID + "  (" + scanResult.level + ")");
                adapter.notifyDataSetChanged();
            }
        }
    };



}
