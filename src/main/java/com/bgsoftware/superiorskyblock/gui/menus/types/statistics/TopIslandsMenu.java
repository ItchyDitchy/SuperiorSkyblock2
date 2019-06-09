package com.bgsoftware.superiorskyblock.gui.menus.types.statistics;

import com.bgsoftware.superiorskyblock.SuperiorSkyblockPlugin;
import com.bgsoftware.superiorskyblock.api.island.Island;
import com.bgsoftware.superiorskyblock.gui.MenuTemplate;
import com.bgsoftware.superiorskyblock.gui.buttons.Button;
import com.bgsoftware.superiorskyblock.gui.buttons.IslandButton;
import com.bgsoftware.superiorskyblock.gui.menus.YamlScroll;
import com.bgsoftware.superiorskyblock.gui.menus.types.warps.IslandWarpsMenu;
import com.bgsoftware.superiorskyblock.utils.HeadUtil;
import com.bgsoftware.superiorskyblock.utils.ItemSerializer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;

public class TopIslandsMenu extends YamlScroll {

    private ItemStack islandItem;
    private ItemStack invalidItem;

    private boolean includeLeader;

    public TopIslandsMenu(Player player) {
        super(player, MenuTemplate.TOP_ISLANDS.getFile());
        create(title, rows);

        includeLeader = file.getBoolean("include-leader", true);
        islandItem = ItemSerializer.getItem(HeadUtil.getMaterial(), file.getConfigurationSection("island_item"));
        invalidItem = ItemSerializer.getItem(HeadUtil.getMaterial(), file.getConfigurationSection("invalid_item"));
        SkullMeta meta = (SkullMeta) invalidItem.getItemMeta();
        meta.setOwner("MHF_Question");
        invalidItem.setItemMeta(meta);

        setList(createButtons());

        setPage(0);
        open();
    }

    private List<Button> createButtons() {
        List<Button> buttons = new ArrayList<>();

        int rank = 1;
        for (Island island : SuperiorSkyblockPlugin.getPlugin().getGrid().getIslands()) {
            buttons.add(new IslandButton(island, rank, islandItem, includeLeader, (clicker, type) -> {
                switch (type) {
                    case LEFT:
                        new IslandValuesMenu(player, island);
                        break;
                    case RIGHT:
                        new IslandWarpsMenu(player, island, IslandWarpsMenu.PreviousMenu.TOP);
                        break;
                }
            }));
            rank++;
        }

        while (buttons.size() % getPageSize() != 0) {
            buttons.add(new Button(invalidItem, (c, t) -> {}));
        }

        return buttons;
    }

}
