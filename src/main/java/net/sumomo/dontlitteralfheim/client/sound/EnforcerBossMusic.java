//package net.sumomo.dontlitteralfheim.client.sound;
//
//import net.minecraft.client.Minecraft;
//import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
//import net.minecraft.client.resources.sounds.SoundInstance;
//import net.minecraft.sounds.SoundEvent;
//import net.minecraft.sounds.SoundSource;
//import net.sumomo.dontlitteralfheim.entity.AlfheimEnforcerEntity;
//import vazkii.botania.common.handler.BotaniaSounds;
//
//public class EnforcerBossMusic extends AbstractTickableSoundInstance {
//
//    private final AlfheimEnforcerEntity enforcer;
//
//    private EnforcerBossMusic(AlfheimEnforcerEntity enforcer, SoundEvent sound, float volume) {
//        super(sound, SoundSource.RECORDS, SoundInstance.createUnseededRandom());
//        this.enforcer = enforcer;
//        this.looping = true;
//        this.delay = 0;
//        this.volume = volume;
//        this.pitch = 1.0F;
//        this.x = enforcer.getX();
//        this.y = enforcer.getY();
//        this.z = enforcer.getZ();
//        this.attenuation = Attenuation.LINEAR;
//    }
//
//    @Override
//    public void tick() {
//        // 死亡・消滅、またはターゲット喪失（中立状態に戻る）で即停止
//        if (!this.enforcer.isAlive() || this.enforcer.isRemoved() || !this.enforcer.isAngry()) {
//            this.stop();
//            return;
//        }
//
//        this.x = this.enforcer.getX();
//        this.y = this.enforcer.getY();
//        this.z = this.enforcer.getZ();
//    }
//
//    public static void playDualTracks(AlfheimEnforcerEntity enforcer) {
//        Minecraft mc = Minecraft.getInstance();
//        // 2曲同時再生で音割れしないよう、各トラックの音量を少し抑えて重ねる
//        EnforcerBossMusic track1 = new EnforcerBossMusic(enforcer, BotaniaSounds.gaiaMusic1, 0.75F);
//        EnforcerBossMusic track2 = new EnforcerBossMusic(enforcer, BotaniaSounds.gaiaMusic2, 0.75F);
//        mc.getSoundManager().play(track1);
//        mc.getSoundManager().play(track2);
//    }
//}