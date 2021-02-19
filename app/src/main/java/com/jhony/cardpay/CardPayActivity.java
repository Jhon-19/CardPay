package com.jhony.cardpay;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import android.os.Bundle;

public class CardPayActivity extends SingleFragmentActivity {

    @Override
    protected Fragment createFragment() {
        return CardPayFragment.newInstance();
    }
}