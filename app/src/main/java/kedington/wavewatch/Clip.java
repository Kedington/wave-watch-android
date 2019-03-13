package kedington.wavewatch;

import com.google.android.gms.maps.model.LatLng;

import org.json.JSONException;
import org.json.JSONObject;

/**
 *  Clip Object to hold the clip_id and LatLng
 */

public class Clip {

    private String clipId;
    private LatLng position;

    /**
     * Parses the passed JSONObject into a clip object with a clipId and position
     * @param clip
     */
    public Clip(JSONObject clip) throws JSONException {
        this.clipId = clip.getString("clip_id");
        this.position = new LatLng(clip.getDouble("latitude"), clip.getDouble("longitude"));
    }

    public String getClipId() {
        return clipId;
    }

    public void setClipId(String clipId) {
        this.clipId = clipId;
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
