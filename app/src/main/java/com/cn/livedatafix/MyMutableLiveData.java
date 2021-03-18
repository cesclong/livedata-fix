package com.cn.livedatafix;

import android.arch.lifecycle.LifecycleOwner;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.Observer;
import android.support.annotation.NonNull;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;

public class MyMutableLiveData<T> extends MutableLiveData<T> {

    @Override
    public void observe(@NonNull LifecycleOwner owner, @NonNull Observer<T> observer) {
        super.observe(owner, observer);
        try {
            hook(observer);
        } catch (Exception e) {

        }
    }

    private void hook(Observer<? super T> observer) throws Exception {
        //reflect field mObservers
        Class<LiveData> liveDataClazz = LiveData.class;
        Field mObserversField = liveDataClazz.getDeclaredField("mObservers");
        mObserversField.setAccessible(true);

        //reflect get getter method
        Object mObservers = mObserversField.get(this);
        Class<?> mObserversClazz = mObservers.getClass();
        Method methodGet = mObserversClazz.getDeclaredMethod("get", Object.class);
        methodGet.setAccessible(true);

        Object entry = methodGet.invoke(mObservers, observer);

        Object observerWrapper = ((Map.Entry) entry).getValue();
        Class<?> mObserver = observerWrapper.getClass().getSuperclass();//observer

        //set mLastVersion'value equals to mVersion'value
        Field mLastVersionField = mObserver.getDeclaredField("mLastVersion");
        mLastVersionField.setAccessible(true);
        Field mVersionField = mObserver.getDeclaredField("mVersion");
        mVersionField.setAccessible(true);

        Object mVersionObject = mVersionField.get(this);
        mLastVersionField.set(observerWrapper, mVersionObject);
    }

//    private void otherHook(Observer<? super T> observer)  throws Exception{
//        Class<LiveData> liveDataClass = LiveData.class;
//        Field fieldmObservers = liveDataClass.getDeclaredField("mObservers");
//        fieldmObservers.setAccessible(true);
//        Object mObservers = fieldmObservers.get(this);
//        Class<?> mObserversClass = mObservers.getClass();
//
//        Method methodget = mObserversClass.getDeclaredMethod("get", Object.class);
//        methodget.setAccessible(true);
//        Object entry = methodget.invoke(mObservers, observer);
//        Object observerWrapper = ((Map.Entry) entry).getValue();
//        Class<?> mObserver = observerWrapper.getClass().getSuperclass();//observer
//
//        Field mLastVersion = mObserver.getDeclaredField("mLastVersion");
//        mLastVersion.setAccessible(true);
//        Field mVersion = liveDataClass.getDeclaredField("mVersion");
//        mVersion.setAccessible(true);
//        Object mVersionObject = mVersion.get(this);
//        mLastVersion.set(observerWrapper,mVersionObject);
//    }

}
