package com.example.mapapp;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class HomeActivity extends AppCompatActivity {

    // ─── Constantes ───────────────────────────────────────────────
    private static final int GPS_PERM_CODE   = 42;
    private static final long MIN_INTERVAL_MS = 60_000L; // 1 minute
    private static final float MIN_DIST_M     = 100f;    // 100 mètres
    private static final String SERVER_URL    = "http://10.0.2.2/track_project/savePoint.php";

    // ─── Widgets ──────────────────────────────────────────────────
    private TextView tvStatusInfo;
    private MaterialButton btnShowMap;

    // ─── Services ─────────────────────────────────────────────────
    private LocationManager gpsManager;
    private RequestQueue httpQueue;

    // ─── Données GPS ──────────────────────────────────────────────
    private double currentLat, currentLon, currentAlt;
    private float  currentAccuracy;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        bindViews();
        initServices();
        checkAndRequestPermissions();
    }

    /** Lie les vues du layout */
    private void bindViews() {
        tvStatusInfo = findViewById(R.id.tvStatusInfo);
        btnShowMap   = findViewById(R.id.btnShowMap);

        // Ouvre la carte au clic
        btnShowMap.setOnClickListener(v ->
                startActivity(new Intent(this, MapViewActivity.class))
        );
    }

    /** Initialise le gestionnaire GPS et la file HTTP */
    private void initServices() {
        gpsManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        httpQueue  = Volley.newRequestQueue(getApplicationContext());
    }

    /** Vérifie les permissions; les demande si nécessaire */
    private void checkAndRequestPermissions() {
        boolean hasFine   = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
        boolean hasCoarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;

        if (hasFine && hasCoarse) {
            beginTracking();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    }, GPS_PERM_CODE);
        }
    }

    /** Démarre l'écoute GPS */
    private void beginTracking() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) return;

        gpsManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                MIN_INTERVAL_MS,
                MIN_DIST_M,
                new LocationListener() {
                    @Override
                    public void onLocationChanged(@NonNull Location loc) {
                        // Récupère les nouvelles coordonnées
                        currentLat      = loc.getLatitude();
                        currentLon      = loc.getLongitude();
                        currentAlt      = loc.getAltitude();
                        currentAccuracy = loc.getAccuracy();

                        refreshStatusDisplay();
                        sendCoordinatesToServer(currentLat, currentLon);
                    }
                }
        );
    }

    /** Met à jour le texte d'état avec les dernières coordonnées */
    private void refreshStatusDisplay() {
        String info = getString(R.string.location_update,
                currentLat, currentLon, currentAlt, currentAccuracy);
        tvStatusInfo.setText(info);
    }

    /** Envoie les coordonnées au backend PHP via POST */
    private void sendCoordinatesToServer(final double lat, final double lon) {
        StringRequest req = new StringRequest(
                Request.Method.POST,
                SERVER_URL,
                response -> { /* Succès silencieux */ },
                error   -> Toast.makeText(this, "Erreur réseau", Toast.LENGTH_SHORT).show()
        ) {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                // Identifiant unique de l'appareil (pas besoin de permission)
                String deviceId = Settings.Secure.getString(
                        getContentResolver(), Settings.Secure.ANDROID_ID);

                Map<String, String> data = new HashMap<>();
                data.put("latitude",  String.valueOf(lat));
                data.put("longitude", String.valueOf(lon));
                data.put("device_id", deviceId);
                data.put("recorded_at",
                        new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                                .format(new Date()));
                return data;
            }
        };

        httpQueue.add(req);
    }

    /** Résultat de la demande de permission */
    @Override
    public void onRequestPermissionsResult(int code,
                                           @NonNull String[] perms, @NonNull int[] results) {
        super.onRequestPermissionsResult(code, perms, results);
        if (code == GPS_PERM_CODE) {
            if (results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) {
                beginTracking();
            } else {
                Toast.makeText(this, getString(R.string.perm_denied),
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Libère les ressources Volley
        if (httpQueue != null) httpQueue.cancelAll(this);
    }
}