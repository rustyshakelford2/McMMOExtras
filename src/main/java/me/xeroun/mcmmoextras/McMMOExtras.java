package me.xeroun.mcmmoextras;

import com.avaje.ebeaninternal.api.ClassUtil;
import com.google.common.collect.Maps;

import java.util.Map;
import java.util.logging.Level;

import me.xeroun.mcmmoextras.expbar.ExpBarCommands;
import me.xeroun.mcmmoextras.expbar.ExpBarEvents;
import me.xeroun.mcmmoextras.expbar.plugins.BarAPI;
import me.xeroun.mcmmoextras.expbar.plugins.BossAPI;
import me.xeroun.mcmmoextras.expbar.plugins.BossBarMessageAPI;
import me.xeroun.mcmmoextras.expbar.plugins.SpigotBarApi;

import net.milkbowl.vault.permission.Permission;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.ServicesManager;
import org.bukkit.plugin.java.JavaPlugin;

public class McMMOExtras extends JavaPlugin {

    private static McMMOExtras instance;

    public static McMMOExtras getInstance() {
        return instance;
    }

    private final Map<String, PlayerData> data = Maps.newHashMap();

    //optional dependencies
    private Permission permission = null;
    private WorldGuardFlagSupport regionsWhitelist;
    private BossAPI bossAPI;

    public PlayerData getData(String playerName) {
        PlayerData playerData = data.computeIfAbsent(playerName, PlayerData::new);
        return playerData;
    }

    public void clearData(Player player) {
        data.remove(player.getName());

        instance.getBossAPI().removeBar(player, null);
    }

    @Override
    public void onEnable() {
        //check the dependencies
        if (getServer().getPluginManager().isPluginEnabled("mcMMO") && initializeBarAPI()) {
            //create a config only if there is none
            saveDefaultConfig();

            instance = this;

            getServer().getPluginManager().registerEvents(new ExpBarEvents(), this);
            getCommand("expbar").setExecutor(new ExpBarCommands(this));

            setupPermissions();
            registerWorldGuardFlag();
        } else {
            //inform the users
            getLogger().log(Level.INFO, "{0} requires mcMMO and BarAPI, BossBarAPI or Spigot 1.9+ to work.", getName());
        }
    }

    @Override
    public void onDisable() {
        //cleanup after reloads
        data.clear();

        //Prevent memory leaks; see this http://bukkit.org/threads/how-to-make-your-plugin-better.77899/
        instance = null;

        Bukkit.getOnlinePlayers().forEach((player) -> bossAPI.removeBar(player, null));
    }

    public BossAPI getBossAPI() {
        return bossAPI;
    }

    public boolean isForbiddenSkillInRegion(Player player, String skill) {
        if (regionsWhitelist != null) {
            return regionsWhitelist.isForbiddenSkillInRegion(player, skill);
        }

        return false;
    }

    public int getMaxSkillLevel(Player player, String skill) {
        if (permission == null || !permission.hasGroupSupport()) {
            //vault hasn't found
            return Integer.MAX_VALUE;
        }

        String primaryGroup = permission.getPrimaryGroup(player);

        String configPath = "permissions." + primaryGroup + '.' + skill;
        return getConfig().getInt(configPath, Integer.MAX_VALUE);
    }

    private boolean setupPermissions() {
        if (getServer().getPluginManager().isPluginEnabled("Vault")) {
            ServicesManager serviceManager = getServer().getServicesManager();
            RegisteredServiceProvider<Permission> permissionProvider = serviceManager.getRegistration(Permission.class);
            if (permissionProvider != null) {
                permission = permissionProvider.getProvider();
            }
        }

        return (permission != null);
    }

    private void registerWorldGuardFlag() {
        if (getConfig().getBoolean("useWorldGuardFlags")) {
            PluginManager pluginManager = getServer().getPluginManager();

            Plugin worldGuardPlugin = pluginManager.getPlugin("WorldGuard");
            Plugin customFlagsPlugin = pluginManager.getPlugin("WGCustomFlags");
            if (worldGuardPlugin == null || customFlagsPlugin == null) {
                getLogger().warning("Using world guards flags requires the plugin WGCustomFlags and WorldGuard");
            } else {
                regionsWhitelist = new WorldGuardFlagSupport(worldGuardPlugin, customFlagsPlugin);
                regionsWhitelist.registerWorldGuardFlag();
            }
        }
    }

    private boolean initializeBarAPI() {
        //load priority. If this plugin is found use it in order to fix the not see bug
        if (ClassUtil.isPresent("org.bukkit.boss.BossBar")) {
            bossAPI = new SpigotBarApi(getConfig());
            return true;
        } else if (getServer().getPluginManager().isPluginEnabled("BossBarAPI")) {
            bossAPI = new BossBarMessageAPI(getConfig());
            return true;
        } else if (getServer().getPluginManager().isPluginEnabled("BarAPI")) {
            bossAPI = new BarAPI();
            return true;
        }

        return false;
    }
}
