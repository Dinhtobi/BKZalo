package com.example.bkzalo.activities;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.View;
import android.widget.Toast;


import com.example.bkzalo.API.UserAPI;
import com.example.bkzalo.databinding.ActivityProfileUserBinding;
import com.example.bkzalo.models.UserModel;
import com.example.bkzalo.utilities.Constants;
import com.example.bkzalo.utilities.PreferenceManager;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileUserActivity extends AppCompatActivity {
    private ActivityProfileUserBinding binding;
    private String encodedImage;
    private PreferenceManager preferenceManager;
    private Boolean passwordvisible = true;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProfileUserBinding.inflate(getLayoutInflater());
        preferenceManager = new PreferenceManager(getApplicationContext());
        setContentView(binding.getRoot());
        binding.imageBack.setEnabled(false);
        LoadprofileUser();
        setListener();

    }
    private void LoadprofileUser(){
        binding.inputName.setText(preferenceManager.getString(Constants.KEY_NAME));
        binding.inputemail.setText(preferenceManager.getString(Constants.KEY_EMAIL));
        binding.inputPassword.setText(preferenceManager.getString(Constants.KEY_PASSWORD));
        byte[] bytes = android.util.Base64.decode(preferenceManager.getString(Constants.KEY_IMAGE), Base64.DEFAULT);
        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        binding.imageProfile.setImageBitmap(bitmap);
        encodedImage = encodedImage(bitmap);
        binding.inputConfirmPassword.setText(preferenceManager.getString(Constants.KEY_PASSWORD));
    }
    private void setListener(){
        binding.imageupdate.setOnClickListener(v->UpdateProfileClick());
        binding.imageclose.setOnClickListener(v->CloseClick());
        binding.imageback.setOnClickListener(v-> onBackPressed());
        binding.imageadd.setOnClickListener(v -> {
                if(isValidSignUpDetails()) {
                Update();
                CloseClick();
                }
            });
    }

    private void UpdateProfileClick() {
        binding.imageadd.setVisibility(View.VISIBLE);
        binding.imageupdate.setVisibility(View.GONE);
        binding.imageback.setVisibility(View.GONE);
        binding.imageclose.setVisibility(View.VISIBLE);
        binding.layoutImage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            pickImage.launch(intent);
        });
        binding.Nickname.setEnabled(true);
        binding.Email.setEnabled(true);
        binding.Password.setEnabled(true);
        binding.layoutImage.setEnabled(true);
        binding.inputPassword.setOnClickListener(v->ShowComfirm());

    }
    private void ShowComfirm(){
        binding.ConfirmPassword.setVisibility(View.VISIBLE);
    }
    private void CloseClick(){
        binding.imageadd.setVisibility(View.GONE);
        binding.imageclose.setVisibility(View.GONE);
        binding.imageback.setVisibility(View.VISIBLE);
        binding.imageupdate.setVisibility(View.VISIBLE);
        binding.Nickname.setEnabled(false);
        binding.Email.setEnabled(false);
        binding.Password.setEnabled(false);
       binding.layoutImage.setEnabled(false);
       binding.ConfirmPassword.setVisibility(View.GONE);
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
                            binding.imageProfile.setImageBitmap(bitmap);
                            encodedImage = encodedImage(bitmap);
                        }catch (FileNotFoundException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
    );
    private void Update(){
        UserModel us = new UserModel();
        us.setId(Long.parseLong(preferenceManager.getString(Constants.KEY_USER_ID)));
        us.setUrl(encodedImage);
        us.setTen(binding.inputName.getText().toString());
        us.setEmail(binding.inputemail.getText().toString());
        us.setPassword(binding.inputPassword.getText().toString());
        UserAPI.userAPI.sendPut(us).enqueue(new Callback<UserModel>() {
            @Override
            public void onResponse(Call<UserModel> call, Response<UserModel> response) {
                showToast("C???p nh???t th??nh c??ng");
                UserModel us = response.body();
                preferenceManager.clear();
                preferenceManager.putBoolean(Constants.KEY_IS_SIGNED_IN, true);
                preferenceManager.putString(Constants.KEY_USER_ID, us.getId().toString());
                preferenceManager.putString(Constants.KEY_NAME, binding.inputName.getText().toString());
                preferenceManager.putString(Constants.KEY_IMAGE, us.getUrl());
                preferenceManager.putString(Constants.KEY_EMAIL,us.getEmail());
                preferenceManager.putString(Constants.KEY_PASSWORD, us.getPassword());
                LoadprofileUser();
            }
            @Override
            public void onFailure(Call<UserModel> call, Throwable t) {
                showToast("L???i m??y ch???");
            }
        });

    }
    private Boolean isValidSignUpDetails(){

        if(binding.inputName.getText().toString().trim().isEmpty()){
            showToast("Nh???p T??n");
            return  false;
        }else if(binding.inputemail.getText().toString().trim().isEmpty()) {
            showToast("Nh???p S??? Email");
            return false;
        }else if(binding.inputPassword.getText().toString().trim().isEmpty()) {
            showToast("Nh???p m???t kh???u");
            return false;
        }else if(binding.inputConfirmPassword.getText().toString().trim().isEmpty()) {
            showToast("X??c th???c l???i m???t kh???u");
            return false;
        }else if(!binding.inputPassword.getText().toString().equals(binding.inputConfirmPassword.getText().toString())) {
            showToast("X??c th???c sai m???t kh???u");
            return false;
        }else {
            return  true;
        }
    }
    private void showToast(String message){
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }
}