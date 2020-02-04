package mindustry.type;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.scene.ui.layout.Table;
import arc.util.ArcAnnotate.*;
import arc.util.Time;
import mindustry.ctype.ContentType;
import mindustry.entities.type.Player;
import mindustry.ctype.UnlockableContent;
import mindustry.graphics.Pal;
import mindustry.ui.*;

public class Mech extends UnlockableContent{
    public boolean flying;
    public float speed = 1.1f;
    public float maxSpeed = 10f;
    public float boostSpeed = 0.75f;
    public float drag = 0.4f;
    public float mass = 1f;
    public float shake = 0f;
    public float health = 200f;

    public float hitsize = 6f;
    public float cellTrnsY = 0f;
    public float mineSpeed = 1f;
    public int drillPower = -1;
    public float buildPower = 1f;
    public int itemCapacity = 30;
    public boolean turnCursor = true;
    public boolean canHeal = false;
    public float compoundSpeed, compoundSpeedBoost;

    /** draw the health and team indicator */
    public boolean drawCell = true;
    /** draw the items on its back */
    public boolean drawItems = true;
    /** draw the engine if it's flying/boosting */
    public boolean drawEngine = true;
    /** light emitted with lighting map rule enabled */
    public float lightEmitted = 50f;

    public Color engineColor = Pal.boostTo;
    public Color engineInnerColor = Color.white;
    public float engineRadius = 1f;
    public float engineInnerRadius = 0.5f;

    public float weaponOffsetX, weaponOffsetY, engineOffset = 5f, engineSize = 2.5f;
    public @NonNull Weapon weapon;

    public TextureRegion baseRegion, legRegion, region;

    public Mech(String name, boolean flying){
        super(name);
        this.flying = flying;
    }

    public Mech(String name){
        this(name, false);
    }

    public void updateAlt(Player player){
    }

    public void draw(Player player){
    }

    public void drawStats(Player player){
        if(drawCell){
            float health = player.healthf();
            Draw.color(Color.black, player.getTeam().color, health + Mathf.absin(Time.time(), health * 5f, 1f - health));
            Draw.rect(player.getPowerCellRegion(),
                player.x + Angles.trnsx(player.rotation, cellTrnsY, 0f),
                player.y + Angles.trnsy(player.rotation, cellTrnsY, 0f),
                player.rotation - 90);
            Draw.reset();
        }
        if(drawItems){
            player.drawBackItems();
        }
        if(lightEmitted > 0f){
            player.drawLight(lightEmitted);
        }
    }

    public void drawShadow(Player player, float offsetX, float offsetY){
        float scl = flying ? 1f : player.boostHeat / 2f;

        Draw.rect(icon(Cicon.full), player.x + offsetX * scl, player.y + offsetY * scl, player.rotation - 90);
    }
    
    public void drawEngine(Player player){
        float size = engineSize * (flying ? 1f : player.boostHeat);
        Draw.color(engineColor);
        Fill.circle(player.x + Angles.trnsx(player.rotation + 180, engineOffset), player.y + Angles.trnsy(player.rotation + 180, engineOffset),
        (size + Mathf.absin(Time.time(), 2f, size / 4f)) * engineRadius);

        Draw.color(engineInnerColor);
        Fill.circle(player.x + Angles.trnsx(player.rotation + 180, engineOffset - 1f), player.y + Angles.trnsy(player.rotation + 180, engineOffset - 1f),
        (size + Mathf.absin(Time.time(), 2f, size / 4f)) * engineInnerRadius);
        Draw.color();
    }

    public float getExtraArmor(Player player){
        return 0f;
    }

    public float spreadX(Player player){
        return 0f;
    }

    public float getRotationAlpha(Player player){
        return 1f;
    }

    public boolean canShoot(Player player){
        return true;
    }

    public void onLand(Player player){
    }

    @Override
    public void init(){
        super.init();

        for(int i = 0; i < 500; i++){
            compoundSpeed *= (1f - drag);
            compoundSpeed += speed;
        }

        for(int i = 0; i < 500; i++){
            compoundSpeedBoost *= (1f - drag);
            compoundSpeedBoost += boostSpeed;
        }
    }

    @Override
    public void displayInfo(Table table){
        ContentDisplay.displayMech(table, this);
    }

    @Override
    public ContentType getContentType(){
        return ContentType.mech;
    }

    @Override
    public void load(){
        weapon.load();
        if(!flying){
            legRegion = Core.atlas.find(name + "-leg");
            baseRegion = Core.atlas.find(name + "-base");
        }

        region = Core.atlas.find(name);
    }

    @Override
    public String toString(){
        return localizedName;
    }
}
