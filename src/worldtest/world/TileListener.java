package worldtest.world;

import com.jme3.terrain.geomipmap.TerrainQuad;

public interface TileListener
{

    /**
     * This event is fired when a TerrainQuad has been constructed
     * and ready to add to the scene. This event occurs BEFORE the
     * TerrainQuad has been added.
     *
     * @param terrainQuad
     * @return false to cancel the tile from loading, else return
     * true to allow the tile to load.
     *
     */
    boolean tileLoaded(TerrainQuad terrainQuad);

    /**
     * This event is fired when a TerrainQuad has been flagged for removal
     * from the scene. This event occurs BEFORE the TerrainQuad has
     * been removed.
     *
     * @param terrainQuad
     * @return false to cancel the tile from unloading, else return
     * true to allow the tile to unload.
     */
    boolean tileUnloaded(TerrainQuad terrainQuad);

    /**
     * This event is fired when a heightmap image is required for
     * a TerrainQuad. This event is only fired when using an
     * ImageBasedWorld
     * @param x The X co-ordinate of the TerrainQuad
     * @param z The Z co-ordinate of the TerrainQuad
     * @return The path of the image file, including the file extension
     * For example: "/Textures/heightmaps/hmap_" + x + "_" + z + ".png"
     */
    String imageHeightmapRequired(int x, int z);
}
