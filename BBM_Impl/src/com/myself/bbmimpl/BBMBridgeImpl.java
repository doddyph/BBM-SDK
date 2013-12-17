package com.myself.bbmimpl;

import java.lang.ref.WeakReference;
import java.util.Enumeration;

import com.myself.bbminterface.BBMBridge;
import com.myself.bbminterface.BBMBridgeCallback;

import net.rim.blackberry.api.bbm.platform.BBMPlatformApplication;
import net.rim.blackberry.api.bbm.platform.BBMPlatformContext;
import net.rim.blackberry.api.bbm.platform.BBMPlatformContextListener;
import net.rim.blackberry.api.bbm.platform.BBMPlatformManager;
import net.rim.blackberry.api.bbm.platform.io.BBMPlatformChannel;
import net.rim.blackberry.api.bbm.platform.io.BBMPlatformChannelListener;
import net.rim.blackberry.api.bbm.platform.io.BBMPlatformData;
import net.rim.blackberry.api.bbm.platform.io.BBMPlatformOutgoingJoinRequest;
import net.rim.blackberry.api.bbm.platform.io.BBMPlatformSession;
import net.rim.blackberry.api.bbm.platform.io.BBMPlatformSessionListener;
import net.rim.blackberry.api.bbm.platform.profile.BBMPlatformContact;
import net.rim.blackberry.api.bbm.platform.profile.BBMPlatformContactList;
import net.rim.blackberry.api.bbm.platform.profile.UserProfile;
import net.rim.blackberry.api.bbm.platform.service.ContactListService;
import net.rim.blackberry.api.bbm.platform.service.MessagingService;
import net.rim.blackberry.api.bbm.platform.service.MessagingServiceListener;
import net.rim.device.api.system.Application;
import net.rim.device.api.system.Bitmap;
import net.rim.device.api.system.ControlledAccessException;
import net.rim.device.api.system.EncodedImage;
import net.rim.device.api.ui.component.Dialog;

/**
 * This library runs on startup and registers an instance of the BBMBridge with 
 * the runtime store. 
 * 
 * All the calls to and callbacks from the BBM API are handled in this class.
 * This class contains a minimal amount of application logic.
 * It also holds a single BBM API channel.
 * 
 * The singleton instance of TicTacToeApp makes calls to this class and receives callbacks
 * from this class through the interface BBMBridgeCallback.
 * 
 * See TicTacToeApp.java for an overview description of how an application can use
 * the BBM API to communicate among devices.
 */
public final class BBMBridgeImpl extends BBMBridge {

    /**
     * This method is invoked when the BlackBerry device starts. 
     */
    public static void libMain(String[] args) {
        if (BBMBridge.getInstance() == null) {
            // Construct the instance of BBMBridge and store it in RuntimeStore.
            // If there are linking problems because BBM platform is not installed or  
            // an incorrect version is installed, the bridge will be absent from the 
            // runtime store. The application will know BBM platform is not available.
            BBMBridge instance = new BBMBridgeImpl();
            BBMBridge.setInstance(instance);
        }
    }

    //====================================================================================
    //  BBMPlatformApplication

    /**
     * An UUID is used to uniquely identify the application in the test environment
     * before the application is available in AppWorld. If the application exists in
     * AppWorld, the UUID will not be used to identify the application.
     *
     * To run this app, you should generate a different UUID, because there is a
     * limit to the number of users who can share the same UUID.
     * Search for "UUID Generator" on the web for instructions to generate a UUID.
     * 
     * http://www.guidgenerator.com/
     * http://www.uuidgenerator.net/
     */
    private static final String UUID = "f935a9ec-72cb-4cea-96e9-eaf9ef38b614";

    /**
     * BBMPlatformApplication serves to provide certain properties of the application
     * to the BBM Social Platform. It is used as a parameter inBBMPlatformManager.register().
     *
     * If your application wants to be invoked in a non-default way, e.g. when
     * you have multiple entry points, you need to subclass from BBMPlatformApplication
     * and override certain methods.
     */
    private final BBMPlatformApplication bbmApp = new BBMPlatformApplication(UUID);

    //====================================================================================
    //  BBMPlatformContextListener

    private final BBMPlatformContextListener contextListener = new BBMPlatformContextListener() {
        public void accessChanged(final boolean isAccessAllowed, int code) {
            // call onAccessChanged() in a background thread to make UI responsive
            new Thread(new Runnable() {
                public void run() {
                    onAccessChanged(isAccessAllowed);
                }
            }).start();
        }
    };
    
    private void onAccessChanged(boolean isAccessAllowed) {
        synchronized (this) {
            // access to BBM not allowed?
            if (!isAccessAllowed) {
                int error = context.getAccessErrorCode();
                
                if (error == BBMPlatformContext.ACCESS_REREGISTRATION_REQUIRED) {
                    // The application must call register() again. In this app, we terminate the
                    // application for the sake of simplicity.
                    Application.getApplication().invokeAndWait(new Runnable() {
                        public void run() {
                            BBMBridgeCallback callback = getCallback();
                            
                            if (callback != null) {
                                Dialog.inform("The application needs to restart.");
                                callback.exitApp();
                            }
                        }
                    });
                    
                    return;
                }
                
                profile = null;
                onInitialized(false);  // initialization fails
                return;
            }
    
            // We can now get UserProfile, ContactListService, MessagingService,
            // UIService, etc, through the BBMPlatformContext.
            profile = context.getUserProfile();
        }

        messaging = context.getMessagingService();
        messaging.setServiceListener(messagingListener);
        
        onInitialized(true);  // initialization succeeds
    }
    
    private void onInitialized(boolean success) {
        BBMBridgeCallback callback = getCallback();
        
        if (callback != null) {
            callback.onInitialized(success);
        }
    }

    //====================================================================================
    //  MessagingServiceListener
    
    private final MessagingServiceListener messagingListener = new MessagingServiceListener() {
        // The user accepted an invitation from a remote user.
        // A new channel is created by the BBM Platform and passed to the user application.
        public void channelCreated(BBMPlatformChannel channel) {}
        
        // BBM created a channel because the user joined the app in BBM
        public void channelCreated(BBMPlatformChannel channel, int menuItemId) {}
        
        // We must provide a BBMPlatformChannelListener when the BBM Platform creates a channel.
        public BBMPlatformChannelListener getChannelListener(BBMPlatformChannel channel) {
            return null;
        }

        // We do not use BBMPlatformSession in this app, but we have to implement these methods.
        public void sessionCreated(BBMPlatformSession session) {}
        
        public void sessionEnded(BBMPlatformContact contact, BBMPlatformSession session) {}
        
        public BBMPlatformSessionListener getSessionListener(BBMPlatformSession session) {
            return null;
        }

        // Called when a file transfer has failed
        public void fileTransferFailed(String path, BBMPlatformContact contact, int code) {}

        public void onContactReachable(BBMPlatformContact contact) {}
        
        public void onMessagesExpired(BBMPlatformContact contact, BBMPlatformData[] data) {}

        // I tried to join a public connection and the host accepts my request
        public void joinRequestAccepted(BBMPlatformOutgoingJoinRequest request, String param) {}
        
        // I tried to join a public connection but the host declines my request
        public void joinRequestDeclined(BBMPlatformOutgoingJoinRequest request, int code) {}
    };
    

    //====================================================================================
    //  fields

    // host application
    private WeakReference callback;  // WeakReference<BBMBridgeCallback>

    // BBM Objects
    private BBMPlatformContext context;
    private UserProfile profile;
    private MessagingService messaging;

    //====================================================================================
    //  initialization

    private BBMBridgeImpl() {}
    
    public void register(BBMBridgeCallback callback) {

        this.callback = new WeakReference(callback);
        
        // After constructing a BBMPlatformApplication, pass it to BBMPlatformManager.register()
        // to obtain a BBMPlatformContext. The BBMPlatformContext serves as the application's
        // doorway to BBM's functions.
        try {
            context = BBMPlatformManager.register(bbmApp);
        } catch (ControlledAccessException e) {
            return;
        }

        // A BBMPlatformContextListener should be given to the BBMPlatformContext object
        // to detect when access to BBM is granted.
        context.setListener(contextListener);

        if (!isAccessAllowed()) {
			requestUserPermission();
		}
        /*
        boolean allowed = context.isAccessAllowed();
        if (!allowed) {
            int error = context.getAccessErrorCode();
            // The user chose not to connect the app to BBM. We'll prompt the user again.                       
            if (error == BBMPlatformContext.ACCESS_BLOCKED_BY_USER) {
                context.requestUserPermission();
            }
        }
        */
    }
    
    public boolean isAccessAllowed() {
		return context.isAccessAllowed();
	}

	public void requestUserPermission() {
		int error = context.getAccessErrorCode();
        // The user chose not to connect the app to BBM. We'll prompt the user again.                       
        if (error == BBMPlatformContext.ACCESS_BLOCKED_BY_USER) {
            context.requestUserPermission();
        }
	}
    
    public String getUserName() {
        return profile.getDisplayName(); 
    }

    public void inviteFriendsToDownload() {
        messaging.sendDownloadInvitation();
    }

    public void changePersonalMessage(String message) {
    	String personalMessage = profile.getPersonalMessage();
        // check if the message needs to be changed
        if (personalMessage != null && personalMessage.equals(message)) return;
        // change the message: BBM will ask if the user actually wants to change
        profile.setPersonalMessage(message);
    }
    
    public void changeDisplayPicture(EncodedImage encodedImage) {
    	changeDisplayPicture(encodedImage.getBitmap());
	}
	
	public void changeDisplayPicture(Bitmap bitmap) {
		Bitmap displayPicture = profile.getDisplayPicture();
		if (displayPicture != null && displayPicture.equals(bitmap)) return;
    	profile.setDisplayPicture(bitmap);
	}
    
    private BBMBridgeCallback getCallback() {
        return (callback == null) ? null : (BBMBridgeCallback) callback.get();
    }

	public void getBBMContactList() {
		ContactListService contactListService = context.getContactListService();
		BBMPlatformContactList contacts = contactListService.getContactList();
		Enumeration enum = contacts.getAll();
		
		while (enum.hasMoreElements()) {
			BBMPlatformContact contact = (BBMPlatformContact) enum.nextElement();
			contact.getDisplayName();
		}
	}
    
}