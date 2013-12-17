package com.myself.bbminterface;

/**
 * This interface defines callback functions for BBMBridge.
 */
public interface BBMBridgeCallback {
    
    public void onInitialized(boolean success);
    public void exitApp();
}
