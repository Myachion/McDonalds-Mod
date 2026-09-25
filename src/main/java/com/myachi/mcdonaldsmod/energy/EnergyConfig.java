package com.myachi.mcdonaldsmod.energy;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.myachi.mcdonaldsmod.McDonaldsMod;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 电网的两个开关，写在 {@code config/mcdonalds-mod-energy.json} 里，改完重启生效。
 *
 * <pre>
 * {
 *   "energyLoss": true,       // 线路电损（按每根电缆的 I²R 算）
 *   "overloadBurnout": true   // 超过额定电流 3 秒直接烧毁（无掉落物）
 * }
 * </pre>
 */
public final class EnergyConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "mcdonalds-mod-energy.json";

    private static boolean energyLoss = true;
    private static boolean overloadBurnout = true;

    private EnergyConfig() {
    }

    public static void load() {
        Path path = FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
        try {
            if (Files.exists(path)) {
                JsonObject json = JsonParser.parseString(Files.readString(path)).getAsJsonObject();
                if (json.has("energyLoss")) {
                    energyLoss = json.get("energyLoss").getAsBoolean();
                }
                if (json.has("overloadBurnout")) {
                    overloadBurnout = json.get("overloadBurnout").getAsBoolean();
                }
            }
            save(path);
        } catch (Exception e) {
            McDonaldsMod.LOGGER.warn("[energy] failed to read {}, using defaults", FILE_NAME, e);
        }
    }

    private static void save(Path path) throws Exception {
        JsonObject json = new JsonObject();
        json.addProperty("energyLoss", energyLoss);
        json.addProperty("overloadBurnout", overloadBurnout);
        Files.createDirectories(path.getParent());
        Files.writeString(path, GSON.toJson(json));
    }

    public static boolean isEnergyLossEnabled() {
        return energyLoss;
    }

    public static boolean isOverloadBurnoutEnabled() {
        return overloadBurnout;
    }
}
