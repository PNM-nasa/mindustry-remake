package mindustry.editor;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.input.*;
import arc.math.*;
import arc.math.geom.*;
import arc.scene.event.*;
import arc.scene.ui.*;
import arc.scene.ui.ImageButton.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.editor.MapObjectivesCanvas.ObjectiveTilemap.ObjectiveTile.*;
import mindustry.editor.MapObjectivesDialog.*;
import mindustry.game.MapObjectives.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;

@SuppressWarnings("unchecked")
public class MapObjectivesCanvas extends WidgetGroup{
    public static final int
        objWidth = 5, objHeight = 2,
        bounds = 100;

    public static final float unitSize = 48f;

    public Seq<MapObjective> objectives = new Seq<>();
    public ObjectiveTilemap tilemap;

    protected MapObjective query;

    private boolean pressed;
    private long visualPressed;

    public MapObjectivesCanvas(){
        setFillParent(true);
        addChild(tilemap = new ObjectiveTilemap());

        addCaptureListener(new InputListener(){
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, KeyCode button){
                if(query != null && button == KeyCode.mouseRight){
                    stopQuery();

                    event.stop();
                    return true;
                }else{
                    return false;
                }
            }
        });

        addCaptureListener(new ElementGestureListener(){
            int pressPointer = -1;

            @Override
            public void pan(InputEvent event, float x, float y, float deltaX, float deltaY){
                if(tilemap.moving != null || tilemap.connecting != null) return;
                tilemap.x = Mathf.clamp(tilemap.x + deltaX, -bounds * unitSize + width, 0f);
                tilemap.y = Mathf.clamp(tilemap.y + deltaY, -bounds * unitSize + height, 0f);
            }

            @Override
            public void tap(InputEvent event, float x, float y, int count, KeyCode button){
                if(query == null) return;

                Vec2 pos = localToDescendantCoordinates(tilemap, Tmp.v1.set(x, y));
                if(tilemap.createTile(
                    Mathf.round((pos.x - objWidth * unitSize / 2f) / unitSize),
                    Mathf.floor((pos.y - unitSize) / unitSize),
                    query
                )){
                    objectives.add(query);
                    stopQuery();
                }
            }

            @Override
            public void touchDown(InputEvent event, float x, float y, int pointer, KeyCode button){
                if(pressPointer != -1) return;
                pressPointer = pointer;
                pressed = true;
                visualPressed = Time.millis() + 100;
            }

            @Override
            public void touchUp(InputEvent event, float x, float y, int pointer, KeyCode button){
                if(pointer == pressPointer){
                    pressPointer = -1;
                    pressed = false;
                }
            }
        });
    }

    public void clearObjectives(){
        stopQuery();
        tilemap.clearTiles();
    }

    protected void stopQuery(){
        if(query == null) return;
        query = null;

        Core.graphics.restoreCursor();
    }

    public void query(MapObjective obj){
        stopQuery();
        query = obj;
    }

    public boolean isQuerying(){
        return query != null;
    }

    public boolean isVisualPressed(){
        return pressed || visualPressed > Time.millis();
    }

    public class ObjectiveTilemap extends WidgetGroup{
        protected final GridBits grid = new GridBits(bounds, bounds);

        /** The connector button that is being pressed. */
        protected @Nullable Connector connecting;
        /** The current tile that is being moved. */
        protected @Nullable ObjectiveTile moving;

        public ObjectiveTilemap(){
            setTransform(false);
            setSize(getPrefWidth(), getPrefHeight());
            touchable(() -> isQuerying() ? Touchable.disabled : Touchable.childrenOnly);
        }

        @Override
        public void draw(){
            validate();
            int minX = Math.max(Mathf.floor((x - 1f) / unitSize), 0), minY = Math.max(Mathf.floor((y - 1f) / unitSize), 0),
                maxX = Math.min(Mathf.ceil((x + width + 1f) / unitSize), bounds), maxY = Math.min(Mathf.ceil((y + height + 1f) / unitSize), bounds);
            float progX = x % unitSize, progY = y % unitSize;

            Lines.stroke(2f);
            Draw.color(Pal.gray, parentAlpha);

            for(int x = minX; x <= maxX; x++) Lines.line(progX + x * unitSize, minY * unitSize, progX + x * unitSize, maxY * unitSize);
            for(int y = minY; y <= maxY; y++) Lines.line(minX * unitSize, progY + y * unitSize, maxX * unitSize, progY + y * unitSize);

            if(isQuerying()){
                int tx, ty;
                Vec2 pos = screenToLocalCoordinates(Core.input.mouse());
                pos.x = x + (tx = Mathf.round((pos.x - objWidth * unitSize / 2f) / unitSize)) * unitSize;
                pos.y = y + (ty = Mathf.floor((pos.y - unitSize) / unitSize)) * unitSize;

                Lines.stroke(4f);
                Draw.color(
                    isVisualPressed() ? Pal.metalGrayDark : validPlace(tx, ty) ? Pal.accent : Pal.remove,
                    parentAlpha * (inPlaceBounds(tx, ty) ? 1f : Mathf.absin(3f, 1f))
                );

                Lines.rect(pos.x, pos.y, objWidth * unitSize, objHeight * unitSize);
            }

            if(moving != null){
                int tx, ty;
                float x = this.x + (tx = Mathf.round(moving.x / unitSize)) * unitSize;
                float y = this.y + (ty = Mathf.round(moving.y / unitSize)) * unitSize;

                Draw.color(
                    validMove(moving, tx, ty) ? Pal.accent : Pal.remove,
                    0.5f * parentAlpha * (inPlaceBounds(tx, ty) ? 1f : Mathf.absin(3f, 1f))
                );

                Fill.crect(x, y, objWidth * unitSize, objHeight * unitSize);
            }

            Draw.reset();
            super.draw();

            Draw.reset();
            Seq<ObjectiveTile> tiles = getChildren().as();

            Connector conTarget = null;
            if(connecting != null){
                Vec2 pos = connecting.localToAscendantCoordinates(this, Tmp.v1.set(connecting.pointX, connecting.pointY));
                if(hit(pos.x, pos.y, true) instanceof Connector con && connecting.canConnectTo(con)) conTarget = con;
            }

            boolean removing = false;
            for(var tile : tiles){
                for(var parent : tile.obj.parents){
                    var parentTile = tiles.find(t -> t.obj == parent);

                    Connector
                        conFrom = parentTile.conChildren,
                        conTo = tile.conParent;

                    if(conTarget != null && (
                        (connecting.findParent && connecting == conTo && conTarget == conFrom) ||
                        (!connecting.findParent && connecting == conFrom && conTarget == conTo)
                    )){
                        removing = true;
                        continue;
                    }

                    Vec2
                        from = conFrom.localToAscendantCoordinates(this, Tmp.v1.set(conFrom.getWidth() / 2f, conFrom.getHeight() / 2f)).add(x, y),
                        to = conTo.localToAscendantCoordinates(this, Tmp.v2.set(conTo.getWidth() / 2f, conTo.getHeight() / 2f)).add(x, y);

                    drawCurve(false, from.x, from.y, to.x, to.y);
                }
            }

            if(connecting != null){
                Vec2
                    mouse = (conTarget == null
                        ? connecting.localToAscendantCoordinates(this, Tmp.v1.set(connecting.pointX, connecting.pointY))
                        : conTarget.localToAscendantCoordinates(this, Tmp.v1.set(conTarget.getWidth() / 2f, conTarget.getHeight() / 2f))
                    ).add(x, y),
                    anchor = connecting.localToAscendantCoordinates(this, Tmp.v2.set(connecting.getWidth() / 2f, connecting.getHeight() / 2f)).add(x, y);

                Vec2
                    from = connecting.findParent ? mouse : anchor,
                    to = connecting.findParent ? anchor : mouse;

                drawCurve(removing, from.x, from.y, to.x, to.y);
            }

            Draw.reset();
        }

        protected void drawCurve(boolean remove, float x1, float y1, float x2, float y2){
            Lines.stroke(4f);
            Draw.color(remove ? Pal.remove : Pal.accent, parentAlpha);

            float dist = Math.abs(x1 - x2) / 2f;
            Lines.curve(x1, y1, x1 + dist, y1, x2 - dist, y2, x2, y2, Math.max(4, (int) (Mathf.dst(x1, y1, x2, y2) / 4f)));

            Draw.reset();
        }

        public boolean inPlaceBounds(int x, int y){
            return Structs.inBounds(x, y, bounds - objWidth + 1, bounds - objHeight + 1);
        }

        public boolean validPlace(int x, int y){
            if(!inPlaceBounds(x, y)) return false;
            for(int tx = 0; tx < objWidth; tx++){
                for(int ty = 0; ty < objHeight; ty++){
                    if(occupied(x + tx, y + ty)) return false;
                }
            }

            return true;
        }

        public boolean validMove(ObjectiveTile tile, int newX, int newY){
            if(!inPlaceBounds(newX, newY)) return false;

            int x = tile.tx, y = tile.ty;
            for(int tx = 0; tx < objWidth; tx++){
                for(int ty = 0; ty < objHeight; ty++){
                    grid.set(x + tx, y + ty, false);
                }
            }

            boolean valid = validPlace(newX, newY);
            for(int tx = 0; tx < objWidth; tx++){
                for(int ty = 0; ty < objHeight; ty++){
                    grid.set(x + tx, y + ty);
                }
            }

            return valid;
        }

        public boolean occupied(int x, int y){
            return grid.get(x, y);
        }

        public boolean createTile(MapObjective obj){
            return createTile(obj.editorX, obj.editorY, obj);
        }

        public boolean createTile(int x, int y, MapObjective obj){
            if(!validPlace(x, y)) return false;

            ObjectiveTile tile = new ObjectiveTile(obj, x, y);
            tile.pack();

            addChild(tile);
            for(int tx = 0; tx < objWidth; tx++){
                for(int ty = 0; ty < objHeight; ty++){
                    grid.set(x + tx, y + ty);
                }
            }

            return true;
        }

        public boolean moveTile(ObjectiveTile tile, int newX, int newY){
            if(!validMove(tile, newX, newY)) return false;

            int x = tile.tx, y = tile.ty;
            for(int tx = 0; tx < objWidth; tx++){
                for(int ty = 0; ty < objHeight; ty++){
                    grid.set(x + tx, y + ty, false);
                }
            }

            tile.pos(newX, newY);

            x = newX;
            y = newY;
            for(int tx = 0; tx < objWidth; tx++){
                for(int ty = 0; ty < objHeight; ty++){
                    grid.set(x + tx, y + ty);
                }
            }

            return true;
        }

        public void removeTile(ObjectiveTile tile){
            if(!tile.isDescendantOf(this)) return;
            tile.remove();

            int x = tile.tx, y = tile.ty;
            for(int tx = 0; tx < objWidth; tx++){
                for(int ty = 0; ty < objHeight; ty++){
                    grid.set(x + tx, y + ty, false);
                }
            }
        }

        public void clearTiles(){
            clearChildren();
            grid.clear();
        }

        @Override
        public float getPrefWidth(){
            return bounds * unitSize;
        }

        @Override
        public float getPrefHeight(){
            return bounds * unitSize;
        }

        public class ObjectiveTile extends Table{
            public final MapObjective obj;
            public int tx, ty;

            public final Mover mover;
            public final Connector conParent, conChildren;

            public ObjectiveTile(MapObjective obj, int x, int y){
                this.obj = obj;
                setTransform(false);
                setClip(false);

                add(conParent = new Connector(true)).size(unitSize);
                add(new ImageButton(Icon.move, new ImageButtonStyle(){{
                    up = Tex.whiteui;
                    imageUpColor = Color.black;
                }})).color(Pal.accent).height(unitSize).growX().get().addCaptureListener(mover = new Mover());
                add(conChildren = new Connector(false)).size(unitSize);

                row().table(Tex.buttonSelectTrans, t -> {
                    t.labelWrap(obj.typeName()).grow()
                        .color(Pal.accent).align(Align.left).padLeft(6f)
                        .ellipsis(true).get().setAlignment(Align.left);

                    t.table(b -> {
                        b.right().defaults().size(32f).pad((unitSize - 32f) / 2f - 4f);
                        b.button(Icon.pencilSmall, () -> {
                            BaseDialog dialog = new BaseDialog("@editor.objectives");
                            dialog.cont.pane(Styles.noBarPane, list -> list.top().table(e -> {
                                e.margin(0f);
                                MapObjectivesDialog.getInterpreter((Class<MapObjective>)obj.getClass()).build(
                                    e, obj.typeName(), new TypeInfo(obj.getClass()),
                                    null, null, null, null,
                                    () -> obj,
                                    res -> {}
                                );
                            }).width(400f).fillY()).grow();

                            dialog.addCloseButton();
                            dialog.show();
                        });
                        b.button(Icon.trashSmall, () -> removeTile(this));
                    }).growY().fillX();
                }).grow().colspan(3);

                setSize(getPrefWidth(), getPrefHeight());
                pos(x, y);
            }

            public void pos(int x, int y){
                tx = obj.editorX = x;
                ty = obj.editorY = y;
                this.x = x * unitSize;
                this.y = y * unitSize;
            }

            @Override
            public float getPrefWidth(){
                return objWidth * unitSize;
            }

            @Override
            public float getPrefHeight(){
                return objHeight * unitSize;
            }

            @Override
            public boolean remove(){
                if(super.remove()){
                    obj.parents.clear();

                    var it = objectives.iterator();
                    while(it.hasNext()){
                        var next = it.next();
                        if(next == obj){
                            it.remove();
                        }else{
                            next.parents.remove(obj);
                        }
                    }

                    return true;
                }else{
                    return false;
                }
            }

            public class Mover extends InputListener{
                public int prevX, prevY;
                public float lastX, lastY;

                @Override
                public boolean touchDown(InputEvent event, float x, float y, int pointer, KeyCode button){
                    if(moving != null) return false;
                    moving = ObjectiveTile.this;

                    prevX = moving.tx;
                    prevY = moving.ty;

                    // Convert to world pos first because the button gets dragged too.
                    Vec2 pos = event.listenerActor.localToStageCoordinates(Tmp.v1.set(x, y));
                    lastX = pos.x;
                    lastY = pos.y;

                    //moving.getScene().cancelTouchFocusExcept(this, event.listenerActor);
                    return true;
                }

                @Override
                public void touchDragged(InputEvent event, float x, float y, int pointer){
                    Vec2 pos = event.listenerActor.localToStageCoordinates(Tmp.v1.set(x, y));

                    moving.moveBy(pos.x - lastX, pos.y - lastY);
                    lastX = pos.x;
                    lastY = pos.y;

                    //moving.getScene().cancelTouchFocusExcept(this, event.listenerActor);
                }

                @Override
                public void touchUp(InputEvent event, float x, float y, int pointer, KeyCode button){
                    if(!moveTile(moving,
                        Mathf.round(moving.x / unitSize),
                        Mathf.round(moving.y / unitSize)
                    )) moving.pos(prevX, prevY);
                    moving = null;
                }
            }

            public class Connector extends Button{
                public float pointX, pointY;
                public final boolean findParent;

                public Connector(boolean findParent){
                    super(new ButtonStyle(){{
                        down = findParent ? Tex.buttonEdgeDown1 : Tex.buttonEdgeDown3;
                        up = findParent ? Tex.buttonEdge1 : Tex.buttonEdge3;
                        over = findParent ? Tex.buttonEdgeOver1 : Tex.buttonEdgeOver3;
                    }});

                    this.findParent = findParent;

                    clearChildren();
                    addCaptureListener(new InputListener(){
                        int conPointer = -1;

                        @Override
                        public boolean touchDown(InputEvent event, float x, float y, int pointer, KeyCode button){
                            if(conPointer != -1) return false;
                            conPointer = pointer;

                            if(connecting != null) return false;
                            connecting = Connector.this;

                            pointX = x;
                            pointY = y;
                            //connecting.getScene().cancelTouchFocusExcept(this, event.listenerActor);
                            return true;
                        }

                        @Override
                        public void touchDragged(InputEvent event, float x, float y, int pointer){
                            if(conPointer != pointer) return;
                            pointX = x;
                            pointY = y;
                            //connecting.getScene().cancelTouchFocusExcept(this, event.listenerActor);
                        }

                        @Override
                        public void touchUp(InputEvent event, float x, float y, int pointer, KeyCode button){
                            if(conPointer != pointer || connecting != Connector.this) return;
                            conPointer = -1;

                            Vec2 pos = Connector.this.localToAscendantCoordinates(ObjectiveTilemap.this, Tmp.v1.set(x, y));
                            if(ObjectiveTilemap.this.hit(pos.x, pos.y, true) instanceof Connector con && con.canConnectTo(Connector.this)){
                                if(findParent){
                                    if(!obj.parents.remove(con.tile().obj)) obj.parents.add(con.tile().obj);
                                }else{
                                    if(!con.tile().obj.parents.remove(obj)) con.tile().obj.parents.add(obj);
                                }
                            }

                            connecting = null;
                        }
                    });
                }

                public boolean canConnectTo(Connector other){
                    return
                        findParent != other.findParent &&
                        tile() != other.tile();
                }

                public ObjectiveTile tile(){
                    return ObjectiveTile.this;
                }

                @Override
                public boolean isPressed(){
                    return super.isPressed() || connecting == this;
                }

                @Override
                public boolean isOver(){
                    return super.isOver() && (connecting == null || connecting.canConnectTo(this));
                }
            }
        }
    }
}
