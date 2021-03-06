package cd4017be.lib.block;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import javax.annotation.Nullable;

import cd4017be.lib.BlockGuiHandler;
import cd4017be.lib.Gui.DataContainer.IGuiData;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.GameRegistry;

public class AdvancedBlock extends BaseBlock {

	public final Class<? extends TileEntity> tileEntity;
	protected EnumBlockRenderType renderType;
	protected AxisAlignedBB[] boundingBox;
	/**1:NeighborAware, 2:BreakCleanup, 4:Interactive, 8:PlaceHarvest, 16:Redstone, 32:Collision, 64:hasGui, 65536:nonOpaque */
	protected int flags;

	/**
	 * 
	 * @param id registry name
	 * @param m material
	 * @param flags 2 = nonOpaque, 1 = noFullBlock, 4 = don't open GUI
	 * @param tile optional TileEntity to register with this block
	 */
	public AdvancedBlock(String id, Material m, SoundType sound, int flags, @Nullable Class<? extends TileEntity> tile) {
		super(id, m);
		this.setSoundType(sound);
		this.fullBlock = (flags & 1) == 0;
		this.flags = flags << 15;
		this.tileEntity = tile;
		if (tile != null) {
			if (INeighborAwareTile.class.isAssignableFrom(tile)) this.flags |= 1;
			if (ISelfAwareTile.class.isAssignableFrom(tile)) this.flags |= 2;
			if (IInteractiveTile.class.isAssignableFrom(tile)) this.flags |= 4;
			if (ITilePlaceHarvest.class.isAssignableFrom(tile)) this.flags |= 8;
			if (IRedstoneTile.class.isAssignableFrom(tile)) this.flags |= 16;
			if (ITileCollision.class.isAssignableFrom(tile)) this.flags |= 32;
			if ((flags & 4) == 0 && IGuiData.class.isAssignableFrom(tile)) this.flags |= 64;
			GameRegistry.registerTileEntity(tileEntity, getRegistryName().toString());
		}
		this.renderType = EnumBlockRenderType.MODEL;
		this.boundingBox = new AxisAlignedBB[]{FULL_BLOCK_AABB};
	}

	@Override
	public boolean hasTileEntity(IBlockState state) {
		return tileEntity != null;
	}

	@Override
	public TileEntity createTileEntity(World world, IBlockState state) {
		if (tileEntity != null) {
			try {
				try {
					return tileEntity.getConstructor(IBlockState.class).newInstance(state);
				} catch (NoSuchMethodException e) {
					return tileEntity.newInstance();
				}
			} catch (InstantiationException | IllegalAccessException | InvocationTargetException ex) {
				ex.printStackTrace();
			}
		}
		return null;
	}

	public interface INeighborAwareTile {
		/**
		 * when neighboring block state changes
		 * @param b event source Block-type
		 * @param src event source position
		 */
		void neighborBlockChange(Block b, BlockPos src);
		/**
		 * when neighboring tileEntity added/removed
		 * @param src event source position
		 */
		void neighborTileChange(BlockPos src);
	}
	
	@Override
	public void neighborChanged(IBlockState state, World world, BlockPos pos, Block b, BlockPos src) {
		if ((flags & 1) == 0) return;
		TileEntity te = world.getTileEntity(pos);
		if (te instanceof INeighborAwareTile) ((INeighborAwareTile)te).neighborBlockChange(b, src);
	}

	@Override
	public void onNeighborChange(IBlockAccess world, BlockPos pos, BlockPos npos) {
		if ((flags & 1) == 0) return;
		TileEntity te = world.getTileEntity(pos);
		if (te instanceof INeighborAwareTile) ((INeighborAwareTile)te).neighborTileChange(npos);
	}

	public interface ISelfAwareTile {
		/**
		 * when this block is about to be removed/changed
		 */
		void breakBlock();
	}

	@Override
	public void breakBlock(World world, BlockPos pos, IBlockState state) {
		if ((flags & 2) == 0) return;
		TileEntity te = world.getTileEntity(pos);
		if (te instanceof ISelfAwareTile) ((ISelfAwareTile)te).breakBlock();
		world.removeTileEntity(pos);
	}

	public interface IInteractiveTile {
		/**
		 * when right-clicked by player
		 * @param player event source player
		 * @param hand event source hand
		 * @param item held item
		 * @param s clicked block face
		 * @param X block relative x hit pos
		 * @param Y block relative y hit pos
		 * @param Z block relative z hit pos
		 * @return consume event
		 */
		boolean onActivated(EntityPlayer player, EnumHand hand, ItemStack item, EnumFacing s, float X, float Y, float Z);
		/**
		 * when left-clicked (hit) by player
		 * @param player event source player
		 */
		void onClicked(EntityPlayer player);
	}

	@Override
	public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing s, float X, float Y, float Z) {
		if ((flags & 4) != 0) {
			TileEntity te = world.getTileEntity(pos);
			if (te instanceof IInteractiveTile && ((IInteractiveTile)te).onActivated(player, hand, player.getHeldItem(hand), s, X, Y, Z)) return true;
		}
		if ((flags & 64) != 0) {
			BlockGuiHandler.openBlockGui(player, world, pos);
			return true;
		}
		return false;
	}

	@Override
	public void onBlockClicked(World world, BlockPos pos, EntityPlayer player) {
		if ((flags & 4) == 0) return;
		TileEntity te = world.getTileEntity(pos);
		if (te instanceof IInteractiveTile) ((IInteractiveTile)te).onClicked(player);
	}

	public interface ITilePlaceHarvest {
		/**
		 * when placed via item
		 * @param entity event source entity
		 * @param item held item
		 */
		void onPlaced(EntityLivingBase entity, ItemStack item);
		/**
		 * ask for theoretically dropped items
		 * @param state state of this block
		 * @param fortune fortune modifier
		 * @return drop list
		 */
		List<ItemStack> dropItem(IBlockState state, int fortune);
	}

	@Override
	public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase entity, ItemStack item) {
		if ((flags & 8) == 0) return;
		TileEntity te = world.getTileEntity(pos);
		if (te instanceof ITilePlaceHarvest) ((ITilePlaceHarvest)te).onPlaced(entity, item);
	}

	@Override
	public void harvestBlock(World world, EntityPlayer player, BlockPos pos, IBlockState state, TileEntity te, ItemStack stack) {
		if (te instanceof ITilePlaceHarvest) {
			//TODO
		} else super.harvestBlock(world, player, pos, state, te, stack);
	}

	@Override
	public void onBlockHarvested(World world, BlockPos pos, IBlockState state, EntityPlayer player) {
		//TODO
		if ((flags & 8) != 0) super.harvestBlock(world, player, pos, state, null, player.getHeldItemMainhand());
	}

	@Override
	public List<ItemStack> getDrops(IBlockAccess world, BlockPos pos, IBlockState state, int fortune) {
		if ((flags & 8) == 0) return super.getDrops(world, pos, state, fortune);
		TileEntity te = world.getTileEntity(pos);
		if (te instanceof ITilePlaceHarvest) return ((ITilePlaceHarvest)te).dropItem(state, fortune);
		else return super.getDrops(world, pos, state, fortune);
	}

	public interface IRedstoneTile {
		/**
		 * ask for emitted redstone signal
		 * @param side face of neighbor block to emit in
		 * @param strong whether asked for strong signal
		 * @return redstone signal
		 */
		int redstoneLevel(EnumFacing side, boolean strong);
		/**
		 * check for redstone connection
		 * @param side face of neighbor block to emit in
		 * @return whether to connect
		 */
		boolean connectRedstone(EnumFacing side);
	}

	@Override
	public boolean canProvidePower(IBlockState state) {
		return (flags & 16) != 0;
	}

	@Override
	public boolean shouldCheckWeakPower(IBlockState state, IBlockAccess world, BlockPos pos, EnumFacing side) {
		return (flags & 16) == 0;
	}

	@Override
	public int getWeakPower(IBlockState state, IBlockAccess world, BlockPos pos, EnumFacing s) {
		if ((flags & 16) == 0) return 0;
		TileEntity te = world.getTileEntity(pos);
		if (te instanceof IRedstoneTile) return ((IRedstoneTile)te).redstoneLevel(s.getOpposite(), false);
		else return 0;
	}

	@Override
	public int getStrongPower(IBlockState state, IBlockAccess world, BlockPos pos, EnumFacing s) {
		if ((flags & 16) == 0) return 0;
		TileEntity te = world.getTileEntity(pos);
		if (te instanceof IRedstoneTile) return ((IRedstoneTile)te).redstoneLevel(s.getOpposite(), true);
		else return 0;
	}

	@Override
	public boolean canConnectRedstone(IBlockState state, IBlockAccess world, BlockPos pos, EnumFacing side) {
		if ((flags & 16) == 0) return false;
		if (side == null) return true;
		TileEntity te = world.getTileEntity(pos);
		if (te instanceof IRedstoneTile) return ((IRedstoneTile)te).connectRedstone(side.getOpposite());
		return false;
	}

	public interface ITileCollision {
		/**
		 * when entity collides into this block (<b>doesn't work on fullCube blocks!</b>)
		 * @param entity collided entity
		 */
		void onEntityCollided(Entity entity);
	}

	@Override
	public void onEntityCollidedWithBlock(World world, BlockPos pos, IBlockState state, Entity entity) {
		if ((flags & 32) == 0) return;
		TileEntity te = world.getTileEntity(pos);
		if (te instanceof ITileCollision) ((ITileCollision)te).onEntityCollided(entity);
	}

	@Override
	public boolean isOpaqueCube(IBlockState state) {
		return (flags & 65536) == 0;
	}

	@Override
	public boolean isFullCube(IBlockState state) {
		return boundingBox[0] == FULL_BLOCK_AABB;
	}

	@Override
	public boolean isNormalCube(IBlockState state) {
		return boundingBox[0] == FULL_BLOCK_AABB;
	}

	@Override
	public boolean isNormalCube(IBlockState state, IBlockAccess world, BlockPos pos) {
		return getBoundingBox(state, world, pos) == FULL_BLOCK_AABB;
	}

	@Override
	public boolean isSideSolid(IBlockState state, IBlockAccess world, BlockPos pos, EnumFacing side) {
		AxisAlignedBB box = getBoundingBox(state, world, pos);
		if (box == FULL_BLOCK_AABB) return true;
		switch (side) {
		case DOWN: return box.minY == FULL_BLOCK_AABB.minY;
		case UP: return box.maxY == FULL_BLOCK_AABB.maxY;
		case NORTH: return box.minZ == FULL_BLOCK_AABB.minZ;
		case SOUTH: return box.maxZ == FULL_BLOCK_AABB.maxZ;
		case WEST: return box.minX == FULL_BLOCK_AABB.minX;
		case EAST: return box.maxX == FULL_BLOCK_AABB.maxX;
		default: return true;
		}
	}

	@Override
	public boolean canBeReplacedByLeaves(IBlockState state, IBlockAccess world, BlockPos pos) {
		return false;
	}

	public void setRenderType(EnumBlockRenderType t) {
		renderType = t;
	}

	@Override
	public EnumBlockRenderType getRenderType(IBlockState state) {
		return renderType;
	}

	private BlockRenderLayer blockLayer = BlockRenderLayer.SOLID;

	public void setBlockLayer(BlockRenderLayer layer) {
		this.blockLayer = layer;
	}

	public BlockRenderLayer getBlockLayer() {
		return this.blockLayer;
	}

	public AdvancedBlock setBlockBounds(AxisAlignedBB box) {
		boundingBox[0] = box;
		return this;
	}

	@Override
	public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
		return boundingBox.length == 1 ? boundingBox[0] : boundingBox[getMetaFromState(state)];
	}

}
