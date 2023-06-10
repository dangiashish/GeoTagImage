package com.codebyashish.geotagimage;

/**
 * 2023, Copyright by Ashish Dangi,
 * <a href="https://github.com/dangiashish">github.com/dangiashish</a>,
 * India
 */
public interface PermissionCallback {
    void onPermissionGranted();

    void onPermissionDenied();
}

