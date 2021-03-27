package mindustry.logic;

import arc.struct.*;

/** Setter/getter enum for logic-controlled objects. */
public enum LAccess{
    totalItems,
    firstItem,
    firstItemNeeded,
    totalLiquids,
    totalPower,
    itemCapacity,
    liquidCapacity,
    powerCapacity,
    powerNetStored,
    powerNetCapacity,
    powerNetIn,
    powerNetOut,
    ammo,
    ammoCapacity,
    health,
    maxHealth,
    heat,
    efficiency,
    timescale,
    rotation,
    x,
    y,
    shootX,
    shootY,
    size,
    dead,
    range, 
    shooting,
    boosting,
    mineX,
    mineY,
    mining,
    team,
    type,
    flag,
    controlled,
    controller,
    commanded,
    name,
    config,
    payloadCount,
    payloadType,

    //values with parameters are considered controllable
    enabled("to"), //"to" is standard for single parameter access
    shoot("x", "y", "shoot"),
    shootp(true, "unit", "shoot"),
    configure(true, "to"),
    color("r", "g", "b");

    public final String[] params;
    public final boolean isObj;

    public static final LAccess[]
        all = values(),
        senseable = Seq.select(all, t -> t.params.length <= 1).toArray(LAccess.class),
        controls = Seq.select(all, t -> t.params.length > 0).toArray(LAccess.class);

    LAccess(String... params){
        this.params = params;
        isObj = false;
    }

    LAccess(boolean obj, String... params){
        this.params = params;
        isObj = obj;
    }

}
