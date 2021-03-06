package me.xeroun.mcmmoextras.expbar;

import com.gmail.nossr50.api.ExperienceAPI;
import com.gmail.nossr50.datatypes.skills.SkillType;
import com.gmail.nossr50.events.experience.McMMOPlayerXpGainEvent;

import me.xeroun.mcmmoextras.McMMOExtras;
import me.xeroun.mcmmoextras.PlayerData;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class ExpBarEvents implements Listener {

    @EventHandler(ignoreCancelled = true)
    public void onExpGain(final McMMOPlayerXpGainEvent xpGainEvent) {
        Player player = xpGainEvent.getPlayer();
        if (!player.hasPermission(McMMOExtras.getInstance().getName().toLowerCase() + ".expbar")) {
            return;
        }

        //set the new value not the old one
        Bukkit.getScheduler().runTask(McMMOExtras.getInstance(), () -> {
            String playerName = player.getName();
            SkillType skill = xpGainEvent.getSkill();
            String skillname = skill.getName();

            int level = ExperienceAPI.getLevel(player, skillname);
            //permission based max levels
            if (level <= McMMOExtras.getInstance().getMaxSkillLevel(player, skillname)
                    //world guard region flag check
                    && !McMMOExtras.getInstance().isForbiddenSkillInRegion(player, skillname)) {
                PlayerData playerData = McMMOExtras.getInstance().getData(playerName);

                playerData.updateExpBar(skill, xpGainEvent.getRawXpGained());
            }
        });
    }

    @EventHandler
    public void onLogout(PlayerQuitEvent quitEvent) {
        McMMOExtras.getInstance().clearData(quitEvent.getPlayer());
    }
}
