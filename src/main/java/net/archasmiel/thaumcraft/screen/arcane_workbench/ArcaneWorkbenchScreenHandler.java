package net.archasmiel.thaumcraft.screen.arcane_workbench;

import net.archasmiel.thaumcraft.block.Blocks;
import net.archasmiel.thaumcraft.blockentity.ArcaneWorkbenchBlockEntity;
import net.archasmiel.thaumcraft.blockentity.inventory.ImplementedInventory;
import net.archasmiel.thaumcraft.item.wandcraft.abilities.VisCraft;
import net.archasmiel.thaumcraft.networking.PacketIDs;
import net.archasmiel.thaumcraft.recipe.VisCraftingRecipe;
import net.archasmiel.thaumcraft.screen.ScreenHandlers;
import net.archasmiel.thaumcraft.screen.arcane_workbench.inventory.CraftingWandInventory;
import net.archasmiel.thaumcraft.screen.arcane_workbench.slot.ResultSlot;
import net.archasmiel.thaumcraft.screen.arcane_workbench.slot.WandSlot;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.inventory.CraftingResultInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;
import net.minecraft.recipe.CraftingRecipe;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeManager;
import net.minecraft.recipe.RecipeMatcher;
import net.minecraft.recipe.book.RecipeBookCategory;
import net.minecraft.screen.AbstractRecipeScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.world.World;

import java.util.Map;
import java.util.Optional;

import static net.archasmiel.thaumcraft.recipe.Recipes.VIS_RECIPE_TYPE;
import static net.minecraft.recipe.RecipeType.CRAFTING;

public class ArcaneWorkbenchScreenHandler extends AbstractRecipeScreenHandler<CraftingInventory> {

    private final int outputIndex;
    private final int wandIndex;
    private final int craftingIndexA;
    private final int craftingIndexB;
    private final int inventoryIndexA;
    private final int inventoryIndexB;
    private final int hotbarIndexA;
    private final int hotbarIndexB;

    private final ArcaneWorkbenchBlockEntity entity;
    private boolean isReading;

    private final CraftingInventory input;
    private final CraftingResultInventory result;
    private final CraftingWandInventory wand;
    private final ScreenHandlerContext context;
    private final PlayerEntity player;





    public ArcaneWorkbenchScreenHandler(int i, PlayerInventory inventory) {
        this(i, inventory, ScreenHandlerContext.EMPTY, null);

    }

    public ArcaneWorkbenchScreenHandler(int syncId, PlayerInventory playerInventory, ScreenHandlerContext context, ArcaneWorkbenchBlockEntity entity) {
        super(ScreenHandlers.ARCANE_WORKBENCH_SCREEN_HANDLER, syncId);

        this.entity = entity;

        this.input = new CraftingInventory(this, 3, 3);
        this.result = new CraftingResultInventory();
        this.wand = new CraftingWandInventory(this);

        this.context = context;
        this.player = playerInventory.player;



        // output slot
        outputIndex = 0;
        this.addSlot(new ResultSlot(playerInventory.player, this.input, this.wand, this.result, 0, 160, 64));

        // wand slot
        wandIndex = 1;
        this.addSlot(new WandSlot(this.wand, 0, 160, 25));

        // crafting slots
        craftingIndexA = 2;
        craftingIndexB = 11;
        for (int i = 0 ; i < 3 ; i++) {
            for (int j = 0; j < 3; j++) {
                this.addSlot(new Slot(this.input, i * 3 + j, 40 + j * 24, 40 + i * 24));
            }
        }

        // inventory
        inventoryIndexA = 11;
        inventoryIndexB = 38;
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                this.addSlot(new Slot(playerInventory, 9 + i * 9 + j, 16 + 18 * j, 151 + 18 * i));
            }
        }

        // hotbar
        hotbarIndexA = 38;
        hotbarIndexB = 47;
        for (int i = 0; i < 9; ++i) {
            this.addSlot(new Slot(playerInventory, i, 16 + 18 * i, 209));
        }
    }


    @Override
    public void onContentChanged(Inventory inventory) {
        // this function also being called on readContainer method, so here is checking for this

        if (!isReading) {
            this.context.run((world, pos) -> updateResult(this, world, this.player, this.input, this.result, this.wand));
            // saving container after updates
            saveContainer();
        }
    }

    protected static void updateResult(ScreenHandler handler, World world, PlayerEntity player, CraftingInventory input, CraftingResultInventory result, CraftingWandInventory wand) {
        if (!world.isClient) {
            ServerPlayerEntity serverPlayerEntity = (ServerPlayerEntity) player;
            ItemStack itemStack = ItemStack.EMPTY;

            if (world.getServer() == null) return;
            RecipeManager manager = world.getServer().getRecipeManager();

            DefaultedList<ItemStack> inv = DefaultedList.ofSize(10, ItemStack.EMPTY);
            for (int i = 0 ; i < 9 ; i++)
                inv.set(i, input.getStack(i));
            inv.set(9, wand.getStack(0));
            ImplementedInventory inventory = () -> inv;

            Optional<CraftingRecipe> optional = manager.getFirstMatch(CRAFTING, input, world);
            Optional<VisCraftingRecipe> optionalVis = manager.getFirstMatch(VIS_RECIPE_TYPE, inventory, world);

            PacketByteBuf buffer = PacketByteBufs.create();

            if (optional.isPresent()) {
                itemStack = optional.get().craft(input);
                buffer.writeVarInt(0);
            }
            else
            if (optionalVis.isPresent()){
                if (optionalVis.get().checkVis(wand.getStack(0))) itemStack = optionalVis.get().craft(inventory);

                buffer.writeVarInt(optionalVis.get().getRecipeVis().entrySet().size());
                for (Map.Entry<String, Float> entry: optionalVis.get().getRecipeVis().entrySet()) {
                    buffer.writeString(entry.getKey());
                    buffer.writeFloat(entry.getValue());
                }
            } else {
                buffer.writeVarInt(0);
            }

            result.setStack(0, itemStack);
            handler.setPreviousTrackedSlot(0, itemStack);
            serverPlayerEntity.networkHandler.sendPacket(new ScreenHandlerSlotUpdateS2CPacket(handler.syncId, handler.nextRevision(), 0, itemStack));
            serverPlayerEntity.networkHandler.sendPacket(new ScreenHandlerSlotUpdateS2CPacket(handler.syncId, handler.nextRevision(), 1, wand.getStack(0)));
            ServerPlayNetworking.send(serverPlayerEntity, PacketIDs.RECIPE_SYNC_CLIENT, buffer);
        }
    }

    @Override
    public void sendContentUpdates() {
        // on content-updates reading content in workbench
        readContainer();
        super.sendContentUpdates();
    }

    private void readContainer() {
        // calls onContentChange every setStack
        isReading = true;
        if (entity != null) {
            BlockState aState = player.getWorld().getBlockState(entity.getPos());

            for (int i = 0; i < 9; i++)
                input.setStack(i, entity.getStack(i));
            input.markDirty();
            wand.setStack(0, entity.getStack(10));
            wand.markDirty();

            BlockState bState = player.getWorld().getBlockState(entity.getPos());
            player.getWorld().updateListeners(entity.getPos(), aState, bState, Block.NOTIFY_ALL);
        }
        isReading = false;

        // calling for recipe checking
        onContentChanged(input);
    }

    private void saveContainer() {
        // doesn't call onContentChange
        if (entity != null) {
            BlockState aState = player.getWorld().getBlockState(entity.getPos());

            for (int i = 0; i < 9; i++) {
                entity.setStack(i, input.getStack(i));
            }
            entity.setStack(10, wand.getStack(0));
            entity.markDirty();

            BlockState bState = player.getWorld().getBlockState(entity.getPos());
            player.getWorld().updateListeners(entity.getPos(), aState, bState, Block.NOTIFY_ALL);
        }
    }



    public void populateRecipeFinder(RecipeMatcher finder) {
        this.input.provideRecipeInputs(finder);
    }

    public void clearCraftingSlots() {
        this.input.clear();
        this.result.clear();
    }

    public boolean matches(Recipe<? super CraftingInventory> recipe) {
        return recipe.matches(this.input, this.player.world);
    }


    public boolean canUse(PlayerEntity player) {
        // important - use correct block
        return canUse(this.context, player, Blocks.ARCANE_WORKBENCH);
    }

    @Override
    public ItemStack transferSlot(PlayerEntity player, int index) {
        ItemStack itemStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot.hasStack()) {
            ItemStack itemStack2 = slot.getStack();
            itemStack = itemStack2.copy();

            // if transferred from result slot
            if (index == outputIndex) {
                this.context.run((world, pos) -> itemStack2.getItem().onCraft(itemStack2, world, player));
                if (!this.insertItem(itemStack2, inventoryIndexA, hotbarIndexB, true)) {
                    return ItemStack.EMPTY;
                }
                slot.onQuickTransfer(itemStack2, itemStack);

            // if transferred from wand slot
            } else if (index == wandIndex) {
                if (!this.insertItem(itemStack2, inventoryIndexA, hotbarIndexB, true)) {
                    return ItemStack.EMPTY;
                }

            // if transferred from inventory slot
            } else if (index >= inventoryIndexA && index < hotbarIndexB) {

                // inserting VisCraft item from inventory to slot
                if (itemStack2.getItem() instanceof VisCraft) {
                    if (!this.insertItem(itemStack2, wandIndex, wandIndex + 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else {
                    if (!this.insertItem(itemStack2, craftingIndexA, craftingIndexB, false)) {
                        if (index < hotbarIndexA) {
                            if (!this.insertItem(itemStack2, hotbarIndexA, hotbarIndexB, false)) {
                                return ItemStack.EMPTY;
                            }
                        } else if (!this.insertItem(itemStack2, inventoryIndexA, inventoryIndexB, false)) {
                            return ItemStack.EMPTY;
                        }
                    }
                }

            // if transferred from crafting slot
            } else if (!this.insertItem(itemStack2, inventoryIndexA, hotbarIndexB, false)) {
                return ItemStack.EMPTY;
            }

            if (itemStack2.isEmpty()) {
                slot.setStack(ItemStack.EMPTY);
            } else {
                slot.markDirty();
            }

            if (itemStack2.getCount() == itemStack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTakeItem(player, itemStack2);
            if (index == 0) {
                player.dropItem(itemStack2, false);
            }
        }

        return itemStack;
    }

    @Override
    public boolean canInsertIntoSlot(ItemStack stack, Slot slot) {
        return slot.getIndex() != outputIndex && super.canInsertIntoSlot(stack, slot);
    }

    public int getCraftingResultSlotIndex() {
        return 0;
    }

    public int getCraftingWidth() {
        return this.input.getWidth();
    }

    public int getCraftingHeight() {
        return this.input.getHeight();
    }

    public int getCraftingSlotCount() {
        return 11;
    }

    public RecipeBookCategory getCategory() {
        return RecipeBookCategory.CRAFTING;
    }

    public boolean canInsertIntoSlot(int index) {
        return index != this.getCraftingResultSlotIndex();
    }
}
