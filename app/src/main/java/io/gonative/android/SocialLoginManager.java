package io.gonative.android;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.GraphRequest;
import com.facebook.login.LoginResult;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;

import java.util.Arrays;

public class SocialLoginManager {
    private static final String TAG = UrlNavigation.class.getName();
    
    public static final String GOOGLE_LOGIN_URL = "accounts.google.com/o/oauth";
    public static final String FACEBOOK_LOGIN_URL = "facebook.com/dialog/oauth";
    
    private CallbackManager mFacebookCallbackManager;
    private GoogleSignInClient mGoogleSignInClient;
    private String mGoogleSignInRedirectUrl;
    
    public void onActivityResult(MainActivity mainActivity, int requestCode, int resultCode, Intent data) {
        if (mFacebookCallbackManager != null) {
            mFacebookCallbackManager.onActivityResult(requestCode, resultCode, data);
        }
        
        if (requestCode == MainActivity.GOOGLE_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            handleGoogleSignInResult(mainActivity, task);
        }
        
    }
    
    public void unregisterFacebookCallbackManager() {
        if (mFacebookCallbackManager == null) return;
        com.facebook.login.LoginManager.getInstance().unregisterCallback(mFacebookCallbackManager);
        mFacebookCallbackManager = null;
    }
    
    public boolean loginViaFacebookSdk(MainActivity mainActivity, Uri uri) {
        if (mainActivity == null || uri == null) return false;
        
        final String redirectUrl = Uri.decode(uri.getQueryParameter("redirect_uri"));
        final String state = uri.getQueryParameter("state");
        final String[] scope = Uri.decode(uri.getQueryParameter("scope")).split(",");
        
        FacebookCallback<LoginResult> facebookCallback = new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                GraphRequest request = GraphRequest.newMeRequest(
                        AccessToken.getCurrentAccessToken(),
                        (object, response) -> {
                            // send facebook user details to web
                            String url = redirectUrl + "?access_token=" +
                                    AccessToken.getCurrentAccessToken().getToken() +
                                    "&state=" +
                                    state;
                            mainActivity.loadUrl(url);
                            unregisterFacebookCallbackManager();
                        });
                
                Bundle parameters = new Bundle();
                parameters.putString("fields", "id,name,link");
                request.setParameters(parameters);
                request.executeAsync();
            }
            
            @Override
            public void onCancel() {
                String url = redirectUrl + "?error=" +
                        Uri.encode("Login Canceled") +
                        "&state=" +
                        state;
                mainActivity.loadUrl(url);
                unregisterFacebookCallbackManager();
            }
            
            @Override
            public void onError(FacebookException exception) {
                String url = redirectUrl + "?error=" +
                        Uri.encode(exception.getMessage()) +
                        "&state=" +
                        state;
                mainActivity.loadUrl(url);
                unregisterFacebookCallbackManager();
            }
        };
        
        mFacebookCallbackManager = CallbackManager.Factory.create();
        
        // login with no button
        com.facebook.login.LoginManager.getInstance().registerCallback(mFacebookCallbackManager, facebookCallback);
        com.facebook.login.LoginManager.getInstance().logInWithReadPermissions(mainActivity, Arrays.asList(scope));
        return true;
    }
    
    public void initGoogleSignIn(Activity activity, String clientId) {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(clientId)
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(activity, gso);
    }
    
    public boolean googleSignIn(MainActivity mainActivity, Uri uri) {
        if (mainActivity == null || uri == null) return false;
        
        mGoogleSignInRedirectUrl = Uri.decode(uri.getQueryParameter("redirect_uri"))
                .replace("storagerelay://", "")
                .replace("https/", "https://")
                .replace("http/", "http://");
        
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        mainActivity.startActivityForResult(signInIntent, MainActivity.GOOGLE_SIGN_IN);
        return true;
    }
    
    private void handleGoogleSignInResult(MainActivity mainActivity, Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            
            // send google user details to web
            String url = mGoogleSignInRedirectUrl + "?id_token=" + account.getIdToken();
            mainActivity.loadUrl(url);
            
        } catch (ApiException e) {
            // The ApiException status code indicates the detailed failure reason.
            // Please refer to the GoogleSignInStatusCodes class reference for more information.
            Log.w(TAG, "Google SignIn result failed = " + e.getStatusCode());
            // send error to web
            String url = mGoogleSignInRedirectUrl + "?error=" + e.getStatusCode();
            mainActivity.loadUrl(url);
            
        }
    }
}
