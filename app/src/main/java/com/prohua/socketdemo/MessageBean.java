package com.prohua.socketdemo;

import java.io.Serializable;

/**
 * Created by Deep on 2018/2/12 0012.
 */

public class MessageBean implements Serializable {
    private String  id;
    private String name;
    private String msg;

    public MessageBean(String id, String name, String msg) {
        this.id = id;
        this.name = name;
        this.msg = msg;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }
}
