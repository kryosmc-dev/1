package me.mascotasx;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.view.AnvilView;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Renombrar la mascota con un yunque (como GPet): se escribe el nombre y se toma el resultado. */
public final class AnvilRename implements Listener {
    private final MascotasX plugin;
    private static final Map<UUID, String> SESSIONS = new ConcurrentHashMap<>();

    public AnvilRename(MascotasX plugin) { this.plugin = plugin; }

    /** Abre el yunque. false si no se pudo (entonces se usa el chat). */
    public static boolean open(MascotasX plugin, Player p, String petId) {
        PetData d = plugin.manager().profile(p.getUniqueId()).owned.get(petId);
        PetType t = plugin.manager().type(petId);
        if (d == null || t == null) return false;
        try {
            SESSIONS.put(p.getUniqueId(), petId);
            InventoryView v = p.openAnvil(p.getLocation(), true);
            if (v == null) { SESSIONS.remove(p.getUniqueId()); return false; }
            ItemStack paper = new ItemStack(Material.NAME_TAG);
            ItemMeta m = paper.getItemMeta();
            m.displayName(Component.text(d.name != null ? d.name : t.name()).decoration(TextDecoration.ITALIC, false));
            paper.setItemMeta(m);
            v.getTopInventory().setItem(0, paper);
            try { v.setTitle("Renombrar mascota"); } catch (Throwable ignored) { }
            plugin.msg(p, "name-anvil");
            return true;
        } catch (Throwable ex) {
            SESSIONS.remove(p.getUniqueId());
            plugin.getLogger().fine("Yunque no disponible, se usa el chat: " + ex.getMessage());
            return false;
        }
    }

    private static boolean mine(Player p) { return SESSIONS.containsKey(p.getUniqueId()); }

    @EventHandler
    public void onPrepare(PrepareAnvilEvent ev) {
        if (!(ev.getView().getPlayer() instanceof Player p) || !mine(p)) return;
        String txt = ev.getView().getRenameText();
        if (txt == null || txt.isBlank()) { ev.setResult(null); return; }
        ItemStack res = new ItemStack(Material.NAME_TAG);
        ItemMeta m = res.getItemMeta();
        m.displayName(Component.text(txt).decoration(TextDecoration.ITALIC, false));
        res.setItemMeta(m);
        ev.setResult(res);
        try { ev.getView().setRepairCost(0); } catch (Throwable ignored) { }
    }

    @EventHandler
    public void onClick(InventoryClickEvent ev) {
        if (!(ev.getWhoClicked() instanceof Player p) || !mine(p)) return;
        if (ev.getView().getTopInventory().getType() != InventoryType.ANVIL) return;
        ev.setCancelled(true); // nada se mueve dentro del yunque
        if (ev.getRawSlot() != 2 || !(ev.getView() instanceof AnvilView av)) return;
        String txt = av.getRenameText();
        String petId = SESSIONS.remove(p.getUniqueId());
        if (txt == null || txt.isBlank() || petId == null) return;
        Bukkit.getScheduler().runTask(plugin, () -> {
            p.closeInventory();
            plugin.listener().setName(p, petId, txt.trim());
        });
    }

    @EventHandler
    public void onDrag(InventoryDragEvent ev) {
        if (ev.getWhoClicked() instanceof Player p && mine(p) && ev.getView().getTopInventory().getType() == InventoryType.ANVIL)
            ev.setCancelled(true);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent ev) {
        if (!(ev.getPlayer() instanceof Player p) || !mine(p)) return;
        if (ev.getInventory().getType() != InventoryType.ANVIL) return;
        SESSIONS.remove(p.getUniqueId());
        ev.getInventory().clear(); // el item de ejemplo no se devuelve al jugador
    }
}
