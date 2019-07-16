package io.anuke.mindustry.world.blocks.distribution;

import io.anuke.arc.Core;
import io.anuke.arc.collection.EnumSet;
import io.anuke.mindustry.content.Fx;
import io.anuke.mindustry.content.Items;
import io.anuke.mindustry.entities.Effects;
import io.anuke.mindustry.entities.type.TileEntity;
import io.anuke.mindustry.graphics.Pal;
import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.type.ItemStack;
import io.anuke.mindustry.ui.Bar;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.meta.BlockFlag;

import static io.anuke.mindustry.Vars.*;
import static io.anuke.mindustry.Vars.itemsValues;

public class ItemsEater extends Block{
    
    public ItemsEater(String name) {
        super(name);
        solid = true;
        update = true;
        destructible = true;
        hasItems = true;
        health = 150;
        flags = EnumSet.of(BlockFlag.target);
    }

    @Override
    public void setBars(){
        super.setBars();
        bars.add("points.bar", e -> new Bar(
                () -> Core.bundle.format("points.regular", (state.points(e.getTeam()) == -1) ? 0 : e.tile.<ItemsEaterEntity>entity().pointsEarned),
                () -> Pal.ammo,
                () -> (state.points[e.getTeam().ordinal()] == -1) ? 0f :(state.points(e.getTeam()) == 0) ? 1f : (int)(e.tile.<ItemsEaterEntity>entity().pointsEarned / state.points[e.getTeam().ordinal()])
        ));
    }

    @Override
    public void removed(Tile tile){
        super.removed(tile);
        state.teams.get(tile.getTeam()).eaters.remove(tile);
    }

    @Override
    public void onProximityUpdate(Tile tile){
        state.teams.get(tile.getTeam()).eaters.add(tile);
    }

    @Override
    public void handleItem(Item item, Tile tile, Tile source){
        if(!netServer.isWaitingForPlayers()){
            tile.<ItemsEater.ItemsEaterEntity>entity().pointsEarned += itemsValues[item.id] * (state.buffedItem != null && state.buffedItem == item ? state.rules.buffMultiplier : 1f);
            Effects.effect((state.buffedItem != null && state.buffedItem == item) ? Fx.itemsIncomeBuffed : Fx.itemsIncome, tile.getX(), tile.getY());
        }
    }

    @Override
    public boolean acceptItem(Item item, Tile tile, Tile source){
        return true;
    }

    @Override
    public TileEntity newEntity(){
        return new ItemsEaterEntity();
    }

    public class ItemsEaterEntity extends TileEntity{
        public float pointsEarned = 0f;
    }
}
