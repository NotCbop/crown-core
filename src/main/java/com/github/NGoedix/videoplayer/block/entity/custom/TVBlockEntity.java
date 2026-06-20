package com.github.NGoedix.videoplayer.block.entity.custom;

import com.github.NGoedix.videoplayer.block.custom.TVBlock;
import com.github.NGoedix.videoplayer.block.entity.ModBlockEntities;
import com.github.NGoedix.videoplayer.network.PacketHandler;
import com.github.NGoedix.videoplayer.util.cache.TextureCache;
import com.github.NGoedix.videoplayer.util.displayers.IDisplay;
import com.github.NGoedix.videoplayer.util.math.geo.AlignedBox;
import com.github.NGoedix.videoplayer.util.math.geo.Axis;
import com.github.NGoedix.videoplayer.util.math.geo.Facing;
import com.github.NGoedix.videoplayer.util.math.geo.Vec3d;
import net.fabricmc.api.Environment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.Level;

import java.util.UUID;

import static net.fabricmc.api.EnvType.CLIENT;

public class TVBlockEntity extends VideoPlayerBlockEntity {

    private UUID playerUsing;

    public TVBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TV_BLOCK_ENTITY, pos, state, false);
    }

    public void tryOpen(Level level, BlockPos blockPos, Player player) {
        if (playerUsing == null) {
            setBeingUsed(player.getUUID());
            openVideoManagerGUI(blockPos, player);
            return;
        }

        for (Player p : level.players())
            if (p.getUUID() == playerUsing)
                return;

        openVideoManagerGUI(blockPos, player);
    }

    public void openVideoManagerGUI(BlockPos blockPos, Player player) {
        setBeingUsed(player.getUUID());
        PacketHandler.sendS2COpenVideoManagerScreen((ServerPlayer) player, blockPos, getUrl(), getVolume(), getTick(), isPlaying());
    }

    public void setBeingUsed(UUID player) {
        this.playerUsing = player;
        setChanged();
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.store("beingUsed", UUIDUtil.CODEC, playerUsing == null ? new UUID(0, 0) : playerUsing);
    }

    @Override
    protected void loadFromNBT(ValueInput input) {
        playerUsing = input.read("beingUsed", UUIDUtil.CODEC).orElse(new UUID(0, 0));
    }

    public void notifyPlayer() {
        PacketHandler.sendS2CFrameVideoMessage(getLevel().getChunkAt(getBlockPos()), getUrl(), getBlockPos(), isPlaying(), getTick());
    }

    public float getSizeX() {
        return 1.4F;
    }

    public float getSizeY() {
        return 0.77F;
    }

    public AlignedBox getBox() {
        Direction direction = getBlockState().getValue(TVBlock.FACING);
        Facing facing = Facing.get(direction);
        AlignedBox box = TVBlock.box(direction);

        Axis one = facing.one();
        Axis two = facing.two();

        if (facing.axis != Axis.Z) {
            one = facing.two();
            two = facing.one();
        }

        box.setMin(one, 0);
        box.setMax(one, getSizeX());

        box.setMin(two, 0);
        box.setMax(two, getSizeY());
        return box;
    }

    @Override
    public void tick() {
        super.tick();
        level.setBlock(getBlockPos(), getBlockState().setValue(TVBlock.LIT, isPlaying()), 3);
    }
}
