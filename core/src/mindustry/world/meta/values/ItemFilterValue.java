package mindustry.world.meta.values;

import arc.func.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

public class ItemFilterValue implements StatValue{
    private final Boolf<Item> filter;
    private final float timePeriod;

    public ItemFilterValue(Boolf<Item> filter){
        this.timePeriod = -1f;
        this.filter = filter;
    }

    public ItemFilterValue(Boolf<Item> filter, float timePeriod){
        this.timePeriod = timePeriod;
        this.filter = filter;
    }

    @Override
    public void display(Table table){
        Seq<Item> list = content.items().select(filter);

        for(int i = 0; i < list.size; i++){
            Item item = list.get(i);

            if(timePeriod > 0f){
                table.add(new ItemDisplay(item, 0, timePeriod, true)).padRight(5);
            }else{
                table.add(new ItemDisplay(item, 0, true)).padRight(5);
            }

            if(i != list.size - 1){
                table.add("/");
            }
        }
    }
}
