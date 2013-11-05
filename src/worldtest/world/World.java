package worldtest.world;

import com.jme3.app.SimpleApplication;
import com.jme3.app.state.AbstractAppState;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.material.Material;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.terrain.geomipmap.TerrainQuad;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

public abstract class World extends AbstractAppState
{
    protected final SimpleApplication app;
    private final PhysicsSpace physicsSpace;

    protected final int blockSize;
    protected final int tileSize;
    protected final int bitshift;
    private final int positionAdjuster;

    private int nViewDistance = 2, eViewDistance = 2, sViewDistance = 2, wViewDistance = 2;

    protected float worldHeight = 1f;

    

    private boolean isLoaded;
    private int totalVisibleChunks = 25;
    private int locX, locZ, lastLocX = Integer.MAX_VALUE, lastLocZ = Integer.MAX_VALUE;

    protected TileListener tileListener;

    // TODO: implement cache
    protected final Map<TerrainLocation, TerrainQuad> worldTiles = new HashMap<TerrainLocation, TerrainQuad>();

    public World(SimpleApplication app, PhysicsSpace physicsSpace, int tileSize, int blockSize)
    {
        this.app = app;
        this.physicsSpace = physicsSpace;

        this.tileSize = tileSize;
        this.blockSize = blockSize;

        this.bitshift = this.bitCalc(blockSize);
        this.positionAdjuster = (this.blockSize - 1) / 2;
    }

    private int bitCalc(int blockSize)
    {
        switch (blockSize)
        {
            case 17: return 4;
            case 33: return 5;
            case 65: return 6;
            case 129: return 7;
            case 257: return 8;
            case 513: return 9;
            case 1024: return 10;
        }

        throw new IllegalArgumentException("Invalid block size specified.");
    }

    /**
     * Set the view distance in tiles for each direction according
     * to the initial view direction Vector3f.UNIT_Z.
     *
     * @param n Tiles to load north of the initial view direction.
     * @param e Tiles to load east of the initial view direction.
     * @param s Tiles to load south of the initial view direction.
     * @param w Tiles to load west of the initial view direction.
     */
    public final void setViewDistance(int n, int e, int s, int w)
    {
        this.nViewDistance = n;
        this.eViewDistance = e;
        this.sViewDistance = s;
        this.wViewDistance = w;

        totalVisibleChunks = (wViewDistance + eViewDistance + 1) * (nViewDistance + sViewDistance + 1);
    }


    /**
     * Set the view distance in tiles for all directions.
     *
     * @param distance Tiles to load in all directions.
     */
    public final void setViewDistance(int distance)
    {
        // this.nViewDistance = this.eViewDistance = this.sViewDistance = this.wViewDistance = distance;
        setViewDistance(distance, distance, distance, distance);
    }

    /**
     *
     * @return notes whether or not this world has loaded all tiles required.
     */
    public final boolean isLoaded()
    {
        return this.isLoaded;
    }

    /**
     *
     * @return the amount of tiles that are currently loaded.
     */
    public final int getLoadedTileCount()
    {
        return this.worldTiles.size();
    }

    /**
     *
     * @return the maximum height of this world.
     */
    public final float getWorldHeight()
    {
        return this.worldHeight;
    }

    /**
     * Set the maximum view height for this world.
     *
     * @param height The maximum height of this world.
     */
    public final void setWorldHeight(float height)
    {
        this.worldHeight = height;
    }

    public void setTileListener(TileListener listener)
    {
        this.tileListener = listener;
    }

    private volatile boolean loadingFinished = true;
    private void updateSurroundingArea(final int locX, final int locZ)
    {
        if (!loadingFinished)
            return;

        Thread thread = new Thread("Terrain Loading Thread")
        {
            @Override
            public void run()
            {
                loadingFinished = false;

                TerrainLocation tLoc = new TerrainLocation(locX, locZ);
                List<TerrainLocation> surroundingChunkLocs = getSurroundingArea(tLoc);

                // remove all new tiles from the current tiles, leaving us with the tiles to remove.
                List<TerrainLocation> oldTiles = new ArrayList<TerrainLocation>(worldTiles.keySet());
                oldTiles.removeAll(surroundingChunkLocs);

                // remove all the current tiles from the new tiles, leaving us with the tiles to load.
                List<TerrainLocation> newTiles = new ArrayList<TerrainLocation>(surroundingChunkLocs);
                newTiles.removeAll(worldTiles.keySet());

                // this collection is only empty when the camera is first added to the world.
                if (newTiles.isEmpty())
                    newTiles = surroundingChunkLocs;

                List<TerrainQuad> chunksToAdd = new ArrayList<TerrainQuad>();

                for (int i = 0; i < newTiles.size(); i++)
                {
                    TerrainLocation thisChunkLoc = newTiles.get(i);

                    TerrainQuad quad = getTerrainQuad(new TerrainLocation(thisChunkLoc.getX(), thisChunkLoc.getZ()));

                    chunksToAdd.add(quad);
                }

                // a final collection of new tiles to load, and old tiles to remove.
                final TerrainState newState = new TerrainState(chunksToAdd, oldTiles);

                // get back to the GL thread and carry out the tasks.
                app.enqueue(new Callable<Boolean>()
                {
                    public Boolean call() throws Exception
                    {
                        // remove old terrain first to keep triangle count down
                        for (int i = 0; i < newState.getOldChunks().size(); i++)
                        {
                            TerrainLocation tLoc = newState.getOldChunks().get(i);
                            TerrainQuad tq = worldTiles.get(tLoc);

                            // push the unloaded event to the listener.
                            boolean allowLoading = tileUnloaded(tq);

                            if (!allowLoading)
                                continue;

                            // remove chunk
                            app.getRootNode().detachChild(tq);

                            physicsSpace.remove(tq);
                            worldTiles.remove(tLoc);
                        }

                        // add new terrain
                        for (int i = 0; i < newState.getNewChunks().size(); i++)
                        {
                            TerrainQuad tq = newState.getNewChunks().get(i);

                            int locX = (int)tq.getLocalTranslation().getX() >> bitshift;
                            int locZ = (int)tq.getLocalTranslation().getZ() >> bitshift;

                            TerrainLocation tLoc = new TerrainLocation(locX, locZ);

                            // push the loaded event to the listener.
                            boolean allowUnloading = tileLoaded(tq);

                            if (!allowUnloading)
                                continue;

                            worldTiles.put(tLoc, tq);
                            app.getRootNode().attachChild(tq);
                            physicsSpace.add(tq);
                        }

                        return loadingFinished = true;
                    }
                });
            }
        };

        thread.start();
    }

    private boolean tileLoaded(TerrainQuad quad)
    {
        if (this.tileListener != null)
            return this.tileListener.tileLoaded(quad);

        return true;
    }
    public boolean tileUnloaded(TerrainQuad quad)
    {
        if (this.tileListener != null)
            return this.tileListener.tileUnloaded(quad);

        return true;
    }

    @Override
    public void update(float tpf)
    {
        // used to signify that the world is ready to join.
        if (this.isLoaded == false && this.worldTiles.size() == this.totalVisibleChunks)
            this.isLoaded = true;

        // check if camera moved to another terrainquad
        float actualX = app.getCamera().getLocation().getX() + positionAdjuster;
        float actualZ = app.getCamera().getLocation().getZ() + positionAdjuster;

        locX = (int)actualX >> this.bitshift;
        locZ = (int)actualZ >> this.bitshift;

        // if the camera hasnt moved, dont update the surrounding chunks.
        if (locX == lastLocX && locZ == lastLocZ)
            return;

       updateSurroundingArea(locX, locZ);

       lastLocX = locX;
       lastLocZ = locZ;
    }

    public abstract TerrainQuad getTerrainQuad(TerrainLocation location);

    private List<TerrainLocation> getSurroundingArea(TerrainLocation chunkLoc)
    {
        int topLx = chunkLoc.getX() - wViewDistance;
        int topLz = chunkLoc.getZ() - nViewDistance;

        int botRx = chunkLoc.getX() + eViewDistance;
        int botRz = chunkLoc.getZ() + sViewDistance;

        List<TerrainLocation> results = new ArrayList<TerrainLocation>();

        for (int x = topLx; x <= botRx; x++)
        {
            for (int z = topLz; z <= botRz; z++)
            {
                results.add(new TerrainLocation(x, z));
            }
        }

        return results;
    }

    /**
     *
     * @param location The X and Z location in which you need the height
     * @return the height of the location. Returns 0f if the tile is not loaded.
     */
    public final float getHeight(Vector3f location)
    {
        int tqLocX = (int)location.getX() >> this.bitshift;
        int tqLocZ = (int)location.getZ() >> this.bitshift;

        TerrainLocation tLoc = new TerrainLocation(tqLocX, tqLocZ);

        TerrainQuad tq = this.worldTiles.get(tLoc);

        if (tq == null)
            return 0f;

        float tqPosX = location.getX() - (tqLocX * this.blockSize);
        float tqPosZ = location.getZ() - (tqLocZ * this.blockSize);

        float height = tq.getHeightmapHeight(new Vector2f(tqPosX, tqPosZ));

        return height * this.worldHeight;
    }

}
