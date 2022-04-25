package ru.kv.client;

public class KVClientRequest {

    private final Long key;
    private final String value;


    public KVClientRequest(final Long key, final String value) {
       // if value == null when request get
        if (key == null ) {
            throw new IllegalArgumentException("Invalid arguments for request");
        }
        this.key = key;
        this.value = value;
    }


    public Long getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

}