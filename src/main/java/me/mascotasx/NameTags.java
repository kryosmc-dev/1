package me.mascotasx;

import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.TextDisplay;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Nombre flotante independiente de la mascota (como el "name entity" de GPet).
 * Cuando la mascota esta volteada su nombre real es "Dinnerbone" (oculto) y este texto,
 * que no se voltea, sigue mostrando el nombre de la mascota encima de ella.
 */
public final class NameTags {
    private NameTags() {}

    private static final Map<UUID, TextDisplay> TAGS = new HashMap<>();

    private static Location spot(LivingEntity e) { return e.getLocation().add(0, e.getHeight() + 0.3, 0); }

    public static void show(LivingEntity e, Component text) {
        TextDisplay td = TAGS.get(e.getUniqueId());
        if (td == null || !td.isValid() || !td.getWorld().equals(e.getWorld())) {
            if (td != null) td.remove();
            td = e.getWorld().spawn(spot(e), TextDisplay.class, x -> {
                x.setPersistent(false);
                x.setBillboard(Display.Billboard.CENTER);
                x.setDefaultBackground(false);
                x.setTeleportDuration(2);
            });
            TAGS.put(e.getUniqueId(), td);
        }
        td.text(text);
    }

    /** Mueve el nombre junto a la mascota. false si el nombre ya no existe (hay que volver a crearlo). */
    public static boolean follow(LivingEntity e) {
        TextDisplay td = TAGS.get(e.getUniqueId());
        if (td == null || !td.isValid() || !td.getWorld().equals(e.getWorld())) return false;
        Location l = spot(e);
        if (td.getLocation().distanceSquared(l) > 0.0004) td.teleport(l);
        return true;
    }

    public static void remove(UUID pet) {
        TextDisplay td = TAGS.remove(pet);
        if (td != null) td.remove();
    }

    public static void removeAll() {
        for (TextDisplay td : new ArrayList<>(TAGS.values())) td.remove();
        TAGS.clear();
    }
}
