/*******************************************************************************
 * HellFirePvP / Astral Sorcery 2019
 *
 * All rights reserved.
 * The source code is available on github: https://github.com/HellFirePvP/AstralSorcery
 * For further details, see the License file there.
 ******************************************************************************/

package hellfirepvp.astralsorcery.common.block.tile;

import hellfirepvp.astralsorcery.common.block.base.BlockStarlightNetwork;
import hellfirepvp.astralsorcery.common.block.base.CustomItemBlock;
import hellfirepvp.astralsorcery.common.constellation.IMinorConstellation;
import hellfirepvp.astralsorcery.common.constellation.IWeakConstellation;
import hellfirepvp.astralsorcery.common.data.research.GatedKnowledge;
import hellfirepvp.astralsorcery.common.data.research.PlayerProgress;
import hellfirepvp.astralsorcery.common.data.research.ProgressionTier;
import hellfirepvp.astralsorcery.common.data.research.ResearchHelper;
import hellfirepvp.astralsorcery.common.item.block.ItemBlockCollectorCrystal;
import hellfirepvp.astralsorcery.common.network.PacketChannel;
import hellfirepvp.astralsorcery.common.network.packet.server.PktPlayEffect;
import hellfirepvp.astralsorcery.common.tile.TileCollectorCrystal;
import hellfirepvp.astralsorcery.common.util.MiscUtils;
import hellfirepvp.astralsorcery.common.util.crystal.CrystalProperties;
import hellfirepvp.astralsorcery.common.util.crystal.CrystalPropertyItem;
import hellfirepvp.astralsorcery.common.util.item.ItemUtils;
import hellfirepvp.observerlib.api.block.BlockStructureObserver;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.ToolType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * This class is part of the Astral Sorcery Mod
 * The complete source code for this mod can be found on github.
 * Class: BlockCollectorCrystal
 * Created by HellFirePvP
 * Date: 10.08.2019 / 20:32
 */
public abstract class BlockCollectorCrystal extends BlockStarlightNetwork implements BlockStructureObserver, CustomItemBlock {

    private static final VoxelShape SHAPE = Block.makeCuboidShape(4.5, 0, 4.5, 11.5, 16, 11.5);
    private static final float PLAYER_HARVEST_HARDNESS = 4F;

    public BlockCollectorCrystal(CollectorCrystalType type) {
        super(Properties.create(Material.GLASS, type.getMaterialColor())
                .hardnessAndResistance(-1.0F, 3600000.0F)
                .harvestTool(ToolType.PICKAXE)
                .harvestLevel(2)
                .sound(SoundType.GLASS)
                .lightValue(11));
    }

    @Override
    public abstract Class<? extends ItemBlockCollectorCrystal> getItemBlockClass();

    @Override
    @OnlyIn(Dist.CLIENT)
    public void addInformation(ItemStack stack, @Nullable IBlockReader world, List<ITextComponent> toolTip, ITooltipFlag flag) {
        super.addInformation(stack, world, toolTip, flag);

        CrystalProperties prop = CrystalProperties.getCrystalProperties(stack);
        int maxSize = ((CrystalPropertyItem) stack.getItem()).getMaxPropertySize(stack);
        Optional<Boolean> missing = CrystalProperties.addPropertyTooltip(prop, toolTip, maxSize);

        if (missing.isPresent()) {
            PlayerProgress clientProgress = ResearchHelper.getClientProgress();
            ProgressionTier tier = clientProgress.getTierReached();
            IWeakConstellation c = ItemBlockCollectorCrystal.getConstellation(stack);
            if (c != null) {
                if (GatedKnowledge.COLLECTOR_TYPE.canSee(tier) && clientProgress.hasConstellationDiscovered(c)) {
                    toolTip.add(new TranslationTextComponent("crystal.collect.type",
                            new TranslationTextComponent(c.getUnlocalizedName()).setStyle(new Style().setColor(TextFormatting.BLUE)))
                        .setStyle(new Style().setColor(TextFormatting.GRAY)));
                    IMinorConstellation tr = ItemBlockCollectorCrystal.getTrait(stack);
                    if (tr != null) {
                        if (GatedKnowledge.CRYSTAL_TRAIT.canSee(tier) && clientProgress.hasConstellationDiscovered(tr)) {
                            toolTip.add(new TranslationTextComponent("crystal.trait",
                                    new TranslationTextComponent(tr.getUnlocalizedName()).setStyle(new Style().setColor(TextFormatting.BLUE)))
                                    .setStyle(new Style().setColor(TextFormatting.GRAY)));
                        } else {
                            toolTip.add(new TranslationTextComponent("progress.missing.knowledge").setStyle(new Style().setColor(TextFormatting.GRAY)));
                        }
                    }
                } else if (!missing.get()) {
                    toolTip.add(new TranslationTextComponent("progress.missing.knowledge").setStyle(new Style().setColor(TextFormatting.GRAY)));
                }
            }
        }
    }

    @Override
    public VoxelShape getShape(BlockState p_220053_1_, IBlockReader p_220053_2_, BlockPos p_220053_3_, ISelectionContext p_220053_4_) {
        return SHAPE;
    }

    @Override
    public float getBlockHardness(BlockState state, IBlockReader world, BlockPos pos) {
        TileCollectorCrystal crystal = MiscUtils.getTileAt(world, pos, TileCollectorCrystal.class, false);
        if (crystal != null && crystal.isPlayerMade()) {
            return PLAYER_HARVEST_HARDNESS;
        }
        return super.getBlockHardness(state, world, pos);
    }

    @Nullable
    @Override
    public TileEntity createNewTileEntity(IBlockReader worldIn) {
        return new TileCollectorCrystal();
    }

    @Override
    public void onBlockPlacedBy(World world, BlockPos pos, BlockState state, @Nullable LivingEntity entity, ItemStack stack) {
        TileCollectorCrystal tcc = MiscUtils.getTileAt(world, pos, TileCollectorCrystal.class, true);
        Item i = stack.getItem();
        if (tcc != null && i instanceof ItemBlockCollectorCrystal) {
            ItemBlockCollectorCrystal ibcc = (ItemBlockCollectorCrystal) i;
            UUID playerUUID = null;
            if (entity instanceof PlayerEntity) {
                playerUUID = entity.getUniqueID();
            }

            tcc.updateData(
                    ItemBlockCollectorCrystal.getConstellation(stack),
                    ItemBlockCollectorCrystal.getTrait(stack),
                    ibcc.getProperties(stack),
                    playerUUID,
                    ibcc.getCollectorType());
        }

        super.onBlockPlacedBy(world, pos, state, entity, stack);
    }

    @Override
    public void onBlockHarvested(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        super.onBlockHarvested(world, pos, state, player);

        TileCollectorCrystal tcc = MiscUtils.getTileAt(world, pos, TileCollectorCrystal.class, true);
        if (tcc != null && !world.isRemote()) {
            //TODO collector burst effect
            //PktPlayEffect pkt = new PktPlayEffect(null);
            //PacketChannel.CHANNEL.sendToAllAround(pkt, PacketChannel.pointFromPos(world, pos, 32));

            if (tcc.isPlayerMade() && !player.isCreative()) {
                ItemStack stack = new ItemStack(this);
                Item i = stack.getItem();
                if (i instanceof ItemBlockCollectorCrystal) {
                    ItemBlockCollectorCrystal ibcc = (ItemBlockCollectorCrystal) i;

                    if (tcc.getCrystalProperties() != null) {
                        ibcc.applyCrystalProperties(stack, tcc.getCrystalProperties());
                    }
                    if (tcc.getConstellationType() != null) {
                        ItemBlockCollectorCrystal.setConstellation(stack, tcc.getConstellationType());
                    }
                    if (tcc.getConstellationTrait() != null) {
                        ItemBlockCollectorCrystal.setTraitConstellation(stack, tcc.getConstellationTrait());
                    }
                    Block.spawnAsEntity(world, pos, stack);
                }
            }
        }
    }
}
