package com.cs639project.covid19tracker;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Button;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.eazegraph.lib.charts.PieChart;
import org.eazegraph.lib.models.PieModel;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.location.Location;
import android.location.LocationManager;
import android.location.LocationListener;


public class MainActivity extends AppCompatActivity implements LocationListener {

    private String version;
    private FirebaseDatabase firebaseDatabase;
    private DatabaseReference databaseReference;
    private String appUrl;
    private TextView tv_confirmed, tv_confirmed_new, tv_active, tv_active_new, tv_recovered, tv_recovered_new, tv_death,
            tv_death_new, tv_tests, tv_tests_new, tv_date, tv_time;

    private SwipeRefreshLayout swipeRefreshLayout;

    private PieChart pieChart;

    private LinearLayout lin_state_data, lin_world_data;

    private String str_confirmed, str_confirmed_new, str_active, str_active_new, str_recovered, str_recovered_new,
            str_death, str_death_new, str_tests, str_tests_new, str_last_update_time;
    private int int_active_new;
    private ProgressDialog progressDialog;
    private boolean doubleBackToExitPressedOnce = false;
    private Toast backPressToast;
    Button btnshowlocation;

    LocationManager locationManager;
    private int LOCATION_PERMISSION_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnshowlocation = (Button)findViewById(R.id.btnshowlocation);
        btnshowlocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(ContextCompat.checkSelfPermission(MainActivity.this,
                        Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
                    //Toast.makeText(MainActivity.this, "You have already granted permission!", Toast.LENGTH_SHORT).show();
                }else {
                    requestLocationPermission();
                }
                getLocation();
            }
        });

        CheckForUpdate();

        getSupportActionBar().setTitle("Covid-19 Tracker (India)");

        Init();

        FetchData();

        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                FetchData();
                swipeRefreshLayout.setRefreshing(false);
            }
        });

        lin_state_data.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, StateWiseDataActivity.class));
            }
        });

        lin_world_data.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, WorldDataActivity.class));
            }
        });
    }

    private void requestLocationPermission(){
        if(ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.ACCESS_FINE_LOCATION)){
            new AlertDialog.Builder(this)
                    .setTitle("Permission needed")
                    .setMessage("This permission is needed to access live location for COVID-19 Precaution!!")
                    .setPositiveButton("ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            ActivityCompat.requestPermissions(MainActivity.this, new String[] {Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_CODE);
                        }
                    })
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.dismiss();
                        }
                    })
                    .create().show();
        }else{
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == LOCATION_PERMISSION_CODE) {
            if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
//                Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show();
            }else{
//                Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show();
            }

        }
    }

    private void CheckForUpdate() {
        try{
            version = this.getPackageManager().getPackageInfo(getPackageName(), 0).versionName;

            firebaseDatabase = FirebaseDatabase.getInstance();
            databaseReference = firebaseDatabase.getReference("Version").child("versionNumber");
            databaseReference.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    String versionName = (String) dataSnapshot.getValue();

                    if(!versionName.equals(version)){

                        androidx.appcompat.app.AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this)
                                .setTitle("New Version Available!")
                                .setMessage("Please update our app to the latest version!")
                                .setPositiveButton("UPDATE", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        DatabaseReference myRef = FirebaseDatabase.getInstance().getReference("Version").child("appUrl");
                                        myRef.addValueEventListener(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                                appUrl = dataSnapshot.getValue().toString();
                                                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(appUrl)));
                                                finish();
                                            }

                                            @Override
                                            public void onCancelled(@NonNull DatabaseError databaseError) {

                                            }
                                        });
                                    }
                                })
                                .setNegativeButton("EXIT", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        finish();
                                    }
                                })
                                .create();

                        alertDialog.setCancelable(false);
                        alertDialog.setCanceledOnTouchOutside(false);

                        alertDialog.show();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });

        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private void FetchData() {

        ShowDialog(this);

        RequestQueue requestQueue = Volley.newRequestQueue(this);
        String apiUrl = "https://api.covid19india.org/data.json";

        pieChart.clearChart();

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                Request.Method.GET,
                apiUrl,
                null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {

                        JSONArray all_state_jsonArray = null;
                        JSONArray testData_jsonArray = null;

                        try {
                            all_state_jsonArray = response.getJSONArray("statewise");
                            testData_jsonArray = response.getJSONArray("tested");
                            JSONObject data_india = all_state_jsonArray.getJSONObject(0);
                            JSONObject test_data_india = testData_jsonArray.getJSONObject(testData_jsonArray.length()-2);

                            str_confirmed = data_india.getString("confirmed");
                            str_confirmed_new = data_india.getString("deltaconfirmed");

                            str_active = data_india.getString("active");

                            str_recovered = data_india.getString("recovered");
                            str_recovered_new = data_india.getString("deltarecovered");

                            str_death = data_india.getString("deaths");
                            str_death_new = data_india.getString("deltadeaths");

                            str_last_update_time = data_india.getString("lastupdatedtime");
                            str_tests = test_data_india.getString("totalsamplestested");
                            str_tests_new = test_data_india.getString("samplereportedtoday");

                            Handler delayToshowProgess = new Handler();

                            delayToshowProgess.postDelayed(new Runnable() {
                                @Override
                                public void run() {

                                    tv_confirmed.setText(NumberFormat.getInstance().format(Integer.parseInt(str_confirmed)));
                                    tv_confirmed_new.setText("+" + NumberFormat.getInstance().format(Integer.parseInt(str_confirmed_new)));

                                    tv_active.setText(NumberFormat.getInstance().format(Integer.parseInt(str_active)));

                                    int_active_new = Integer.parseInt(str_confirmed_new)
                                            - (Integer.parseInt(str_recovered_new) + Integer.parseInt(str_death_new));
                                    tv_active_new.setText("+"+NumberFormat.getInstance().format(int_active_new));

                                    tv_recovered.setText(NumberFormat.getInstance().format(Integer.parseInt(str_recovered)));
                                    tv_recovered_new.setText("+"+NumberFormat.getInstance().format(Integer.parseInt(str_recovered_new)));

                                    tv_death.setText(NumberFormat.getInstance().format(Integer.parseInt(str_death)));
                                    tv_death_new.setText("+"+NumberFormat.getInstance().format(Integer.parseInt(str_death_new)));

                                    tv_tests.setText(NumberFormat.getInstance().format(Long.parseLong(str_tests)));
                                    tv_tests_new.setText("+"+NumberFormat.getInstance().format(Long.parseLong(str_tests_new)));

                                    tv_date.setText(FormatDate(str_last_update_time, 1));
                                    tv_time.setText(FormatDate(str_last_update_time, 2));

                                    pieChart.addPieSlice(new PieModel("Active", Integer.parseInt(str_active), Color.parseColor("#007afe")));
                                    pieChart.addPieSlice(new PieModel("Recovered", Integer.parseInt(str_recovered), Color.parseColor("#08a045")));
                                    pieChart.addPieSlice(new PieModel("Deceased", Integer.parseInt(str_death), Color.parseColor("#F6404F")));

                                    pieChart.startAnimation();

                                    DismissDialog();

                                }
                            }, 1000);



                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        error.printStackTrace();
                    }
                });
        requestQueue.add(jsonObjectRequest);
    }

    public void ShowDialog(Context context) {
        progressDialog = new ProgressDialog(context);
        progressDialog.show();
        progressDialog.setContentView(R.layout.progress_dialog);
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
    }

    public void DismissDialog() {
        progressDialog.dismiss();
    }

    public String FormatDate(String date, int testCase) {
        Date mDate = null;
        String dateFormat;
        try {
            mDate = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.US).parse(date);
            if (testCase == 0) {
                dateFormat = new SimpleDateFormat("dd MMM yyyy, hh:mm a").format(mDate);
                return dateFormat;
            } else if (testCase == 1) {
                dateFormat = new SimpleDateFormat("dd MMM yyyy").format(mDate);
                return dateFormat;
            } else if (testCase == 2) {
                dateFormat = new SimpleDateFormat("hh:mm a").format(mDate);
                return dateFormat;
            } else {
                Log.d("error", "Wrong input! Choose from 0 to 2");
                return "Error";
            }
        } catch (ParseException e) {
            e.printStackTrace();
            return date;
        }
    }

    private void Init() {
        tv_confirmed = findViewById(R.id.activity_main_confirmed_textview);
        tv_confirmed_new = findViewById(R.id.activity_main_confirmed_new_textview);
        tv_active = findViewById(R.id.activity_main_active_textview);
        tv_active_new = findViewById(R.id.activity_main_active_new_textview);
        tv_recovered = findViewById(R.id.activity_main_recovered_textview);
        tv_recovered_new = findViewById(R.id.activity_main_recovered_new_textview);
        tv_death = findViewById(R.id.activity_main_death_textview);
        tv_death_new = findViewById(R.id.activity_main_death_new_textview);
        tv_tests = findViewById(R.id.activity_main_samples_textview);
        tv_tests_new = findViewById(R.id.activity_main_samples_new_textview);
        tv_date = findViewById(R.id.activity_main_date_textview);
        tv_time = findViewById(R.id.activity_main_time_textview);

        pieChart = findViewById(R.id.activity_main_piechart);
        swipeRefreshLayout = findViewById(R.id.activity_main_swipe_refresh_layout);
        lin_state_data = findViewById(R.id.activity_main_statewise_lin);
        lin_world_data = findViewById(R.id.activity_main_world_data_lin);
    }

    void getLocation(){
        try{
            locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 5, this);
        }
        catch (SecurityException e){
            e.printStackTrace();
        }
    }
    @Override
    public void onLocationChanged(Location location){
        Location currentLocation = new Location("location A");
        currentLocation.setLatitude(location.getLatitude());
        currentLocation.setLongitude(location.getLongitude());
        Location destination = new Location("location B");
        destination.setLatitude(40.737290296358964);
        destination.setLongitude(-74.07035296978339);
        double distance = currentLocation.distanceTo(destination);
        //Toast.makeText(getApplicationContext(), "Distance between: " +distance, Toast.LENGTH_LONG).show();
        if(distance>10){
            Toast.makeText(getApplicationContext(), "Your GPS Shows that you are away from your house, PLEASE WEAR A MASK!", Toast.LENGTH_LONG).show();
        }else{
            Toast.makeText(getApplicationContext(), "Dont leave", Toast.LENGTH_SHORT).show();
        }
    }
    @Override
    public void onProviderDisabled(String provider){
        Toast.makeText(getApplicationContext(), "Please Provide GPS and Internet", Toast.LENGTH_SHORT).show();
    }
    @Override
    public void onStatusChanged(String provider, int status, Bundle extras){

    }
    @Override
    public void onProviderEnabled(String provider){

    }

    @Override
    public void onBackPressed() {
        if (doubleBackToExitPressedOnce) {
            backPressToast.cancel();
            super.onBackPressed();
            return;
        }
        doubleBackToExitPressedOnce = true;
        backPressToast = Toast.makeText(this, "Please click BACK again to exit", Toast.LENGTH_SHORT);
        backPressToast.show();

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                doubleBackToExitPressedOnce = false;
            }
        }, 2000);
    }



}
