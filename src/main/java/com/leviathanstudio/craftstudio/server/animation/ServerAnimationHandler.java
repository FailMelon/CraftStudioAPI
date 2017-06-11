package com.leviathanstudio.craftstudio.server.animation;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import com.leviathanstudio.craftstudio.CraftStudioApi;
import com.leviathanstudio.craftstudio.common.animation.AnimationHandler;
import com.leviathanstudio.craftstudio.common.animation.Channel;
import com.leviathanstudio.craftstudio.common.animation.CustomChannel;
import com.leviathanstudio.craftstudio.common.animation.IAnimated;
import com.leviathanstudio.craftstudio.common.network.EndAnimationMessage;
import com.leviathanstudio.craftstudio.common.network.FireAnimationMessage;
import com.leviathanstudio.craftstudio.common.network.FireEndAnimationMessage;

import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.network.NetworkRegistry.TargetPoint;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.SERVER)
public class ServerAnimationHandler<T extends IAnimated> extends AnimationHandler<T>
{
    /** Map with all the animations. */
    private Map<String, Channel>          animChannels       = new HashMap<>();

    private Map<T, Map<String, AnimInfo>> currentAnimInfo    = new HashMap<>();

    private Map<T, Map<String, Float>>    startingAnimations = new HashMap<>();

    public ServerAnimationHandler() {
        super();
    }

    @Override
    public void addAnim(String modid, String animNameIn, String modelNameIn, boolean looped) {
        ResourceLocation anim = new ResourceLocation(modid, animNameIn);
        this.animChannels.put(anim.toString(), new Channel(anim.toString(), 60.0F, looped));
        this.channelIds.add(anim.toString());
    }

    @Override
    public void addAnim(String modid, String animNameIn, String modelNameIn, CustomChannel customChannelIn) {
        ResourceLocation anim = new ResourceLocation(modid, animNameIn);
        this.animChannels.put(anim.toString(), new Channel(anim.toString(), 60.0F, false));
        this.channelIds.add(anim.toString());
    }

    @Override
    public void addAnim(String modid, String invertedAnimationName, String animationToInvert) {
        ResourceLocation anim = new ResourceLocation(modid, invertedAnimationName);
        ResourceLocation inverted = new ResourceLocation(modid, animationToInvert);
        boolean looped = this.animChannels.get(inverted.toString()).looped;
        this.animChannels.put(anim.toString(), new Channel(anim.toString(), 60.0F, looped));
        this.channelIds.add(anim.toString());
    }

    @Override
    public void startAnimation(String ress, float startingFrame, T animatedElement) {
        if (!this.animChannels.containsKey(ress))
            return;
        if (!(animatedElement instanceof Entity))
            return;
        Map<String, Float> startingAnimMap = this.startingAnimations.get(animatedElement);
        if (startingAnimMap == null)
            this.startingAnimations.put(animatedElement, startingAnimMap = new HashMap<>());
        startingAnimMap.put(ress, startingFrame);
        Entity e = (Entity) animatedElement;
        CraftStudioApi.NETWORK.sendToAllAround(new FireAnimationMessage(ress, animatedElement, startingFrame),
                new TargetPoint(e.dimension, e.posX, e.posY, e.posZ, 100));
    }

    public void serverStartAnimation(String ress, float endingFrame, T animatedElement) {
        if (!this.animChannels.containsKey(ress))
            return;

        Map<String, Float> startingAnimMap = this.startingAnimations.get(animatedElement);
        if (startingAnimMap == null)
            return;

        Map<String, AnimInfo> animInfoMap = this.currentAnimInfo.get(animatedElement);
        if (animInfoMap == null)
            this.currentAnimInfo.put(animatedElement, animInfoMap = new HashMap<>());

        if (startingAnimMap.get(ress) == null) {
            CraftStudioApi.getLogger().warn("The animation called " + ress + " doesn't exist!");
            return;
        }

        Channel anim = this.animChannels.get(ress);
        anim.totalFrames = (int) endingFrame;
        animInfoMap.remove(ress);

        animInfoMap.put(ress, new AnimInfo(System.nanoTime(), startingAnimMap.get(ress)));
        startingAnimMap.remove(ress);
    }

    @Override
    public void stopAnimation(String res, T animatedElement) {
        if (!(animatedElement instanceof Entity))
            return;

        if (!this.animChannels.containsKey(res)) {
            CraftStudioApi.getLogger().warn("The animation stopped " + res + " doesn't exist!");
            return;
        }

        Map<String, AnimInfo> animInfoMap = this.currentAnimInfo.get(animatedElement);
        if (animInfoMap == null)
            return;

        Entity e = (Entity) animatedElement;
        CraftStudioApi.NETWORK.sendToAllAround(new EndAnimationMessage(res, animatedElement),
                new TargetPoint(e.dimension, e.posX, e.posY, e.posZ, 100));
        animInfoMap.remove(res);
    }

    @Override
    public void stopStartAnimation(String animToStop, String animToStart, float startingFrame, T animatedElement) {
        if (!(animatedElement instanceof Entity))
            return;
        Entity e = (Entity) animatedElement;

        if (!this.animChannels.containsKey(animToStop)) {
            CraftStudioApi.getLogger().warn("The animation stopped " + animToStop + " doesn't exist!");
            return;
        }
        if (!this.animChannels.containsKey(animToStart))
            return;
        Map<String, AnimInfo> animInfoMap = this.currentAnimInfo.get(animatedElement);
        if (animInfoMap != null)
            animInfoMap.remove(animToStop);

        Map<String, Float> startingAnimMap = this.startingAnimations.get(animatedElement);
        if (startingAnimMap == null)
            this.startingAnimations.put(animatedElement, startingAnimMap = new HashMap<>());
        startingAnimMap.put(animToStart, startingFrame);

        CraftStudioApi.NETWORK.sendToAllAround(new FireEndAnimationMessage(animToStart, animatedElement, startingFrame, animToStop),
                new TargetPoint(e.dimension, e.posX, e.posY, e.posZ, 100));
    }

    @Override
    public void animationsUpdate(T animatedElement) {
        Map<String, AnimInfo> animInfoMap = this.currentAnimInfo.get(animatedElement);
        if (animInfoMap == null)
            return;

        for (Iterator<Entry<String, AnimInfo>> it = animInfoMap.entrySet().iterator(); it.hasNext();) {
            Entry<String, AnimInfo> animInfo = it.next();
            float prevFrame = animInfo.getValue().prevTime;
            boolean animStatus = this.canUpdateAnimation(this.animChannels.get(animInfo.getKey()), animatedElement);
            if (!animStatus)
                it.remove();
        }
    }

    @Override
    public boolean isAnimationActive(String name, T animatedElement) {
        Map<String, AnimInfo> animInfoMap = this.currentAnimInfo.get(animatedElement);
        if (animInfoMap == null)
            return false;

        for (Entry<String, AnimInfo> animInfo : animInfoMap.entrySet())
            if (animInfo.getKey().equals(name))
                return true;
        return false;
    }

    /** Update animation values. Return false if the animation should stop. */
    @Override
    public boolean canUpdateAnimation(Channel channel, T animatedElement) {
        Map<String, AnimInfo> animInfoMap = this.currentAnimInfo.get(animatedElement);
        if (animInfoMap == null)
            return false;

        AnimInfo animInfo = animInfoMap.get(channel.name);
        if (animInfo == null)
            return false;

        long currentTime = System.nanoTime();

        double deltaTime = (currentTime - animInfo.prevTime) / 1000000000.0;
        float numberOfSkippedFrames = (float) (deltaTime * channel.fps);

        float currentFrame = animInfo.currentFrame + numberOfSkippedFrames;

        if (currentFrame < channel.totalFrames - 1) {
            animInfo.prevTime = currentTime;
            animInfo.currentFrame = currentFrame;
            return true;
        }
        if (channel.looped) {
            animInfo.prevTime = currentTime;
            animInfo.currentFrame = 0F;
            return true;
        }
        return false;
    }

    @Override
    public void clientStartAnimation(String res, float startingFrame, T animatedElement) {}

    @Override
    public void removeAnimated(T animated) {
        super.removeAnimated(animated);
        this.currentAnimInfo.remove(animated);
        this.startingAnimations.remove(animated);
    }
}
