package com.workshopcraft.dire.client;

import com.workshopcraft.dire.gas.GasBlock;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

public class GasWispParticle extends SingleQuadParticle {
    private final float peakAlpha;

    private GasWispParticle(ClientLevel level, double x, double y, double z, SpriteSet sprites, RandomSource random) {
        super(level, x, y, z, 0.0, 0.0, 0.0, sprites.get(random));
        BlockState state = level.getBlockState(BlockPos.containing(x, y - 0.5, z));
        int lvl = state.getBlock() instanceof GasBlock ? state.getValue(GasBlock.LEVEL) : 1;
        this.xd = (random.nextDouble() - 0.5) * 0.012;
        this.yd = (random.nextDouble() - 0.6) * 0.006;
        this.zd = (random.nextDouble() - 0.5) * 0.012;
        this.friction = 0.97F;
        this.gravity = 0.0F;
        this.hasPhysics = false;
        this.lifetime = 50 + random.nextInt(40);
        this.quadSize = 0.06F + random.nextFloat() * 0.05F + lvl * 0.006F;
        this.roll = this.oRoll = random.nextFloat() * Mth.TWO_PI;
        float shade = 0.85F + random.nextFloat() * 0.15F;
        setColor(0.55F * shade, 0.95F * shade, 0.25F * shade);
        this.peakAlpha = 0.45F + lvl * 0.05F;
        this.alpha = 0.0F;
    }

    @Override
    public void tick() {
        super.tick();
        float t = (float) this.age / this.lifetime;
        this.alpha = this.peakAlpha * Math.min(1.0F, Math.min(t * 5.0F, (1.0F - t) * 2.5F));
    }

    @Override
    public int getLightCoords(float a) {
        return LightCoordsUtil.FULL_BRIGHT;
    }

    @Override
    protected SingleQuadParticle.Layer getLayer() {
        return SingleQuadParticle.Layer.TRANSLUCENT;
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType options, ClientLevel level, double x, double y, double z,
                                       double xAux, double yAux, double zAux, RandomSource random) {
            return new GasWispParticle(level, x, y, z, sprites, random);
        }
    }
}
