package mindustry.entities.def;

import arc.func.*;
import arc.util.io.*;
import mindustry.annotations.Annotations.*;
import mindustry.entities.*;
import mindustry.gen.*;

import static mindustry.Vars.player;

@Component
@BaseComponent
abstract class EntityComp{
    private transient boolean added;
    transient int id = EntityGroup.nextId();

    boolean isAdded(){
        return added;
    }

    void update(){}

    void remove(){
        added = false;
    }

    void add(){
        added = true;
    }

    boolean isLocal(){
        return ((Object)this) == player || ((Object)this) instanceof Unitc && ((Unitc)((Object)this)).controller() == player;
    }

    boolean isNull(){
        return false;
    }

    <T> T as(Class<T> type){
        return (T)this;
    }

    <T> T with(Cons<T> cons){
        cons.get((T)this);
        return (T)this;
    }

    @InternalImpl
    abstract int classId();

    @InternalImpl
    abstract boolean serialize();

    @MethodPriority(1)
    void read(Reads read){
        afterRead();
    }

    void write(Writes write){

    }

    void afterRead(){

    }
}
