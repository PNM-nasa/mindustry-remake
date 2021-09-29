package mindustry.entities.abilities;

import arc.util.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.gen.*;

public class RepairFieldAbility extends Ability{
    public float amount = 1, reload = 100, range = 60;
    public boolean instant;
    public Effect healEffect = Fx.heal;
    public Effect activeEffect = Fx.healWaveDynamic;
    public boolean parentizeEffects = false;

    protected float timer = -1f;
    protected boolean wasHealed = false;

    RepairFieldAbility(){}

    public RepairFieldAbility(float amount, float reload, float range){
        this.amount = amount;
        this.reload = reload;
        this.range = range;
    }

    @Override
    public void update(Unit unit){
        if(timer < 0){
            timer = instant ? reload : 0f;
        }

        timer += Time.delta;

        if(timer >= reload){
            wasHealed = false;

            Units.nearby(unit.team, unit.x, unit.y, range, other -> {
                if(other.damaged()){
                    healEffect.at(other, parentizeEffects);
                    wasHealed = true;
                }
                other.heal(amount);
            });

            if(wasHealed){
                activeEffect.at(unit, range);
            }

            timer = 0f;
        }
    }
}
