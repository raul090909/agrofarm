package ru.agrofarm.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class AuthRequests {

    private AuthRequests() {}

    public record Register(
            @NotBlank(message = "Укажите ФИО")
            @Size(min = 2, max = 92, message = "ФИО должно содержать от 2 до 92 символов")
            @Pattern(regexp = "^[А-Яа-яЁёA-Za-z\\- ]+$", message = "ФИО может содержать только буквы, пробел и дефис")
            String fullName,

            @NotBlank(message = "Укажите email")
            @Email(message = "Некорректный email")
            @Size(max = 100, message = "Email не длиннее 100 символов")
            String email,

            @NotBlank(message = "Укажите пароль")
            @Size(min = 6, max = 64, message = "Пароль должен содержать от 6 до 64 символов")
            @Pattern(regexp = "^(?=.*[A-Za-zА-Яа-яЁё])(?=.*\\d).+$", message = "Пароль должен содержать буквы и цифры")
            String password) {}

    public record Login(
            @NotBlank(message = "Укажите email") String email,
            @NotBlank(message = "Укажите пароль") String password) {}

    public record AgronomistLogin(
            @NotBlank(message = "Укажите логин") String login,
            @NotBlank(message = "Укажите пароль") String password) {}

    public record Profile(
            @NotBlank(message = "Укажите ФИО")
            @Size(min = 2, max = 92, message = "ФИО должно содержать от 2 до 92 символов")
            @Pattern(regexp = "^[А-Яа-яЁёA-Za-z\\- ]+$", message = "ФИО может содержать только буквы, пробел и дефис")
            String fullName) {}

    public record ChangePassword(
            @NotBlank(message = "Укажите текущий пароль") String oldPassword,
            @NotBlank(message = "Укажите новый пароль")
            @Size(min = 6, max = 64, message = "Пароль должен содержать от 6 до 64 символов")
            @Pattern(regexp = "^(?=.*[A-Za-zА-Яа-яЁё])(?=.*\\d).+$", message = "Пароль должен содержать буквы и цифры")
            String newPassword) {}
}
