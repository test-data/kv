package ru.kv.server;


import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class KeyValueMap {

    private final Map<Long, String> map;

    public KeyValueMap () {
        //this.map = new ConcurrentHashMap<>(16, 0.9f, 1);
        this.map = new ConcurrentHashMap<>(80, 0.85f, 3);
        //this.map = new HashMap<>(16, 0.9f, 1);
    }

    public OpResult get(long key) {
        ReturnCode code;
        final String value = map.get(key);
        if (value == null) {
            code = ReturnCode.KEY_DOES_NOT_EXIST;
        } else {
            code = ReturnCode.SUCCESS;
        }

        return new OpResult(code, value);
    }

    public OpResult put(Long key, String value) {
        map.put(key, value);
        return new OpResult(ReturnCode.SUCCESS, "");
    }

    public static class OpResult {
        public final ReturnCode returnCode;
        public final String data;

        OpResult(final ReturnCode returnCode, final String data) {
            this.returnCode = returnCode;
            this.data = data;
        }
    }

    public enum ReturnCode {
        SUCCESS,
        KEY_DOES_NOT_EXIST
    }
}
