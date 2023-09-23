package com.atypon.loadpalncer.consistenthashing;

public interface IConsistentHashing<T> {
    int hash(String key);

    void add(String node);


    String get(String key);


}
