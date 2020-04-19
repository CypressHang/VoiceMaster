package com.example.voicemaster.ui.home;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class HomeViewModel extends ViewModel {

    private MutableLiveData<String> mText;

    public HomeViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("欢迎使用\n没有图片了\n\n假装主界面很好看");

    }

    public LiveData<String> getText() {
        return mText;
    }
}