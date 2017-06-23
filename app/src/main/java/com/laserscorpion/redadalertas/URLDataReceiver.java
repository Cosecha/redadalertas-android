package com.laserscorpion.redadalertas;

public interface URLDataReceiver {
    /**
     * Callback to alert the TorURLLoader caller that the Tor network activity is complete,
     * and provide the response if the request succeeded. Implement this if you want to use
     * TorURLLoader.
     * @param successful whether the request successfully returned data
     * @param data the body of the HTTP response if successful, null otherwise
     */
    public void requestComplete(boolean successful, String data);
}
