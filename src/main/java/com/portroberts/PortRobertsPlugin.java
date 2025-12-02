package com.portroberts;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Provides;
import javax.inject.Inject;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.*;
import net.runelite.api.gameval.NpcID;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Slf4j
@PluginDescriptor(
	name = "Port Roberts"
)
public class PortRobertsPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private PortRobertsConfig config;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private PortRobertsOverlay portRobertsOverlay;


    private static final Set<Integer> MarketGuards = ImmutableSet.of(
            NpcID.PORT_ROBERTS_MARKET_GUARD,
            NpcID.PORT_ROBERTS_MARKET_GUARD_2,
            NpcID.PORT_ROBERTS_MARKET_GUARD_3
    );

    @Getter(AccessLevel.PACKAGE)
    private final List<NPC> Guards = new ArrayList<>();

    @Getter(AccessLevel.PACKAGE)
    private final List<GameObject> CballStall = new ArrayList<>();

    @Getter(AccessLevel.PACKAGE)
    private final List<GameObject> OreStall = new ArrayList<>();

    @Getter(AccessLevel.PACKAGE)
    private int currentTick = 0;

    @Getter(AccessLevel.PACKAGE)
    private boolean calibrating = true;


	@Override
	protected void startUp() throws Exception
	{
		log.debug("Example started!");
        overlayManager.add(portRobertsOverlay);
	}

	@Override
	protected void shutDown() throws Exception
	{
		log.debug("Example stopped!");
        // Clear text from guards, if you don't do this it just sits there
        for (NPC npc : Guards) {
            npc.setOverheadText("");
        }
        this.CballStall.clear();
        this.OreStall.clear();
        this.Guards.clear();
        overlayManager.remove(portRobertsOverlay);
	}

    @Subscribe
    public void onNpcSpawned(NpcSpawned npcSpawned) {
         if (MarketGuards.contains(npcSpawned.getNpc().getId())) {
            Guards.add(npcSpawned.getNpc());
         }
    }

    @Subscribe
    public void onNpcDespawned(NpcDespawned npcDespawned) {
        if (Guards.contains(npcDespawned.getNpc())) {
            npcDespawned.getNpc().setOverheadText("");
        }
        Guards.remove(npcDespawned.getNpc());
    }

    @Subscribe
    public void onGameObjectSpawned(GameObjectSpawned gameObjectSpawned) {
        GameObject object = gameObjectSpawned.getGameObject();
        if (object.getId() == net.runelite.api.gameval.ObjectID.PORT_ROBERTS_MARKET_STALL_CBALL) {
            this.CballStall.add(object);
        }
        if (object.getId() == net.runelite.api.gameval.ObjectID.PORT_ROBERTS_MARKET_STALL_ORE) {
            this.OreStall.add(object);
        }
    }

    @Subscribe
    public void onGameObjectDespawned(GameObjectDespawned gameObjectDespawned) {
        GameObject object = gameObjectDespawned.getGameObject();
        this.CballStall.remove(object);
        this.OreStall.remove(object);
    }

    @Subscribe
    public void onWorldViewUnloaded(WorldViewUnloaded e)
    {
        if (e.getWorldView().isTopLevel())
        {
            this.CballStall.clear();
            this.OreStall.clear();
        }
    }

    @Subscribe
    public void onClientTick(ClientTick tick) {
        for (NPC npc : Guards) {
            if (config.showNumbers()) {
                String output = getGuardString();
                npc.setOverheadText(output);
            } else {
                npc.setOverheadText("");
            }
        }
    }

    private String getGuardString() {
        String colorCode;
        Set<Integer> RedTicks = Set.of(8, 9, 10, 18, 19, 20, 28, 29, 30);

        // Determine msg color
        if (RedTicks.contains(this.currentTick) ) {
            colorCode = "<col=ff0000>";
        } else {
            colorCode = "<col=00ff00>";
        }

        // Separate countdown for the ore stall
        int totalTicks;
        if (this.currentTick <= 10) {
            totalTicks = 10;
        } else {
            totalTicks = 30;
        }

        // the 31'st tick is actually the 1'st but the guard doesn't officially step on our start tile until tick 2.
        String output;
        if (this.currentTick > 30) {
            output = colorCode + 1 + "</col>";
        } else {
            output = colorCode + String.valueOf(totalTicks - this.currentTick) + "</col>";
        }

        // If we haven't nailed the cycle down yet just throw a calibration message
        if (calibrating) {
            output = "";
        }
        return output;
    }

    @Subscribe
    public void onGameTick(GameTick gameTick) {
        WorldPoint tile = new WorldPoint(1867,3295, 0);
        for (NPC npc : Guards) {
            WorldPoint npcTile = npc.getWorldLocation();
            if (npcTile.equals(tile) & (this.currentTick > 10)) {
                this.currentTick = 1;
                this.calibrating = false;
            }
        }



        this.currentTick++;
//        if (this.currentTick > 30) {
//            this.currentTick = 1;
//        }
    }



	@Provides
    PortRobertsConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(PortRobertsConfig.class);
	}
}
