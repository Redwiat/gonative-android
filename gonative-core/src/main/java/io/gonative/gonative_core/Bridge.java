package io.gonative.gonative_core;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import io.gonative.android.library.IOUtils;

public abstract class Bridge {
    private final GoNativeContext mContext;
    private ArrayList<String> jsFiles = null;

    protected Bridge(Context context) {
        mContext = new GoNativeContext(context);
    }

    public void onApplicationCreate() {
        for (BridgeModule plugin: getPlugins()) {
            plugin.onApplicationCreate(mContext);
        }
    }

    public <T extends Activity & GoNativeActivity> void onActivityCreate(T activity, boolean isRoot) {
        for (BridgeModule plugin: getPlugins()) {
            plugin.onActivityCreate(activity, isRoot);
        }
    }

    public <T extends Activity & GoNativeActivity> boolean shouldOverrideUrlLoading(T activity, Uri url, JSONObject params) {
        for (BridgeModule plugin: getPlugins()) {
            if (plugin.shouldOverrideUrlLoading(activity, url, params)) {
                return true;
            }
        }

        return false;
    }

    public <T extends Activity & GoNativeActivity> void onActivityResult(T activity, int requestCode, int resultCode, Intent data) {
        for (BridgeModule plugin: getPlugins()) {
            plugin.onActivityResult(activity, requestCode, resultCode, data);
        }
    }

    public <T extends Activity & GoNativeActivity> void injectJSLibraries(T activity) {
        if (jsFiles == null) {
            jsFiles = new ArrayList<>();
            try {
                String [] paths = activity.getAssets().list("");
                for (String file: paths) {
                    if (file.endsWith("-plugin.js") && !file.equals("GoNativeJSBridgeLibrary.js")) {
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        InputStream is = new BufferedInputStream(activity.getAssets().open(file));
                        IOUtils.copy(is, baos);
                        jsFiles.add(baos.toString());
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        for (String jsContent: jsFiles) {
            activity.runJavascript(jsContent);
        }
    }

    protected abstract List<BridgeModule> getPlugins();
}
