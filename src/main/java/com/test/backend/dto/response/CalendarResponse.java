package com.test.backend.dto.response;

import java.util.List;
import java.util.Map;

public record CalendarResponse(Map<String, List<TodoResponse>> data) {}
