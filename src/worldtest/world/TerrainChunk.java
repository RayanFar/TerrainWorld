package worldtest.world;

import com.jme3.scene.Node;
import com.jme3.terrain.geomipmap.TerrainLodControl;
import com.jme3.terrain.geomipmap.TerrainQuad;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TerrainChunk extends TerrainQuad
{
    private long cacheTime;

    private Node staticRigidObjects;
    private Node staticNonRigidObjects;

    private TerrainLodControl lodControl;

    public TerrainChunk(String name, int patchSize, int totalSize, float[] heightmap)
    {
        super(name, patchSize, totalSize, heightmap);

        File file = new File("./world/" + this.getName() + ".chunk");
        if (!file.exists())
        {
            try
            {
                this.save();
            }
            catch (IOException ex)
            {
                Logger.getLogger(TerrainChunk.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public long getCacheTime() { return this.cacheTime; }
    public void setCacheTime() { this.cacheTime = System.currentTimeMillis(); }

    public void save() throws IOException
    {
        float[] hmap = this.getHeightMap();

        ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream("./world/" + this.getName() + ".chunk"));
        out.writeObject(hmap);
        out.close();
    }
}
