package tagplugin.tagplugin;

import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;

public final class TagPlugin extends JavaPlugin {
    private File tagConfigFile;
    private File configFile;

    private YamlConfiguration tagConfig = null;
    private YamlConfiguration config = null;

    // Player names mapped to their tags
    private static final HashMap<String, String> tags = new HashMap<String, String>();
    // Permission stubs mapped to their tags
    private static final HashMap<String, String> permission_tags = new HashMap<String, String>();

    public static String tag_format = "&a[&f%tag%&a]&r ";
    public static String tag_placeholder = "";
    public static int max_tag_length = 0;
    public static boolean allow_tag_colors = true;
    public static List<String> disallowed_tags = Arrays.asList("admin, mod");
    public static boolean alphanumeric_only = true;
    public static boolean enable_permission_tags = true;
    public static boolean enable_tab_list_tag = true;
    public static String tab_list_tag_format = "[%tag%]&r ";

    @Override
    public void onEnable() {
        tagConfigFile = new File(getDataFolder(), "tags.yml");
        configFile = new File(getDataFolder(), "config.yml");

        loadConfig();

        getCommand("newtag").setExecutor(new Commands(this));
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
    }

    private void loadConfig() {
        config = getConfiguration();

        boolean edited = false;
        if (!config.contains("tag_format")) {
            config.set("tag_format", tag_format);
            edited = true;
        }
        if (!config.contains("tag_placeholder")) {
            config.set("tag_placeholder", tag_placeholder);
            edited = true;
        }
        if (!config.contains("max_tag_length")) {
            config.set("max_tag_length", max_tag_length);
            edited = true;
        }
        if (!config.contains("allow_tag_colors")) {
            config.set("allow_tag_colors", allow_tag_colors);
            edited = true;
        }
        if (!config.contains("disallowed_tags")) {
            config.set("disallowed_tags", disallowed_tags);
            edited = true;
        }
        if (!config.contains("alphanumeric_only")) {
            config.set("alphanumeric_only", alphanumeric_only);
            edited = true;
        }
        if (!config.contains("enable_permission_tags")) {
            config.set("enable_permission_tags", enable_permission_tags);
            edited = true;
        }
        if (!config.contains("enable_tab_list_tag")) {
            config.set("enable_tab_list_tag", enable_tab_list_tag);
            edited = true;
        }
        if (!config.contains("tab_list_tag_format"))  {
            config.set("tab_list_tag_format", tab_list_tag_format);
            edited = true;
        }

        tag_placeholder = config.getString("tag_placeholder");
        tag_format = config.getString("tag_format");
        max_tag_length = config.getInt("max_tag_length");
        allow_tag_colors = config.getBoolean("allow_tag_colors");
        disallowed_tags = config.getStringList("disallowed_tags");
        alphanumeric_only = config.getBoolean("alphanumeric_only");
        enable_permission_tags = config.getBoolean("enable_permission_tags");
        enable_tab_list_tag = config.getBoolean("enable_tab_list_tag");
        tab_list_tag_format = config.getString("tab_list_tag_format");

        if (config.contains("tags")) { // old config, move tags to new file
            getTagConfig().set("tags", config.getConfigurationSection("tags"));
            saveTagConfig();
            config.set("tags", null);
            edited = true;
        }

        // load existing tags
        ConfigurationSection sect = getTagsSection();
        for (String player : sect.getKeys(false)) {
            tags.put(player, sect.getString(player));
        }

        // load configured permissible tags
        sect = getPermissionsSection();
        for (String permission : sect.getKeys(false)) {
            permission_tags.put(permission, sect.getString(permission));
        }

        if (edited) {
            getLogger().log(Level.INFO, "Attempting to add defualt value to config...");
            try {
                config.save(configFile);
            } catch (IOException ex) {
                getLogger().log(Level.WARNING, "Could not save config.yml", ex);
            }
        }

        getLogger().info("Configurations loaded successfully!");
    }

    private YamlConfiguration getConfiguration() {
        if (config == null) {
            if (!configFile.exists()) {
                this.saveResource("config.yml", true);
            }
            config = YamlConfiguration.loadConfiguration(configFile);
        }
        return config;
    }

    private YamlConfiguration getTagConfig() {
        if (tagConfig == null) {
            if (!tagConfigFile.exists()) {
                this.saveResource("tags.yml", true);
            }
            tagConfig = YamlConfiguration.loadConfiguration(tagConfigFile);
        }
        return tagConfig;
    }

    private ConfigurationSection getTagsSection() {
        ConfigurationSection sect = getTagConfig().getConfigurationSection("tags");
        if (sect == null) {
            sect = getTagConfig().createSection("tags");
        }
        return sect;
    }

    private ConfigurationSection getPermissionsSection() {
        ConfigurationSection sect = getTagConfig().getConfigurationSection("permission_tags");
        if (sect == null) {
            sect = getTagConfig().createSection("permission_tags");
        }
        return sect;
    }

    private void saveTagConfig() {
        try {
            tagConfig.save(tagConfigFile);
        } catch (IOException ex) {
            getLogger().log(Level.SEVERE, "Could not save tags.yml", ex);
        }
    }

    public void saveTag(String player, String tag) {
        ConfigurationSection sect = getTagsSection();
        if (tag == null || tag.isEmpty()) {
            sect.set(player, null);
            tags.remove(player);
        } else {
            sect.set(player, tag);
            tags.put(player, tag);
        }
        saveTagConfig();
    }

    /**
     * Checks if a given player a unique tag set.
     * @param player
     * @return
     */
    public boolean hasTagSet(String player) {
        return tags.containsKey(player);
    }

    /**
     * Returns the formatted tag for use in tab list
     * @param tag
     * @return the formatted tag
     */
    public String formatTabListTag(String tag) {
        return getFormattedTag(tag, TagPlugin.tab_list_tag_format);
    }

    /**
     * Formats a tag for use in the chat
     * @param tag the tag text to be formatted
     * @return the formatted tag
     */
    public String formatChatTag(String tag) {
        return getFormattedTag(tag, TagPlugin.tag_format);
    }

    private String getFormattedTag(String tag, String format) {
        if (TagPlugin.allow_tag_colors) {
            format = format.replace("%tag%", tag); // replace placeholder with tag
            format = ChatColor.translateAlternateColorCodes('&', format); // colorize tag format + tag
        } else {
            format = ChatColor.translateAlternateColorCodes('&', format); // colorize tag format only
            format = format.replace("%tag%", tag); // replace placeholder
        }
        return format;
    }

    /**
     * Returns the active tag of a player.
     * @param player
     * @return the unique player tag, or
     * if not found, the eligible permission tag.
     */
    public String getTag(Player player) {
        if (hasTagSet(player.getName())) {
            return getPlayerTag(player.getName());
        }
        if (TagPlugin.enable_permission_tags && !player.isOp()) {
            return getPermissionTag(player);
        }
        return null;
    }

    /**
     * Gets the unique tag set to a player.
     * @param player
     * @return null if no tag is set
     */
    public String getPlayerTag(String player) {
        return tags.get(player);
    }

    /**
     * Returns the permission tag for a player.
     * @param s
     * @return null if the player is not eligible for any permission tags
     */
    public String getPermissionTag(Player s) {
        for (String perm : permission_tags.keySet()) {
            if (s.hasPermission("newtag.ptag." + perm)) {
                return permission_tags.get(perm);
            }
        }
        return null;
    }

}