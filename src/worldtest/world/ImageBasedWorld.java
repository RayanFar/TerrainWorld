package worldtest.world;

import com.jme3.app.SimpleApplication;
import com.jme3.asset.AssetNotFoundException;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.collision.shapes.HeightfieldCollisionShape;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.material.Material;
import com.jme3.math.Vector3f;
import com.jme3.terrain.geomipmap.TerrainLodControl;
import com.jme3.terrain.geomipmap.TerrainQuad;
import com.jme3.terrain.geomipmap.lodcalc.DistanceLodCalculator;
import com.jme3.terrain.heightmap.AbstractHeightMap;
import com.jme3.terrain.heightmap.ImageBasedHeightMap;
import com.jme3.texture.Texture;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ImageBasedWorld extends World
{
    private Material terrainMaterial;

    public ImageBasedWorld(SimpleApplication app, PhysicsSpace physicsSpace, int tileSize, int blockSize)
    {
        super(app, physicsSpace, tileSize, blockSize);
    }

    public final Material getMaterial() { return this.terrainMaterial; }
    public final void setMaterial(Material material) { this.terrainMaterial = material; }

    @Override
    public TerrainQuad getTerrainQuad(TerrainLocation location)
    {
        TerrainQuad tq = this.worldTiles.get(location);

        if (tq != null)
            return tq;

        float[] heightmap = null;

        // fire the imageHeightmapRequired event to obtain the image path
        String imagePath = tileListener.imageHeightmapRequired(location.getX(), location.getZ());

        try
        {
            Texture hmapImage = app.getAssetManager().loadTexture(imagePath);
            AbstractHeightMap map = new ImageBasedHeightMap(hmapImage.getImage());
            map.load();

            heightmap = map.getHeightMap();
        }
        catch (AssetNotFoundException ex)
        {
            Logger.getLogger("com.jme").log(Level.INFO, "Image not found: {0}", imagePath);
            // The assetManager already logs null assets. don't re-iterate the point.
            heightmap = new float[this.blockSize * this.blockSize];
            Arrays.fill(heightmap, 0f);
        }

        String tqName = "TerrainQuad_" + location.getX() + "_" + location.getZ();

        tq = new TerrainQuad(tqName, this.tileSize, this.blockSize, heightmap);
        // tq.setLocalScale(new Vector3f(1f, this.worldHeight, 1f));

        // set position
        int tqLocX = location.getX() << this.bitshift;
        int tqLoxZ = location.getZ() << this.bitshift;
        tq.setLocalTranslation(new Vector3f(tqLocX, 0, tqLoxZ));

        // add LOD
        TerrainLodControl control = new TerrainLodControl(tq, app.getCamera());
        control.setLodCalculator( new DistanceLodCalculator(this.tileSize, 2.7f));
        tq.addControl(control);

        // add rigidity
        tq.addControl(new RigidBodyControl(new HeightfieldCollisionShape(heightmap), 0));

        tq.setMaterial(terrainMaterial);
        return tq;


    }

}
