package worldtest.world;

public final class TerrainLocation
{
    private final int x, z;

    public TerrainLocation(int x, int z)
    {
        this.x = x;
        this.z = z;
    }

    public TerrainLocation(float x, float z)
    {
        this.x = (int)x;
        this.z = (int)z;
    }

    public int getX() { return this.x; }
    public int getZ() { return this.z; }

    @Override
    public boolean equals(Object obj)
    {
        if ((obj instanceof TerrainLocation) == false)
            return false;

        TerrainLocation location = (TerrainLocation)obj;

        // check hash first, quicker.
        if (this.hashCode() != location.hashCode())
            return false;

        // hash collision fallback, slower.
        return (this.x == location.getX() && this.z == location.getZ());
    }

    @Override
    public int hashCode()
    {
        // prime numbers are faster!
        int hash = 7;
        hash = 97 * hash + this.x;
        hash = 97 * hash + this.z;
        return hash;
    }
}