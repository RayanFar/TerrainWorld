package worldtest.world;

import com.jme3.terrain.geomipmap.TerrainQuad;
import java.util.List;

public final class TerrainState
{
    private final List<TerrainQuad> newChunks;
    private final List<TerrainLocation> oldChunks;

    public TerrainState(List<TerrainQuad> newChunks, List<TerrainLocation> oldChunks)
    {
        this.newChunks = newChunks;
        this.oldChunks = oldChunks;
    }

    public List<TerrainQuad> getNewChunks() { return this.newChunks; }
    public List<TerrainLocation> getOldChunks() { return this.oldChunks; }
}