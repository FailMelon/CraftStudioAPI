package com.leviathanstudio.craftstudio;

import com.leviathanstudio.craftstudio.client.registry.AssetAnimation;
import com.leviathanstudio.craftstudio.client.registry.AssetModel;
import com.leviathanstudio.craftstudio.client.registry.CSRegistryHelper;
import com.leviathanstudio.craftstudio.client.registry.RegistryHandler;
import com.leviathanstudio.craftstudio.client.util.EnumRenderType;
import com.leviathanstudio.craftstudio.client.util.EnumResourceType;
import com.leviathanstudio.craftstudio.common.animation.AnimationHandler;
import com.leviathanstudio.craftstudio.common.animation.IAnimated;
import com.leviathanstudio.craftstudio.proxy.CSClientProxy;
import com.leviathanstudio.craftstudio.proxy.CSCommonProxy;
import com.leviathanstudio.craftstudio.proxy.CSServerProxy;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.RegistryBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Main class of the CraftStudioApi
 *
 * @author ZeAmateis
 * @author Timmypote
 * @since 0.3.0
 */
@Mod(CraftStudioApi.API_ID)
public class CraftStudioApi {
    public static final String API_ID = "craftstudioapi";

    private static final Logger LOGGER = LogManager.getLogger("CraftStudio");
    private static CSCommonProxy proxy = DistExecutor.runForDist(() -> CSClientProxy::new, () -> CSServerProxy::new);

    private static IForgeRegistry<AssetModel> modelRegistry = null;
    private static IForgeRegistry<AssetAnimation> animationRegistry = null;
    AssetModel testModel = new AssetModel(EnumResourceType.MODEL, EnumRenderType.BLOCK, new ResourceLocation(API_ID, "craftstudio_api_test"));

    public CraftStudioApi() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::clientSetup);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::commonSetup);
        MinecraftForge.EVENT_BUS.register(this);
    }

    public static Logger getLogger() {
        return CraftStudioApi.LOGGER;
    }

    /**
     * Helper to create an AnimationHandler to registry animation to your
     * entity/block
     *
     * @param <T>
     * @param animatedClass which implements IAnimated (Entity or TileEntity)
     */
    public static <T extends IAnimated> AnimationHandler<T> getNewAnimationHandler(Class<T> animatedClass) {
        return CraftStudioApi.proxy.getNewAnimationHandler(animatedClass);

    }

    public void clientSetup(FMLClientSetupEvent event) {
        CraftStudioApi.proxy.clientSetup(event);
        loadCraftStudioLoaders(event);
    }

    public void commonSetup(FMLCommonSetupEvent event) {
        CraftStudioApi.proxy.commonSetup(event);
    }

    @SubscribeEvent
    public void onRegisterModel(RegistryEvent.Register<AssetModel> event) {
        event.getRegistry().register(testModel.setRegistryName("test", "test_model"));
    }

    @SubscribeEvent
    public void onCreateRegistry(RegistryEvent.NewRegistry event) {
        RegistryBuilder<AssetModel> modelBuilder = new RegistryBuilder<>();
        RegistryBuilder<AssetAnimation> animationBuilder = new RegistryBuilder<>();

        modelBuilder.setName(new ResourceLocation(API_ID, "models"));
        modelBuilder.setType(AssetModel.class);
        modelBuilder.setIDRange(0, 10_000);
        modelRegistry = modelBuilder.create();

        animationBuilder.setName(new ResourceLocation(API_ID, "animations"));
        animationBuilder.setType(AssetAnimation.class);
        animationBuilder.setIDRange(0, 10_000);
        animationRegistry = animationBuilder.create();
    }

    public void loadCraftStudioLoaders(FMLClientSetupEvent event) {
        RegistryHandler.init();
        CSRegistryHelper.loadModels();
        CSRegistryHelper.loadAnims();
    }


}