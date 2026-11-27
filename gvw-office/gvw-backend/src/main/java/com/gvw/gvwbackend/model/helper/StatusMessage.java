package com.gvw.gvwbackend.model.helper;

import com.gvw.gvwbackend.model.ErrorMessageType;

public record StatusMessage(String title, String subTitle, ErrorMessageType type) {}
