package com.abo47.oresandstuff.command;

import com.abo47.oresandstuff.OresAndStuffConfig;
import com.abo47.oresandstuff.item.ScannerItem;
import com.abo47.oresandstuff.world.NodeLocatorService;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceLocation;

public final class DevCommands {
    private DevCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("tpnearestnode").requires(source -> source.hasPermission(2)).then(Commands.argument("ore", ResourceLocationArgument.id()).executes(context -> teleport(context.getSource(), ResourceLocationArgument.getId(context, "ore")))).executes(context -> {
            ServerPlayer player = context.getSource().getPlayerOrException();
            return teleport(context.getSource(), ScannerItem.getSelectedType(player.getMainHandItem()));
        }));
    }

    private static int teleport(CommandSourceStack source, ResourceLocation type) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            var result = NodeLocatorService.findNearest(player.serverLevel(), player.blockPosition(), type, OresAndStuffConfig.scanner().radiusCap);
            if (result == null) {
                source.sendFailure(Component.translatable("No node found for %s", type));
                return 0;
            }
            player.teleportTo(result.pos().getX() + 0.5D, result.pos().getY() + 1.0D, result.pos().getZ() + 0.5D);
            source.sendSuccess(() -> Component.translatable("Teleported to %s node at %s", type, result.pos()), false);
            return 1;
        } catch (Exception exception) {
            return 0;
        }
    }
}
