package com.example.madcampweek3.LocalScan;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.madcampweek3.R;
import com.google.gson.JsonObject;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;

import com.example.madcampweek3.RetrofitService.AccountService;
import com.example.madcampweek3.RetrofitService.RetrofitClient;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class LocalScan extends Fragment {
    public static final int REQUEST_ENABLE_BT = 1;

    private RecyclerView recyclerView;
    private FriendAdapter mAdapter;
    private RecyclerView.LayoutManager layoutManager;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fargment_localscan, container, false);

        /* Set recycler view */
        recyclerView = (RecyclerView) view.findViewById(R.id.friends_list);
        layoutManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(layoutManager);
        mAdapter = new FriendAdapter();
        recyclerView.setAdapter(mAdapter);

        return view;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        /* Register for broadcasts when a device is discovered */
        super.onCreate(savedInstanceState);

        /* Init bluetooth */
        BluetoothAdapter bluetoothAdapter =  BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            // Device doesn't support Bluetooth
            return;
        }
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        // TODO: Delete permission request
        requestPermissions(
                new String[] {Manifest.permission.ACCESS_FINE_LOCATION},
                1
        );
        /* Scan bluetooth */
        IntentFilter bluetoothFilter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        getActivity().registerReceiver(receiver, bluetoothFilter);

        bluetoothAdapter.startDiscovery();
    }

    // Create a BroadcastReceiver for ACTION_FOUND.
    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String name = device.getName();
                if (name != null) {
                    Log.d("Bluetooth", "Find name: " + name);
                }
                String deviceHardwareAddress = device.getAddress(); // MAC address

                tryFindUser(deviceHardwareAddress, new FindUserResponse() {
                    @Override
                    public void onResponseReceived(String name) {
                        mAdapter.addFriend(new Friend(name));
                    }
                });
            }
        }
    };

    @Override
    public void onDestroy() {
        super.onDestroy();
        getActivity().unregisterReceiver(receiver);
    }

    private void tryFindUser(String address, FindUserResponse findUserResponse) {
        /* Init */
        Retrofit retrofit = RetrofitClient.getInstnce();
        AccountService service = retrofit.create(AccountService.class);

        /* Send macAddress */
        service.findUser(address).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(@NotNull Call<JsonObject> call, @NotNull Response<JsonObject> response) {
                if (response.body() == null) {
                    try { // Can't find user
                        assert response.errorBody() != null;
                        Log.d("AccountService", "res: " + response.errorBody().string());
                    } catch (IOException e) {
                        e.printStackTrace();;
                    }
                } else {
                    String res = null;
                    if (response.body().has("userName")) {
                        res = response.body().get("userName").toString();
                    }
                    Log.d("AccountService", "res:" + res);
                    findUserResponse.onResponseReceived(res);
                }
            }

            @Override
            public void onFailure(@NotNull Call<JsonObject> call, @NotNull Throwable t) {
                Log.d("AccountService", "Failed API call with call: " + call
                        + ", exception: " + t);
            }
        });
    }

    interface FindUserResponse {
        void onResponseReceived(String name);
    }
}