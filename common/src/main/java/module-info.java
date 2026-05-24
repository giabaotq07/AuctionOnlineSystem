module app.common {
  requires transitive com.google.gson;

  exports app.common.dto;
  exports app.common.enums;
  exports app.common.exception;
  exports app.common.mapper;
  exports app.common.models;
  exports app.common.protocol;
  exports app.common.utils;

  opens app.common.dto to
      com.google.gson;
  opens app.common.enums to
      com.google.gson;
  opens app.common.models to
      com.google.gson;
  opens app.common.protocol to
      com.google.gson;
}
