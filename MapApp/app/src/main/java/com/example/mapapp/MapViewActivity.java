package com.example.mapapp;


import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

public class MapViewActivity extends AppCompatActivity {

    private static final String FETCH_URL = "http://10.0.2.2/track_project/fetchPoints.php";
    private static final double DEFAULT_LAT  = 33.9716;  // Casablanca par défaut
    private static final double DEFAULT_LON  = -6.8498;
    private static final double DEFAULT_ZOOM = 14.0;

    private MapView osmMap;
    private RequestQueue httpQueue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialise OSMDroid AVANT setContentView
        Configuration.getInstance().load(
                getApplicationContext(),
                getSharedPreferences("osm_prefs", MODE_PRIVATE)
        );

        setContentView(R.layout.activity_map_view);

        setupMap();
        httpQueue = Volley.newRequestQueue(getApplicationContext());
        fetchAndDisplayMarkers();
    }

    /** Configure la vue de carte avec zoom et position de départ */
    private void setupMap() {
        osmMap = findViewById(R.id.osmMap);
        osmMap.setTileSource(TileSourceFactory.MAPNIK);
        osmMap.setBuiltInZoomControls(true);
        osmMap.setMultiTouchControls(true);

        // Centre initial sur le Maroc
        osmMap.getController().setZoom(DEFAULT_ZOOM);
        osmMap.getController().setCenter(new GeoPoint(DEFAULT_LAT, DEFAULT_LON));
    }

    /** Récupère les positions depuis le serveur et les affiche sur la carte */
    private void fetchAndDisplayMarkers() {
        JsonObjectRequest jsonReq = new JsonObjectRequest(
                Request.Method.POST,
                FETCH_URL,
                null,
                response -> {
                    try {
                        parseAndPlotPositions(response);
                    } catch (JSONException e) {
                        Toast.makeText(this, "Erreur JSON", Toast.LENGTH_SHORT).show();
                        e.printStackTrace();
                    }
                },
                error -> {
                    Toast.makeText(this, "Serveur inaccessible", Toast.LENGTH_SHORT).show();
                    error.printStackTrace();
                }
        );

        httpQueue.add(jsonReq);
    }

    /** Parse le JSON et ajoute un marqueur par position */
    private void parseAndPlotPositions(JSONObject response) throws JSONException {
        JSONArray pts = response.getJSONArray("points");

        for (int i = 0; i < pts.length(); i++) {
            JSONObject pt  = pts.getJSONObject(i);
            double lat     = pt.getDouble("latitude");
            double lon     = pt.getDouble("longitude");
            String recDate = pt.optString("recorded_at", "—");

            placeMarker(lat, lon, "Point " + (i + 1), recDate);
        }

        // Rafraîchit l'affichage de la carte
        osmMap.invalidate();

        Toast.makeText(this, pts.length() + " point(s) chargé(s)", Toast.LENGTH_SHORT).show();
    }

    /** Crée et ajoute un marqueur coloré à la carte */
    private void placeMarker(double lat, double lon, String title, String snippet) {
        Marker m = new Marker(osmMap);
        m.setPosition(new GeoPoint(lat, lon));
        m.setTitle(title);
        m.setSnippet(snippet);
        m.setIcon(new android.graphics.drawable.BitmapDrawable(
                getResources(), buildDotIcon(40, Color.parseColor("#00BCD4"))
        ));
        m.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);

        osmMap.getOverlays().add(m);
    }

    /**
     * Génère un disque coloré comme icône de marqueur.
     * @param size taille en pixels
     * @param color couleur du disque
     */
    private Bitmap buildDotIcon(int size, int color) {
        Bitmap bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);

        // Halo blanc autour du point
        Paint halo = new Paint(Paint.ANTI_ALIAS_FLAG);
        halo.setColor(Color.WHITE);
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, halo);

        // Point coloré au centre
        Paint dot = new Paint(Paint.ANTI_ALIAS_FLAG);
        dot.setColor(color);
        canvas.drawCircle(size / 2f, size / 2f, size / 2.8f, dot);

        return bmp;
    }

    // ─── Cycle de vie OSMDroid ────────────────────────────────────

    @Override
    protected void onResume() {
        super.onResume();
        osmMap.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        osmMap.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (httpQueue != null) httpQueue.cancelAll(this);
    }
}
