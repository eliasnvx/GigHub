package io.eliasnvx.gighub.bukkit.config;

import io.eliasnvx.gighub.bukkit.GigHubPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Менеджер локализации GigHub
 */
public class LocaleManager {
    private final GigHubPlugin plugin;
    private final Map<String, FileConfiguration> locales = new HashMap<>();
    private String defaultLocale = "en_US";

    public LocaleManager(GigHubPlugin plugin) {
        this.plugin = plugin;
        loadLocales();
    }

    /**
     * Загружает все файлы локализации
     */
    private void loadLocales() {
        File langDir = new File(plugin.getDataFolder(), "lang");
        if (!langDir.exists()) {
            langDir.mkdirs();
        }

        // Гарантируем наличие базовых локалей (не перезаписывает существующие)
        saveDefaultLocale("en_US.yml");
        saveDefaultLocale("ru_RU.yml");
        // Загружаем все YAML файлы из папки lang и мержим дефолтные значения из jar
        File[] files = langDir.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files != null) {
            for (File file : files) {
                String localeName = file.getName().replace(".yml", "");

                // Текущая конфигурация на диске
                FileConfiguration config = YamlConfiguration.loadConfiguration(file);

                // Дефолтные значения из ресурсов jar, если доступны
                InputStream defaultStream = plugin.getResource("lang/" + file.getName());
                if (defaultStream != null) {
                    YamlConfiguration defaults = YamlConfiguration.loadConfiguration(new InputStreamReader(defaultStream));
                    config.setDefaults(defaults);
                    config.options().copyDefaults(true);
                    try {
                        config.save(file); // сохраняем, чтобы недостающие ключи появились на диске
                    } catch (IOException e) {
                        plugin.getLogger().warning("Failed to save merged locale file: " + file.getName() + ": " + e.getMessage());
                    }
                }

                locales.put(localeName, config);
                plugin.getLogger().info("Loaded locale: " + localeName);
            }
        }
    }

    /**
     * Сохраняет дефолтный файл локализации из resources
     */
    private void saveDefaultLocale(String fileName) {
        File file = new File(plugin.getDataFolder(), "lang/" + fileName);
        if (!file.exists()) {
            plugin.saveResource("lang/" + fileName, false);
        }
        // УБРАЛИ ПЕРЕЗАПИСЬ - больше не затираем кастомные файлы
    }

    /**
     * Получает локализованное сообщение
     */
    public String getMessage(String key) {
        return getMessage(defaultLocale, key);
    }

    /**
     * Получает локализованное сообщение для указанной локали
     */
    public String getMessage(String locale, String key) {
        FileConfiguration config = locales.get(locale);
        if (config == null) {
            config = locales.get(defaultLocale);
        }
        
        if (config == null) {
            plugin.getLogger().warning("[LocaleManager] Config is null for locale: " + locale);
            return key; // Возвращаем ключ если локализация не найдена
        }

        // Новая структура: ищем ключ напрямую (без префикса messages)
        String message = config.getString(key);
        
        // Fallback: пробуем старую структуру с префиксом messages
        if (message == null) {
            message = config.getString("messages." + key);
        }
        
        if (message == null) {
            plugin.getLogger().warning("[LocaleManager] Key not found: '" + key + "' for locale: " + locale);
        }
        // 3) Если всё ещё не найдено, пробуем взять из ресурсов jar напрямую
        if (message == null) {
            InputStream in = plugin.getResource("lang/" + locale + ".yml");
            if (in == null && !defaultLocale.equals(locale)) {
                in = plugin.getResource("lang/" + defaultLocale + ".yml");
            }
            if (in != null) {
                YamlConfiguration defaults = YamlConfiguration.loadConfiguration(new InputStreamReader(in));
                message = defaults.getString("messages." + key);
                if (message == null) {
                    message = defaults.getString(key);
                }
            }
            if (message == null) {
                return key;
            }
        }

        // 4) Подставляем {prefix}, если он есть в конфиге
        String prefix = config.getString("prefix", "");
        if (prefix != null && !prefix.isEmpty()) {
            message = message.replace("{prefix}", prefix);
        }

        return message;
    }

    /**
     * Получает локализованное сообщение с заменой плейсхолдеров
     */
    public String getMessage(String key, String... placeholders) {
        return getMessage(defaultLocale, key, placeholders);
    }

    /**
     * Получает локализованное сообщение для указанной локали с заменой плейсхолдеров
     */
    public String getMessage(String locale, String key, String... placeholders) {
        String message = getMessage(locale, key);
        
        // Заменяем плейсхолдеры {0}, {1}, etc.
        for (int i = 0; i < placeholders.length; i++) {
            message = message.replace("{" + i + "}", placeholders[i]);
        }
        
        return message;
    }

    /**
     * Получает локализованное название типа контракта
     */
    public String getContractTypeName(String locale, String contractType) {
        return getMessage(locale, "contract-types." + contractType);
    }

    /**
     * Получает локализованное название типа контракта (дефолтная локаль)
     */
    public String getContractTypeName(String contractType) {
        return getContractTypeName(defaultLocale, contractType);
    }

    /**
     * Устанавливает локаль по умолчанию
     */
    public void setDefaultLocale(String locale) {
        if (locales.containsKey(locale)) {
            this.defaultLocale = locale;
        }
    }

    /**
     * Получает текущую локаль по умолчанию
     */
    public String getDefaultLocale() {
        return defaultLocale;
    }

    /**
     * Перезагружает локализации
     */
    public void reload() {
        locales.clear();
        loadLocales();
    }
}
