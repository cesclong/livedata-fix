package com.cn.livedatafix;

import android.arch.lifecycle.LifecycleOwner;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.Observer;
import android.os.Looper;
import android.support.annotation.NonNull;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * LiveData Bus
 *
 * @author shilong
 * @since [历史 创建日期:3/19/21]
 */
public final class LiveDataBus {
    private static LiveDataBus liveDataBus;

    private final Map<String, MutableLiveData<Object>> mDataMap;

    private LiveDataBus() {
        mDataMap = new HashMap<>();
    }

    public static LiveDataBus get() {
        if (liveDataBus != null) {
            return liveDataBus;
        }

        synchronized (LiveDataBus.class) {
            if (liveDataBus == null) {
                liveDataBus = new LiveDataBus();
            }
        }
        return liveDataBus;
    }

    public void remove(String key) {
        if (mDataMap.containsKey(key)) {
            mDataMap.remove(key);
        }
    }

    public <T> MyMutableLiveData<T> with(String key, Class<T> clazz) {
        if (!mDataMap.containsKey(key)) {
            mDataMap.put(key, new MyMutableLiveData<Object>());
        }
        return (MyMutableLiveData<T>) mDataMap.get(key);
    }

    public MyMutableLiveData<Object> with(String key) {
        return with(key, Object.class);
    }

    public <T> void post(String key, T t) {
        if (Looper.getMainLooper() == Looper.myLooper()) {
            with(key).setValue(t);
        } else {
            with(key).postValue(t);
        }
    }


    // hook修改mLastVersion ，跟mVersion 相等，防止新注册的Observer 数据倒灌
    public static class MyMutableLiveData<T> extends MutableLiveData<T> {

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
    }
}
