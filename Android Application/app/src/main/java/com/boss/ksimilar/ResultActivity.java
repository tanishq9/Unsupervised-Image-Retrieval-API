package com.boss.ksimilar;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class ResultActivity extends AppCompatActivity {
    private static final String TAG = "ResultActivity";
    private static final String IP = "http://192.168.1.4:3000/image";
    RecyclerView recyclerView;
    ArrayList<String> urls = new ArrayList<>();
    DividerItemDecorator dividerItemDecoration;
    ResultAdapter resultAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);
        bindViews();
    }

    private void bindViews() {
        recyclerView = findViewById(R.id.recyclerView);
        urls.add(IP + "0");
        urls.add(IP + "1");
        urls.add(IP + "2");
        urls.add(IP + "3");
        urls.add(IP + "4");
        urls.add(IP + "5");
        urls.add(IP + "6");
        urls.add(IP + "7");
        urls.add(IP + "8");
        resultAdapter = new ResultAdapter(this, urls);
        recyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        dividerItemDecoration = new DividerItemDecorator(ContextCompat.getDrawable(this, R.drawable.divider));
        recyclerView.addItemDecoration(dividerItemDecoration);
        recyclerView.setAdapter(resultAdapter);
    }

    @Override
    public void onBackPressed() {
        Log.e(TAG, "Back Key Pressed");
        setResult(Activity.RESULT_OK, new Intent());
        finish();
    }

}
