package com.example.mycards.main;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import com.example.mycards.R;
import com.example.mycards.data.entities.JMDictEntry;
import com.example.mycards.data.repositories.DefaultJMDictRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import javax.inject.Inject;

public class MainActivity extends AppCompatActivity {

    @Inject
    ExecutorService activityExecutors;

    private static final String TAG = "MainActivity";

    @Inject
    SharedViewModelFactory sharedViewModelFactory;
    SharedViewModel sharedViewModel;

    @Inject
    DefaultJMDictRepository jmDictRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //TODO - Consider splash screen at start up
        ((MyCardsApplication) getApplicationContext()).appComponent.inject(this);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sharedViewModel = new ViewModelProvider(this, sharedViewModelFactory).get(SharedViewModel.class);

        //TODO - Issue: do not want this to run if the database has already been populated... (eg screen config change)
        Future<?> future = activityExecutors.submit(() -> jmDictRepository
                .insertAll(getPrePopulatedData(this)));
        try {
            Log.d(TAG, "Waiting for the jmdict db to pre-populate...");
            future.get();
            Log.d(TAG, "jmdict db has pre-populated!");
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static synchronized List<JMDictEntry> getPrePopulatedData(Context context) {
        int realResource = R.raw.reverse_jmdictentries_plain;

        List<JMDictEntry> dictEntries = new ArrayList<>();

        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        try(InputStream jsonStream = context.getResources().openRawResource(realResource)) {
            dictEntries = mapper.readValue(jsonStream,
                    new TypeReference<List<JMDictEntry>>() {
                    });
        } catch (IOException e) {
            e.printStackTrace();
        }

        return dictEntries;
    }
    
    @Override
    public void onDestroy() {
        activityExecutors.shutdown();   //close thread pool
        super.onDestroy();
    }
}