package net.sumomo.dontlitteralfheim.entity;

public enum EnforcerAttackState {
    IDLE(0),
    TRASH_BARRAGE(1),
    MANA_LASER(2),
    REPULSION_NOVA(3),
    SUMMON_PIXIES(4),
    BABYLON_BARRAGE(5),
    RADIAL_CHARGE_BEAM(6),
    LIGHTNING_STRIKE(7),
    UNLIMITED_BLADE_BARRAGE(8);

    public final int id;

    EnforcerAttackState(int id) {
        this.id = id;
    }

    public static EnforcerAttackState byId(int id) {
        for (EnforcerAttackState state : values()) {
            if (state.id == id) {
                return state;
            }
        }
        return IDLE;
    }
}