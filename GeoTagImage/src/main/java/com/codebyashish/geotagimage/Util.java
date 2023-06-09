package com.codebyashish.geotagimage;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;

public class Util {

    public static boolean isGoogleMapsLinked(Context context) {
        PackageManager packageManager = context.getPackageManager();
        String apiKey = null;

        try {
            ApplicationInfo applicationInfo = packageManager.getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
            Bundle metaData = applicationInfo.metaData;

            if (metaData != null && metaData.containsKey("com.google.android.geo.API_KEY")) {
                apiKey = metaData.getString("com.google.android.geo.API_KEY");
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        return (apiKey != null);
    }

    public static String getMapKey(Context context){
        PackageManager packageManager = context.getPackageManager();
        String apiKey = null;

        try {
            ApplicationInfo applicationInfo = packageManager.getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
            Bundle metaData = applicationInfo.metaData;

            if (metaData != null && metaData.containsKey(context.getPackageName())) {
                apiKey = metaData.getString(context.getPackageName());
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        return apiKey;
    }

}
