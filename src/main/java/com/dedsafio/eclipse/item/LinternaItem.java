package com.dedsafio.eclipse.item;

import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class LinternaItem extends Item {
    /** Durabilidad = segundos de luz (1200 = 20 minutos). */
    public static final int MAX_DURABILITY = 1200;
    /** Cooldown (ticks) tras apagarla antes de poder encenderla otra vez: 2 segundos. */
    public static final int COOLDOWN_TICKS = 40;
    private static final String TAG_ON = "Encendida";

    public LinternaItem(Properties properties) {
        super(properties);
    }

    public static boolean isOn(ItemStack stack) {
        return stack.getItem() instanceof LinternaItem && stack.hasTag() && stack.getTag().getBoolean(TAG_ON);
    }

    public static void setOn(ItemStack stack, boolean on) {
        stack.getOrCreateTag().putBoolean(TAG_ON, on);
    }

    /** True si el jugador sostiene una linterna encendida (mano principal o secundaria). */
    public static boolean isHolding(Player player) {
        return isOn(player.getMainHandItem()) || isOn(player.getOffhandItem());
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            if (isOn(stack)) {
                setOn(stack, false);
                player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
                level.playSound(null, player.blockPosition(), SoundEvents.LEVER_CLICK, SoundSource.PLAYERS, 0.6F, 0.8F);
            } else {
                setOn(stack, true);
                level.playSound(null, player.blockPosition(), SoundEvents.LEVER_CLICK, SoundSource.PLAYERS, 0.6F, 1.3F);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (level.isClientSide || !isOn(stack) || !(entity instanceof Player player)) return;
        final boolean inMain = player.getMainHandItem() == stack;
        final boolean inOff = player.getOffhandItem() == stack;
        if (!inMain && !inOff) {
            setOn(stack, false); // se apaga sola si no la llevas en la mano
            return;
        }
        if (level.getGameTime() % 20 == 0) { // 1 de durabilidad por segundo encendida
            stack.hurtAndBreak(1, player,
                    p -> p.broadcastBreakEvent(inMain ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND));
        }
    }

    @Override
    public boolean onDroppedByPlayer(ItemStack item, Player player) {
        setOn(item, false);
        return true;
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged || oldStack.getItem() != newStack.getItem();
    }

    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack repair) {
        return repair.is(Items.REDSTONE);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.dedsafio_eclipse.linterna"));
        tooltip.add(Component.translatable("tooltip.dedsafio_eclipse.linterna2"));
    }
}
