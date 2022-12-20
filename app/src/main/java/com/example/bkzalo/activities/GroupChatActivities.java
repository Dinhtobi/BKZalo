package com.example.bkzalo.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.example.bkzalo.API.BoxMessageAPI;
import com.example.bkzalo.API.ListMemberGroupAPI;
import com.example.bkzalo.API.ListMessageAPI;
import com.example.bkzalo.API.MessageAPI;
import com.example.bkzalo.API.SetOnlineAPI;
import com.example.bkzalo.adapters.GroupChatAdapter;
import com.example.bkzalo.databinding.ActivityGroupChatActivitiesBinding;
import com.example.bkzalo.models.BoxLastMessage;
import com.example.bkzalo.models.Chat;
import com.example.bkzalo.models.Group;
import com.example.bkzalo.models.UserModel;
import com.example.bkzalo.utilities.Constants;
import com.example.bkzalo.utilities.PreferenceManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class GroupChatActivities extends AppCompatActivity {
    private  final  int NOTIFICATION_ID = 1;
    private ActivityGroupChatActivitiesBinding binding;
    private Group groupreceived;
    private List<Chat> chatMessages;
    private GroupChatAdapter groupChatAdapter;
    private PreferenceManager preferenceManager;
    private String conversionId = null;
    private int sizesend  ;
    private  int sizereceid;
    private Timer timer;
    private TimerTask task ;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityGroupChatActivitiesBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setListeners();
        loadUserReceive();
        init();
        Reload();
    }
    private void Reload(){
        timer = new Timer();
        task = new TimerTask() {
            @Override
            public void run() {
                listenAvailabilityOfReceiver();
                listenMessage();
            }
        };

        timer.scheduleAtFixedRate(task, 0, 1000);
    }
    private void init(){
        preferenceManager = new PreferenceManager(getApplicationContext());
        chatMessages = new ArrayList<>();
        groupChatAdapter = new GroupChatAdapter(
                chatMessages,
                preferenceManager.getString(Constants.KEY_USER_ID)
        );
        binding.chatRecyclerView.setAdapter(groupChatAdapter);
    }

    private void sendMessage() {
        Chat mes = new Chat();
        mes.setId_nguoigui(Long.parseLong(preferenceManager.getString(Constants.KEY_USER_ID)));
        mes.setId_nhomchat(groupreceived.getId_nhomchat());
        Date dnow = new Date();
        SimpleDateFormat ft = new SimpleDateFormat("yyyy.MM.dd 'at' hh:mm:ss");
        mes.setNoidung(binding.inputMessage.getText().toString());
        mes.setThoigiantao(ft.format(dnow));
        MessageAPI.messageAPI.SendChat(mes).enqueue(new Callback<Chat>() {
            @Override
            public void onResponse(Call<Chat> call, Response<Chat> response) {

            }

            @Override
            public void onFailure(Call<Chat> call, Throwable t) {
                showToast("Fail");
            }
        });
        if (conversionId != null) {
            BoxLastMessage box = new BoxLastMessage() ;
            box.setId_nguoigui(Long.parseLong(preferenceManager.getString(Constants.KEY_USER_ID)));
            box.setId_hopchat(Long.parseLong(conversionId));
            box.setTinnhancuoi(binding.inputMessage.getText().toString());
            box.setThoigiantao(ft.format(dnow));
            updateConversion(box);
        }else {
            BoxLastMessage box = new BoxLastMessage();
            box.setId_nguoigui(Long.parseLong(preferenceManager.getString(Constants.KEY_USER_ID)));
            box.setId_nhomchat(groupreceived.getId_nhomchat());
            box.setTinnhancuoi(binding.inputMessage.getText().toString());
            box.setThoigiantao(ft.format(dnow));
            box.setType("Add");
            addConversion(box);
        }
        binding.inputMessage.setText(null);
    }

    private void listenAvailabilityOfReceiver() {
        SetOnlineAPI.setOnlineapi.ListOnl().enqueue(new Callback<List<UserModel>>() {
            @Override
            public void onResponse(Call<List<UserModel>> call, Response<List<UserModel>> response) {
                List<UserModel> list = response.body();
                groupreceived = (Group) getIntent().getSerializableExtra(Constants.KEY_GROUP);

                ListMemberGroupAPI.listMemberGroupApi.listmember(groupreceived).enqueue(new Callback<List<UserModel>>() {
                    @Override
                    public void onResponse(Call<List<UserModel>> call, Response<List<UserModel>> response) {
                        List<UserModel> listuser = response.body();
                            if(CheckOnl(list, listuser)){
                                binding.textAvailability.setVisibility(View.VISIBLE);
                            }
                            else {
                                binding.textAvailability.setVisibility(View.GONE);
                            }
                    }

                    @Override
                    public void onFailure(Call<List<UserModel>> call, Throwable t) {

                    }
                });

            }

            @Override
            public void onFailure(Call<List<UserModel>> call, Throwable t) {

            }
        });
    }
    private boolean CheckOnl(List<UserModel> listuser, List<UserModel> listmember){
        boolean onl = false;
        for(UserModel i : listuser){
            if(onl){
                break;
            }
            for(UserModel j : listmember){
                if( i.getId().equals(j.getId()) && i.getTrangthai() == 1 && !j.getId().toString().equals(preferenceManager.getString(Constants.KEY_USER_ID)) ){
                    onl = true;
                    break;
                }
            }
        }
        return  onl;
    }
    private void listenMessage() {
        Chat mes = new Chat();
        mes.setId_nguoigui(Long.parseLong(preferenceManager.getString(Constants.KEY_USER_ID)));
        mes.setId_nhomchat(groupreceived.getId_nhomchat());
        ListMessageAPI.listmessageapi.ListMes(mes).enqueue(new Callback<List<Chat>>() {
            @Override
            public void onResponse(Call<List<Chat>> call, Response<List<Chat>> response) {
                List<Chat> listchat = response.body();
                sizesend = listchat.size();
                eventListener(listchat);
            }
            @Override
            public void onFailure(Call<List<Chat>> call, Throwable t) {
                showToast("Fail");
            }
        });
        groupreceived = (Group) getIntent().getSerializableExtra(Constants.KEY_GROUP);
        String currentUserId = preferenceManager.getString(Constants.KEY_USER_ID);
        ListMemberGroupAPI.listMemberGroupApi.listmember(groupreceived).enqueue(new Callback<List<UserModel>>() {
            @Override
            public void onResponse(Call<List<UserModel>> call, Response<List<UserModel>> response) {
                List<UserModel> list = response.body();
                for (UserModel i : list) {
                    if (currentUserId.equals(i.getId().toString())) {
                        continue;
                    } else {
                        Chat mes2 = new Chat();
                        mes2.setId_nguoigui(i.getId());
                        mes2.setId_nhomchat(groupreceived.getId_nhomchat());
                        ListMessageAPI.listmessageapi.ListMes(mes2).enqueue(new Callback<List<Chat>>() {
                            @Override
                            public void onResponse(Call<List<Chat>> call, Response<List<Chat>> response) {
                                List<Chat> listchat = response.body();
                                sizereceid += listchat.size();
                                eventListener(listchat);
                            }

                            @Override
                            public void onFailure(Call<List<Chat>> call, Throwable t) {
                                showToast("Fail");
                            }
                        });
                    }

                }
            }

            @Override
            public void onFailure(Call<List<UserModel>> call, Throwable t) {

            }
        });

    }
    private  void eventListener(List<Chat> chat) {
        int count = chatMessages.size();
        int size = sizesend + sizereceid;
        if(chatMessages.size() == size){
        }else {
            if (chat != null) {
                for (Chat i : chat) {
                    Chat chatMessage = new Chat();
                    chatMessage.setId_tinnhan(i.getId_tinnhan());
                    chatMessage.setId_nguoigui(i.getId_nguoigui());
                    chatMessage.setId_nguoinhan(i.getId_nguoinhan());
                    chatMessage.setNoidung(i.getNoidung());
                    chatMessage.setThoigiantao(i.getThoigiantao());
                    if(CheckMes(chatMessage)){
                        chatMessages.add(chatMessage);
                    }
                }
                Collections.sort(chatMessages, (obj1, obj2) -> obj1.getThoigiantao().compareTo(obj2.getThoigiantao()));
                if (count == 0) {
                    groupChatAdapter.notifyDataSetChanged();
                } else {
                    groupChatAdapter.notifyItemRangeInserted(chatMessages.size(), chatMessages.size());
                    binding.chatRecyclerView.smoothScrollToPosition(chatMessages.size() - 1);
                }
                binding.chatRecyclerView.setVisibility(View.VISIBLE);
            }
            binding.progressBar.setVisibility(View.GONE);
            if (conversionId == null) {
                checkForConversion();
            }
        }
    }

    private Bitmap getBitmapFromEncodedString (String encodedImage) {
        byte[] bytes = android.util.Base64.decode(encodedImage, android.util.Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }

    private void loadUserReceive() {
        groupreceived = (Group) getIntent().getSerializableExtra(Constants.KEY_GROUP);
        binding.textName.setText(groupreceived.getTennhom());
        List<UserModel> listuser = new ArrayList<>();
        ListMemberGroupAPI.listMemberGroupApi.listmember(groupreceived).enqueue(new Callback<List<UserModel>>() {
            @Override
            public void onResponse(Call<List<UserModel>> call, Response<List<UserModel>> response) {
                List<UserModel> list = response.body();
                for(int i = 0 ; i<list.size(); i++){
                    UserModel newmember = new UserModel();
                    newmember.setTen(list.get(i).getTen());
                    newmember.setId(list.get(i).getId());
                    newmember.setUrl(list.get(i).getUrl());
                    newmember.setTrangthai(list.get(i).getTrangthai());
                    listuser.add(newmember);
                }
                groupChatAdapter.setData(listuser);
            }

            @Override
            public void onFailure(Call<List<UserModel>> call, Throwable t) {

            }
        });
    }

    private void setListeners() {
        binding.imageBack.setOnClickListener(v -> onBackPressed());
        binding.layoutSend.setOnClickListener(v -> sendMessage());
        binding.imageInfo.setOnClickListener(v->Info());
    }
    private void Info(){
        Intent intent = new Intent(getApplicationContext(), InfoGroup.class);
        startActivity(intent);
    }
    @Override
    public void onBackPressed() {

        int count = getSupportFragmentManager().getBackStackEntryCount();

        if (count == 0) {
            super.onBackPressed();
            timer.cancel();
            task.cancel();
        } else {
            getSupportFragmentManager().popBackStack();
        }
    }

    private void addConversion(BoxLastMessage conversion) {
        BoxMessageAPI.boxmessageAPI.converBox(conversion).enqueue(new Callback<BoxLastMessage>() {
            @Override
            public void onResponse(Call<BoxLastMessage> call, Response<BoxLastMessage> response) {
                showToast("Success!");
            }

            @Override
            public void onFailure(Call<BoxLastMessage> call, Throwable t) {
                showToast("Lỗi dữ liệu!");
            }
        });
    }

    private void updateConversion(BoxLastMessage boxLastMessage) {
        BoxMessageAPI.boxmessageAPI.Update(boxLastMessage).enqueue(new Callback<BoxLastMessage>() {
            @Override
            public void onResponse(Call<BoxLastMessage> call, Response<BoxLastMessage> response) {
                showToast("Success!");
            }

            @Override
            public void onFailure(Call<BoxLastMessage> call, Throwable t) {
                showToast("Lỗi dữ liệu!");
            }
        });
    }

    private void  checkForConversion() {
        if (chatMessages.size() != 0) {
            checkForConversionRemotely(
                    groupreceived.getId_nhomchat().toString()
            );

        }
    }

    private void checkForConversionRemotely( String groupreceived) {
        BoxLastMessage last = new BoxLastMessage();
        last.setId_nhomchat(Long.parseLong(groupreceived));
        last.setType("Check");
        BoxMessageAPI.boxmessageAPI.converBox(last).enqueue(new Callback<BoxLastMessage>() {
            @Override
            public void onResponse(Call<BoxLastMessage> call, Response<BoxLastMessage> response) {
                BoxLastMessage box = response.body();
                conversion(box);
            }

            @Override
            public void onFailure(Call<BoxLastMessage> call, Throwable t) {
                showToast("Lỗi tải dữ liệu!");
            }
        });
    }

    private final void  conversion(BoxLastMessage boxLastMessage){
        if (boxLastMessage != null) {
            BoxLastMessage box = boxLastMessage;
            conversionId = box.getId_hopchat().toString();
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        listenAvailabilityOfReceiver();
    }
    private void showToast(String message){
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }
    private boolean CheckMes(Chat chat){
        boolean add = true ;
        for (int i = 0 ; i < chatMessages.size() ; i++){
            if(chatMessages.get(i).getId_tinnhan() == chat.getId_tinnhan() ){
                add = false ;
                break;
            }
        }
        return add;
    }
}