package com.example.madcampweek3.LocalScan;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.madcampweek3.R;
import com.example.madcampweek3.RetrofitService.AccountService;
import com.example.madcampweek3.RetrofitService.ImageService;
import com.example.madcampweek3.RetrofitService.RetrofitClient;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

import static android.content.Context.MODE_PRIVATE;
import static com.example.madcampweek3.MainActivity.MainActivity.PROFILE_IMAGE_KIND;
import static com.example.madcampweek3.MainActivity.MainActivity.PROFILE_IMAGE_NAME;


public class PendingFragment extends Fragment {

    private RecyclerView pendingRecyclerView;
    private PendingAdapter pendingAdapter;
    private RecyclerView.LayoutManager pendingLayoutManager;
    private SwipeRefreshLayout swipeRefreshLayout;
    private List<Pending> pendings = new ArrayList<>();
    private HashMap<String, Bitmap> pendingProfiles = new HashMap<>();
    public static String userID;





    public PendingFragment() {
        // Required empty public constructor
    }




    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_pending, container, false);
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh_layout);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refresh();
                swipeRefreshLayout.setRefreshing(false);
            }

        });
        /* Set pending recycler view */
        pendingRecyclerView = (RecyclerView) view.findViewById(R.id.pending_list);
        pendingLayoutManager = new LinearLayoutManager(getContext());
        pendingRecyclerView.setLayoutManager(pendingLayoutManager);
        pendingAdapter = new PendingAdapter();
        pendingRecyclerView.setAdapter(pendingAdapter);

        downloadPendingList();
        pendingAdapter.notifyDataSetChanged();
        return view;
    }

    private void refresh()
    {
        pendingAdapter.clear();
        downloadPendingList();
        pendingAdapter.setPending(pendings);
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        /* Register for broadcasts when a device is discovered */
        super.onCreate(savedInstanceState);

        SharedPreferences appData = getContext().getSharedPreferences("appData", MODE_PRIVATE);
        userID = appData.getString("ID","");

    }
    @Override
    public void onDestroy() {
        super.onDestroy();
    }
    @Override
    public void onResume(){
        super.onResume();
        pendingAdapter.notifyDataSetChanged();
    }
    private void downloadPendingList() {
        /* Init retrofit */
        Retrofit retrofit = RetrofitClient.getInstnce();
        AccountService service = retrofit.create(AccountService.class);

        /* Get matching list */
        service.getLike(userID).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(@NotNull Call<JsonObject> call, @NotNull Response<JsonObject> response) {
                if (response.body() == null) {
                    try { // Failure
                        assert response.errorBody() != null;
                        Log.d("AccountService", "res:" + response.errorBody().string());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else { // Success
                    ArrayList<String> friendIDList = getFriendIDList(response);
                    addPendingList(response);
                    downloadProfileForPendingList(friendIDList);
                }
            }

            @Override
            public void onFailure(@NotNull Call<JsonObject> call, @NotNull Throwable t) {
                Log.d("AccountService", "Failed API call with call: " + call
                        + ", exception: " + t);
            }
        });
    }


    private void addPendingList(Response<JsonObject> response) {
        JsonArray friendIDList = new JsonArray ();
        JsonArray friendNameList = new JsonArray ();

        if (response.body().has("friendID")) {
            friendIDList = response.body().getAsJsonArray("friendID");
        }

        if (response.body().has("friendName")) {
            friendNameList = response.body().getAsJsonArray("friendName");
        }

        for (int i = 0; i < friendNameList.size(); ++i) {
            String friendID = friendIDList.get(i).toString();
            friendID= friendID.substring(1, friendID.length() - 1);
            String friendName = friendNameList.get(i).toString();
            friendName = friendName.substring(1, friendName.length() - 1);

            pendings.add(new Pending(friendID, friendName));
        }
    }
    private ArrayList<Number> reviseIntimacyScore(JsonArray intimacyScore) {
        int totalScore = 0;
        ArrayList<Number> res = new ArrayList<>();
        for (JsonElement score: intimacyScore) {
            totalScore += score.getAsInt();
        }
        for (JsonElement score: intimacyScore) {
            res.add(score.getAsDouble() / totalScore * 100);
        }

        return res;
    }
    private void downloadProfileForPendingList(ArrayList<String> friendIDList) {
        /* Init retrofit */
        Retrofit retrofit = RetrofitClient.getInstnce();
        ImageService service = retrofit.create(ImageService.class);

        /* Get matching list */
        String fileName = "1" + "_" + PROFILE_IMAGE_NAME;
        for (String id: friendIDList) {
            service.downloadProfile(id, PROFILE_IMAGE_KIND, fileName).enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(@NotNull Call<ResponseBody> call, @NotNull Response<ResponseBody> response) {
                    if (response.body() == null) {
                        try { // Failure
                            assert response.errorBody() != null;
                            Log.d("ImageService", "res:" + response.errorBody().string());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } else { // Success
                        /* Change profile image */
                        InputStream stream = response.body().byteStream();
                        Bitmap bitmap = BitmapFactory.decodeStream(stream);
                        String friendID = call.request().url().queryParameter("id");
                        pendingProfiles.put(friendID, bitmap);
                        if (pendingProfiles.size() == pendings.size()) {
                            addProfileToPending();
                            setPendingList();
                        }
                    }
                }

                @Override
                public void onFailure(@NotNull Call<ResponseBody> call, @NotNull Throwable t) {
                    Log.d("ImageService", "Failed API call with call: " + call
                            + ", exception: " + t);
                }
            });
        }
    }
    private void setPendingList() {
        pendingAdapter.setPending(pendings);
    }
    private ArrayList<String> getFriendIDList(Response<JsonObject> response) {
        ArrayList<String> result = new ArrayList<>();
        JsonArray friendIDList = new JsonArray ();

        assert response.body() != null;

        if (response.body().has("friendID")) {
            friendIDList = response.body().getAsJsonArray("friendID");
        }

        for (int i = 0; i < friendIDList.size(); ++i) {
            String friendID = friendIDList.get(i).toString();
            friendID = friendID.substring(1, friendID.length() - 1);
            result.add(friendID);
        }

        return result;
    }
    private void addProfileToPending() {
        for (int i = 0; i < pendings.size(); ++i) {
            String friendID = pendings.get(i).getId();
            pendings.get(i).setProfile(pendingProfiles.get(friendID));
        }
    }
}
