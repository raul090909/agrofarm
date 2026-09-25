package ru.agrofarm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Интеграционные тесты REST API «Сельхозфермы».
 * Приложение поднимается целиком (контроллеры, сервисы, JPA) на встроенной базе H2
 * с демонстрационными данными из DataInitializer.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.DisplayName.class)
class ApiIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    private String login(String email, String password) throws Exception {
        MvcResult r = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk()).andReturn();
        return json.readTree(r.getResponse().getContentAsString()).get("token").asText();
    }

    private String agronomist() throws Exception {
        MvcResult r = mvc.perform(post("/api/agronomist/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"login\":\"agronom\",\"password\":\"agro123\"}"))
                .andExpect(status().isOk()).andReturn();
        return json.readTree(r.getResponse().getContentAsString()).get("token").asText();
    }

    private static MockHttpServletRequestBuilder auth(MockHttpServletRequestBuilder b, String token) {
        return b.header("Authorization", "Bearer " + token);
    }

    private JsonNode body(MvcResult r) throws Exception {
        return json.readTree(r.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
    }

    private long firstUnitId(String token) throws Exception {
        return body(mvc.perform(auth(get("/api/units"), token)).andReturn()).get(0).get("id").asLong();
    }

    private long categoryId(String token, String name) throws Exception {
        for (JsonNode c : body(mvc.perform(auth(get("/api/categories"), token)).andReturn())) {
            if (c.get("name").asText().equals(name)) return c.get("id").asLong();
        }
        throw new IllegalStateException(name);
    }

    private String opJson(long unit, long cat, String type, String amount, String date) {
        return "{\"unitId\":" + unit + ",\"categoryId\":" + cat + ",\"type\":\"" + type + "\",\"amount\":" + amount
                + ",\"operationDate\":\"" + date + "\"}";
    }

    @Test @DisplayName("TC01 Регистрация с корректными данными возвращает токен и создаёт участок")
    void registerOk() throws Exception {
        MvcResult r = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Пётр Иванов\",\"email\":\"Petr.New@Test.ru\",\"password\":\"secret1\"}"))
                .andExpect(status().isCreated()).andReturn();
        JsonNode b = body(r);
        assertThat(b.get("token").asText()).hasSizeGreaterThan(30);
        assertThat(b.get("profile").get("email").asText()).isEqualTo("petr.new@test.ru");
        assertThat(b.get("profile").has("passwordHash")).isFalse();
        mvc.perform(auth(get("/api/units"), b.get("token").asText()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
    }

    @Test @DisplayName("TC02 Регистрация с существующим email отклоняется (409)")
    void registerDuplicate() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Иван Дубль\",\"email\":\"IVAN@test.ru\",\"password\":\"secret1\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Пользователь с таким email уже зарегистрирован"));
    }

    @Test @DisplayName("TC03 Регистрация со слабым паролем и неверным email отклоняется (400)")
    void registerInvalid() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Иван\",\"email\":\"bad-email\",\"password\":\"123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.email").exists())
                .andExpect(jsonPath("$.fields.password").exists());
    }

    @Test @DisplayName("TC04 Вход с неверным паролем — 401 без уточнения причины")
    void loginWrongPassword() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"anna@test.ru\",\"password\":\"wrong1\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Неверный email или пароль"));
    }

    @Test @DisplayName("TC05 После 5 неудачных попыток вход блокируется (429)")
    void bruteForceLock() throws Exception {
        for (int i = 0; i < 5; i++) {
            mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                    .content("{\"email\":\"locked@test.ru\",\"password\":\"x\"}")).andExpect(status().isUnauthorized());
        }
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"locked@test.ru\",\"password\":\"x\"}"))
                .andExpect(status().isTooManyRequests());
    }

    @Test @DisplayName("TC06 Запрос без токена — 401, фермер в разделе агронома — 403")
    void accessControl() throws Exception {
        mvc.perform(get("/api/units")).andExpect(status().isUnauthorized());
        String t = login("ivan@test.ru", "pass123");
        mvc.perform(auth(get("/api/agronomist/dashboard"), t)).andExpect(status().isForbidden());
        mvc.perform(auth(get("/export/csv?from=2026-01-01&to=2026-01-31"), t)).andExpect(status().isForbidden());
        String a = agronomist();
        mvc.perform(auth(get("/api/units"), a)).andExpect(status().isForbidden());
    }

    @Test @DisplayName("TC07 Добавление корректной операции расхода")
    void createOperation() throws Exception {
        String t = login("oleg@test.ru", "pass123");
        long unit = firstUnitId(t);
        long cat = categoryId(t, "Прочие расходы");
        mvc.perform(auth(post("/api/operations"), t).contentType(MediaType.APPLICATION_JSON)
                        .content(opJson(unit, cat, "expense", "1500.50", LocalDate.now().toString())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.amount").value(1500.50))
                .andExpect(jsonPath("$.categoryName").value("Прочие расходы"));
    }

    @Test @DisplayName("TC08 Операция с нулевой суммой и датой в будущем отклоняется")
    void operationValidation() throws Exception {
        String t = login("oleg@test.ru", "pass123");
        long unit = firstUnitId(t);
        long cat = categoryId(t, "Корма");
        mvc.perform(auth(post("/api/operations"), t).contentType(MediaType.APPLICATION_JSON)
                        .content(opJson(unit, cat, "expense", "0", LocalDate.now().toString())))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("Сумма должна быть больше нуля"));
        mvc.perform(auth(post("/api/operations"), t).contentType(MediaType.APPLICATION_JSON)
                        .content(opJson(unit, cat, "expense", "100", LocalDate.now().plusDays(3).toString())))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("Дата операции не может быть в будущем"));
    }

    @Test @DisplayName("TC09 Тип операции должен совпадать с типом статьи")
    void operationTypeMismatch() throws Exception {
        String t = login("oleg@test.ru", "pass123");
        mvc.perform(auth(post("/api/operations"), t).contentType(MediaType.APPLICATION_JSON)
                        .content(opJson(firstUnitId(t), categoryId(t, "Продажа мяса"), "expense", "100", LocalDate.now().toString())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Тип операции не совпадает с типом статьи"));
    }

    @Test @DisplayName("TC10 Фермер не может использовать чужой участок и удалять чужие операции")
    void isolation() throws Exception {
        String anna = login("anna@test.ru", "pass123");
        String ivan = login("ivan@test.ru", "pass123");
        long annaUnit = firstUnitId(anna);
        long annaOp = body(mvc.perform(auth(get("/api/operations/recent?limit=1"), anna)).andReturn()).get(0).get("id").asLong();
        mvc.perform(auth(post("/api/operations"), ivan).contentType(MediaType.APPLICATION_JSON)
                        .content(opJson(annaUnit, categoryId(ivan, "Корма"), "expense", "100", LocalDate.now().toString())))
                .andExpect(status().isNotFound());
        mvc.perform(auth(delete("/api/operations/" + annaOp), ivan)).andExpect(status().isNotFound());
        mvc.perform(auth(get("/api/operations/recent?limit=1"), anna))
                .andExpect(jsonPath("$[0].id").value(annaOp));
    }

    @Test @DisplayName("TC11 Лимит нельзя установить на статью дохода; повторный лимит отклоняется")
    void limitRules() throws Exception {
        String t = login("oleg@test.ru", "pass123");
        mvc.perform(auth(post("/api/limits"), t).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"categoryId\":" + categoryId(t, "Продажа мяса") + ",\"amount\":1000,\"period\":\"month\"}"))
                .andExpect(status().isBadRequest());
        String ok = "{\"categoryId\":" + categoryId(t, "Топливо и ГСМ") + ",\"amount\":1000,\"period\":\"quarter\"}";
        mvc.perform(auth(post("/api/limits"), t).contentType(MediaType.APPLICATION_JSON).content(ok))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("ok"));
        mvc.perform(auth(post("/api/limits"), t).contentType(MediaType.APPLICATION_JSON).content(ok))
                .andExpect(status().isConflict());
    }

    @Test @DisplayName("TC12 Превышение лимита создаёт автоматическое уведомление фермеру")
    void limitNotification() throws Exception {
        String t = login("oleg@test.ru", "pass123");
        long cat = categoryId(t, "Электроэнергия и вода");
        mvc.perform(auth(post("/api/limits"), t).contentType(MediaType.APPLICATION_JSON)
                .content("{\"categoryId\":" + cat + ",\"amount\":500,\"period\":\"month\"}")).andExpect(status().isCreated());
        mvc.perform(auth(post("/api/operations"), t).contentType(MediaType.APPLICATION_JSON)
                .content(opJson(firstUnitId(t), cat, "expense", "700", LocalDate.now().toString()))).andExpect(status().isCreated());
        JsonNode recs = body(mvc.perform(auth(get("/api/recommendations"), t)).andReturn());
        assertThat(recs.get(0).get("auto").asBoolean()).isTrue();
        assertThat(recs.get(0).get("message").asText()).contains("Превышен лимит", "Электроэнергия и вода");
    }

    @Test @DisplayName("TC13 Нельзя удалить единственный участок и общую статью")
    void deleteRules() throws Exception {
        MvcResult r = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content("{\"fullName\":\"Мария Лис\",\"email\":\"maria@test.ru\",\"password\":\"secret1\"}")).andReturn();
        String t = body(r).get("token").asText();
        mvc.perform(auth(delete("/api/units/" + firstUnitId(t)), t)).andExpect(status().isConflict());
        mvc.perform(auth(delete("/api/categories/" + categoryId(t, "Корма")), t)).andExpect(status().isForbidden());
    }

    @Test @DisplayName("TC14 Агроном видит аномалии всех трёх видов на демо-данных")
    void anomalies() throws Exception {
        String a = agronomist();
        mvc.perform(auth(get("/api/agronomist/anomalies"), a))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.limitAnomalies[0].status").value("exceeded"))
                .andExpect(jsonPath("$.expenseGrowth.length()").value(org.hamcrest.Matchers.greaterThan(0)))
                .andExpect(jsonPath("$.unprofitableUnits.length()").value(org.hamcrest.Matchers.greaterThan(0)));
    }

    @Test @DisplayName("TC15 Агроном отправляет рекомендацию, фермер её видит и отмечает прочитанной")
    void recommendationFlow() throws Exception {
        String a = agronomist();
        String t = login("anna@test.ru", "pass123");
        long userId = body(mvc.perform(auth(get("/api/profile"), t)).andReturn()).get("id").asLong();
        mvc.perform(auth(post("/api/agronomist/recommendations"), a).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":" + userId + ",\"topic\":\"crops\",\"message\":\"Проведите обработку от фитофторы\"}"))
                .andExpect(status().isCreated());
        JsonNode first = body(mvc.perform(auth(get("/api/recommendations"), t)).andReturn()).get(0);
        assertThat(first.get("message").asText()).isEqualTo("Проведите обработку от фитофторы");
        assertThat(first.get("read").asBoolean()).isFalse();
        mvc.perform(auth(put("/api/recommendations/" + first.get("id").asLong() + "/read"), t)).andExpect(status().isOk());
        mvc.perform(auth(get("/api/recommendations"), t)).andExpect(jsonPath("$[0].read").value(true));
        mvc.perform(auth(post("/api/agronomist/recommendations"), a).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":" + userId + ",\"message\":\"   \"}"))
                .andExpect(status().isBadRequest());
    }

    @Test @DisplayName("TC16 Выгрузка CSV и Excel за период")
    void export() throws Exception {
        String a = agronomist();
        String from = LocalDate.now().minusMonths(6).toString();
        String to = LocalDate.now().toString();
        MvcResult csv = mvc.perform(auth(get("/export/csv?from=" + from + "&to=" + to), a))
                .andExpect(status().isOk()).andReturn();
        String text = csv.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        assertThat(text).contains("Дата;Фермер;Участок");
        assertThat(text.lines().count()).isGreaterThan(50);
        MvcResult xlsx = mvc.perform(auth(get("/export/excel?from=" + from + "&to=" + to), a))
                .andExpect(status().isOk()).andReturn();
        byte[] bytes = xlsx.getResponse().getContentAsByteArray();
        assertThat(bytes[0]).isEqualTo((byte) 'P'); // xlsx — ZIP-архив, сигнатура PK
        mvc.perform(auth(get("/export/csv?from=" + to + "&to=" + from), a)).andExpect(status().isBadRequest());
    }

    @Test @DisplayName("TC17 Сводка за месяц: прибыль = доходы − расходы")
    void summary() throws Exception {
        String t = login("ivan@test.ru", "pass123");
        JsonNode s = body(mvc.perform(auth(get("/api/stats/summary"), t)).andExpect(status().isOk()).andReturn());
        assertThat(s.get("profit").decimalValue())
                .isEqualByComparingTo(s.get("income").decimalValue().subtract(s.get("expense").decimalValue()));
        mvc.perform(auth(get("/api/stats/summary?month=2026-13"), t)).andExpect(status().isBadRequest());
    }

    @Test @DisplayName("TC18 Выход из системы аннулирует токен")
    void logout() throws Exception {
        String t = login("oleg@test.ru", "pass123");
        mvc.perform(auth(post("/api/auth/logout"), t)).andExpect(status().isOk());
        mvc.perform(auth(get("/api/units"), t)).andExpect(status().isUnauthorized());
    }
}
