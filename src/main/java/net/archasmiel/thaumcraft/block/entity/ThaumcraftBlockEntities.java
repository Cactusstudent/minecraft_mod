package net.archasmiel.thaumcraft.block.entity;

import net.archasmiel.thaumcraft.Thaumcraft;
import net.archasmiel.thaumcraft.register.BlockRegister;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

public class ThaumcraftBlockEntities {

    public static BlockEntityType<ArcaneWorkbenchBlockEntity> ARCANE_WORKBENCH;

    public static void register(){
        ARCANE_WORKBENCH = Registry.register(
            Registry.BLOCK_ENTITY_TYPE,
            new Identifier(Thaumcraft.MOD_ID, "arcane_workbench"),
            FabricBlockEntityTypeBuilder.create(
                    ArcaneWorkbenchBlockEntity::new,
                    BlockRegister.ARCANE_WORKBENCH.getBlock()
            ).build(null)
        );
    }

}