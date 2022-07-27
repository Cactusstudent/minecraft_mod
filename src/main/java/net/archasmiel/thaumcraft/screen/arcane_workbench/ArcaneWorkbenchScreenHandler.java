package net.archasmiel.thaumcraft.screen.arcane_workbench;

import net.archasmiel.thaumcraft.block.Blocks;
import net.archasmiel.thaumcraft.blockentity.ArcaneWorkbenchBlockEntity;
import net.archasmiel.thaumcraft.blockentity.inventory.ImplementedInventory;
import net.archasmiel.thaumcraft.recipe.Recipes;
import net.archasmiel.thaumcraft.recipe.VisShapedRecipe;
import net.archasmiel.thaumcraft.recipe.VisShapelessRecipe;
import net.archasmiel.thaumcraft.screen.ScreenHandlers;
import net.archasmiel.thaumcraft.screen.arcane_workbench.inventory.CraftingWandInventory;
import net.archasmiel.thaumcraft.screen.arcane_workbench.slot.ResultSlot;
import net.archasmiel.thaumcraft.screen.arcane_workbench.slot.WandSlot;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.inventory.CraftingResultInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;
import net.minecraft.recipe.*;
import net.minecraft.recipe.book.RecipeBookCategory;
import net.minecraft.screen.AbstractRecipeScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.world.World;

import java.util.Optional;

public class ArcaneWorkbenchScreenHandler extends AbstractRecipeScreenHandler<CraftingInventory> {

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
        this.addSlot(new ResultSlot(playerInventory.player, this.input, this.wand, this.result, 0, 160, 64));

        // wand slot
        this.addSlot(new WandSlot(this.wand, 0, 160, 25));

        /// craft slots
        for (int i = 0 ; i < 3 ; i++)
            for (int j = 0 ; j < 3 ; j++)
                this.addSlot(new Slot(this.input, i*3 + j, 40 + j*24, 40 + i*24));





        /// inventory
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                this.addSlot(new Slot(playerInventory, 9 + i*9 + j, 16 + 18*j, 151 + 18*i));
            }
        }



        /// hotbar
        for (int i = 0; i < 9; ++i) {
            this.addSlot(new Slot(playerInventory, i, 16 + 18*i, 209));
        }

    }

    /* LOADING ITEMS FROM BLOCK ENTITY */
    public void onContentChanged(Inventory inventory) {
        if (!isReading) {
            this.context.run((world, pos) -> updateResult(this, world, this.player, this.input, this.result, this.wand));
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

            Optional<CraftingRecipe> optional = manager.getFirstMatch(RecipeType.CRAFTING, input, world);
            if (optional.isPresent()) {
                itemStack = optional.get().craft(input);
            }

            Optional<VisShapedRecipe> optionalVisShaped = manager.getFirstMatch(Recipes.VIS_SHAPED_RECIPE_TYPE, inventory, world);
            if (optionalVisShaped.isPresent()){
                itemStack = optionalVisShaped.get().craft(inventory);
            }

            Optional<VisShapelessRecipe> optionalVisShapeless = manager.getFirstMatch(Recipes.VIS_SHAPELESS_RECIPE_TYPE, inventory, world);
            if (optionalVisShapeless.isPresent()){
                itemStack = optionalVisShapeless.get().craft(inventory);
            }

            result.setStack(0, itemStack);
            handler.setPreviousTrackedSlot(0, itemStack);
            serverPlayerEntity.networkHandler.sendPacket(new ScreenHandlerSlotUpdateS2CPacket(handler.syncId, handler.nextRevision(), 0, itemStack));
        }
    }

    @Override
    public void sendContentUpdates() {
        readContainer();
        super.sendContentUpdates();
    }

    private void readContainer() {
        isReading = true;
        if (entity != null) {
            BlockState aState = player.getWorld().getBlockState(entity.getPos());

            for (int i = 0; i < 9; i++)
                input.setStack(i, entity.getStack(i));
            wand.setStack(0, entity.getStack(10));
            input.markDirty();
            wand.markDirty();

            BlockState bState = player.getWorld().getBlockState(entity.getPos());
            player.getWorld().updateListeners(entity.getPos(), aState, bState, Block.NOTIFY_ALL);

        }
        isReading = false;
        onContentChanged(input);
    }

    private void saveContainer() {
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

    public void close(PlayerEntity player) {
        super.close(player);
    }

    public boolean canUse(PlayerEntity player) {
        return canUse(this.context, player, Blocks.ARCANE_WORKBENCH);
    }

    public ItemStack transferSlot(PlayerEntity player, int index) {
        ItemStack itemStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot.hasStack()) {
            ItemStack itemStack2 = slot.getStack();
            itemStack = itemStack2.copy();

            // if transferred from result slot
            if (index == 0) {
                this.context.run((world, pos) -> itemStack2.getItem().onCraft(itemStack2, world, player));
                if (!this.insertItem(itemStack2, 10, 46, true)) {
                    return ItemStack.EMPTY;
                }
                slot.onQuickTransfer(itemStack2, itemStack);
            // if transferred from inventory
            } else if (index >= 11 && index < 47) {
                if (!this.insertItem(itemStack2, 2, 11, false)) {
                    if (index < 38) {
                        if (!this.insertItem(itemStack2, 38, 47, false)) {
                            return ItemStack.EMPTY;
                        }
                    } else if (!this.insertItem(itemStack2, 11, 38, false)) {
                        return ItemStack.EMPTY;
                    }
                }
            // if transferred from crafting
            } else if (!this.insertItem(itemStack2, 11, 47, false)) {
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

    public boolean canInsertIntoSlot(ItemStack stack, Slot slot) {
        return slot.inventory != this.result && super.canInsertIntoSlot(stack, slot);
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
