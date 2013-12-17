package com.myself.bbminterface;

import net.rim.device.api.system.Bitmap;
import net.rim.device.api.system.EncodedImage;
import net.rim.device.api.system.RuntimeStore;

public abstract class BBMBridge {

    //====================================================================================
    //  static fields and methods

    // This is a key for RuntimeStore for the implementation instance of BBMBridge. 
    private static final long BBM_BRIDGE_GUID = 0xc019c33e785448f6L;// com.sec.chaton.bbminterface.BBMBridge
    
    /**
     * Saves the instance of the {@link BBMBridge} implementation in the RuntimeStore,
     * so that the app project can retrieve it using {@link #getInstance()}.
     */
    public static void setInstance(BBMBridge instance) {
        RuntimeStore.getRuntimeStore().put(BBMBridge.BBM_BRIDGE_GUID, instance);
    }

    /**
     * Gets the instance of the the bridge. This may return null if the  
     * BBM platform is not available or not installed. The caller must
     * take the unavailability of the platform into consideration.
     * <p>
     * In order to use the bridge, {@link #start()} must be called to register
     * the app with the BBM platform.
     * 
     * @return an instance to the BBMBridge or null if the platform is not available.
     */
    public static BBMBridge getInstance() {
        try {
            return (BBMBridge) RuntimeStore.getRuntimeStore().get(BBMBridge.BBM_BRIDGE_GUID); 
        } catch (ClassCastException e) {
            // Whatever is in the runtime store isn't what we expect, so return null.
            return null;
        }
    }
    
    //====================================================================================
    //  abstract methods

    /**
     * Starts the registration process with the platform. This may cause
     * user prompts if called multiple times.
     */
    public abstract void register(BBMBridgeCallback callback);
   
    public abstract boolean isAccessAllowed();
    
    public abstract void requestUserPermission();
    
    /**
     * Invites friends to download the app.
     */
    public abstract void inviteFriendsToDownload();
    
    public abstract void changePersonalMessage(String message);
    
    public abstract void changeDisplayPicture(EncodedImage image);

    public abstract void changeDisplayPicture(Bitmap bitmap);
    
    public abstract void getBBMContactList();
}
