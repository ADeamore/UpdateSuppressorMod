package net.adeamore.updatesuppressionhandler.item.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.lighting.LightEngine;
import net.minecraft.world.level.material.FluidState;

public class lightsuppressoritem extends Item {
    public lightsuppressoritem(Properties pProperties){
        super(pProperties);
    }

    @Override
    public InteractionResult useOn(UseOnContext pContext) {

        boolean success = false;

        BlockPos positionClicked = pContext.getClickedPos();
        Level lev = pContext.getLevel();
        BlockState bs = lev.getBlockState(positionClicked);

        success = setBlock(positionClicked, Blocks.DIRT.defaultBlockState(),16, 0, lev);
        if(success){
            System.out.println("set block to dirt");
            success = setBlockNoLight(positionClicked, bs,16, 0, lev);
            if(success){
                System.out.println("set block back to " + bs.getBlock().getName());
                BlockState bsa = lev.getBlockState(positionClicked.above());
                success = setBlock(positionClicked.above(), Blocks.DIRT.defaultBlockState(),16, 0, lev);
                if(success){
                    System.out.println("set above block to dirt");
                    success = setBlockNoLight(positionClicked.above(), bsa,16, 0, lev);
                    if(success) System.out.println("set above block back to " + bs.getBlock().getName());
                }
            }

        }

        //setBlock(positionClicked,Blocks.AIR.defaultBlockState(),16, 0, l);
        if(success){
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

    public boolean setBlockNoLight(BlockPos pPos, BlockState pState, int pFlags, int pRecursionLeft, Level l) {
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

            BlockState blockstate = setBlockStateNoLight(pPos, pState, (pFlags & 64) != 0,levelchunk);
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

    public BlockState setBlockStateNoLight(BlockPos pPos, BlockState pState, boolean pIsMoving, LevelChunk lc) {
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

                /*
                if (LightEngine.hasDifferentLightProperties(lc, pPos, blockstate, pState)) {
                    ProfilerFiller profilerfiller = lc.level.getProfiler();
                    profilerfiller.push("updateSkyLightSources");
                    lc.skyLightSources.update(lc, j, i, l);
                    profilerfiller.popPush("queueCheckLight");
                    lc.level.getChunkSource().getLightEngine().checkBlock(pPos);
                    profilerfiller.pop();
                }

                 */

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
