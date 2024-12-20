package com.indusface.plugins.entity;

import java.net.HttpURLConnection;
import net.sf.json.JSONObject;

public class StartScanResponse {

    private boolean success;

    private JSONObject response;

    public StartScanResponse(int code, String response) {
        if (code != HttpURLConnection.HTTP_OK) {
            success = false;
        } else {
            success = true;
            this.response = JSONObject.fromObject(response);
        }
    }

    public StartScanResponse() {
        this.success = false;
        this.response = new JSONObject();
    }

    public boolean isSuccess() {
        return success;
    }

    public String getScanLogId() {
        return response.getJSONObject("result").getString("scanlogid");
    }
}
