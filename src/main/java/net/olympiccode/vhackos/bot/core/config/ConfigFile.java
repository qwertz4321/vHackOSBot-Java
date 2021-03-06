package net.olympiccode.vhackos.bot.core.config;

import com.google.gson.*;
import org.reflections.Reflections;
import org.reflections.scanners.FieldAnnotationsScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public class ConfigFile {

    JsonObject configJson = new JsonObject();
    File file = new File("config.json");
    Logger LOG = LoggerFactory.getLogger("vHackOSBot-Config");
    public ConfigFile() {
        LOG.info("Creating ConfigFile...");
    }

    public void setupConfig() {
        LOG.info("Loading config...");
        long time = System.currentTimeMillis();
        try {
            if (!file.exists()) {
                file.createNewFile();
                configJson = new JsonObject();
            } else {
                String config = Files.readAllLines(Paths.get("config.json"), Charset.forName("UTF-8")).stream().collect(Collectors.joining());
                if (config.startsWith("{")) configJson = (JsonObject) new JsonParser().parse(config);
            }
            Set<Field> fields = new Reflections("net.olympiccode.vhackos.bot.core", new FieldAnnotationsScanner()).getFieldsAnnotatedWith(ConfigOption.class);
            fields.forEach(field -> {
                Annotation annotation = field.getAnnotation(ConfigOption.class);
                try {
                    String path = (String) ConfigOption.class.getMethod("path").invoke(annotation);
                    String defaultValue = (String) ConfigOption.class.getMethod("defaultValue").invoke(annotation);
                    String[] options = (String[]) ConfigOption.class.getMethod("options").invoke(annotation);
                    if (!configJson.has(path)) configJson.add(path, new JsonParser().parse(defaultValue));
                    if (field.getType().isEnum()) {
                        Class<?> en = field.getType();
                        field.set(field.getDeclaringClass(), Enum.valueOf((Class<Enum>) field.getType(), configJson.get(path).getAsString()));
                    } else if (field.getType().isArray()) {
                        JsonArray array = configJson.get(path).getAsJsonArray();
                        String[] larray = new String[array.size()];
                        for (int i = 0; i < array.size(); i++) {
                            larray[i] = array.get(i).getAsString();
                        }
                        field.set(field.getDeclaringClass(), larray);
                    } else if (field.getType().equals(boolean.class) || field.getType().equals(Boolean.class)) {
                        field.set(field.getDeclaringClass(), configJson.get(path).getAsBoolean());
                    } else if (field.getType().equals(int.class) || field.getType().equals(Integer.class)) {
                        field.set(field.getDeclaringClass(), configJson.get(path).getAsInt());
                    } else {
                        if (Arrays.asList(options).get(0).isEmpty() || Arrays.asList(options).contains(configJson.get(path).getAsString())) {
                            field.set(field.getDeclaringClass(), configJson.get(path).getAsString());
                        } else {
                            configJson.add(path, new JsonParser().parse(defaultValue));
                            field.set(field.getDeclaringClass(), defaultValue);
                        }
                    }
                } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                    e.printStackTrace();
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
        LOG.info("Loaded config in " + (System.currentTimeMillis() - time) + "ms.");
        save();
    }

    public void save() {
        LOG.info("Saving config...");
        long time = System.currentTimeMillis();
        try {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();

            Files.write(Paths.get("config.json"), gson.toJson(configJson).toString().getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
        LOG.info("Saved config in " + (System.currentTimeMillis() - time) + "ms.");
    }
}
