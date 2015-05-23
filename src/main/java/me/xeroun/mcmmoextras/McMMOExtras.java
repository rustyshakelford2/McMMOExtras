package me.xeroun.mcmmoextras;

import com.google.common.collect.Maps;

import java.util.Map;
import java.util.logging.Level;

import me.xeroun.mcmmoextras.expbar.ExpBarCommands;
import me.xeroun.mcmmoextras.expbar.ExpBarEvents;

import net.milkbowl.vault.permission.Permission;

import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.ServicesManager;
import org.bukkit.plugin.java.JavaPlugin;

public class McMMOExtras extends JavaPlugin {

    private static final int CURSE_PROJECT_ID = 69564;

    private static McMMOExtras instance;

    public static McMMOExtras getInstance() {
        return instance;
    }

    private final Map<String, PlayerData> data = Maps.newHashMap();
    private Permission permission = null;

    public PlayerData getData(String player) {
        PlayerData playerData = data.get(player);
        //get the data and check if the data exists at once
        if (playerData == null) {
            //lazy loading
            playerData = new PlayerData(player);
            data.put(player, playerData);
        }

        return playerData;
    }

    public void clearData(String player) {
        data.remove(player);
    }

    @Override
    public void onEnable() {
        if (getConfig().getBoolean("autoUpdate")) {
            Updater updater = new UpdaterFix(this, getFile(), CURSE_PROJECT_ID, true, new Updater.UpdateCallback() {

                @Override
                public void onFinish(Updater updater) {
                    if (updater.getResult() == Updater.UpdateResult.SUCCESS) {
                        getLogger().log(Level.INFO, "Downloaded a new update ({0})", updater.getLatestName());
                    }
                }
            });
        }

        //check the dependencies
        if (getServer().getPluginManager().isPluginEnabled("mcMMO")
                && getServer().getPluginManager().isPluginEnabled("BarAPI")) {
            //create a config only if there is none
            saveDefaultConfig();

            instance = this;

            getServer().getPluginManager().registerEvents(new ExpBarEvents(), this);
            getCommand("expbar").setExecutor(new ExpBarCommands(this));

            setupPermissions();
        } else {
            //inform the users
            getLogger().log(Level.INFO, "{0} requires mcMMO and BarAPI to function.", getName());
        }
    }

    @Override
    public void onDisable() {
        //cleanup after reloads
        data.clear();

        //Prevent memory leaks; see this http://bukkit.org/threads/how-to-make-your-plugin-better.77899/
        instance = null;
    }

    public int getMaxSkillLevel(Player player, String skill) {
        if (permission == null) {
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
}
