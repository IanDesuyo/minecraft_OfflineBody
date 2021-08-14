package io.github.IanDesuyo.offlineBody;

import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.GameMode;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;

import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.event.NPCDeathEvent;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.trait.trait.Equipment;
import net.citizensnpcs.api.trait.trait.Inventory;
import net.citizensnpcs.trait.SkinTrait;

public class App extends JavaPlugin implements Listener {

    @Override
    public void onEnable() {
        getLogger().info("Hello!");
        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        getLogger().info("Bye!");
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    private void onPlayerQuit(PlayerQuitEvent e) {
        Player player = e.getPlayer();
        if (player.hasPermission("offlinebody.bypass") || player.getGameMode() != GameMode.SURVIVAL) {
            return;
        }
        PlayerInventory inventory = player.getInventory();

        int randomId = ThreadLocalRandom.current().nextInt(10000);
        while (CitizensAPI.getNPCRegistry().getById(randomId) != null) {
            randomId = ThreadLocalRandom.current().nextInt(10000);
        }
        NPC npc = CitizensAPI.getNPCRegistry().createNPC(EntityType.PLAYER, player.getUniqueId(), randomId,
                player.getDisplayName() + "§c(Offline)");

        npc.setProtected(false);
        npc.getOrAddTrait(SkinTrait.class).setSkinName(player.getDisplayName(), true);

        Inventory npcInventory = npc.getOrAddTrait(Inventory.class);
        Equipment npcEquipment = npc.getOrAddTrait(Equipment.class);

        npcInventory.setContents(inventory.getStorageContents());
        npcEquipment.set(Equipment.EquipmentSlot.HELMET, inventory.getHelmet());
        npcEquipment.set(Equipment.EquipmentSlot.CHESTPLATE, inventory.getChestplate());
        npcEquipment.set(Equipment.EquipmentSlot.LEGGINGS, inventory.getLeggings());
        npcEquipment.set(Equipment.EquipmentSlot.BOOTS, inventory.getBoots());

        // handle held slot
        int heldSlot = inventory.getHeldItemSlot();
        ItemStack slot0 = inventory.getItem(0);
        npcEquipment.set(Equipment.EquipmentSlot.HAND, inventory.getItemInMainHand());
        npcInventory.setItem(heldSlot, slot0);

        npcEquipment.set(Equipment.EquipmentSlot.OFF_HAND, inventory.getItemInOffHand());

        npc.spawn(player.getLocation());
        getLogger().info(npc.getId() + " spawned");
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    private void onPlayerJoin(PlayerJoinEvent e) {
        Player player = e.getPlayer();
        NPC npc = CitizensAPI.getNPCRegistry().getByUniqueIdGlobal(player.getUniqueId());
        if (npc == null) {
            getLogger().info(player.getDisplayName() + "'s npc isn't exist");
            return;
        }

        PlayerInventory inventory = player.getInventory();
        inventory.clear();
        if (npc.isSpawned()) {
            getLogger().info(npc.getId() + " isSpawned");
            Inventory npcInventory = npc.getOrAddTrait(Inventory.class);
            Equipment npcEquipment = npc.getOrAddTrait(Equipment.class);

            inventory.setStorageContents(Arrays.copyOfRange(npcInventory.getContents(), 0, 36));
            inventory.setHelmet(npcInventory.getContents()[39]);
            inventory.setChestplate(npcInventory.getContents()[38]);
            inventory.setLeggings(npcInventory.getContents()[37]);
            inventory.setBoots(npcInventory.getContents()[36]);

            // handle held slot
            int heldSlot = inventory.getHeldItemSlot();
            ItemStack slot0 = npcEquipment.get(Equipment.EquipmentSlot.HAND);
            inventory.setItem(0, npcInventory.getContents()[heldSlot]);
            inventory.setItem(heldSlot, slot0);

            inventory.setItemInOffHand(npcEquipment.get(Equipment.EquipmentSlot.OFF_HAND));

            player.teleport(npc.getEntity().getLocation());
        } else {
            getLogger().info(npc.getId() + " not isSpawned");
            player.sendMessage("You're Dead when offline");
            player.setHealth(0);
        }
        npc.destroy();
        getLogger().info(npc.getId() + " destroyed");
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    private void onNPCDeath(NPCDeathEvent e) {
        NPC npc = e.getNPC();
        e.getDrops().clear();
        for (ItemStack item : npc.getOrAddTrait(Inventory.class).getContents()) {
            e.getDrops().add(item);
        }
        npc.despawn();
        getLogger().info(npc.getId() + " death, despawned");
    }
}