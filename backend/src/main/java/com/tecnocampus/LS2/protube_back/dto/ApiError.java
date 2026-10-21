package com.tecnocampus.LS2.protube_back.dto;

import java.util.Map;

public record ApiError(String message, Map<String, String> fieldErrors) {}
