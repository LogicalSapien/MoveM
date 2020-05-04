package com.logicalsapien.movem;

import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

class RequestTask extends AsyncTask<String, String, String> {

    @Override
    protected String doInBackground(String... uri) {
        String responseString = null;
        try {
            URL url = new URL(uri[0]);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            if(conn.getResponseCode() == HttpsURLConnection.HTTP_OK){
                // Do normal input or output stream reading
                Log.i("HttpSuccess", "Yes");
            }
            else {
//                response = "FAILED"; // See documentation for more info on response handling
                Log.i("HttpSuccess", "No");
            }
        } catch (Exception e) {
            //TODO Handle problems..
        }
        return responseString;
    }

    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);
        //Do anything with response..
    }
}