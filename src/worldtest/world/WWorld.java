package worldtest.world;

import com.jme3.app.state.AbstractAppState;
import com.jme3.asset.AssetNotFoundException;
import com.jme3.bullet.collision.shapes.HeightfieldCollisionShape;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.material.Material;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.terrain.geomipmap.TerrainLodControl;
import com.jme3.terrain.geomipmap.TerrainQuad;
import com.jme3.terrain.geomipmap.lodcalc.DistanceLodCalculator;
import com.jme3.terrain.heightmap.ImageBasedHeightMap;
import com.jme3.texture.Texture;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import worldtest.Main;

public final class WWorld extends AbstractAppState
{
    /*
    private final Main main;



    // private int viewDistance = 2;
    // private int totalChunks = (viewDistance + viewDistance + 1) * (viewDistance + viewDistance + 1);

    private boolean isLoaded = false;

    // general generation
    private WorldGenType genType;
    // private float worldHeight;

    // noise generation
    private HeightmapGenerator heightmapGenerator;

    // image / scene generation
    private String imagePath;

    private final Map<TerrainLocation, TerrainQuad> worldChunks = new HashMap<TerrainLocation, TerrainQuad>();

    private Material terrainMaterial;

    private WWorld(Main main, int blockSize, int tileSize, WorldGenType type)
    {
        this.main = main;



        this.adjuster = (this.blockSize - 1) / 2;
    }

    public void setHeightmapGenerator(HeightmapGenerator generator)
    {
        this.heightmapGenerator = generator;
    }










    public boolean isLoaded() { return this.isLoaded; }
    public int getLoadedChunkCount() { return this.worldChunks.size(); }

    // set initial lastLocation to to something we know the player isn't at right now.
    // this will force an update when the camera first joins the world.
    private int locX, locZ, lastLocX = Integer.MAX_VALUE, lastLocZ = Integer.MAX_VALUE;

    // put the point check in the middle of the block instead of the corner.
    int adjuster;

    @Override
    public void update(float tpf)
    {
        // signify that the world is ready to join.
        if (this.isLoaded == false && this.worldChunks.size() == this.totalChunks)
            this.isLoaded = true;

        // check if moved to another terrainquad
        float actualX = main.getCamera().getLocation().getX() + adjuster;
        float actualZ = main.getCamera().getLocation().getZ() + adjuster;

        locX = (int)actualX >> this.bitshift;
        locZ = (int)actualZ >> this.bitshift;

        if (locX == lastLocX && locZ == lastLocZ)
            return;

       updateSurroundingArea(locX, locZ);

       lastLocX = locX;
       lastLocZ = locZ;
    }

    private void updateSurroundingArea(final int locX, final int locZ)
    {
        // rather than make use of a ScheduledThreadPoolExecutor, we're only going to use one
        // thread and keep it friendly to single and dual-core machines.
        Thread thread = new Thread("Terrain Loading Thread")
        {
            @Override
            public void run()
            {
                TerrainLocation tLoc = new TerrainLocation(locX, locZ);
                List<TerrainLocation> surroundingChunkLocs = getSurroundingArea(tLoc, viewDistance);

                List<TerrainQuad> chunksToAdd = new ArrayList<TerrainQuad>();

                List<TerrainLocation> oldChunks = new ArrayList<TerrainLocation>(worldChunks.keySet());
                oldChunks.removeAll(surroundingChunkLocs);

                List<TerrainLocation> newChunks = new ArrayList<TerrainLocation>(surroundingChunkLocs);
                newChunks.removeAll(worldChunks.keySet());

                // this collection is usually returned empty when the camera is first added to the world.
                if (newChunks.isEmpty())
                    newChunks = surroundingChunkLocs;

                for (int i = 0; i < newChunks.size(); i++)
                {
                    TerrainLocation thisChunkLoc = newChunks.get(i);

                    int chunkX = Math.round(thisChunkLoc.getX());
                    int chunkZ = Math.round(thisChunkLoc.getZ());

                    TerrainQuad quad = getQuad(new TerrainLocation(chunkX, chunkZ));

                    chunksToAdd.add(quad);
                }

                final TerrainState newState = new TerrainState(chunksToAdd, oldChunks);

                // we now have a list of:
                // >> new chunks to add to the scene.
                // >> old chunks to remove from the scene
                // get back to the GL thread and carry out the tasks.

                main.enqueue(new Callable<Boolean>()
                {
                    public Boolean call() throws Exception
                    {
                        // remove old terrain first to try and keep triangle count down
                        for (int i = 0; i < newState.getOldChunks().size(); i++)
                        {
                            TerrainLocation tLoc = newState.getOldChunks().get(i);
                            TerrainQuad tq = worldChunks.get(tLoc);

                            // err on the side of caution.
                            if (tq == null)
                                continue;

                            // remove chunk
                            main.getRootNode().detachChild(tq);
                            main.getPhysicsSpace().remove(tq);
                            worldChunks.remove(tLoc);
                        }

                        // add new terrain
                        for (int i = 0; i < newState.getNewChunks().size(); i++)
                        {
                            TerrainQuad tq = newState.getNewChunks().get(i);

                            if (tq == null)
                                continue;

                            int locX = (int)tq.getLocalTranslation().getX() >> bitshift;
                            int locZ = (int)tq.getLocalTranslation().getZ() >> bitshift;

                            TerrainLocation tLoc = new TerrainLocation(locX, locZ);

                            worldChunks.put(tLoc, tq);
                            main.getRootNode().attachChild(tq);
                            main.getPhysicsSpace().add(tq);
                        }

                        return true;
                    }
                });
            }
        };

        thread.start();
    }

    private TerrainQuad getQuad(TerrainLocation location)
    {
        TerrainQuad tq = this.worldChunks.get(location);

        if (tq != null)
            return tq;

        String tqName = "chunk_" + location.getX() + "_" + location.getZ();

        // java 1.5 legacy...
        int usingNoise = this.useNoise ? 1 : 0;

        float[] heightmap = new float[this.blockSize * this.blockSize];

        switch(usingNoise)
        {
            case 0:
            {
                String imgPath = this.imagePath + "hmap_" + location.getX() + "_" + location.getZ() + ".png";
                Texture heightMapImage = null;

                try
                {
                    heightMapImage = main.getAssetManager().loadTexture(imgPath);
                }
                catch (AssetNotFoundException ex)
                {
                    return null;
                }

                ImageBasedHeightMap map = new ImageBasedHeightMap(heightMapImage.getImage(), this.worldHeight);
                heightmap = map.getHeightMap();

                break;
            }
            case 1:
            {
                heightmap = this.heightmapGenerator.get(location);
                break;
            }
            default:
            {
                Arrays.fill(heightmap, 0f);
                break;
            }
        }

        tq = new TerrainQuad(tqName, this.tileSize, this.blockSize, heightmap);

        // set position
        int tqLocX = location.getX() << this.bitshift;
        int tqLoxZ = location.getZ() << this.bitshift;

        tq.setLocalTranslation(new Vector3f(tqLocX, 0, tqLoxZ));

        // add LOD
        TerrainLodControl control = new TerrainLodControl(tq, main.getCamera());
        control.setLodCalculator( new DistanceLodCalculator(this.tileSize, 2.7f));
        tq.addControl(control);

        // add rigidity
        tq.addControl(new RigidBodyControl(new HeightfieldCollisionShape(heightmap), 0));

        tq.setMaterial(terrainMaterial);
        return tq;
    }

    private List<TerrainLocation> getSurroundingArea(TerrainLocation chunkLoc, int distance)
    {
        int topLx = chunkLoc.getX() - distance;
        int topLz = chunkLoc.getZ() - distance;

        int botLx = chunkLoc.getX() + distance;
        int botLz = chunkLoc.getZ() + distance;

        List<TerrainLocation> results = new ArrayList<TerrainLocation>();

        for (int x = topLx; x <= botLx; x++)
        {
            for (int z = topLz; z <= botLz; z++)
            {
                results.add(new TerrainLocation(x, z));
            }
        }

        return results;
    }

    public float getHeight(Vector3f location)
    {
        int tqLocX = (int)location.getX() >> this.bitshift;
        int tqLocZ = (int)location.getZ() >> this.bitshift;

        TerrainLocation tLoc = new TerrainLocation(tqLocX, tqLocZ);

        TerrainQuad tq = this.worldChunks.get(tLoc);

        if (tq == null)
            return 0f;

        float tqPosX = location.getX() - (tqLocX * this.blockSize);
        float tqPosZ = location.getZ() - (tqLocZ * this.blockSize);

        float height = tq.getHeightmapHeight(new Vector2f(tqPosX, tqPosZ));

        return height;
    }
    */
}
