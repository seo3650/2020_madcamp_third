package com.example.madcampweek3.Profile;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import androidx.fragment.app.Fragment;

import com.example.madcampweek3.Account.AccountActivity;
import com.example.madcampweek3.R;
import com.example.madcampweek3.RetrofitService.AccountService;
import com.example.madcampweek3.RetrofitService.FriendService;
import com.example.madcampweek3.RetrofitService.ImageService;
import com.example.madcampweek3.RetrofitService.RetrofitClient;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

import static android.content.Context.MODE_PRIVATE;
import static com.example.madcampweek3.Account.AccountEditFragment.PROFILE_IMAGE_KIND;
import static com.example.madcampweek3.Account.AccountEditFragment.PROFILE_IMAGE_NAME;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ProfileFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
// TODO: Using observer, realize asynchronous execution


public class ProfileFragment extends Fragment {

    SharedPreferences appData ;
    String userId = "";
    int age;
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private ListView theListView;
    private ArrayList<Item> items = new ArrayList<>();
    private HashMap<String, Bitmap> profiles = new HashMap<>();



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile,container,false);

        /* Load userID */
        appData =getContext().getSharedPreferences("appData", MODE_PRIVATE);
        userId = appData.getString("ID","");
        // get our list view
        theListView = view.findViewById(R.id.mainListView);

        // prepare elements to display
        downloadTodayCardList();

        return view;
    }
        // Inflate the layout for this fragment




    public ProfileFragment() {
        // Required empty public constructor
    }



    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }
    private void setFoldingProfile(){
        /* Init */
        Retrofit retrofit = RetrofitClient.getInstnce();
        AccountService service = retrofit.create(AccountService.class);
    }

    private void downloadTodayCardList() {
        /* Init retrofit */
        Retrofit retrofit = RetrofitClient.getInstnce();
        FriendService service = retrofit.create(FriendService.class);

        /* Prepare input */
        Calendar time = Calendar.getInstance();
        int date = time.get(Calendar.DAY_OF_YEAR) + (365 * time.get(Calendar.YEAR));

        /* Get today list */
        service.getTodayFriend(userId, date).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(@NotNull Call<JsonObject> call, @NotNull Response<JsonObject> response) {
                if (response.body() == null) {
                    try { // Failure
                        assert response.errorBody() != null;
                        Log.d("FriendService", "res: " + response.errorBody().string());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else { // Success
                    Log.d("FriendService", "res: " + response.body().toString());

                    ArrayList<String> friendIDList = getFriendIDList(response);
                    prepareItemList(response);
                    downloadTodayCardImage(friendIDList);
                }
            }

            @Override
            public void onFailure(@NotNull Call<JsonObject> call, @NotNull Throwable t) {
                Log.d("FriendService", "Failed API call with call: " + call
                        + ", exception: " + t);
            }
        });
    }

    private void downloadTodayCardImage(ArrayList<String> friendIDList) {
        /* Init retrofit */
        Retrofit retrofit = RetrofitClient.getInstnce();
        ImageService service = retrofit.create(ImageService.class);

        /* Prepare input */
        String fileName = 1 + "_" + PROFILE_IMAGE_NAME;

        for (String id: friendIDList) {
            /* Get today list */
            service.downloadProfile(id, PROFILE_IMAGE_KIND, fileName).enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(@NotNull Call<ResponseBody> call, @NotNull Response<ResponseBody> response) {
                    if (response.body() == null) {
                        try { // Failure
                            assert response.errorBody() != null;
                            Log.d("ImageService", "res: " + response.errorBody().string());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } else { // Success
                        Log.d("ImageService", "res: " + response.body().toString());

                        /* Change profile image */
                        InputStream stream = response.body().byteStream();
                        Bitmap bitmap = BitmapFactory.decodeStream(stream);
                        String friendID = call.request().url().queryParameter("id");
                        profiles.put(friendID, bitmap);
                        if (profiles.size() == items.size()) {
                            addProfileToItem();
                            setList();
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

    private void getAgeOfFriend(String friendID)
    {
        Retrofit retrofit = RetrofitClient.getInstnce();
        AccountService service = retrofit.create(AccountService.class);

        service.downloadProfile(friendID).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.body() == null) {
                    try { // Profile download failure
                        assert response.errorBody() != null;
                        Log.d("LoginService", "res:" + response.errorBody().string());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    // Profile download success
                    Log.d("LoginService", "res:" + response.message());


                    /* Change profile info */
                    if (response.body().has("age")) {
                        age = response.body().get("age").getAsInt();
                    }

                }
            }
            @Override
            public void onFailure (Call < JsonObject > call, Throwable t){
                // Profile download success
                Log.d("ProfileService", "Failed API call with call: " + call
                        + ", exception: " + t);

            }

        });

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

    private void prepareItemList(Response<JsonObject> response) {
        JsonArray friendIDList = new JsonArray ();
        JsonArray friendNameList = new JsonArray ();
        JsonArray positionList = new JsonArray ();
        JsonArray contactTimeList = new JsonArray ();

        assert response.body() != null;
        if (response.body().has("friendID")) {
            friendIDList = response.body().getAsJsonArray("friendID");
        }
        if (response.body().has("friendName")) {
            friendNameList = response.body().getAsJsonArray("friendName");
        }
        if (response.body().has("position")) {
            positionList = response.body().getAsJsonArray("position");
        }
        if (response.body().has("contactTime")) {
            contactTimeList = response.body().getAsJsonArray("contactTime");
        }
        int age_friend;

        for (int i = 0; i < friendIDList.size(); ++i) {
            String friendID = friendIDList.get(i).toString();
            friendID = friendID.substring(1, friendID.length() - 1);

            String friendName = friendNameList.get(i).toString();
            friendName = friendName.substring(1, friendName.length() - 1);
            JsonArray positions = positionList.get(i).getAsJsonArray();
            String srcPosition = positions.get(0).toString();
            srcPosition = srcPosition.substring(3, srcPosition.length() - 3);
            String destPosition = positions.get(1).toString();
            destPosition = destPosition.substring(3, destPosition.length() - 3);
//            StringBuilder contactTime = new StringBuilder();
            String contactTime = contactTimeList.get(i).getAsJsonArray().get(0).getAsString();
//            for (JsonElement t: contactTimeList.get(i).getAsJsonArray()) {
//                String time = t.getAsString();
//                contactTime.append(time);
//            }

            items.add(new Item(friendID, friendName, srcPosition, destPosition, contactTime.toString(), friendName));
        }
    }

    private void addProfileToItem() {
        for (int i = 0; i < items.size(); ++i) {
            String friendID = items.get(i).getId();
            items.get(i).setProfile(profiles.get(friendID));
        }
    }

    private void setList() {
        // add custom btn handler to first list item
//        items.get(0).setRequestBtnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                Intent intent = new Intent(getActivity(), AccountActivity.class);
//                startActivity(intent);
//            }
//        });

        // create custom adapter that holds elements and their state (we need hold a id's of unfolded elements for reusable elements)
        final FoldingCellListAdapter adapter = new FoldingCellListAdapter(this.getContext(), items);

        // add default btn handler for each request btn on each item if custom handler not found
        adapter.setDefaultRequestBtnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), AccountActivity.class);
                startActivity(intent);
                getActivity().finish();
            }
        });

        // set elements to adapter
        theListView.setAdapter(adapter);

        // set on click event listener to list view
        theListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int pos, long l) {
                // toggle clicked cell state
                ((FoldingCell) view).toggle(false);
                // register in adapter that state for selected cell is toggled
                adapter.registerToggle(pos);
            }
        });
    }

}