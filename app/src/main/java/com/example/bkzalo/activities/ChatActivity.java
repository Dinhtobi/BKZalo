package com.example.bkzalo.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.bkzalo.API.BoxMessageAPI;
import com.example.bkzalo.API.ListMessageAPI;
import com.example.bkzalo.API.MessageAPI;
import com.example.bkzalo.API.SetOnlineAPI;
import com.example.bkzalo.adapters.ChatAdapter;
import com.example.bkzalo.databinding.ActivityChatBinding;
import com.example.bkzalo.models.BoxLastMessage;
import com.example.bkzalo.models.Message;
import com.example.bkzalo.models.UserModel;
import com.example.bkzalo.utilities.Constants;
import com.example.bkzalo.utilities.PreferenceManager;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
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

public class ChatActivity extends AppCompatActivity  {
    private ActivityChatBinding binding;
    private UserModel receiverUser;
    private List<Message> chatMessages;
    private ChatAdapter chatAdapter;
    private PreferenceManager preferenceManager;
    private String conversionId = null;
    private int sizesend  ;
    private  int sizereceid;
    private  Timer timer;
    private TimerTask task ;
    private String encodedImage;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setListeners();
        loadReceiverDetails();
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
        chatAdapter = new ChatAdapter(
                chatMessages,
                preferenceManager.getString(Constants.KEY_USER_ID),
                getBitmapFromEncodedString(receiverUser.getUrl())
        );
        binding.chatRecyclerView.setAdapter(chatAdapter);
    }

    private void sendMessage(int fileformat) {
        Message mes = new Message();
        mes.setId_nguoigui(Long.parseLong(preferenceManager.getString(Constants.KEY_USER_ID)));
        mes.setId_nguoinhan(receiverUser.getId());
        Date dnow = new Date();
        SimpleDateFormat ft = new SimpleDateFormat("yyyy.MM.dd 'at' hh:mm:ss");
        mes.setNoidung(binding.inputMessage.getText().toString());
        mes.setThoigiantao(ft.format(dnow));
        mes.setFileformat(fileformat);
        MessageAPI.messageAPI.SendChat(mes).enqueue(new Callback<Message>() {
            @Override
            public void onResponse(Call<Message> call, Response<Message> response) {

            }

            @Override
            public void onFailure(Call<Message> call, Throwable t) {
                showToast("Fail");
            }
        });
        if (conversionId != null) {
            BoxLastMessage box = new BoxLastMessage() ;
            box.setId_hopchat(Long.parseLong(conversionId));
            if(fileformat == 1){
                box.setTinnhancuoi( preferenceManager.getString(Constants.KEY_NAME)+" ???? g???i 1 ???nh");
            }else{
                box.setTinnhancuoi(binding.inputMessage.getText().toString());
            }
            box.setThoigiantao(ft.format(dnow));
            updateConversion(box);
        }else {
            BoxLastMessage box = new BoxLastMessage();
            box.setId_nguoigui(Long.parseLong(preferenceManager.getString(Constants.KEY_USER_ID)));
            box.setTensender(preferenceManager.getString(Constants.KEY_NAME));
            box.setUrlsender(preferenceManager.getString(Constants.KEY_IMAGE));
            box.setId_nguoinhan(receiverUser.getId());
            box.setTenreceider(receiverUser.getTen());
            box.setUrlreceider(receiverUser.getUrl());
            if(fileformat == 1){
                box.setTinnhancuoi(preferenceManager.getString(Constants.KEY_NAME)+" ???? g???i 1 ???nh");
            }else{
                box.setTinnhancuoi(binding.inputMessage.getText().toString());
            }
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
                    if(CheckOnl(list)){
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
    private boolean CheckOnl(List<UserModel> list){
        boolean onl = false;
        for(UserModel i : list){
            if(i.getId().toString().equals(receiverUser.getId().toString()) && i.getTrangthai() == 1 ){
                onl = true;
                break;
            }
        }
        return  onl;
    }
    private void listenMessage() {
        Message mes = new Message();
        mes.setId_nguoigui(Long.parseLong(preferenceManager.getString(Constants.KEY_USER_ID)));
        mes.setId_nguoinhan(receiverUser.getId());
        ListMessageAPI.listmessageapi.ListMes(mes).enqueue(new Callback<List<Message>>() {
            @Override
            public void onResponse(Call<List<Message>> call, Response<List<Message>> response) {
                List<Message> listchat = response.body();
                sizesend = listchat.size();
                 eventListener(listchat);
            }
            @Override
            public void onFailure(Call<List<Message>> call, Throwable t) {
                showToast("Fail");
            }
        });
        Message mes2 = new Message();
        mes2.setId_nguoigui(receiverUser.getId());
        mes2.setId_nguoinhan(Long.parseLong(preferenceManager.getString(Constants.KEY_USER_ID)));
        ListMessageAPI.listmessageapi.ListMes(mes2).enqueue(new Callback<List<Message>>() {
            @Override
            public void onResponse(Call<List<Message>> call, Response<List<Message>> response) {
                List<Message> listchat = response.body();
                    sizereceid = listchat.size();
                    eventListener(listchat);
            }

            @Override
            public void onFailure(Call<List<Message>> call, Throwable t) {
                showToast("Fail");
            }
        });
    }
    private  void eventListener(List<Message> chat) {
        int count = chatMessages.size();
        int size = sizesend + sizereceid;
            if(chatMessages.size() == size){

        }else {
            if (chat != null) {
                for (Message i : chat) {
                    Message chatMessage = new Message();
                    chatMessage.setId_tinnhan(i.getId_tinnhan());
                    chatMessage.setId_nguoigui(i.getId_nguoigui());
                    chatMessage.setId_nguoinhan(i.getId_nguoinhan());
                    chatMessage.setNoidung(i.getNoidung());
                    chatMessage.setThoigiantao(i.getThoigiantao());
                    chatMessage.setFileformat(i.getFileformat());
                    if(CheckMes(chatMessage)){
                        chatMessages.add(chatMessage);
                    }
                }
                Collections.sort(chatMessages, (obj1, obj2) -> obj1.getThoigiantao().compareTo(obj2.getThoigiantao()));
                if (count == 0) {
                    chatAdapter.notifyDataSetChanged();
                } else {
                    chatAdapter.notifyItemRangeInserted(chatMessages.size(), chatMessages.size());
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

    private void loadReceiverDetails() {
        receiverUser = (UserModel) getIntent().getSerializableExtra(Constants.KEY_USERMODEL);
        binding.textName.setText(receiverUser.getTen());
    }

    private void setListeners() {
        binding.imageBack.setOnClickListener(v -> onBackPressed());
        binding.layoutSend.setOnClickListener(v -> sendMessage(0));
        binding.upload.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            pickImage.launch(intent);
        });
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
                showToast("L???i d??? li???u!");
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
                showToast("L???i d??? li???u!");
            }
        });
    }

    private void  checkForConversion() {
        if (chatMessages.size() != 0) {
            checkForConversionRemotely(
                    preferenceManager.getString(Constants.KEY_USER_ID),
                    receiverUser.getId().toString()
            );
            checkForConversionRemotely(
                    receiverUser.getId().toString(),
                    preferenceManager.getString(Constants.KEY_USER_ID)
            );
        }
    }

    private void checkForConversionRemotely(String senderId, String receiverId) {
       BoxLastMessage last = new BoxLastMessage();
       last.setId_nguoigui(Long.parseLong(senderId));
       last.setId_nguoinhan(Long.parseLong(receiverId));
       last.setType("Check");
       BoxMessageAPI.boxmessageAPI.converBox(last).enqueue(new Callback<BoxLastMessage>() {
           @Override
           public void onResponse(Call<BoxLastMessage> call, Response<BoxLastMessage> response) {
                BoxLastMessage box = response.body();
                conversion(box);
           }

           @Override
           public void onFailure(Call<BoxLastMessage> call, Throwable t) {
                showToast("L???i t???i d??? li???u!");
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
    private boolean CheckMes(Message chat){
            boolean add = true ;
            for (int i = 0 ; i < chatMessages.size() ; i++){
                if(chatMessages.get(i).getId_tinnhan() == chat.getId_tinnhan() ){
                    add = false ;
                    break;
                }
            }
            return add;
    }
    private  String encodedImage(Bitmap bitmap){
        int previewWidth = 150;
        int previewHeight = bitmap.getHeight() * previewWidth / bitmap.getWidth();
        Bitmap previewBitmap = Bitmap.createScaledBitmap(bitmap, previewWidth, previewHeight, false);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        previewBitmap.compress(Bitmap.CompressFormat.JPEG, 50, byteArrayOutputStream);
        byte[] bytes = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(bytes, Base64.DEFAULT);
    }
    private final ActivityResultLauncher<Intent> pickImage = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if(result.getResultCode() == RESULT_OK) {
                    if(result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        try{
                            InputStream inputStream = getContentResolver().openInputStream(imageUri);
                            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                            encodedImage = encodedImage(bitmap);
                            binding.inputMessage.setText(encodedImage);
                            sendMessage(1);
                        }catch (FileNotFoundException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
    );
}