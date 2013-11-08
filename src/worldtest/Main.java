package worldtest;

import com.jme3.app.SimpleApplication;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.font.BitmapText;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.system.AppSettings;
import com.jme3.terrain.noise.ShaderUtils;
import com.jme3.terrain.noise.basis.FilteredBasis;
import com.jme3.terrain.noise.filter.IterativeFilter;
import com.jme3.terrain.noise.filter.OptimizedErode;
import com.jme3.terrain.noise.filter.PerturbFilter;
import com.jme3.terrain.noise.filter.SmoothFilter;
import com.jme3.terrain.noise.fractal.FractalSum;
import com.jme3.terrain.noise.modulator.NoiseModulator;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture.WrapMode;
import worldtest.world.ImageBasedWorld;
import worldtest.world.NoiseBasedWorld;
import worldtest.world.TerrainChunk;
import worldtest.world.TileListener;
import worldtest.world.World;

public class Main extends SimpleApplication
{
    private static SimpleApplication app;
    private BulletAppState bulletAppState;

    private World world;

    float maxWorldHeight = 260;
    int tileSize = 65;
    int blockSize = 129;

    public static void main(String[] args)
    {
        app = new Main();

        app.setShowSettings(true);
        app.setDisplayStatView(true);

        AppSettings settings = new AppSettings(true);
        settings.setResolution(1280, 720);
        settings.setFullscreen(false);
        // settings.setFrameRate(20);
        app.setSettings(settings);
        app.start();
    }

    public PhysicsSpace getPhysicsSpace()
    {
        return this.bulletAppState.getPhysicsSpace();
    }

    @Override
    public void simpleInitApp()
    {
        bulletAppState = new BulletAppState();
        // bulletAppState.setThreadingType(BulletAppState.ThreadingType.PARALLEL);
        stateManager.attach(bulletAppState);

        // 1mph = 0.44704f
        float camSpeed = 0.44704f * 100;
        this.flyCam.setMoveSpeed(camSpeed);

        // display loading data
        initDebugInfo();

        // add sun
        DirectionalLight sun = new DirectionalLight();
        sun.setColor(ColorRGBA.White);
        sun.setDirection(new Vector3f(-1f, 0, 0));
        rootNode.addLight(sun);

        // set sky color
        this.viewPort.setBackgroundColor(new ColorRGBA(0.357f, 0.565f, 0.878f, 1f));

        createWorldWithNoise();
        // OR
        // createWorldWithImages();

        // attach to state manager so we can monitor movement.
        this.stateManager.attach(world);

        this.getCamera().setLocation(new Vector3f(0, 150, 0));
    }

    private Material createTerrainMaterial()
    {
        Material terrainMaterial = new Material(this.assetManager, "Common/MatDefs/Terrain/HeightBasedTerrain.j3md");

        float grassScale = 16;
        float dirtScale = 16;
        float rockScale = 16;

        // GRASS texture
        Texture grass = this.assetManager.loadTexture("Textures/Terrain/splat/grass.jpg");
        grass.setWrap(WrapMode.Repeat);
        terrainMaterial.setTexture("region1ColorMap", grass);
        terrainMaterial.setVector3("region1", new Vector3f(88, 200, grassScale));

        // DIRT texture
        Texture dirt = this.assetManager.loadTexture("Textures/Terrain/splat/dirt.jpg");
        dirt.setWrap(WrapMode.Repeat);
        terrainMaterial.setTexture("region2ColorMap", dirt);
        terrainMaterial.setVector3("region2", new Vector3f(0, 90, dirtScale));

        // ROCK texture

        Texture rock = this.assetManager.loadTexture("Textures/Terrain/Rock/Rock.PNG");
        rock.setWrap(WrapMode.Repeat);
        terrainMaterial.setTexture("region3ColorMap", rock);
        terrainMaterial.setVector3("region3", new Vector3f(198, 260, rockScale));

        terrainMaterial.setTexture("region4ColorMap", rock);
        terrainMaterial.setVector3("region4", new Vector3f(198, 260, rockScale));

        Texture rock2 = this.assetManager.loadTexture("Textures/Terrain/Rock2/rock.jpg");
        rock2.setWrap(WrapMode.Repeat);

        terrainMaterial.setTexture("slopeColorMap", rock2);
        terrainMaterial.setFloat("slopeTileFactor", 32);

        terrainMaterial.setFloat("terrainSize", blockSize);

        return terrainMaterial;
    }

    private void createWorldWithNoise()
    {
        NoiseBasedWorld newWorld = new NoiseBasedWorld(app, bulletAppState.getPhysicsSpace(), tileSize, blockSize);

        newWorld.setWorldHeight(192f);

        newWorld.setViewDistance(3);
        // newWorld.setViewDistance(14, 1, 2, 1);

        newWorld.setCacheTime(5000);

        Material terrainMaterial = createTerrainMaterial();
        newWorld.setMaterial(terrainMaterial);

        // create a noise generator
        FractalSum base = new FractalSum();

        base.setRoughness(0.7f);
        base.setFrequency(1.0f);
        base.setAmplitude(1.0f);
        base.setLacunarity(3.12f);
        base.setOctaves(8);
        base.setScale(0.02125f);
        base.addModulator(new NoiseModulator()
                {
                    @Override
                    public float value(float... in) {
                        return ShaderUtils.clamp(in[0] * 0.5f + 0.5f, 0, 1);
                    }
                });

        FilteredBasis ground = new FilteredBasis(base);

        PerturbFilter perturb = new PerturbFilter();
        perturb.setMagnitude(0.119f);

        OptimizedErode therm = new OptimizedErode();
        therm.setRadius(5);
        therm.setTalus(0.011f);

        SmoothFilter smooth = new SmoothFilter();
        smooth.setRadius(1);
        smooth.setEffect(0.7f);

        IterativeFilter iterate = new IterativeFilter();
        iterate.addPreFilter(perturb);
        iterate.addPostFilter(smooth);
        iterate.setFilter(therm);
        iterate.setIterations(1);

        ground.addPreFilter(iterate);

        newWorld.setFilteredBasis(ground);

        this.world = newWorld;
    }

    private void createWorldWithImages()
    {
        ImageBasedWorld newWorld = new ImageBasedWorld(app, bulletAppState.getPhysicsSpace(), tileSize, blockSize);

        newWorld.setWorldHeight(192f);

        newWorld.setViewDistance(3);
        // newWorld.setViewDistance(14, 1, 2, 1);

        Material terrainMaterial = createTerrainMaterial();
        newWorld.setMaterial(terrainMaterial);

        TileListener tileListener = new TileListener()
        {
            public boolean tileLoaded(TerrainChunk terrainChunk) { return true; }
            public boolean tileUnloaded(TerrainChunk terrainChunk) { return true; }

            public String imageHeightmapRequired(int x, int z)
            {
                String path = new StringBuilder()
                        .append("Textures/heightmaps/hmap_")
                        .append(x)
                        .append("_")
                        .append(z)
                        .append(".jpg")
                        .toString();

                return path;
            }

        };

        newWorld.setTileListener(tileListener);

        this.world = newWorld;
    }

    private BitmapText hudText;
    private void initDebugInfo()
    {
        guiFont = assetManager.loadFont("Interface/Fonts/Default.fnt");

        hudText = new BitmapText(guiFont, false);
        hudText.setSize(guiFont.getCharSet().getRenderedSize());
        hudText.setColor(ColorRGBA.Green);
        hudText.setLocalTranslation(10, settings.getHeight()-10, 0);
        guiNode.attachChild(hudText);
    }

    private void displayDebugInfo()
    {
        StringBuilder sb = new StringBuilder()
                .append("Loaded: ").append(world.getLoadedTileCount())
                .append("\n")
                .append("Cached: ").append(world.getCachedTilesCount())
                .append("\n")
                .append("Generating: ").append(world.getQuedGeneratingTilesCount())
                .append("\n")
                .append("Adding: ").append(world.getQuedGeneratedTilesCount());


        hudText.setText(sb.toString());
    }

    private boolean hasJoined = false;

    @Override
    public void simpleUpdate(float tpf)
    {
        displayDebugInfo();

        if (world.isLoaded() == false || hasJoined)
            return;

        hasJoined = true;

        float height = world.getHeight(new Vector3f(0, 0, 0));
        this.getCamera().setLocation(new Vector3f(0, height + 2, 0));
    }

    @Override
    public void simpleRender(RenderManager rm)
    {

    }

    @Override
    public void destroy()
    {
        super.destroy();

        if (world != null)
            world.close();
    }

}
