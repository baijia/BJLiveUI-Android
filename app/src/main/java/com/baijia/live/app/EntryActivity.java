package com.baijia.live.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.baijiayun.live.ui.LiveSDKWithUI;
import com.baijiayun.livecore.LiveSDK;
import com.baijiayun.livecore.context.LPConstants;

/**
 * Created by Shubo on 2017/3/20.
 */

public class EntryActivity extends AppCompatActivity {

    private SharedPreferences sp;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_entry);

        sp = getSharedPreferences("live_temp", Context.MODE_PRIVATE);
        //data
        String code1 = sp.getString("code", "");
        String domain = sp.getString("domain", null);
        if (!TextUtils.isEmpty(code1))
            ((EditText) findViewById(R.id.activity_entry_join_code)).setText(code1);
        if (!TextUtils.isEmpty(domain))
            ((EditText) findViewById(R.id.activity_entry_customer_domain)).setText(domain);

        findViewById(R.id.enter).setOnClickListener(v -> {
            String code = ((EditText) findViewById(R.id.activity_entry_join_code)).getText().toString();
            String name = ((EditText) findViewById(R.id.activity_entry_name)).getText().toString();
            String domain1 = ((EditText) findViewById(R.id.activity_entry_customer_domain)).getText().toString();
            LiveSDK.customEnvironmentPrefix = domain1;

            SharedPreferences.Editor editor = sp.edit();
            editor.putString("code", code);
            editor.putString("domain", domain1);
            editor.apply();
            LiveSDKWithUI.enterRoom(EntryActivity.this, code, name, msg -> Toast.makeText(EntryActivity.this, msg, Toast.LENGTH_SHORT).show());
        });

        ((RadioGroup) findViewById(R.id.rg_env)).setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rg_env_product) {
                LiveSDK.deployType = LPConstants.LPDeployType.Product;
            } else if (checkedId == R.id.rg_env_beta) {
                LiveSDK.deployType = LPConstants.LPDeployType.Beta;
            } else {
                LiveSDK.deployType = LPConstants.LPDeployType.Test;
            }
        });
    }
}
