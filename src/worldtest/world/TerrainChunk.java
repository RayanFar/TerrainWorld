package worldtest.world;

import com.jme3.scene.Node;
import com.jme3.terrain.geomipmap.TerrainQuad;

public class TerrainChunk extends TerrainQuad
{
    private long cacheTime;

    private Node staticRigidObjects;
    private Node staticNonRigidObjects;

    public TerrainChunk(String name, int patchSize, int totalSize, float[] heightmap)
    {
        super(name, patchSize, totalSize, heightmap);
    }

    public long getCacheTime() { return this.cacheTime; }
    public void setCacheTime() { this.cacheTime = System.currentTimeMillis(); }

}
