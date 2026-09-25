package ru.agrofarm.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Запросы мобильного приложения на изменение данных фермы. */
public final class FarmRequests {

    private FarmRequests() {}

    public record Unit(
            @NotBlank(message = "Укажите название участка")
            @Size(max = 60, message = "Название не длиннее 60 символов")
            String name,

            @NotBlank(message = "Укажите тип участка")
            @Pattern(regexp = "field|livestock|greenhouse|storage", message = "Недопустимый тип участка")
            String type,

            @DecimalMin(value = "0.0", message = "Площадь не может быть отрицательной")
            @DecimalMax(value = "100000.0", message = "Площадь не больше 100 000 га")
            @Digits(integer = 8, fraction = 2, message = "Площадь: не более 2 знаков после запятой")
            BigDecimal areaHa,

            @Min(value = 0, message = "Поголовье не может быть отрицательным")
            @Max(value = 1_000_000, message = "Поголовье не больше 1 000 000")
            Integer headCount,

            @Size(max = 255, message = "Описание не длиннее 255 символов")
            String description) {}

    public record Operation(
            @NotNull(message = "Выберите участок") Long unitId,
            @NotNull(message = "Выберите статью") Long categoryId,

            @NotBlank(message = "Укажите тип операции")
            @Pattern(regexp = "income|expense", message = "Тип операции: income или expense")
            String type,

            @NotNull(message = "Укажите сумму")
            @DecimalMin(value = "0.01", message = "Сумма должна быть больше нуля")
            @DecimalMax(value = "9999999999.99", message = "Слишком большая сумма")
            @Digits(integer = 10, fraction = 2, message = "Сумма: не более 2 знаков после запятой")
            BigDecimal amount,

            @DecimalMin(value = "0.001", message = "Количество должно быть больше нуля")
            @Digits(integer = 9, fraction = 3, message = "Количество: не более 3 знаков после запятой")
            BigDecimal quantity,

            @Pattern(regexp = "кг|ц|т|л|шт|гол", message = "Недопустимая единица измерения")
            String quantityUnit,

            @Size(max = 255, message = "Описание не длиннее 255 символов")
            String description,

            @NotNull(message = "Укажите дату операции")
            LocalDate operationDate) {}

    public record CategoryReq(
            @NotBlank(message = "Укажите название статьи")
            @Size(max = 60, message = "Название не длиннее 60 символов")
            String name,

            @NotBlank(message = "Укажите тип статьи")
            @Pattern(regexp = "income|expense", message = "Тип статьи: income или expense")
            String type,

            @Size(max = 30) String icon) {}

    public record Limit(
            @NotNull(message = "Выберите статью") Long categoryId,

            @NotNull(message = "Укажите сумму лимита")
            @DecimalMin(value = "1", message = "Лимит должен быть не меньше 1 ₽")
            @Digits(integer = 10, fraction = 2, message = "Сумма: не более 2 знаков после запятой")
            BigDecimal amount,

            @NotBlank(message = "Укажите период")
            @Pattern(regexp = "month|quarter", message = "Период: month или quarter")
            String period) {}

    public record RecommendationReq(
            @NotNull(message = "Выберите фермера") Long userId,

            @Pattern(regexp = "general|crops|livestock|finance", message = "Недопустимая тема")
            String topic,

            @NotBlank(message = "Текст рекомендации не может быть пустым")
            @Size(max = 1000, message = "Текст не длиннее 1000 символов")
            String message) {}

    public record RecommendationEdit(
            @NotBlank(message = "Текст рекомендации не может быть пустым")
            @Size(max = 1000, message = "Текст не длиннее 1000 символов")
            String message) {}
}
