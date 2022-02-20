package com.picassos.noted.models;

import java.io.Serializable;

public class NotificationModel implements Serializable {

    public Long id;
    public String title;
    public String content;

    public Boolean read = false;
    public Long created_at;
}
