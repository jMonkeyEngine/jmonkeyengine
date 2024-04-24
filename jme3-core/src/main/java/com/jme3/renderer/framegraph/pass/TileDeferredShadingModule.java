/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.jme3.renderer.framegraph.pass;

import com.jme3.renderer.framegraph.pass.DeferredShadingModule;
import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.material.TechniqueDef;
import com.jme3.material.logic.TileBasedDeferredSinglePassLightingLogic;
import com.jme3.material.logic.TileInfoProvider;
import com.jme3.material.logic.TiledRenderGrid;
import com.jme3.renderer.framegraph.MyFrameGraph;
import com.jme3.renderer.framegraph.RenderContext;
import com.jme3.renderer.framegraph.parameters.ValueRenderParam;
import java.util.LinkedList;

/**
 *
 * @author codex
 */
public class TileDeferredShadingModule extends DeferredShadingModule implements TileInfoProvider {
    
    public static final String TILE_INFO = "TileDeferredShading.TileInfo";
    private static final String MATDEF = "Common/MatDefs/ShadingCommon/TileBasedDeferredShading.j3md";
    private static final String PASS = "TileBasedDeferredPass";
    
    private ValueRenderParam<TiledRenderGrid> tileInfo;
    private final LinkedList<TileBasedDeferredSinglePassLightingLogic> logic = new LinkedList<>();
    
    public TileDeferredShadingModule(AssetManager assetManager) {
        super(assetManager);
    }
    
    @Override
    public void initialize(MyFrameGraph frameGraph) {
        super.initialize(frameGraph);
        tileInfo = addParameter(new ValueRenderParam<>(TILE_INFO, null, new TiledRenderGrid()));
    }
    @Override
    public void prepare(RenderContext context) {
        super.prepare(context);
        //context.getRenderManager().calculateTileInfo();
    }
    @Override
    public void execute(RenderContext context) {
        tileInfo.produce().update(context.getRenderManager().getCurrentCamera());
        super.execute(context);
    }
    @Override
    protected Material createMaterial() {
        return new Material(assetManager, MATDEF);
    }
    @Override
    protected void assignTechniqueLogic(Material mat) {
        for (TechniqueDef t : mat.getMaterialDef().getTechniqueDefs(PASS)) {
            TileBasedDeferredSinglePassLightingLogic l = new TileBasedDeferredSinglePassLightingLogic(t, this);
            logic.add(l);
            t.setLogic(l);
        }
    }
    @Override
    public TiledRenderGrid getTiledRenderGrid() {
        return tileInfo.produce();
    }
    
}
