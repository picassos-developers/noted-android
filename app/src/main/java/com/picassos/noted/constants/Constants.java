package com.picassos.noted.constants;

// Configuration File: this file used for app configuration.

public class Constants {

    /**
     * SPLASH_SCREEN_TIMEOUT used for
     * splash screen interval.
     * 1000 m = 1 sec
     */
    public static final int SPLASH_SCREEN_TIMEOUT = 1000;

    /**
     * EMAIL_ADDRESS used for feedback
     * and other contact options
     */
    public static final String EMAIL_ADDRESS = "hello@picassos.xyz";

    /**
     * app privacy policy url.
     * make sure it's a valid url in
     * order to be approved by the
     * Google Play Store support team
     */
    public static final String PRIVACY_POLICY_URL = "https://google.com";

    /**
     * app terms of use url
     * make sure it's a valid url
     * in order to be approved by the
     * Google Play Store support team
     */
    public static final String TERMS_OF_USE_URL = "https://google.com";

    /**
     * enable google admob ads
     * 1. native ad
     * 2. rewarded ad
     */
    public static final boolean ENABLE_GOOGLE_ADMOB_ADS = true;
    public static final String GOOGLE_ADMOB_NATIVE_AD_UNIT_ID = "ca-app-pub-5451983796114290/6371660017";
    public static final String GOOGLE_ADMOB_REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917";

    /**
     * enable fingerprint option
     */
    public static final boolean ENABLE_FINGERPRINT_LOGIN = true;

}