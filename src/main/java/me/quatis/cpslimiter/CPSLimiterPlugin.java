package me.quatis.cpslimiter;

import org.bukkit.plugin.java.JavaPlugin;

public class CPSLimiterPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        // Config dosyasini olustur
        saveDefaultConfig();

        // Listener'i kaydet
        getServer().getPluginManager().registerEvents(new CPSListener(this), this);

        getLogger().info("CPS Limiter aktif edildi!");
    }

    @Override
    public void onDisable() {
        getLogger().info("CPS Limiter deaktif edildi!");
    }
}
