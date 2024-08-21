package net.adeamore.updatesuppressionhandler.item.custom;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.OutgoingChatMessage;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.server.commands.SetBlockCommand;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Interaction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.lighting.LightEngine;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import net.minecraft.world.level.Level;
import net.minecraft.client.multiplayer.ClientLevel;

import java.util.Map;
import java.util.Objects;

import static net.minecraft.sounds.SoundSource.MASTER;


public class updatesuppressoritem extends Item{

    public updatesuppressoritem(Properties pProperties){
        super(pProperties);
    }

    @Override
    public InteractionResult useOn(UseOnContext pContext) {
        boolean success = false;
        BlockPos positionClicked = pContext.getClickedPos();
        Level lev = pContext.getLevel();
        BlockEntity b = lev.getBlockEntity(positionClicked);
        success = setBlock(positionClicked,Blocks.AIR.defaultBlockState(),16, 0, lev);
        //setBlock(positionClicked,Blocks.AIR.defaultBlockState(),16, 0, l);



        if(success) {
            int x = pContext.getClickedPos().getX();
            int y = pContext.getClickedPos().getY();
            int z = pContext.getClickedPos().getZ();
            double dx = x;
            double dy = y;
            double dz = z;
            Player pPlayer = pContext.getPlayer();
            BlockPos pClickedPos = pContext.getClickedPos();
            Level pLev = pContext.getLevel();
            System.out.println("Block update suppressed at: " + String.valueOf(x) + ", " + String.valueOf(y) + ", " + String.valueOf(z) + ". By " + pContext.getPlayer().getName() );
            pPlayer.playSound(SoundEvents.AMETHYST_BLOCK_PLACE,1f,1f);
            return InteractionResult.SUCCESS;
        }else{
            return InteractionResult.FAIL;
        }
    }

    public boolean setBlock(BlockPos pPos, BlockState pState, int pFlags, int pRecursionLeft, Level l) {
        if (l.isOutsideBuildHeight(pPos)) {
            return false;
        } else {
            LevelChunk levelchunk = l.getChunkAt(pPos);
            Block block = pState.getBlock();

            pPos = pPos.immutable(); // Forge - prevent mutable BlockPos leaks
            net.minecraftforge.common.util.BlockSnapshot blockSnapshot = null;
            if (l.captureBlockSnapshots && !l.isClientSide) {
                blockSnapshot = net.minecraftforge.common.util.BlockSnapshot.create(l.dimension, l, pPos, pFlags);
                l.capturedBlockSnapshots.add(blockSnapshot);
            }

            BlockState old = l.getBlockState(pPos);
            int oldLight = old.getLightEmission(l, pPos);
            int oldOpacity = old.getLightBlock(l, pPos);

            BlockState blockstate = setBlockState(pPos, pState, (pFlags & 64) != 0,levelchunk);
            if (blockstate == null) {
                if (blockSnapshot != null) l.capturedBlockSnapshots.remove(blockSnapshot);
                return false;
            } else {
                BlockState blockstate1 = l.getBlockState(pPos);

                if (blockSnapshot == null) { // Don't notify clients or update physics while capturing blockstates
                    //l.markAndNotifyBlock(pPos, levelchunk, blockstate, pState, pFlags, pRecursionLeft);
                }

                return true;
            }
        }
    }

    public BlockState setBlockState(BlockPos pPos, BlockState pState, boolean pIsMoving, LevelChunk lc) {
        int i = pPos.getY();
        LevelChunkSection levelchunksection = lc.getSection(lc.getSectionIndex(i));
        boolean flag = levelchunksection.hasOnlyAir();
        if (flag && pState.isAir()) {
            return null;
        } else {
            int j = pPos.getX() & 15;
            int k = i & 15;
            int l = pPos.getZ() & 15;
            BlockState blockstate = setBlockStatePreLC(j, k, l, pState,levelchunksection);
            if (blockstate == pState) {
                return null;
            } else {
                Block block = pState.getBlock();
                ((Heightmap)lc.heightmaps.get(Heightmap.Types.MOTION_BLOCKING)).update(j, i, l, pState);
                ((Heightmap)lc.heightmaps.get(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES)).update(j, i, l, pState);
                ((Heightmap)lc.heightmaps.get(Heightmap.Types.OCEAN_FLOOR)).update(j, i, l, pState);
                ((Heightmap)lc.heightmaps.get(Heightmap.Types.WORLD_SURFACE)).update(j, i, l, pState);
                boolean flag1 = levelchunksection.hasOnlyAir();
                if (flag != flag1) {
                    lc.level.getChunkSource().getLightEngine().updateSectionStatus(pPos, flag1);
                }

                if (LightEngine.hasDifferentLightProperties(lc, pPos, blockstate, pState)) {
                    ProfilerFiller profilerfiller = lc.level.getProfiler();
                    profilerfiller.push("updateSkyLightSources");
                    lc.skyLightSources.update(lc, j, i, l);
                    profilerfiller.popPush("queueCheckLight");
                    lc.level.getChunkSource().getLightEngine().checkBlock(pPos);
                    profilerfiller.pop();
                }

                boolean flag2 = blockstate.hasBlockEntity();
                if ((!blockstate.is(block) || !pState.hasBlockEntity()) && flag2) {
                    //lc.removeBlockEntity(pPos);
                }

                if (!levelchunksection.getBlockState(j, k, l).is(block)) {
                    return null;
                } else {
                    //pState.onPlace(lc.level, pPos, blockstate, pIsMoving);

                    if (pState.hasBlockEntity()) {
                        /*
                        BlockEntity blockentity = lc.getBlockEntity(pPos, LevelChunk.EntityCreationType.CHECK);
                        if (blockentity == null) {
                            blockentity = ((EntityBlock)block).newBlockEntity(pPos, pState);
                            if (blockentity != null) {
                                lc.addAndRegisterBlockEntity(blockentity);
                            }
                        } else {
                            blockentity.setBlockState(pState);
                            lc.updateBlockEntityTicker(blockentity);
                        }
                         */
                    }

                    lc.unsaved = true;
                    return blockstate;
                }
            }
        }
    }

    public void onRemove(Level pLevel, BlockPos pPos, BlockState pNewState, boolean pMovedByPiston, BlockState bs) {
        bs.getBlock().onRemove(bs.asState(), pLevel, pPos, pNewState, pMovedByPiston);
    }

    public BlockState setBlockStatePreLC(int pX, int pY, int pZ, BlockState pState, LevelChunkSection lcs) {
        return setBlockStateLC(pX, pY, pZ, pState, true,lcs);
    }

    public BlockState setBlockStateLC(int pX, int pY, int pZ, BlockState pState, boolean pUseLocks, LevelChunkSection lc) {
        BlockState $$6;
        if (pUseLocks) {
            $$6 = (BlockState)lc.states.getAndSet(pX, pY, pZ, pState);
        } else {
            $$6 = (BlockState)lc.states.getAndSetUnchecked(pX, pY, pZ, pState);
        }

        FluidState $$7 = $$6.getFluidState();
        FluidState $$8 = pState.getFluidState();
        /*if (!$$6.isAir()) {
            --lc.nonEmptyBlockCount;
            if ($$6.isRandomlyTicking()) {
                --lc.tickingBlockCount;
            }
        }

        if (!$$7.isEmpty()) {
            --lc.tickingFluidCount;
        }

        if (!pState.isAir()) {
            ++lc.nonEmptyBlockCount;
            if (pState.isRandomlyTicking()) {
                ++lc.tickingBlockCount;
            }
        }

        if (!$$8.isEmpty()) {
            ++lc.tickingFluidCount;
        }
        */

        return $$6;
    }

}
