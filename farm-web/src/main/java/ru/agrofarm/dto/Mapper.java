package ru.agrofarm.dto;

import ru.agrofarm.entity.*;

import java.util.LinkedHashMap;
import java.util.Map;

/** Преобразование сущностей в JSON-структуры ответа (без служебных полей вроде хэша пароля). */
public final class Mapper {

    private Mapper() {}

    public static Map<String, Object> user(AppUser u) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", u.getId());
        m.put("fullName", u.getFullName());
        m.put("email", u.getEmail());
        m.put("createdAt", u.getCreatedAt() != null ? u.getCreatedAt().toString() : null);
        return m;
    }

    public static Map<String, Object> unit(FarmUnit u) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", u.getId());
        m.put("name", u.getName());
        m.put("type", u.getType());
        m.put("typeLabel", u.getTypeLabel());
        m.put("areaHa", u.getAreaHa());
        m.put("headCount", u.getHeadCount());
        m.put("description", u.getDescription());
        m.put("createdAt", u.getCreatedAt() != null ? u.getCreatedAt().toString() : null);
        return m;
    }

    public static Map<String, Object> category(Category c) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", c.getId());
        m.put("name", c.getName());
        m.put("type", c.getType());
        m.put("icon", c.getIcon());
        m.put("global", Boolean.TRUE.equals(c.getGlobal()));
        return m;
    }

    public static Map<String, Object> operation(Operation o) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", o.getId());
        m.put("unitId", o.getUnit().getId());
        m.put("unitName", o.getUnit().getName());
        m.put("categoryId", o.getCategory().getId());
        m.put("categoryName", o.getCategory().getName());
        m.put("categoryIcon", o.getCategory().getIcon());
        m.put("type", o.getType());
        m.put("amount", o.getAmount());
        m.put("quantity", o.getQuantity());
        m.put("quantityUnit", o.getQuantityUnit());
        m.put("description", o.getDescription());
        m.put("operationDate", o.getOperationDate().toString());
        return m;
    }

    public static Map<String, Object> recommendation(Recommendation r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", r.getId());
        m.put("userId", r.getUser().getId());
        m.put("userName", r.getUser().getFullName());
        m.put("userEmail", r.getUser().getEmail());
        m.put("topic", r.getTopic());
        m.put("message", r.getMessage());
        m.put("read", Boolean.TRUE.equals(r.getRead()));
        m.put("auto", Boolean.TRUE.equals(r.getAuto()));
        m.put("createdAt", r.getCreatedAt() != null ? r.getCreatedAt().toString() : null);
        return m;
    }
}
