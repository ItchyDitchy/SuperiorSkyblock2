package com.bgsoftware.superiorskyblock.menu;

import com.bgsoftware.superiorskyblock.Locale;
import com.bgsoftware.superiorskyblock.api.island.Island;
import com.bgsoftware.superiorskyblock.api.island.IslandPermission;
import com.bgsoftware.superiorskyblock.api.island.IslandRole;
import com.bgsoftware.superiorskyblock.api.island.PermissionNode;
import com.bgsoftware.superiorskyblock.api.wrappers.SuperiorPlayer;
import com.bgsoftware.superiorskyblock.utils.FileUtil;
import com.bgsoftware.superiorskyblock.utils.ItemBuilder;
import com.bgsoftware.superiorskyblock.utils.threads.Executor;
import com.bgsoftware.superiorskyblock.wrappers.SoundWrapper;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class IslandPermissionsMenu extends SuperiorMenu {

    private static Map<String, Object> permissionsData = new HashMap<>();

    private static Inventory inventory = null;
    private static String title;
    private static List<Integer> slots;
    private static int previousSlot, currentSlot, nextSlot;
    private static ItemStack previousButton, currentButton, nextButton;

    private Island island;
    private Object permissionHolder;
    private String permissionHolderName;
    private SuperiorPlayer superiorPlayer;

    private int currentPage = 1;

    private IslandPermissionsMenu(SuperiorPlayer superiorPlayer, Island island, Object permissionHolder, String permissionHolderName){
        super("permissionsPage");
        this.superiorPlayer = superiorPlayer;
        this.island = island;
        this.permissionHolder = permissionHolder;
        this.permissionHolderName = permissionHolderName;
    }

    @Override
    public void onClick(InventoryClickEvent e) {
        if(e.getRawSlot() == previousSlot || e.getRawSlot() == nextSlot || e.getRawSlot() == currentSlot){
            if(e.getRawSlot() == currentSlot)
                return;

            boolean nextPage = slots.size() * currentPage < IslandPermission.values().length;

            if((!nextPage && e.getRawSlot() == nextSlot) || (currentPage == 1 && e.getRawSlot() == previousSlot))
                return;

            open(superiorPlayer, e.getRawSlot() == nextSlot ? currentPage + 1 : currentPage - 1, null);
        }
        else {
            if(e.getCurrentItem() == null)
                return;

            int permissionsAmount = IslandPermission.values().length;

            int indexOf = slots.indexOf(e.getRawSlot());

            if(indexOf >= permissionsAmount || indexOf == -1)
                return;

            IslandPermission permission = IslandPermission.values()[indexOf + (slots.size() * (currentPage - 1))];
            String permissionName = permission.name().toLowerCase();

            if(!permissionsData.containsKey(permissionName + "-permission-enabled"))
                return;

            if (!island.hasPermission(superiorPlayer, permission)) {
                SoundWrapper soundWrapper = get(permissionName + "-no-access-sound", SoundWrapper.class);
                if (soundWrapper != null)
                    soundWrapper.playSound(superiorPlayer.asPlayer());
                //noinspection unchecked
                List<String> commands = get(permissionName + "-no-access-commands", List.class);
                if (commands != null)
                    commands.forEach(command -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.replace("%player%", superiorPlayer.getName())));
                Locale.LACK_CHANGE_PERMISSION.send(superiorPlayer);
                return;
            }

            PermissionNode permissionNode;

            if (permissionHolder instanceof IslandRole)
                permissionNode = island.getPermisisonNode((IslandRole) permissionHolder);
            else {
                permissionNode = island.getPermisisonNode((SuperiorPlayer) permissionHolder);
            }

            boolean currentValue = permissionNode.hasPermission(permission);

            if (permissionHolder instanceof IslandRole)
                island.setPermission((IslandRole) permissionHolder, permission, !currentValue);
            else
                island.setPermission((SuperiorPlayer) permissionHolder, permission, !currentValue);

            Locale.UPDATED_PERMISSION.send(superiorPlayer, permissionHolderName);

            SoundWrapper soundWrapper = get(permissionName + "-has-access-sound", SoundWrapper.class);
            if (soundWrapper != null)
                soundWrapper.playSound(superiorPlayer.asPlayer());
            //noinspection unchecked
            List<String> commands = get(permissionName + "-has-access-commands", List.class);
            if (commands != null)
                commands.forEach(command -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.replace("%player%", superiorPlayer.getName())));

            open(superiorPlayer, currentPage, null);
        }
    }

    @Override
    public void open(SuperiorPlayer superiorPlayer, SuperiorMenu previousMenu) {
        open(superiorPlayer, 1, previousMenu);
    }

    @Override
    public Inventory getInventory() {
        return null;
    }

    private void open(SuperiorPlayer superiorPlayer, int page, SuperiorMenu previousMenu) {
        if (Bukkit.isPrimaryThread()) {
            Executor.async(() -> open(superiorPlayer, page, previousMenu));
            return;
        }

        Inventory inv = Bukkit.createInventory(this, inventory.getSize(), title);
        inv.setContents(inventory.getContents());

        int permissionsAmount = IslandPermission.values().length;

        for(int i = 0; i < slots.size() && (i + (slots.size() * (page - 1))) < permissionsAmount; i++){
            IslandPermission permission = IslandPermission.values()[i + (slots.size() * (page - 1))];
            String permissionName = permission.name().toLowerCase();
            if (permissionsData.containsKey(permissionName + "-permission-enabled")) {
                ItemStack permissionItem = get(permissionName + "-permission-enabled", ItemStack.class);

                PermissionNode permissionNode;

                if (permissionHolder instanceof IslandRole)
                    permissionNode = island.getPermisisonNode((IslandRole) permissionHolder);
                else
                    permissionNode = island.getPermisisonNode((SuperiorPlayer) permissionHolder);

                if (!permissionNode.hasPermission(permission))
                    permissionItem = get(permissionName + "-permission-disabled", ItemStack.class);

                inv.setItem(slots.get(i), permissionItem);
            }
        }

        inv.setItem(previousSlot, new ItemBuilder(previousButton)
                .replaceAll("{0}", (page == 1 ? "&c" : "&a")).build());

        inv.setItem(currentSlot, new ItemBuilder(currentButton)
                .replaceAll("{0}", page + "").build());

        inv.setItem(nextSlot, new ItemBuilder(nextButton)
                .replaceAll("{0}", (permissionsAmount > page * slots.size() ? "&a" : "&c")).build());

        this.previousMenu = previousMenu;

        this.currentPage = page;

        Executor.sync(() -> superiorPlayer.asPlayer().openInventory(inv));
    }

    private static <T> T get(String key, Class<T> type){
        return type.cast(permissionsData.get(key));
    }

    public static void init(){
        IslandPermissionsMenu islandPanelMenu = new IslandPermissionsMenu(null, null, null, null);

        File file = new File(plugin.getDataFolder(), "guis/permissions-gui.yml");

        if(!file.exists())
            FileUtil.saveResource("guis/permissions-gui.yml");

        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);

        title = ChatColor.translateAlternateColorCodes('&', cfg.getString("permissions-gui.title", "&lPermissions Controller ({0})"));
        inventory = FileUtil.loadGUI(islandPanelMenu, cfg.getConfigurationSection("permissions-gui"), 1, "&lPermissions Controller ({0})");

        slots = new ArrayList<>();
        Arrays.stream(cfg.getString("permissions-gui.slots").split(","))
                .forEach(slot -> slots.add(Integer.valueOf(slot)));

        previousButton = FileUtil.getItemStack(cfg.getConfigurationSection("permissions-gui.previous-page"));
        currentButton = FileUtil.getItemStack(cfg.getConfigurationSection("permissions-gui.current-page"));
        nextButton = FileUtil.getItemStack(cfg.getConfigurationSection("permissions-gui.next-page"));
        previousSlot = cfg.getInt("permissions-gui.previous-page.slot");
        currentSlot = cfg.getInt("permissions-gui.current-page.slot");
        nextSlot = cfg.getInt("permissions-gui.next-page.slot");
        inventory.setItem(previousSlot, previousButton);
        inventory.setItem(currentSlot, currentButton);
        inventory.setItem(nextSlot, nextButton);

        ConfigurationSection section = cfg.getConfigurationSection("permissions-gui.permissions");

        for(String permission : section.getKeys(false)){
            permission = permission.toLowerCase();
            permissionsData.put(permission + "-has-access-sound", getSound(section.getConfigurationSection(permission + ".has-access.sound")));
            permissionsData.put(permission + "-has-access-commands", getSound(section.getConfigurationSection(permission + ".has-access.commands")));
            permissionsData.put(permission + "-no-access-sound", getSound(section.getConfigurationSection(permission + ".no-access.sound")));
            permissionsData.put(permission + "-no-access-commands", getSound(section.getConfigurationSection(permission + ".no-access.commands")));
            permissionsData.put(permission + "-permission-enabled",
                    FileUtil.getItemStack(section.getConfigurationSection(permission + ".permission-enabled")));
            permissionsData.put(permission + "-permission-disabled",
                    FileUtil.getItemStack(section.getConfigurationSection(permission + ".permission-disabled")));
        }
    }

    public static void openInventory(SuperiorPlayer superiorPlayer, SuperiorMenu previousMenu, Island island, Object permissionHolder, String permissionHolderName){
        new IslandPermissionsMenu(superiorPlayer, island, permissionHolder, permissionHolderName).open(superiorPlayer, previousMenu);
    }

}