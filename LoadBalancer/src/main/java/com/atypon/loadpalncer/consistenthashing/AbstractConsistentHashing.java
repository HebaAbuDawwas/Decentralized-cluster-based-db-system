package com.atypon.loadpalncer.consistenthashing;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

public abstract class AbstractConsistentHashing<T> implements IConsistentHashing<T> {
    private static final SortedMap<Integer, String> circle = new TreeMap<>();

    public AbstractConsistentHashing() {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            List<String> nodes = objectMapper.readValue(new File("/usr/src/app/NodesGroup.json"), new TypeReference<>() {
            });
            for (String node : nodes) {
                add(node);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void add(String node) {
        int numberOfReplicas = 5;
        for (int i = 0; i < numberOfReplicas; i++) {
            circle.put(hash(node + i), node);
        }
    }

    public String get(String key) {
        if (circle.isEmpty()) {
            return null;
        }
        int hash = hash(key);
        if (!circle.containsKey(hash)) {
            SortedMap<Integer, String> tailMap = circle.tailMap(hash);
            hash = tailMap.isEmpty() ? circle.firstKey() : tailMap.firstKey();
        }
        return circle.get(hash);
    }


    public int hash(String key) {
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(key.getBytes());
            byte[] digest = md5.digest();
            int result = 0;
            for (int i = 0; i < 4; i++) {
                result = (result << 8) - Byte.MIN_VALUE + (int) digest[i];
            }
            return result;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 not supported", e);
        }
    }

}