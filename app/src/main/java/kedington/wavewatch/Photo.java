package kedington.wavewatch;

import com.google.android.gms.maps.model.LatLng;

import org.json.JSONException;
import org.json.JSONObject;

/**
 *  Photo Object
 */

public class Photo {

    private String photoId;
    private LatLng position;

    /**
     * Parses the passed JSONObject into a clip object with a photoId and position
     * @param photo
     */
    public Photo(JSONObject photo) throws JSONException {
        this.photoId = photo.getString("photo_id");
        this.position = new LatLng(photo.getDouble("latitude"), photo.getDouble("longitude"));
    }

    public String getPhotoId() {
        return photoId;
    }

    public void setPhotoId(String photoId) {
        this.photoId = photoId;
    }

    public LatLng getPosition() {
        return position;
    }

    public void setPosition(LatLng position) {
        this.position = position;
    }

    public void setPosition(double latitude, double longitude) {
        this.position = new LatLng(latitude, longitude);
    }
}
