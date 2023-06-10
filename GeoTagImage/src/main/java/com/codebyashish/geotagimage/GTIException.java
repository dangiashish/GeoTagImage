package com.codebyashish.geotagimage;

/**
 * 2023, Copyright by Ashish Dangi,
 * <a href="https://github.com/dangiashish">github.com/dangiashish</a>,
 * India
 */
public class GTIException extends Exception {

    public GTIException(String message) {
        super(message);
    }

    public GTIException(String message, Throwable cause) {
        super(message, cause);
    }

    private int errorCode;

    public GTIException(String message, int errorCode) {
        super(message);
        this.errorCode = errorCode;
    }


    public int getErrorCode() {
        return errorCode;
    }
}
