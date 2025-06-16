package com.isaru66.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class UploadSuccessResponse {

  public UploadSuccessResponse(String string, String url) {
    this.statusMessage = string;
    this.url = url;
  }

  @JsonProperty("statusMessage")
  private String statusMessage;

  @JsonProperty("url")
  private String url;
}
