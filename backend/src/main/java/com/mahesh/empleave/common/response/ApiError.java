package com.mahesh.empleave.common.response;import java.time.*;import java.util.*;public record ApiError(Instant timestamp,int status,String error,String message,Map<String,String> fields){}
