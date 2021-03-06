/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.settings.deviceinfo;

import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.PhoneFactory;

import android.content.Context;
import android.os.Bundle;
import android.os.SystemProperties;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.sim.Sim;
import android.sim.SimManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;

import com.android.settings.R;
import com.sprd.android.support.featurebar.FeatureBarHelper;
import android.widget.TextView;
import android.view.ViewGroup;

public class ImeiInformation extends PreferenceActivity {

    private static final String KEY_PRL_VERSION = "prl_version";
    private static final String KEY_MIN_NUMBER = "min_number";
    private static final String KEY_MEID_NUMBER = "meid_number";
    private static final String KEY_ICC_ID = "icc_id";
    private static final String KEY_IMEI = "imei";
    private static final String KEY_IMEI_SV = "imei_sv";
    private static final String NAME_CMCC = "cmcc";
    private static final String PROPERTY_NAME = "ro.operator";
    private static final String PROPERTY_VALUE = SystemProperties.get(PROPERTY_NAME, "");
    private static final int SIM_COUNT_1 = 1;
    private static final int SIM_COUNT_2 = 2;

    private boolean isMultiSIM = false;
    /*SPRD:add for bug 648423@{*/
    private FeatureBarHelper mFeatureBarHelper;
    /*@}*/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        /*SPRD:add for bug 648423@{*/
        mFeatureBarHelper = new FeatureBarHelper(this);
        ViewGroup vg = mFeatureBarHelper.getFeatureBar();
        if (vg != null) {
            TextView option = (TextView) mFeatureBarHelper.getOptionsKeyView();
            TextView center = (TextView) mFeatureBarHelper.getCenterKeyView();
            vg.removeView(option);
            vg.removeView(center);
        }
        /*@}*/
        final TelephonyManager telephonyManager =
            (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
        isMultiSIM = telephonyManager.isMultiSim();
        initPreferenceScreen(isMultiSIM);
    }

    // Since there are multiple phone for dsds, therefore need to show information for different
    // phones.
    private void initPreferenceScreen(boolean isMultiSIM) {
        int slotCount = isMultiSIM ? SIM_COUNT_2 : SIM_COUNT_1;
        for (int slotId = 0; slotId < slotCount; slotId ++) {
            addPreferencesFromResource(R.xml.device_info_phone_status);
            setPreferenceValue(slotId);
            setNewKey(slotId);
        }
    }

    private void setPreferenceValue(int phoneId) {
        final Phone phone = PhoneFactory.getPhone(phoneId);

        if (phone != null) {
            if (phone.getPhoneType() == TelephonyManager.PHONE_TYPE_CDMA) {
                setSummaryText(KEY_MEID_NUMBER, phone.getMeid());
                setSummaryText(KEY_MIN_NUMBER, phone.getCdmaMin());

                if (getResources().getBoolean(R.bool.config_msid_enable)) {
                    findPreference(KEY_MIN_NUMBER).setTitle(R.string.status_msid_number);
                }

                setSummaryText(KEY_PRL_VERSION, phone.getCdmaPrlVersion());
                removePreferenceFromScreen(KEY_IMEI_SV);

                if (phone.getLteOnCdmaMode() == PhoneConstants.LTE_ON_CDMA_TRUE) {
                    // Show ICC ID and IMEI for LTE device
                    setSummaryText(KEY_ICC_ID, phone.getIccSerialNumber());
                    setSummaryText(KEY_IMEI, phone.getImei());
                } else {
                    // device is not GSM/UMTS, do not display GSM/UMTS features
                    // check Null in case no specified preference in overlay xml
                    removePreferenceFromScreen(KEY_IMEI);
                    removePreferenceFromScreen(KEY_ICC_ID);
                }
            } else {
                if (NAME_CMCC.equals(PROPERTY_VALUE)) {
                    setSummaryText(KEY_IMEI, TelephonyManager.getDefault(0).getDeviceId());
                }else {
                    setSummaryText(KEY_IMEI, phone.getImei());
                }
                setSummaryText(KEY_IMEI_SV, phone.getDeviceSvn());
                // device is not CDMA, do not display CDMA features
                // check Null in case no specified preference in overlay xml
                removePreferenceFromScreen(KEY_PRL_VERSION);
                removePreferenceFromScreen(KEY_MEID_NUMBER);
                removePreferenceFromScreen(KEY_MIN_NUMBER);
                removePreferenceFromScreen(KEY_ICC_ID);
            }
        }
    }

    // Modify the preference key with prefix "_", so new added information preference can be set
    // related phone information.
    private void setNewKey(int slotId) {
        final PreferenceScreen prefScreen = getPreferenceScreen();
        final int count = prefScreen.getPreferenceCount();
        for (int i = 0; i < count; i++) {
            Preference pref = prefScreen.getPreference(i);
            String key = pref.getKey();
            if (!key.startsWith("_")){
                key = "_" + key + String.valueOf(slotId);
                pref.setKey(key);
                updateTitle(pref, slotId);
            }
        }
    }

    private void updateTitle(Preference pref, int slotId) {
        if (pref != null) {
            String title = pref.getTitle().toString();
            if (isMultiSIM) {
                // Slot starts from 1, slotId starts from 0 so plus 1
                title += " " + getResources().getString(R.string.slot_number, slotId + 1);
            }
            pref.setTitle(title);
        }
    }

    private void setSummaryText(String key, String text) {
        final Preference preference = findPreference(key);

        if (TextUtils.isEmpty(text)) {
            text = getResources().getString(R.string.device_info_default);
        }

        if (preference != null) {
            preference.setSummary(text);
        }
    }

    /**
     * Removes the specified preference, if it exists.
     * @param key the key for the Preference item
     */
    private void removePreferenceFromScreen(String key) {
        final Preference preference = findPreference(key);
        if (preference != null) {
            getPreferenceScreen().removePreference(preference);
        }
    }
}
