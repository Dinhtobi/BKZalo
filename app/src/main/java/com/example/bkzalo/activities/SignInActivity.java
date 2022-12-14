package com.example.bkzalo.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.example.bkzalo.API.SignInAPI;
import com.example.bkzalo.databinding.ActivitySignInBinding;
import com.example.bkzalo.models.UserModel;
import com.example.bkzalo.utilities.Constants;
import com.example.bkzalo.utilities.PreferenceManager;


import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class SignInActivity extends AppCompatActivity {

    public ActivitySignInBinding binding;
    private PreferenceManager preferenceManager;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        System.out.println("vitri 1");
        preferenceManager = new PreferenceManager(getApplicationContext());
      //  CheckLogout();
        if(preferenceManager.getBoolean(Constants.KEY_IS_SIGNED_IN)) {
            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
            startActivity(intent);
            finish();
            System.out.println("vitri 2");
        }
        binding = ActivitySignInBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setListeners();
    }
    private void setListeners(){
        binding.textCreateNewAccount.setOnClickListener( v ->
                startActivity(new Intent(getApplicationContext() , SignUpActivity.class)));
        binding.buttonSignIn.setOnClickListener(v -> {
            if(isValidSignInDetails()) {
                signIn();
            }
        });
    }

    private void signIn() {
        loading(true);
        UserModel us = new UserModel();
        us.setEmail(binding.inputEmail.getText().toString());
        us.setPassword(binding.inputPassword.getText().toString());
        SignInAPI.signinAPI.sendPost(us)
                .enqueue(new Callback<UserModel>() {
                    @Override
                    public void onResponse(Call<UserModel> call, Response<UserModel> response) {
                        loading(false);
                        if(response.body() != null){
                        UserModel userresponse = response.body();
                        //l??u v??o b??? nh??? ?????m?
                        preferenceManager.putBoolean(Constants.KEY_IS_SIGNED_IN, true);
                        preferenceManager.putString(Constants.KEY_USER_ID, userresponse.getId().toString());
                        preferenceManager.putString(Constants.KEY_NAME, userresponse.getTen());
                        preferenceManager.putString(Constants.KEY_IMAGE, userresponse.getUrl());
                        preferenceManager.putString(Constants.KEY_EMAIL,userresponse.getEmail());
                        preferenceManager.putString(Constants.KEY_PASSWORD, userresponse.getPassword());
                        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                    }
                    }

                    @Override
                    public void onFailure(Call<UserModel> call, Throwable t) {
                        loading(false);
                        showToast("Sai Email v?? m???t kh???u");
                    }
                });
    }
    public void CheckLogout(){
        SharedPreferences sp = getSharedPreferences("DangNhap" , MODE_PRIVATE);
        if(sp.contains("Logout")){
            showToast(sp.getString("Logout",""));
            SharedPreferences.Editor editor = sp.edit();
            editor.remove("Logout");
            editor.commit();
        }
    }
    private void loading(Boolean isLoading){
        if(isLoading){
            binding.buttonSignIn.setVisibility(View.INVISIBLE);
            binding.progressBar.setVisibility(View.VISIBLE);
        }else {
            binding.progressBar.setVisibility(View.INVISIBLE);
            binding.buttonSignIn.setVisibility(View.VISIBLE);
        }
    }
    //th??ng b??o
    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }
    // ki???m tra ????ng nh???p
    private Boolean isValidSignInDetails() {
        if(binding.inputEmail.getText().toString().trim().isEmpty()) {
            showToast("Nh???p Email");
            return false;
        }
//        else if(!Patterns.EMAIL_ADDRESS.matcher(binding.inputEmail.getText().toString()).matches()) {
//            showToast("X??c th???c Email");
//            return false;
//        }
        else if(binding.inputPassword.getText().toString().trim().isEmpty()) {
            showToast("Nh???p m???t kh???u");
            return false;
        }else {
            return true;
        }
    }
}