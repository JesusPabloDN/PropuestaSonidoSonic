package com.example.propuestasonidosonic.data;

public interface Callback<T> {
    void onSuccess(T result);
    void onError(String mensaje);
}
