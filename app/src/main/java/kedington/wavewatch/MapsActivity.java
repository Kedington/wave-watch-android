package kedington.wavewatch;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.VisibleRegion;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;


public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

    private static final String TAG = "WAVEWATCHDEBUG";
    private GoogleMap mMap;

    private FusedLocationProviderClient mFusedLocationProviderClient;

    // Default location San Diego California
    private final LatLng mDefaultLocation = new LatLng(32.7157, 117.1611);
    private static final int DEFAULT_ZOOM = 15;
    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    private boolean mLocationPermissionGranted;

    // Last Known location from Fused Location Provider
    private Location mLastKnownLocation;

    private ImageView hiddenImage;
    private Button mSearchButton;
    private Button mPhotoButton;

    // Photo Variables
    static final int REQUEST_TAKE_PHOTO = 1;
    private String currentPhotoPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        // Construct a FusedLocationProvider
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        // Set up the image view for displaying waves
        hiddenImage = (ImageView) findViewById(R.id.hidden_image);
        hiddenImage.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                hiddenImage.setVisibility(View.INVISIBLE);
            }
        });

        // Set up Button listener to switch to taking a photo
        mPhotoButton = findViewById(R.id.photo_button_id);
        mPhotoButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                dispatchTakePictureIntent();
            }
        });

        // Set listener for search button
        mSearchButton = findViewById(R.id.search_button_id);
        mSearchButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Make call to server to get nearby photos then call parseResponse()
                JSONArray response = requestPhotos(mMap.getCameraPosition().target);
                parseResponse(response);
            }
        });

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        try {
            // Customise the styling of the base map using a JSON object defined
            // in a raw resource file.
            // https://developers.google.com/maps/documentation/android-sdk/styling
            boolean success = googleMap.setMapStyle(
                    MapStyleOptions.loadRawResourceStyle(
                            this, R.raw.style_json));

            if (!success) {
                Log.e(TAG, "Style parsing failed.");
            }
        } catch (Resources.NotFoundException e) {
            Log.e(TAG, "Can't find style. Error: ", e);
        }

        mMap = googleMap;

        mMap.setOnMarkerClickListener(this);

        updateLocationUI();

        getDeviceLocation();

    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        Photo photo = (Photo) marker.getTag();
        marker.setVisible(false);
        displayImage(photo.getPhotoId());
        return false;
    }

    public void displayImage(String url) {
        new DownloadImageTask((ImageView) findViewById(R.id.hidden_image))
                .execute(url);
    }

    public JSONArray requestPhotos(LatLng location) {
        try {
            JSONArray array = new JSONArray();
            JSONObject obj = new JSONObject().put("photo_id", "http://1.bp.blogspot.com/-hNC-oT6f-fY/TeXxO26yjvI/AAAAAAAAAOY/qfkOqdKkBi8/s1600/platon-photographer-putin-man-of-the-year-portrait.jpg");
            obj.put("latitude", 33.023586);
            obj.put("longitude", -117.088658);
            JSONObject obj2 = new JSONObject().put("photo_id", "2");
            obj2.put("latitude", 33.022508);
            obj2.put("longitude", -117.070333);
            array.put(obj);
            array.put(obj2);
            return array;
        } catch (JSONException e) {
            return null;
        }
    }

    /**
     * Parses the response from the Server into photo objects and adds markers to the map
     * @param results
     */
    public void parseResponse(JSONArray results) {
        for (int i = 0; i < results.length(); i++) {
            try {
                Photo photo = new Photo(results.getJSONObject(i));
                if (mMap != null && isVisibleOnMap(photo.getPosition())) {
                    Marker mMarker = mMap.addMarker(new MarkerOptions().position(photo.getPosition()).title(photo.getPhotoId())
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
                    mMarker.setTag(photo);
                }
            } catch (JSONException e) {
                Log.e(TAG, "Error parsing Json");
            }

        }
    }

    public boolean isVisibleOnMap(LatLng latLng) {
        VisibleRegion vr = mMap.getProjection().getVisibleRegion();
        return vr.latLngBounds.contains(latLng);
    }

    /**
     * Updates the Location to the current users Location
     */
    private void updateLocationUI() {
        if (mMap == null) {
            return;
        } try {
            if (mLocationPermissionGranted) {
                mMap.setMyLocationEnabled(true);
                mMap.getUiSettings().setMyLocationButtonEnabled(true);
                getDeviceLocation();
            } else {
                mMap.setMyLocationEnabled(false);
                mMap.getUiSettings().setMyLocationButtonEnabled(false);
                mLastKnownLocation = null;
                getLocationPermission();
            }
            mMap.getUiSettings().setMapToolbarEnabled(false);
        } catch (SecurityException e)  {
            Log.e("Exception: %s", e.getMessage());
        }
    }

    private void getDeviceLocation() {
        /*
         * Get the best and most recent location of the device, which may be null in rare
         * cases when a location is not available.
         */
        try {
            if (mLocationPermissionGranted) {
                Task<Location> locationResult = mFusedLocationProviderClient.getLastLocation();
                locationResult.addOnCompleteListener(this, new OnCompleteListener<Location>() {
                    @Override
                    public void onComplete(@NonNull Task<Location> task) {
                        if (task.isSuccessful()) {
                            // Set the map's camera position to the current location of the device.
                            mLastKnownLocation = task.getResult();
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                                    new LatLng(mLastKnownLocation.getLatitude(),
                                            mLastKnownLocation.getLongitude()), DEFAULT_ZOOM));
                        } else {
                            Log.d(TAG, "Current location is null. Using defaults.");
                            Log.e(TAG, "Exception: %s", task.getException());
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(mDefaultLocation, DEFAULT_ZOOM));
                            mMap.getUiSettings().setMyLocationButtonEnabled(false);
                        }
                    }
                });
            }
        } catch(SecurityException e)  {
            Log.e("Exception: %s", e.getMessage());
        }
    }

    /* *******************************
     *      Location Permissions     *
     *********************************/

    /**
     * Request location permission, so that we can get the location of the
     * device. The result of the permission request is handled by a callback,
     * onRequestPermissionsResult.
     */
    private void getLocationPermission() {
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mLocationPermissionGranted = true;
            updateLocationUI();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        mLocationPermissionGranted = false;
        switch (requestCode) {
            case PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mLocationPermissionGranted = true;
                }
            }
        }
        updateLocationUI();
    }

    /* *******************************
     *      Camera Methods           *
     *********************************/

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );
        // Save a file: path for use with ACTION_VIEW intents
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    private Bitmap getCurrentBitmap() throws IOException {
        return MediaStore.Images.Media.getBitmap(this.getContentResolver(),
                Uri.fromFile(new File(currentPhotoPath)));
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "kedington.wavewatch.android.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {

        // Handler for photo taken
        if (resultCode == RESULT_OK && requestCode == REQUEST_TAKE_PHOTO) {
            try {
                hiddenImage.setVisibility(View.VISIBLE);
                hiddenImage.setImageBitmap(getCurrentBitmap());
            } catch (Exception e) {

            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
        ImageView bmImage;
        public DownloadImageTask(ImageView bmImage) {
            this.bmImage = bmImage;
        }

        protected Bitmap doInBackground(String... urls) {
            String urldisplay = urls[0];
            Bitmap mIcon11 = null;
            try {
                InputStream in = new java.net.URL(urldisplay).openStream();
                mIcon11 = BitmapFactory.decodeStream(in);
            } catch (Exception e) {
                Log.e("Error", e.getMessage());
                e.printStackTrace();
            }
            return mIcon11;
        }

        protected void onPostExecute(Bitmap result) {
            bmImage.setVisibility(View.VISIBLE);
            bmImage.setImageBitmap(result);
        }
    }
}
