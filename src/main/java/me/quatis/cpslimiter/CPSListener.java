package me.quatis.cpslimiter;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.*;

public class CPSListener implements Listener {

    private final CPSLimiterPlugin plugin;
    private final Map<UUID, List<Long>> leftClicks = new HashMap<>();
    private final Map<UUID, List<Long>> rightClicks = new HashMap<>();
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    public CPSListener(CPSLimiterPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        if (player.hasPermission("cpslimiter.bypass")) {
            return;
        }

        UUID uuid = player.getUniqueId();
        long currentTime = System.currentTimeMillis();

        // Check cooldown
        if (cooldowns.containsKey(uuid)) {
            long expiry = cooldowns.get(uuid);
            if (currentTime < expiry) {
                event.setCancelled(true);
                long remaining = (expiry - currentTime) / 1000;
                String msg = plugin.getConfig().getString("cooldown-message");
                if (msg != null && !msg.isEmpty()) {
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                            msg.replace("%time%", String.valueOf(remaining + 1))));
                }
                return;
            } else {
                cooldowns.remove(uuid);
            }
        }

        Action action = event.getAction();
        boolean isLeft = (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK);
        boolean isRight = (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK);

        if (!isLeft && !isRight)
            return;

        Map<UUID, List<Long>> targetMap = isLeft ? leftClicks : rightClicks;
        String limitPath = isLeft ? "cps-limit" : "right-cps-limit";
        String clickType = isLeft ? "Left Click" : "Right Click";

        List<Long> clicks = targetMap.get(uuid);
        if (clicks == null) {
            clicks = new ArrayList<Long>();
            targetMap.put(uuid, clicks);
        }
        clicks.add(currentTime);

        // Remove clicks older than 1 second
        Iterator<Long> iterator = clicks.iterator();
        while (iterator.hasNext()) {
            Long time = iterator.next();
            if (currentTime - time > 1000) {
                iterator.remove();
            }
        }

        // Debug mesaj - Configden ayarlanabilir
        if (plugin.getConfig().getBoolean("debug-cps", true)) {
            player.sendMessage(clickType + " CPS: " + clicks.size());
        }

        int maxCps = plugin.getConfig().getInt(limitPath, 15);
        if (clicks.size() > maxCps) {
            if (plugin.getConfig().getBoolean("cancel-hit", true)) {
                event.setCancelled(true);
            }

            if (plugin.getConfig().getBoolean("enable-alert", true)) {
                String message = plugin.getConfig().getString("alert-message");
                if (message != null) {
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
                }
            }

            // Add cooldown
            int cooldownSec = plugin.getConfig().getInt("cooldown-seconds", 3);
            if (cooldownSec > 0) {
                cooldowns.put(uuid, currentTime + (cooldownSec * 1000));
            }

            // Clear current clicks so they don't immediately re-trigger after cooldown
            // (give them a fresh start)
            clicks.clear();
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        leftClicks.remove(uuid);
        rightClicks.remove(uuid);
        cooldowns.remove(uuid);
    }
}
